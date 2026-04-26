package com.grill_bros.backend.exceptions;

public class RateLimitException extends AppException {
    public RateLimitException() {
        super("RATE_LIMIT", "You have reached the maximum rate limit for your payment.");
    }
}
