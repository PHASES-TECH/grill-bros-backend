package com.grill_bros.backend.records;

import java.time.Duration;

public record RateLimitPolicy(
        int maxRequests,
        Duration window
) {
}
