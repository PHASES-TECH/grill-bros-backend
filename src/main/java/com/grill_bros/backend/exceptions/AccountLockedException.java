package com.grill_bros.backend.exceptions;

public class AccountLockedException extends AppException {
    public AccountLockedException() {
        super("ACCOUNT_LOCKED", "Your account has been locked due to too many failed login attempts");
    }
}
