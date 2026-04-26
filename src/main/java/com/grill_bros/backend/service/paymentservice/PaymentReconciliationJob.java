package com.grill_bros.backend.service.paymentservice;

import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.grill_bros.backend.records.PaymentStatus.*;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final MoMoClient momoClient;

    @Scheduled(fixedDelay = 180000)
    public void reconcile() {

        List<Payment> payments = paymentRepository.findStalePayments(
                Instant.now().minusSeconds(180)
        );

        for (Payment payment : payments) {
            var status = momoClient.getTransactionStatus(payment.getExternalId());

            boolean isOlderThan10Minutes = payment.getCreatedAt()
                    .isBefore(Instant.now().minus(10, ChronoUnit.MINUTES));

            switch (status) {
                case SUCCESSFUL -> payment.transitionTo(SUCCESSFUL, null);
                case FAILED    -> payment.transitionTo(FAILED, null);
                case PENDING    -> {
                    if (isOlderThan10Minutes) {
                        payment.transitionTo(PaymentStatus.TIMEOUT, null);
                    }
                }
            }

            paymentRepository.save(payment);
        }
    }
}
