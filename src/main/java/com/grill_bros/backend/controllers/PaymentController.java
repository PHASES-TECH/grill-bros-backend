package com.grill_bros.backend.controllers;

import com.grill_bros.backend.dto.paymentdto.InitiatePaymentRequest;
import com.grill_bros.backend.service.paymentservice.PaymentService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PermitAll
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(
            @RequestBody InitiatePaymentRequest req,
            @RequestHeader("Idempotency-Key") String idemKey
    ) {
        return ResponseEntity.ok(paymentService.initiatePayment(req, idemKey));
    }

    @PermitAll
    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> status(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
    }

    @PermitAll
    @PatchMapping("/{externalId}/payment-status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String externalId) {
        return ResponseEntity.ok(paymentService.checkAndUpdateStatus(externalId));
    }
}
