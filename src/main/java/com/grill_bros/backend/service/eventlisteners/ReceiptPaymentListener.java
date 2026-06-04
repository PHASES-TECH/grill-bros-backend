package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.paymentrecords.PaymentSucceededEvent;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.service.utilsservice.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiptPaymentListener {

    private final PaymentRepository paymentRepository;
    private final ReceiptService receiptService;

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentSuccess(
            PaymentSucceededEvent event
    ) {
        Payment payment = paymentRepository.findById(event.paymentId()).orElseThrow(() -> new ResourceNotFoundException("Payment"));

        try {
            receiptService.generateAndSendReceipt(payment);

        } catch (Exception ex) {
            log.error(
                    "SMS failed for order={}",
                    event.orderNumber(),
                    ex
            );
        }
    }
}
