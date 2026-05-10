package com.grill_bros.backend.records;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    COMPLETED,
    DELIVERED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING    -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED  -> next == PREPARING || next == CANCELLED;
            case PREPARING  -> next == READY || next == CANCELLED;
            case READY      -> next == COMPLETED || next  == DELIVERED || next == CANCELLED;
            case COMPLETED,
                 DELIVERED,
                 CANCELLED  -> false;
        };
    }
}
