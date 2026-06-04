package com.grill_bros.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.dto.paymentdto.MomoCallbackDto;
import com.grill_bros.backend.dto.paymentdto.PaystackApiResponse;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import com.grill_bros.backend.service.paymentservice.PaymentService;
import com.grill_bros.backend.service.paymentservice.WebhookSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Paystack server-to-server event callbacks")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final WebhookSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;

    @PostMapping("/paystack")
    @Operation(
            summary     = "Paystack webhook receiver",
            description = "Receives charge.success, charge.failed, and other Paystack events. " +
                    "Validates HMAC-SHA512 signature before processing. " +
                    "Always returns 200 OK on valid requests (even for unhandled event types) " +
                    "to prevent unnecessary Paystack retries."
    )
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Paystack-Signature", required = false)
            String signature) {

        if (!signatureValidator.isValid(rawBody, signature)) {
            log.warn("Paystack webhook rejected: invalid signature");
            return ResponseEntity
                    .status(401)
                    .body(ApiResponse.error("Invalid webhook signature"));
        }

        PaystackApiResponse.WebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, PaystackApiResponse.WebhookEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse Paystack webhook payload: {}", e.getMessage());
            // Return 200 so Paystack doesn't retry a malformed payload
            return ResponseEntity.ok(ApiResponse.ok(null, "Payload parse error — ignored"));
        }

        log.info("Paystack webhook received: event={} reference={}",
                event.getEvent(),
                event.getData() != null ? event.getData().getReference() : "null");

        try {
            paymentService.processWebhook(event, rawBody);
        } catch (Exception e) {
            log.error("Error processing Paystack webhook event={} reference={}: {}",
                    event.getEvent(),
                    event.getData() != null ? event.getData().getReference() : "null",
                    e.getMessage(), e);
        }

        return ResponseEntity.ok(ApiResponse.ok(null, "Webhook received"));
    }
}