package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.events.PaymentCompletedEvent;
import com.grill_bros.backend.model.PaymentEvent;
import com.grill_bros.backend.records.PaymentEventType;
import com.grill_bros.backend.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentAuditListener {

    private final PaymentEventRepository paymentEventRepository;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentSuccess(PaymentCompletedEvent event) {

        paymentEventRepository.save(
                PaymentEvent.of(
                        event.getPayment(),
                        PaymentEventType.PAYMENT_SUCCESS,
                        null,
                        event.getStatus(),
                        null
                )
        );
    }
}
