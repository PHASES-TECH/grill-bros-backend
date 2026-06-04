package com.grill_bros.backend.dto.paymentdto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private String orderId;

    @Email(message = "Invalid email address")
    private String email;

}
