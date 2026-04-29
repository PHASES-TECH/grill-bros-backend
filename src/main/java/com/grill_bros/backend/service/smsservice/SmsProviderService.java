package com.grill_bros.backend.service.smsservice;

import com.grill_bros.backend.dto.bulksms.SmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsProviderService {

    @Value("${sms.provider.api.url}")
    private String smsApiUrl;

    @Value("${sms.provider.api.key}")
    private String smsApiKey;

    @Value("${sms.provider.sender.id}")
    private String senderId;

    private final RestTemplate restTemplate;

    public SmsResponse sendSms(List<String> phoneNumbers, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // mNotify API key goes in the URL as a query param, not the header
            String url = smsApiUrl + "?key=" + smsApiKey;

            // Conform to mNotify payload structure
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("recipient", phoneNumbers);
            requestBody.put("sender", senderId);
            requestBody.put("message", message);
            requestBody.put("is_schedule", false);
            requestBody.put("schedule_date", "");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                return SmsResponse.builder()
                        .success(true)
                        .messageId(body.get("code").toString()) // mNotify returns "code"
                        .build();
            } else {
                return SmsResponse.builder()
                        .success(false)
                        .errorMessage("SMS provider returned status: " + response.getStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumbers, e.getMessage());
            return SmsResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        // Already in correct format (233xxxxxxxxx)
        if (cleaned.startsWith("233") && cleaned.length() >= 12) {
            return cleaned;
        }

        // Remove leading 0 if present
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }

        // Add Ghana country code
        return "233" + cleaned;
    }
}
