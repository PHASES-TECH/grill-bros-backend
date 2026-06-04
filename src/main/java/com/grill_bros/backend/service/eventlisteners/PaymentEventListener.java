package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.events.PaymentCompletedEvent;
import com.grill_bros.backend.model.PaymentEvent;
import com.grill_bros.backend.records.PaymentEventType;
import com.grill_bros.backend.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentEventRepository repository;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Async
    public void onPaymentCompleted(PaymentCompletedEvent event) {

        PaymentEvent audit = PaymentEvent.of(
                event.getPayment(),
                PaymentEventType.PAYMENT_SUCCESS,
                null,
                event.getStatus(),
                null
        );

        repository.save(audit);

        log.info("Payment event logged for order={}", event.getOrder().getOrderNumber());
    }
}
