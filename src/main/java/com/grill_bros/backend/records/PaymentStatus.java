package com.grill_bros.backend.records;

public enum PaymentStatus {
    INITIATED,
    PENDING,
    SUCCESSFUL,
    FAILED,
    CANCELLED,
    TIMEOUT;

    public boolean isTerminal() {
        return this == SUCCESSFUL || this == FAILED
                || this == CANCELLED  || this == TIMEOUT;
    }

    public boolean isSuccessful() {
        return this == SUCCESSFUL;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case INITIATED -> next == PENDING || next == FAILED || next == TIMEOUT;
            case PENDING   -> next == SUCCESSFUL || next == FAILED
                    || next == CANCELLED  || next == TIMEOUT;
            default        -> false;
        };
    }
}
