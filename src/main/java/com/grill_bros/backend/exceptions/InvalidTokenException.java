package com.grill_bros.backend.exceptions;

public class InvalidTokenException extends AppException {
    public InvalidTokenException() {
        super("INVALID_TOKEN", "The invitation token is has been used");
    }
}
