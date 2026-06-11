package com.grill_bros.backend.service.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.dto.paymentdto.*;
import com.grill_bros.backend.exceptions.DuplicateResourceException;
import com.grill_bros.backend.exceptions.InvalidStateTransitionException;
import com.grill_bros.backend.exceptions.PaystackException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.exceptions.passwordexceptions.BadRequestException;
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
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository      paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaystackClient         paystackClient;
    private final CacheService           cache;
    private final ObjectMapper           objectMapper;
    private final PaymentEventPublisher eventPublisher;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentInitiatedResponse initiatePayment(InitiatePaymentRequest req) {
        var order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order"));

        if (order.getStatus() == OrderStatus.CANCELLED ||
                order.getStatus() == OrderStatus.EXPIRED) {
            throw new InvalidStateTransitionException(
                    "Cannot pay for cancelled or expired order");
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

//        String email = StringUtils.hasText(req.getEmail())
//                ? req.getEmail()
//                : order.getCustomerEmail();

        if (!StringUtils.hasText(req.getEmail())) {
            throw new BadRequestException("Email is required");
        }

//        if (!StringUtils.hasText(email)) {
//            throw new BadRequestException(
//                    "Customer email is required to initiate Paystack payment. " +
//                            "Please provide an email address.");
//        }

        String reference = generateReference(order.getOrderNumber());

        Payment payment = Payment.create(
                order, reference,
                req.getEmail(), order.getCustomerPhone(),
                order.getTotalAmount());
        paymentRepository.save(payment);

        log.info("PAYMENT_INITIATED paymentId={} status={}", payment.getId(), PaymentStatus.INITIATED);

        try {
            PaystackApiResponse.InitializeData init =
                    paystackClient.initializeTransaction(
                            reference,
                            req.getEmail(),
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

            eventPublisher.paymentInitiated(payment);

            log.info("Payment initiated: reference={} order={} amount={}GHS",
                    reference, order.getOrderNumber(), order.getTotalAmount());

            return PaymentInitiatedResponse.from(payment);

        } catch (PaystackException e) {
            payment.markFailed("Paystack initialisation error: " + e.getMessage());
            paymentRepository.save(payment);
            log.info( "PAYSTACK_INIT_FAILED", payment,
                    PaymentStatus.PENDING, PaymentStatus.FAILED, e.getMessage());
            log.error("Paystack init failed for order={}: {}", order.getOrderNumber(), e.getMessage());
            throw new BadRequestException(
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

//        String lockKey = RedisKeys.orderLock(payment.getOrder().getId());
//        boolean locked = cache.acquireLock(lockKey, RedisKeys.TTL_ORDER_LOCK_SECONDS);
//        if (!locked) {
//            log.warn("Could not acquire lock for orderId={}, another process is handling it",
//                    payment.getOrder().getId());
//            return PaymentStatusResponse.from(payment);
//        }

        try {
            PaystackApiResponse.VerifyData verifyData =
                    paystackClient.verifyTransaction(reference);

            PaymentStatus previous = payment.getStatus();

            switch (verifyData.getStatus()) {
                case "success" -> handleSuccess(payment, verifyData);
                case "failed" -> handleFailed(payment, verifyData);
                case "abandoned" -> {
                    payment.markFailed("Payment abandoned by customer");
                    paymentRepository.save(payment);
                    log.info("PAYMENT_ABANDONED payment={} previous={}", payment, previous);
                }
                default -> log.info("Payment still pending: reference={} paystackStatus={}",
                        reference, verifyData.getStatus());
            }

        } catch (Exception e) {
            log.error("Error occurred", e.getMessage());
        }
//        finally {
//            cache.releaseLock(lockKey);
//        }
        return PaymentStatusResponse.from(payment);
    }

    @Transactional
    public void processWebhook(PaystackApiResponse.WebhookEvent event, String rawPayload) {
        if (event == null || event.getData() == null) {
            log.warn("Received empty or malformed webhook event");
            return;
        }

        String reference = event.getData().getReference();
        String eventType = event.getEvent();

        log.info("Processing Paystack webhook: event={} reference={}", eventType, reference);

        String replayKey = "grill-bros:webhook:seen:paystack:" + reference + ":" + eventType;
        if (cache.exists(replayKey)) {
            log.info("Duplicate webhook ignored: event={} reference={}", eventType, reference);
            return;
        }

        cache.setRaw(replayKey, "1", 3600); // TTL 1 hour

        switch (eventType) {
            case "charge.success" -> {
                try {
                    verifyAndUpdate(reference);
                } catch (ResourceNotFoundException e) {
                    log.warn("Webhook for unknown reference={}: {}", reference, e.getMessage());
                }
            }
            case "charge.failed" -> {
                paymentRepository.findByReference(reference).ifPresent(payment -> {
                    if (!payment.getStatus().isTerminal()) {
                        PaymentStatus previous = payment.getStatus();
                        payment.markFailed(event.getData().getGatewayResponse());
                        paymentRepository.save(payment);
                        log.info( "WEBHOOK_CHARGE_FAILED payment={} previous={} status={} payload={}", payment,
                                previous, PaymentStatus.FAILED, rawPayload);
//                        cachePaymentStatus(payment);
                        log.info("Payment marked FAILED via webhook: reference={}", reference);
                    }
                });
            }
            default -> log.debug("Unhandled webhook event type: {}", eventType);
        }
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatusByOrderId(String orderId) {
        String cacheKey = RedisKeys.paymentStatus(orderId.toString());
        var cached = cache.get(cacheKey, PaymentStatusResponse.class);
        if (cached.isPresent()) {
            log.debug("Payment status cache hit: orderId={}", orderId);
            return cached.get();
        }

        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for order"));

        PaymentStatusResponse response = PaymentStatusResponse.from(payment);

        cache.set(cacheKey, response, RedisKeys.TTL_PAYMENT_STATUS_SECONDS);
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatusByReference(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for order"));

        PaymentStatusResponse response = PaymentStatusResponse.from(payment);

        return response;
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
        var order = orderRepository.findById(payment.getOrder().getId()).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
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

    private void cachePaymentStatus(Payment payment) {
        try {
            PaymentStatusResponse response = PaymentStatusResponse.from(payment);
            long ttl = payment.getStatus().isTerminal()
                    ? 3600  // terminal: cache for 1h
                    : RedisKeys.TTL_PAYMENT_STATUS_SECONDS;
            cache.set(RedisKeys.paymentStatus(payment.getOrder().getId()),
                    response, ttl);
        } catch (Exception e) {
            log.warn("Failed to cache payment status: {}", e.getMessage());
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
