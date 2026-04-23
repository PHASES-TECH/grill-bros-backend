package com.grill_bros.backend.records;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING    -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED  -> next == PREPARING || next == CANCELLED;
            case PREPARING  -> next == READY;
            case READY      -> next == COMPLETED;
            case COMPLETED,
                 CANCELLED  -> false;
        };
    }
}
