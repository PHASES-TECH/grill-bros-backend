package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.paymentrecords.PaymentSucceededEvent;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.service.utilsservice.EmailService;
import com.grill_bros.backend.service.utilsservice.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailPaymentListener {

    private final ReceiptService receiptService;
    private final PaymentRepository paymentRepository;

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentSuccess(
            PaymentSucceededEvent event
    ) {
        Payment payment = paymentRepository.findById(event.paymentId()).orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        try {
            receiptService.generateAndSendReceipt(payment);
        }
        catch (Exception ex) {
            log.error("Email failed", ex);
        }
    }
}
