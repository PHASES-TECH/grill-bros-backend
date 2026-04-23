package com.grill_bros.backend.events;

import com.grill_bros.backend.model.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

// ─────────────────────────────────────────────────────────────────────────────
// Fired when a new order is created (PENDING state)
// ─────────────────────────────────────────────────────────────────────────────
@Getter
public class OrderCreatedEvent extends ApplicationEvent {

    private final String       orderId;
    private final String     orderNumber;
    private final String     customerName;
    private final String     customerPhone;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(Object source, Order order) {
        super(source);
        this.orderId       = order.getId();
        this.orderNumber   = order.getOrderNumber();
        this.customerName  = order.getCustomerName();
        this.customerPhone = order.getCustomerPhone();
        this.totalAmount   = order.getTotalAmount();
    }
}