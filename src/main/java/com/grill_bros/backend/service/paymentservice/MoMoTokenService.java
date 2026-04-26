package com.grill_bros.backend.service.paymentservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class MoMoTokenService {

    private String cachedToken;
    private Instant expiresAt;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mtn.momo.primary.key}")
    private String subscriptionKey;

    @Value("${api.user.key}")
    private String apiUser;

    @Value("${mtn.momo.api.key}")
    private String apiKey;

    public String getToken() {
        if (cachedToken == null || isExpired()) {
            refreshToken();
        }
        return cachedToken;
    }

    private boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }

    private void refreshToken() {
        String url = "https://sandbox.momodeveloper.mtn.com/collection/token/";

        String credentials = apiUser + ":" + apiKey;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedCredentials);
        headers.set("Ocp-Apim-Subscription-Key", subscriptionKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();

        if (body == null || !body.containsKey("access_token")) {
            throw new RuntimeException("Failed to retrieve MoMo token");
        }

        this.cachedToken = (String) body.get("access_token");

        Integer expiresIn = (Integer) body.getOrDefault("expires_in", 3600);

        this.expiresAt = Instant.now().plusSeconds(expiresIn - 60);
    }
}
