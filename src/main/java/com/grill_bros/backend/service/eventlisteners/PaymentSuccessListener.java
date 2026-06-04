package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.events.PaymentCompletedEvent;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentSuccessListener {

    private final OrderRepository orderRepository;
    private final SmsProviderService smsService;

    @EventListener
    @Transactional
    public void handleSuccess(PaymentCompletedEvent event) {

        Order order = orderRepository.findById(event.getOrder().getId())
                .orElseThrow();

        order.confirm();
        orderRepository.save(order);

        smsService.sendSms(
                List.of(event.getPhoneNumber()),
                "Your order " + event.getOrder().getOrderNumber() + " is confirmed!"
        );
    }
}
