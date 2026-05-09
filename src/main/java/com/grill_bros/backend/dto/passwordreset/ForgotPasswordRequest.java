package com.grill_bros.backend.dto.passwordreset;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String email;
}
