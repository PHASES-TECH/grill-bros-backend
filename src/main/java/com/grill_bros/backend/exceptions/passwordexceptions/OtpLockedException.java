package com.grill_bros.backend.exceptions.passwordexceptions;

public class OtpLockedException extends RuntimeException {
    public OtpLockedException(String message) {
        super(message);
    }
}
