package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.common.PagedResponse;
import com.grill_bros.backend.dto.menudtos.CategoryResponse;
import com.grill_bros.backend.dto.paymentdto.InitiatePaymentRequest;
import com.grill_bros.backend.dto.paymentdto.PaymentResponse;
import com.grill_bros.backend.service.paymentservice.PaymentService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<PaymentResponse> result = paymentService.getAllPayments(pageable);

        return ResponseEntity.ok(
                ApiResponse.ok(PagedResponse.of(result))
        );
    }

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
