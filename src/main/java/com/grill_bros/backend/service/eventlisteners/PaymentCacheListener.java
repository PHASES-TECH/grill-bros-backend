package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.dto.paymentdto.PaymentStatusResponse;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.paymentrecords.PaymentSucceededEvent;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCacheListener {

    private final CacheService cache;
    private final PaymentRepository paymentRepository;

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentSuccess(
            PaymentSucceededEvent event
    ) {
        Payment payment = paymentRepository.findById(event.paymentId()).orElseThrow(() ->new ResourceNotFoundException("Payment"));
        cachePaymentStatus(payment);
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
}
