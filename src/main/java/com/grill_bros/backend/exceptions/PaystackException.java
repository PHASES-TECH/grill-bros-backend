package com.grill_bros.backend.exceptions;

public class PaystackException extends RuntimeException {
    public PaystackException(String message) {
        super(message);
    }

    public PaystackException(String message, Throwable cause) {
        super(message, cause);
    }
}
