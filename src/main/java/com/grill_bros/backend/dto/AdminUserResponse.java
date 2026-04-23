package com.grill_bros.backend.dto;

import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.Role;
import lombok.Data;

import java.time.Instant;

public class AdminUserResponse {

    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private boolean isVerified;
    private boolean isPhoneNumberVerified;
    private boolean active;
    private Instant lastLoginAt;

    public AdminUserResponse(
            String id,
            String fullName,
            String email,
            String phoneNumber,
            Role role,
            boolean isVerified,
            boolean isPhoneNumberVerified,
            boolean active,
            Instant lastLoginAt
    ) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.isVerified = isVerified;
        this.isPhoneNumberVerified = isPhoneNumberVerified;
        this.active = active;
        this.lastLoginAt = lastLoginAt;
    }

    public static AdminUserResponse from(Users user) {
        return new AdminUserResponse(
                user.getId().toString(), // ⚠️ if ULID → String, if UUID → convert toString()
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isVerified(),
                user.isPhoneNumberVerified(),
                user.isActive(),
                user.getLastLoginAt()
        );
    }

    // Getters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public Role getRole() { return role; }
    public boolean isVerified() { return isVerified; }
    public boolean isPhoneNumberVerified() { return isPhoneNumberVerified; }
    public boolean isActive() { return active; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
