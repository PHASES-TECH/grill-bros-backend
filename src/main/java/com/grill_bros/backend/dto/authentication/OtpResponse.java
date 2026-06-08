package com.grill_bros.backend.dto.authentication;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OtpResponse {
    private String message;
    private String phoneNumber;
    private String email;
    private Instant expiresAt;
}
