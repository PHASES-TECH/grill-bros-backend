package com.grill_bros.backend.dto.passwordreset;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OtpResponsePasswordReset {
    private String message;
    private String phoneNumber;
    private Instant expiresAt;
}
