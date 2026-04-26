package com.grill_bros.backend.controllers;

import com.grill_bros.backend.dto.paymentdto.MomoCallbackDto;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CacheService cache;

    @PostMapping("/momo/callback")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody MomoCallbackDto payload,
            @RequestHeader("X-Callback-Id") String callbackId,
            @RequestHeader("X-Callback-Signature") String signature
    ) {

        // 1. REPLAY PROTECTION
        String replayKey = "grillbros:webhook:seen:" + callbackId;
        if (cache.exists(replayKey)) return ResponseEntity.ok().build();

        cache.setRaw(replayKey, "1", 3600);

        // 2. FETCH PAYMENT
        Payment payment = paymentRepository
                .findByExternalIdWithOrder(payload.getReferenceId())
                .orElseThrow();

        if (payment.getStatus().isTerminal()) return ResponseEntity.ok().build();

        // 3. UPDATE
        if ("SUCCESSFUL".equals(payload.getStatus())) {
            payment.transitionTo(PaymentStatus.SUCCESSFUL, null);

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

        } else {
            payment.transitionTo(PaymentStatus.FAILED, null);
        }

        paymentRepository.save(payment);

        return ResponseEntity.ok().build();
    }
}