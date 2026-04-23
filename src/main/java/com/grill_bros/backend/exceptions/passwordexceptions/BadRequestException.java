package com.grill_bros.backend.exceptions.passwordexceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
