package com.grill_bros.backend.records;

public enum Role {
    SUPER_ADMIN,
    REGULAR_ADMIN,
    USER;

    public boolean isSuperAdmin() { return this == SUPER_ADMIN; }
}
