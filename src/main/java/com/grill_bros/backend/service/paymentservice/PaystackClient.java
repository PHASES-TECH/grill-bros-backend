package com.grill_bros.backend.service.paymentservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grill_bros.backend.dto.paymentdto.PaystackApiResponse;
import com.grill_bros.backend.dto.paymentdto.PaystackProperties;
import com.grill_bros.backend.exceptions.PaystackException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaystackClient {

    private final PaystackProperties paystackProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public PaystackApiResponse.InitializeData initializeTransaction(
            String reference,
            String email,
            BigDecimal amountGhs) {

        long amountPesewas = amountGhs
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        Map<String, Object> body = Map.of(
                "reference",    reference,
                "email",        email,
                "amount",       amountPesewas,
                "currency",     paystackProperties.getCurrency(),
                "callback_url", paystackProperties.getCallbackUrl()
        );

        log.info("Paystack initialize: reference={} email={} amount={}GHS",
                reference, email, amountGhs);

        String responseBody = post("/transaction/initialize", body);

        PaystackApiResponse.Envelope<PaystackApiResponse.InitializeData> envelope =
                parseEnvelope(responseBody,
                        new TypeReference<>() {});

        assertSuccess(envelope, "Transaction initialization failed");
        return envelope.getData();
    }

    public PaystackApiResponse.VerifyData verifyTransaction(String reference) {
        log.info("Paystack verify: reference={}", reference);

        String responseBody = get("/transaction/verify/" + reference);

        PaystackApiResponse.Envelope<PaystackApiResponse.VerifyData> envelope =
                parseEnvelope(responseBody, new TypeReference<>() {});

        assertSuccess(envelope, "Transaction verification failed");
        return envelope.getData();
    }


    private String post(String path, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(paystackProperties.getBaseUrl() + path))
                    .header("Authorization", "Bearer " + paystackProperties.getSecretKey())
                    .header("Content-Type",  "application/json")
                    .header("Cache-Control", "no-cache")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Paystack POST {} → HTTP {}", path, response.statusCode());
            assertHttpSuccess(response, path);
            return response.body();

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaystackException("Network error calling Paystack: " + e.getMessage(), e);
        }
    }

    private String get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(paystackProperties.getBaseUrl() + path))
                    .header("Authorization", "Bearer " + paystackProperties.getSecretKey())
                    .header("Cache-Control", "no-cache")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Paystack GET {} → HTTP {}", path, response.statusCode());
            assertHttpSuccess(response, path);
            return response.body();

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaystackException("Network error calling Paystack: " + e.getMessage(), e);
        }
    }

    private <T> PaystackApiResponse.Envelope<T> parseEnvelope(
            String json,
            TypeReference<PaystackApiResponse.Envelope<T>> typeRef
    ) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("Failed to parse Paystack response. Response body: {}", json, e);

            throw new PaystackException(
                    "Unexpected Paystack response format",
                    e
            );
        }
    }

    private void assertSuccess(PaystackApiResponse.Envelope<?> envelope, String context) {
        if (!envelope.isStatus()) {
            throw new PaystackException(context + ": " + envelope.getMessage());
        }
    }

    private void assertHttpSuccess(HttpResponse<String> response, String path) {
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            log.error("Paystack HTTP error {} on {}: {}", code, path, response.body());
            throw new PaystackException(
                    "Paystack returned HTTP " + code + " for " + path + ": " + response.body());
        }
    }
}
