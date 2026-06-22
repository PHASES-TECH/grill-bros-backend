package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.records.RateLimitPolicy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class RateLimitPolicyProvider {

    private final Map<String, RateLimitPolicy> policies = Map.of(
            "/api/v1/auth/login",
            new RateLimitPolicy(5, Duration.ofMinutes(1)),

            "/auth/google",
            new RateLimitPolicy(5, Duration.ofMinutes(1)),

            "/auth/google/verify-otp",
            new RateLimitPolicy(5, Duration.ofMinutes(10)),

            "/auth/reset-password",
            new RateLimitPolicy(3, Duration.ofMinutes(10)),

            "/auth/password/verify-otp",
            new RateLimitPolicy(5, Duration.ofMinutes(10)),

            "/auth/login/request-otp",
            new RateLimitPolicy(5, Duration.ofMinutes(1)),

            "/auth/login/verify-otp",
            new RateLimitPolicy(5, Duration.ofMinutes(10))
    );

    public RateLimitPolicy getPolicy(String path) {
        return policies.getOrDefault(
                path,
                new RateLimitPolicy(100, Duration.ofMinutes(1))
        );
    }
}
