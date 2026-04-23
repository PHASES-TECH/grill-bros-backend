package com.grill_bros.backend.events;

import com.grill_bros.backend.records.OrderStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final String        orderId;
    private final String      orderNumber;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(Object source, String orderId, String orderNumber,
                                   OrderStatus oldStatus, OrderStatus newStatus) {
        super(source);
        this.orderId     = orderId;
        this.orderNumber = orderNumber;
        this.oldStatus   = oldStatus;
        this.newStatus   = newStatus;
    }
}
