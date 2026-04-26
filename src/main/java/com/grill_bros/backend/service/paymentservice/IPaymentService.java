package com.grill_bros.backend.service.paymentservice;

import com.grill_bros.backend.dto.paymentdto.InitiatePaymentRequest;
import com.grill_bros.backend.dto.paymentdto.PaymentInitiatedResponse;
import com.grill_bros.backend.dto.paymentdto.PaymentStatusResponse;

public interface IPaymentService {
    PaymentInitiatedResponse initiatePayment(InitiatePaymentRequest request, String idempotencyKey);
    PaymentStatusResponse getPaymentStatus(String orderId);
    PaymentStatusResponse checkAndUpdateStatus(String externalId);
}
