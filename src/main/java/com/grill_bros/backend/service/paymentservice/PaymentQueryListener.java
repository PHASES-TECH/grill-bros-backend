package com.grill_bros.backend.service.paymentservice;

import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.dto.paymentdto.PaymentStatusResponse;
import com.grill_bros.backend.events.PaymentCompletedEvent;
import com.grill_bros.backend.service.cacheservice.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryListener {

    private final CacheService cache;

    @EventListener
    @Async
    public void onPaymentCompleted(PaymentCompletedEvent event) {

        try {
            String key = RedisKeys.paymentStatus(event.getOrder().getId().toString());

            PaymentStatusResponse snapshot = PaymentStatusResponse.from(event.getPayment());

            cache.set(key, snapshot,
                    event.getStatus().isTerminal() ? 3600 : 300);

            log.info("Payment cache updated for order={}", event.getOrder().getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to update payment cache", e);
        }
    }
}
