package com.grill_bros.backend.dto.passwordreset;

import lombok.Data;

@Data
public class VerifyOtpRequestPasswordReset {
    private String email;
    private String otp;
}
