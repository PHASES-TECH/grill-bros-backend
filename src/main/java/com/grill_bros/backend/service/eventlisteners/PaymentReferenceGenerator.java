package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentReferenceGenerator {

    private final PaymentRepository paymentRepository;

    private static final String PREFIX = "PS";

    public String generate(String orderNumber) {

        int attempts = 0;

        while (attempts < 10) {

            String reference = buildReference(orderNumber);

            if (!paymentRepository.existsByReference(reference)) {
                return reference;
            }

            attempts++;
        }

        throw new IllegalStateException(
                "Unable to generate unique payment reference after 10 attempts"
        );
    }

    private String buildReference(String orderNumber) {

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();

        return PREFIX + "-" + orderNumber + "-" + suffix;
    }
}
