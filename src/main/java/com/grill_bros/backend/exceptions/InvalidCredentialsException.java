package com.grill_bros.backend.exceptions;

public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid email or password");
    }
}
