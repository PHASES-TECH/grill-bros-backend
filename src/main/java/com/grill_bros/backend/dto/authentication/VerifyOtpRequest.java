package com.grill_bros.backend.dto.authentication;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String phoneNumber;
    private String otp;
}
