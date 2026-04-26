package com.grill_bros.backend.service.paymentservice;

import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MoMoClient {

    private final RestClient restClient;
    private final RestTemplate restTemplate;
    private final MoMoTokenService momoTokenService;
    private final PaymentRepository paymentRepository;

    @Value("${mtn.momo.primary.key}")
    private String subscriptionKey;

    @Value("${api.user.key}")
    private String apiUser;

    @Value("${mtn.momo.api.key}")
    private String apikey;

    public void requestToPay(String externalId, String phoneNumber, String amount, String currency) {
        String token = momoTokenService.getToken();

        String url = "https://sandbox.momodeveloper.mtn.com/collection/v1_0/requesttopay";

//        String credentials = apiUser + ":" + apikey;
//        String encoded = Base64.getEncoder()
//                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.add("X-Reference-Id", externalId);
        headers.add("X-Target-Environment", "sandbox");
        headers.add("Ocp-Apim-Subscription-Key", subscriptionKey);

        // Optional but IMPORTANT for async updates
//        headers.add("X-Callback-Url", "https://myapp.com/momo/callback");

        Map<String, Object> body = Map.of(
                "amount", amount,
                "currency", "EUR", // change to GHS in production
                "externalId", externalId,
                "payer", Map.of(
                        "partyIdType", "MSISDN",
                        "partyId", phoneNumber
                ),
                "payerMessage", "Payment request",
                "payeeNote", "Order payment"
        );

        HttpEntity<?> request = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

        if (response.getStatusCode() != HttpStatus.ACCEPTED) {
            throw new RuntimeException("Failed to initiate MoMo payment");
        }
    }

    public String getPaymentStatus(String externalId) {
        String token = momoTokenService.getToken();

        String url = "https://sandbox.momodeveloper.mtn.com/collection/v1_0/requesttopay/" + externalId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.add("X-Target-Environment", "sandbox");
        headers.add("Ocp-Apim-Subscription-Key", subscriptionKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
        );

        return (String) response.getBody().get("status");
    }

    public PaymentStatus getTransactionStatus(String externalId) {
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        return payment.getStatus();
    }
}
