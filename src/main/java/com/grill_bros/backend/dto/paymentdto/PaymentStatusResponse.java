package com.grill_bros.backend.dto.paymentdto;

import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentStatusResponse {

    private String orderId;
    private PaymentStatus paymentStatus;
    private String paymentId;
    private OrderStatus orderStatus;
    private BigDecimal amount;
    private String message;

}
