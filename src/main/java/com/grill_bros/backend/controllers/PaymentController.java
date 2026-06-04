package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.common.PagedResponse;
import com.grill_bros.backend.dto.menudtos.CategoryResponse;
import com.grill_bros.backend.dto.paymentdto.*;
import com.grill_bros.backend.service.paymentservice.PaymentService;
import com.grill_bros.backend.service.utilsservice.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Paystack payment initiation, callback, and status")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReceiptService receiptService;

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

    @GetMapping("/receipts")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentReceiptResponse>>> getAllPaymentReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<PaymentReceiptResponse> result = receiptService.getAllPaymentReceipts(pageable);

        return ResponseEntity.ok(
                ApiResponse.ok(PagedResponse.of(result))
        );
    }

    @PostMapping("/initiate")
    @Operation(
            summary     = "Initiate a Paystack payment for an order",
            description = "Creates a Paystack transaction and returns the authorizationUrl. " +
                    "Frontend must redirect the customer to this URL to complete payment. " +
                    "If a pending payment already exists for the order, the existing " +
                    "authorizationUrl is returned (idempotent)."
    )
    public ResponseEntity<ApiResponse<PaymentInitiatedResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest req) {

        log.info("Payment initiation request for orderId={}", req.getOrderId());
        PaymentInitiatedResponse response = paymentService.initiatePayment(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

//    @GetMapping("/verify")
//    @Operation(
//            summary     = "Paystack payment callback — called after customer completes payment",
//            description = "Paystack redirects the customer to this URL after payment. " +
//                    "The 'reference' query parameter identifies the transaction. " +
//                    "This endpoint verifies the transaction with Paystack API and " +
//                    "updates the order and payment status accordingly."
//    )
//    public ResponseEntity<ApiResponse<PaymentStatusResponse>> handleCallback(
//            @Parameter(description = "Paystack transaction reference", required = true)
//            @RequestParam String reference,
//
//            @Parameter(description = "Paystack transaction reference (alias)")
//            @RequestParam(required = false) String trxref) {
//
//        // Paystack sends both 'reference' and 'trxref' — use whichever is present
//        String ref = reference != null ? reference : trxref;
//
//        log.info("Payment callback received: reference={}", ref);
//        PaymentStatusResponse status = paymentService.verifyAndUpdate(ref);
//        return ResponseEntity.ok(ApiResponse.ok(status));
//    }

    @GetMapping("/callback")
    public PaymentStatusResponse callback(@RequestParam String reference) {
        return paymentService.getStatusByReference(reference);
    }

    @GetMapping("/{orderId}/status")
    @Operation(
            summary     = "Get current payment status for an order",
            description = "Returns the latest payment status. Redis-cached for fast polling. " +
                    "Frontend should poll this while waiting for payment confirmation " +
                    "and stop once a terminal status is received " +
                    "(SUCCESSFUL, FAILED, CANCELLED, TIMEOUT)."
    )
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getStatus(
            @Parameter(description = "Order UUID", required = true)
            @PathVariable String orderId) {

        return ResponseEntity.ok(
                ApiResponse.ok(paymentService.getStatusByOrderId(orderId)));
    }
}
