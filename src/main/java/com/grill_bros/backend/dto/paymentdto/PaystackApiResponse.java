package com.grill_bros.backend.dto.paymentdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

public final class PaystackApiResponse {
    private PaystackApiResponse() {}

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Envelope<T> {
        private boolean status;
        private String  message;
        private T       data;
    }


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InitializeData {

        @JsonProperty("authorization_url")
        private String authorizationUrl;

        @JsonProperty("access_code")
        private String accessCode;

        private String reference;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifyData {

        private Long   id;                // Paystack internal transaction ID
        private String status;            // success | failed | abandoned

        private String reference;
        private String message;

        @JsonProperty("gateway_response")
        private String gatewayResponse;   // "Approved", "Declined", etc.

        private String channel;           // card, mobile_money, bank, ussd

        /** Amount in kobo/pesewas (smallest currency unit) */
        private long amount;

        private String currency;

        @JsonProperty("paid_at")
        private Instant paidAt;

        @JsonProperty("created_at")
        private Instant createdAt;

        private CustomerData customer;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CustomerData {
            private String email;
            private String phone;

            @JsonProperty("first_name")
            private String firstName;

            @JsonProperty("last_name")
            private String lastName;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookEvent {

        private String event;      // charge.success | charge.failed | transfer.success
        private VerifyData data;
    }
}
