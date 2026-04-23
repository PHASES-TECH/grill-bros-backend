package com.grill_bros.backend.records;

public enum Role {
    SUPER_ADMIN,
    REGULAR_ADMIN;

    public boolean isSuperAdmin() { return this == SUPER_ADMIN; }
}
