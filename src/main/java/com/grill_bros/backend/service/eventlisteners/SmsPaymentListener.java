package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.records.paymentrecords.PaymentSucceededEvent;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
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
public class SmsPaymentListener {

    private final SmsProviderService smsService;
    private final OrderRepository orderRepository;

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentSuccess(
            PaymentSucceededEvent event
    ) {

        try {
            Order order = orderRepository.findById(event.orderId()).orElseThrow(() -> new ResourceNotFoundException("Order"));

            String message = String.format(
                    "Your order is confirmed! Your order ID is %s. Track your order status anytime using this token %s",
                    order.getOrderNumber(),
                    order.getTrackingToken()
            );

            smsService.sendSms(List.of(event.customerPhone()), message);

        } catch (Exception ex) {
            log.error(
                    "SMS failed for order={}",
                    event.orderNumber(),
                    ex
            );
        }
    }
}
