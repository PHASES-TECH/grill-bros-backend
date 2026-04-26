package com.grill_bros.backend.dto.paymentdto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiatePaymentRequest {

    private String orderId;
    private String phoneNumber;

}
