package com.grill_bros.backend.service.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.dto.paymentdto.*;
import com.grill_bros.backend.exceptions.DuplicateResourceException;
import com.grill_bros.backend.exceptions.InvalidStateTransitionException;
import com.grill_bros.backend.exceptions.PaystackException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.model.PaymentEvent;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentEventType;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.PaymentEventRepository;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import com.grill_bros.backend.service.eventlisteners.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaystackClient paystackClient;
    private final PaymentEventPublisher eventPublisher;
    private final CacheService cache;
    private final ObjectMapper objectMapper;
    private final PaymentEventRepository paymentEventRepository;

    @Transactional
    public PaymentInitiatedResponse initiatePayment(InitiatePaymentRequest req) {

        var order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Cannot initiate payment for order " + order.getOrderNumber() +
                            " — current status: " + order.getStatus());
        }

        var existing = paymentRepository.findByOrder_Id(req.getOrderId());
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (p.getStatus() == PaymentStatus.SUCCESSFUL) {
                throw new DuplicateResourceException(
                        "Payment already completed for order: " + order.getOrderNumber());
            }
            if (p.getStatus() == PaymentStatus.PENDING
                    || p.getStatus() == PaymentStatus.INITIATED) {
                log.info("Returning existing pending payment for order={}",
                        order.getOrderNumber());
                return PaymentInitiatedResponse.from(p);
            }
            // FAILED / CANCELLED / TIMEOUT → allow retry, fall through
        }

        String email = resolveEmail(req, order);

        if (!StringUtils.hasText(email)) {
            throw new InvalidStateTransitionException(
                    "Customer email is required to initiate Paystack payment. " +
                            "Please provide an email address.");
        }

        String reference = generateReference(order.getOrderNumber());

        Payment payment = Payment.create(
                order, reference,
                email, order.getCustomerPhone(),
                order.getTotalAmount());
        paymentRepository.save(payment);

        log.info("PAYMENT_INITIATED paymentId={} status={}", payment.getId(), PaymentStatus.INITIATED);

        try {
            PaystackApiResponse.InitializeData init =
                    paystackClient.initializeTransaction(
                            reference,
                            email,
                            order.getTotalAmount());

            payment.setAuthorizationUrl(init.getAuthorizationUrl());
            payment.setAccessCode(init.getAccessCode());
            payment.transitionTo(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            // Cache status for quick poll reads
//            cachePaymentStatus(payment);

            logEvent(payment, PaymentEventType.PAYMENT_INITIATED,
                    PaymentStatus.INITIATED, PaymentStatus.PENDING,
                    toJson(init));

            log.info("Payment initiated: reference={} order={} amount={}GHS",
                    reference, order.getOrderNumber(), order.getTotalAmount());

            return PaymentInitiatedResponse.from(payment);

        } catch (PaystackException e) {
            payment.markFailed("Paystack initialisation error: " + e.getMessage());
            paymentRepository.save(payment);
            log.info( "PAYSTACK_INIT_FAILED", payment,
                    PaymentStatus.PENDING, PaymentStatus.FAILED, e.getMessage());
            log.error("Paystack init failed for order={}: {}", order.getOrderNumber(), e.getMessage());
            throw new InvalidStateTransitionException(
                    "Payment gateway error. Please try again.");
        }
    }

    @Transactional
    public PaymentStatusResponse verifyAndUpdate(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment"));

        if (payment.getStatus().isTerminal()) {
            log.debug("Payment already in terminal state={} for reference={}",
                    payment.getStatus(), reference);
            return PaymentStatusResponse.from(payment);
        }

        String lockKey = RedisKeys.orderLock(payment.getOrder().getId());
        boolean locked = cache.acquireLock(lockKey, RedisKeys.TTL_ORDER_LOCK_SECONDS);
        if (!locked) {
            log.warn("Could not acquire lock for orderId={}, another process is handling it",
                    payment.getOrder().getId());
            return PaymentStatusResponse.from(payment);
        }

        try {
            // ── Call Paystack to get authoritative status ─────────────────
            PaystackApiResponse.VerifyData verifyData =
                    paystackClient.verifyTransaction(reference);

            PaymentStatus previous = payment.getStatus();

            switch (verifyData.getStatus()) {
                case "success" -> handleSuccess(payment, verifyData);
                case "failed"  -> handleFailed(payment, verifyData);
                case "abandoned" -> {
                    payment.markFailed("Payment abandoned by customer");
                    paymentRepository.save(payment);
                    log.info("PAYMENT_ABANDONED payment={} previous={}", payment, previous);
                }
                default -> log.info("Payment still pending: reference={} paystackStatus={}",
                        reference, verifyData.getStatus());
            }

            return PaymentStatusResponse.from(payment);

        } finally {
            cache.releaseLock(lockKey);
        }
    }

    private void handleSuccess(Payment payment,
                               PaystackApiResponse.VerifyData verifyData) {
        if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
            return; // already processed
        }

        PaymentStatus previous = payment.getStatus();

        payment.markSuccessful(
                verifyData.getId() != null ? verifyData.getId().toString() : null,
                verifyData.getGatewayResponse(),
                verifyData.getChannel(),
                verifyData.getPaidAt() != null ? verifyData.getPaidAt() : Instant.now());
        paymentRepository.save(payment);

        logEvent(payment, PaymentEventType.PAYMENT_SUCCESS,
                previous, PaymentStatus.SUCCESSFUL, toJson(verifyData));

        // Confirm the order
        var order = orderRepository.findById(payment.getOrder().getId()).orElseThrow();
        try {
            order.transitionTo(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        } catch (IllegalStateException e) {
            log.warn("Order already past PENDING for orderId={}: {}",
                    payment.getOrder().getId(), e.getMessage());
        }

        eventPublisher.paymentSucceeded(payment);

        log.info("Payment SUCCESSFUL: reference={} order={} amount={}GHS channel={}",
                payment.getReference(), order.getOrderNumber(),
                payment.getAmount(), payment.getChannel());
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(
            Pageable pageable
    ) {
        return paymentRepository.findAll(pageable)
                .map(PaymentResponse::from);
    }

    private void handleFailed(Payment payment,
                              PaystackApiResponse.VerifyData verifyData) {
        PaymentStatus previous = payment.getStatus();
        payment.markFailed(verifyData.getGatewayResponse());
        paymentRepository.save(payment);

        logEvent(payment, PaymentEventType.PAYMENT_FAILED,
                previous, PaymentStatus.FAILED, toJson(verifyData));

        var order = orderRepository.findById(payment.getOrder().getId()).orElseThrow();
        try {
            order.transitionTo(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        } catch (IllegalStateException e) {
            log.warn("Order already past PENDING for orderId={}: {}",
                    payment.getOrder().getId(), e.getMessage());
        }

        eventPublisher.paymentFailed(payment);

        log.info("Payment FAILED: reference={} reason={}",
                payment.getReference(), verifyData.getGatewayResponse());
    }

    private String resolveEmail(InitiatePaymentRequest req, Order order) {
        return StringUtils.hasText(req.getEmail())
                ? req.getEmail()
                : order.getCustomerEmail();
    }

    private void logEvent(Payment payment, PaymentEventType eventType,
                          PaymentStatus oldStatus, PaymentStatus newStatus,
                          JsonNode payload) {
        try {
            paymentEventRepository.save(
                    PaymentEvent.of(payment, eventType, oldStatus, newStatus, payload));
        } catch (Exception e) {
            log.warn("Failed to write payment event log: {}", e.getMessage());
        }
    }



    private String generateReference(String orderNumber) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String reference = "PS-" + orderNumber + "-" + suffix;

        int attempts = 0;
        while (paymentRepository.existsByReference(reference)) {
            suffix    = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            reference = "PS-" + orderNumber + "-" + suffix;
            if (++attempts > 10) throw new IllegalStateException("Cannot generate unique reference");
        }
        return reference;
    }

    private JsonNode toJson(Object obj) {
        return obj != null ? objectMapper.valueToTree(obj) : null;
    }
}
