package com.grill_bros.backend.exceptions;

public class AccountDisabledException extends AppException {
    public AccountDisabledException() {
        super("ACCOUNT_LOCKED", "Your account has been locked due to too many failed login attempts");
    }
}
