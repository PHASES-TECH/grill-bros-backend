package com.grill_bros.backend.service.paymentservice;

import com.grill_bros.backend.dto.paymentdto.PaystackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSignatureValidator {

    private final PaystackProperties props;

    private static final String HMAC_ALGORITHM = "HmacSHA512";

    public boolean isValid(String rawBody, String signatureHeader) {
        if (rawBody == null || signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Webhook rejected: missing body or signature header");
            return false;
        }

        try {
            String expected = computeHmac(rawBody, props.getWebhookSecret());
            boolean valid   = expected.equalsIgnoreCase(signatureHeader);

            if (!valid) {
                log.warn("Webhook signature mismatch. expected={} received={}",
                        expected.substring(0, 16) + "...",
                        signatureHeader.substring(0, Math.min(16, signatureHeader.length())) + "...");
            }
            return valid;

        } catch (Exception e) {
            log.error("Webhook signature validation error: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmac(String data, String secret) throws Exception {
        Mac        mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
