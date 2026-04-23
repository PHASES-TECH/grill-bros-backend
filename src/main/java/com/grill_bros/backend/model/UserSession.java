package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    private String email;

    @Column(name = "token", length = 500)
    private String token; // JWT token or session ID

    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private LocalDateTime logoutTime;
    private Boolean isActive;

    private LocalDateTime expiresAt;

}
