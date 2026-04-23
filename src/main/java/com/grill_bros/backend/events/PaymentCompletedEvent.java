package com.grill_bros.backend.events;

import com.grill_bros.backend.records.PaymentStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
// ─────────────────────────────────────────────────────────────────────────────
// Fired when MoMo payment reaches a terminal state
// ─────────────────────────────────────────────────────────────────────────────
@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final String          orderId;
    private final String        orderNumber;
    private final PaymentStatus status;
    private final BigDecimal    amount;
    private final String        phoneNumber;
    private final String        failureReason;

    public PaymentCompletedEvent(Object source, String orderId, String orderNumber,
                                 PaymentStatus status, BigDecimal amount,
                                 String phoneNumber, String failureReason) {
        super(source);
        this.orderId       = orderId;
        this.orderNumber   = orderNumber;
        this.status        = status;
        this.amount        = amount;
        this.phoneNumber   = phoneNumber;
        this.failureReason = failureReason;
    }
}
