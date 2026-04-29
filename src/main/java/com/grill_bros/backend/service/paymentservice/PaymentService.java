package com.grill_bros.backend.service.paymentservice;

import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.dto.paymentdto.InitiatePaymentRequest;
import com.grill_bros.backend.dto.paymentdto.PaymentInitiatedResponse;
import com.grill_bros.backend.dto.paymentdto.PaymentResponse;
import com.grill_bros.backend.dto.paymentdto.PaymentStatusResponse;
import com.grill_bros.backend.exceptions.DuplicateResourceException;
import com.grill_bros.backend.exceptions.RateLimitException;
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
import com.grill_bros.backend.service.utilsservice.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final MoMoClient momoClient;
    private final CacheService cache;
    private final ReceiptService receiptService;

    @Override
    @Transactional
    public PaymentInitiatedResponse initiatePayment(InitiatePaymentRequest request, String idempotencyKey) {
        String idemCacheKey = RedisKeys.paymentIdem(idempotencyKey);
        var cached = cache.get(idemCacheKey, PaymentInitiatedResponse .class);
        if (cached.isPresent()) return cached.get();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order not payable");
        }

        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new DuplicateResourceException("Payment already initiated");
        }

        long count = cache.increment(RedisKeys.paymentRateLimit(request.getPhoneNumber()), 60);
        if (count > 5) throw new RateLimitException();

        String externalId = UUID.randomUUID().toString();

        Payment payment = Payment.create(order, externalId, request.getPhoneNumber(), order.getTotalAmount());
        paymentRepository.save(payment);

        paymentEventRepository.save(PaymentEvent.initiated(payment));

        try {
            momoClient.requestToPay(externalId, request.getPhoneNumber(), order.getTotalAmount().toString(), "GHS");
            payment.transitionTo(PaymentStatus.PENDING, null);
        } catch (Exception ex) {
            payment.transitionTo(PaymentStatus.FAILED, ex.getMessage());
            System.out.println(ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }

        paymentRepository.save(payment);

        cache.set(RedisKeys.paymentStatus(order.getId()), payment.getStatus().name(), 180);

        PaymentInitiatedResponse response = PaymentInitiatedResponse.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .status(payment.getStatus())
                .pollUrl("/api/v1/payments/" + order.getId() + "/status")
                .build();

        // CACHE IDEMPOTENCY
        cache.set(idemCacheKey, response, 86400);

        return response;
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(String orderId) {
        String cacheKey = RedisKeys.paymentStatus(orderId);
        var cached = cache.getRaw(cacheKey);

        if (cached.isPresent()) {
            return PaymentStatusResponse.builder().orderId(orderId).paymentStatus(PaymentStatus.valueOf(cached.get())).build();
        }

        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment"));

        cache.set(cacheKey, payment.getStatus().name(), 180);

        return PaymentStatusResponse.builder().orderId(orderId).paymentStatus(payment.getStatus()).build();

    }

    @Override
    public PaymentStatusResponse checkAndUpdateStatus(String externalId) {
        String momoStatus = momoClient.getPaymentStatus(externalId);

        ObjectMapper objectMapper = new ObjectMapper();

        String payload = objectMapper.writeValueAsString(
                Map.of("momoStatus", momoStatus)
        );

        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment"));

        PaymentStatus newStatus = mapMoMoStatus(momoStatus);
        PaymentStatus oldStatus = payment.getStatus();

        if (!oldStatus.equals(newStatus) && !oldStatus.isTerminal()) {

            payment.transitionTo(newStatus, null);

            Order order = payment.getOrder();
            if (newStatus == PaymentStatus.SUCCESSFUL) {
                order.setStatus(OrderStatus.CONFIRMED);
            } else if (newStatus == PaymentStatus.FAILED) {
                order.setStatus(OrderStatus.CANCELLED);
            }

            orderRepository.save(order);
            paymentRepository.save(payment);

            paymentEventRepository.save(
                    PaymentEvent.statusChange(payment, oldStatus, newStatus, payload)
            );
        }

        if(newStatus == PaymentStatus.SUCCESSFUL) {
            receiptService.generateAndSendReceipt(payment);
        }

        return PaymentStatusResponse.builder()
                .orderId(payment.getOrder().getId())
                .paymentId(payment.getId())
                .paymentStatus(payment.getStatus())
                .orderStatus(payment.getOrder().getStatus())
                .amount(payment.getAmount())
                .message(mapMessage(payment.getStatus()))
                .build();
    }

    private PaymentStatus mapMoMoStatus(String status) {
        if (status == null) return PaymentStatus.PENDING;

        return switch (status.toUpperCase()) {
            case "SUCCESSFUL" -> PaymentStatus.SUCCESSFUL;
            case "FAILED"     -> PaymentStatus.FAILED;
            case "PENDING"    -> PaymentStatus.PENDING;
            default           -> PaymentStatus.PENDING;
        };
    }

    private String mapMessage(PaymentStatus status) {
        return switch (status) {
            case INITIATED   -> "Payment initiated.";
            case PENDING     -> "Waiting for mobile money approval.";
            case SUCCESSFUL  -> "Payment successful.";
            case FAILED      -> "Payment failed. Please try again.";
            case CANCELLED ->   "Payment cancelled.";
            case TIMEOUT     -> "Payment request expired. Please retry.";
        };
    }
}
