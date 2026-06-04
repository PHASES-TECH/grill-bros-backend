package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.events.PaymentCompletedEvent;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationListener {

    private final SmsProviderService smsProviderService;

    @EventListener
    @Async
    public void onPaymentCompleted(PaymentCompletedEvent event) {

        if (event.getStatus().isTerminal() && event.getStatus().isSuccessful()) {

            String message = String.format(
                    "Your order %s is confirmed. Track it with token %s",
                    event.getOrder().getOrderNumber(),
                    event.getOrder().getTrackingToken()
            );

            smsProviderService.sendSms(
                    List.of(event.getPhoneNumber()),
                    message
            );

            log.info("SMS sent for order={}", event.getOrder().getOrderNumber());
        }
    }
}
