package com.grill_bros.backend.records;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
//    READY,
    COMPLETED,
    PAYMENT_FAILED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING    -> next == CONFIRMED || next == CANCELLED || next == PAYMENT_FAILED || next == EXPIRED;
            case CONFIRMED  -> next == PREPARING || next == CANCELLED || next == PAYMENT_FAILED;
            case PREPARING  -> next == COMPLETED || next == CANCELLED;
//            case READY      -> next == COMPLETED || next  == DELIVERED || next == CANCELLED;
            case COMPLETED,
                 CANCELLED, PAYMENT_FAILED, EXPIRED  -> false;
        };
    }
}
