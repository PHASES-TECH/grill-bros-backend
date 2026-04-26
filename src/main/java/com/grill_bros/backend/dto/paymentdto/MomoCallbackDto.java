package com.grill_bros.backend.dto.paymentdto;

import lombok.Data;

@Data
public class MomoCallbackDto {
    private String referenceId;
    private String status;
    private String financialTransactionId;
}
