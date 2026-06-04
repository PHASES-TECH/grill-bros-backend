package com.grill_bros.backend.events;

import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.PaymentStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final Order          order;
    private final PaymentStatus status;
    private final BigDecimal    amount;
    private final String        phoneNumber;
    private final String        failureReason;
    private final Payment payment;

    public PaymentCompletedEvent(Object source, Order order,
                                 PaymentStatus status, BigDecimal amount,
                                 String phoneNumber, String failureReason, Payment payment) {
        super(source);
        this.order      = order;
        this.status        = status;
        this.amount        = amount;
        this.phoneNumber   = phoneNumber;
        this.failureReason = failureReason;
        this.payment         = payment;
    }
}
