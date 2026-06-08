package com.grill_bros.backend.model;

import com.grill_bros.backend.records.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_authentication_otp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthenticationOtp extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private String otp;

    private String phoneNumber;

    private String email;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Boolean isUsed = false;

    private Instant usedAt;

    @Column(nullable = false)
    private Integer attemptCount = 0;

    @Column(nullable = false)
    private Boolean isLocked = false;

    public boolean isUsed() {
        return this.isUsed;
    }
}

