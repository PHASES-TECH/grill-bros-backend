package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.paymentrecords.PaymentFailedEvent;
import com.grill_bros.backend.records.paymentrecords.PaymentSucceededEvent;
import com.grill_bros.backend.records.paymentrecords.PaymentinitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void paymentSucceeded(Payment payment) {
        publisher.publishEvent(
                new PaymentSucceededEvent(
                        payment.getId(),
                        payment.getOrder().getId(),
                        payment.getOrder().getOrderNumber(),
                        payment.getCustomerPhone(),
                        payment.getAmount()
                )
        );
    }

    public void paymentFailed(Payment payment) {
        publisher.publishEvent(
                new PaymentFailedEvent(
                        payment.getId(),
                        payment.getOrder().getId(),
                        payment.getGatewayResponse()
                )
        );
    }

    public void paymentInitiated(Payment payment) {
        publisher.publishEvent(
                new PaymentinitiatedEvent(
                        payment.getId(),
                        payment.getOrder().getId(),
                        payment.getReference()
                )
        );
    }
}
