package com.grill_bros.backend.dto.paymentdto;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "paystack")
public class PaystackProperties {

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.public-key}")
    private String publicKey;

    @Value("${paystack.base-url}")
    private String baseUrl      = "https://api.paystack.co";

    @Value("${supabase.url}")
    private String currency     = "GHS";

    /**
     * HMAC-SHA512 key for validating incoming webhook signatures.
     * Must match the secret set in Paystack Dashboard → Settings → Webhooks.
     * Usually the same as secretKey for Paystack.
     */
    private String webhookSecret;

    @Value("${paystack.callback-url")
    private String callbackUrl;
}
