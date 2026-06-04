package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.events.PaymentCompletedEvent;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.paymentrecords.PaymentSucceededEvent;
import com.grill_bros.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderPaymentListener {

    private final OrderRepository orderRepository;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPaymentSuccess(
            PaymentSucceededEvent event
    ) {

        Order order = orderRepository.findById(
                event.orderId()
        ).orElseThrow();

        if (order.getStatus() == OrderStatus.PENDING) {

            order.transitionTo(
                    OrderStatus.CONFIRMED
            );

            orderRepository.save(order);
        }
    }
}
