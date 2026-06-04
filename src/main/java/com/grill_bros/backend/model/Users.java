package com.grill_bros.backend.model;

import com.grill_bros.backend.records.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Table(name = "admins", uniqueConstraints = {
        @UniqueConstraint(name = "uk_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_phone_number", columnNames = "phoneNumber")
})
@Setter
@NoArgsConstructor
@Entity
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(unique = true)
    private String googleId;

    private Instant lastGoogleLogin;

    private boolean isVerified;

    private boolean isPhoneNumberVerified;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at")
    protected Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    protected Instant updatedAt;

    public static Users create(String email, String passwordHash,
                                   String fullName, Role role) {
        Users u     = new Users();
        u.email         = email;
        u.passwordHash  = passwordHash;
        u.fullName      = fullName;
        u.role          = role;
        return u;
    }

    public boolean isAdminRole() {
        return this.role != Role.USER;
    }

    public void recordLogin()  { this.lastLoginAt = Instant.now(); }
    public void deactivate()   { this.active = false; }
    public void activate()     { this.active = true;  }

}
