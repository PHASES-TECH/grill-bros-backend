package com.grill_bros.backend.dto.paymentdto;

import com.grill_bros.backend.records.PaymentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitiatedResponse {

    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private String pollUrl;

}
