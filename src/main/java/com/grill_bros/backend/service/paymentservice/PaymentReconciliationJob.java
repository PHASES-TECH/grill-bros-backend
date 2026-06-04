package com.grill_bros.backend.service.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grill_bros.backend.exceptions.PaystackException;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.model.PaymentEvent;
import com.grill_bros.backend.records.PaymentEventType;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.PaymentEventRepository;
import com.grill_bros.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.grill_bros.backend.records.PaymentStatus.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentRepository      paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaystackClient         paystackClient;
    private final PaymentService         paymentService;
    private final ObjectMapper objectMapper;

    private static final int STALE_THRESHOLD_MINUTES  = 3;
    private static final int TIMEOUT_THRESHOLD_MINUTES = 15;

    @Scheduled(fixedDelay = 180_000, initialDelay = 60_000)
    public void reconcile() {
        Instant staleThreshold   = Instant.now().minus(STALE_THRESHOLD_MINUTES,  ChronoUnit.MINUTES);
        Instant timeoutThreshold = Instant.now().minus(TIMEOUT_THRESHOLD_MINUTES, ChronoUnit.MINUTES);

        List<Payment> stalePayments = paymentRepository.findStalePayments(staleThreshold);

        if (stalePayments.isEmpty()) {
            log.debug("Reconciliation: no stale payments found");
            return;
        }

        log.info("Reconciliation: checking {} stale payment(s)", stalePayments.size());

        for (Payment payment : stalePayments) {
            try {
                reconcileOne(payment, timeoutThreshold);
            } catch (Exception e) {
                log.error("Reconciliation failed for reference={}: {}",
                        payment.getReference(), e.getMessage());
            }
        }
    }

    @Transactional
    private void reconcileOne(Payment payment, Instant timeoutThreshold) {
        // If past the timeout window — mark TIMEOUT without calling Paystack
        if (payment.getInitiatedAt().isBefore(timeoutThreshold)) {
            log.warn("Payment TIMEOUT: reference={} age={}min",
                    payment.getReference(),
                    ChronoUnit.MINUTES.between(payment.getInitiatedAt(), Instant.now()));

            PaymentStatus previous = payment.getStatus();
            payment.transitionTo(PaymentStatus.TIMEOUT);
            payment.setGatewayResponse("Payment timed out — no confirmation received");
            paymentRepository.save(payment);

            JsonNode payload = objectMapper.createObjectNode()
                    .put("message", "Exceeded " + TIMEOUT_THRESHOLD_MINUTES + " minute threshold");

            paymentEventRepository.save(PaymentEvent.of(
                    payment, PaymentEventType.RECONCILIATION_TIMEOUT,
                    previous, PaymentStatus.TIMEOUT,
                    payload));
            return;
        }

        // Within timeout window — call Paystack to check
        try {
            log.debug("Reconciliation verify: reference={}", payment.getReference());
            paymentService.verifyAndUpdate(payment.getReference());

            JsonNode payload = objectMapper.createObjectNode()
                    .put("message", "Checked at " + Instant.now());

            paymentEventRepository.save(PaymentEvent.of(
                    payment, PaymentEventType.RECONCILIATION_CHECK,
                    payment.getStatus(), payment.getStatus(),
                    payload));

        } catch (PaystackException e) {
            log.warn("Paystack API error during reconciliation for reference={}: {}",
                    payment.getReference(), e.getMessage());
        }
    }
}
