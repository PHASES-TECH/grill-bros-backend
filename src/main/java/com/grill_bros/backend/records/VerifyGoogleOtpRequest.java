package com.grill_bros.backend.records;

public record VerifyGoogleOtpRequest(
        String email,
        String otp
) {
}
