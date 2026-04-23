package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByTokenAndIsActiveTrue(String token);
    List<UserSession> findByEmailAndIsActiveTrue(String email);
    List<UserSession> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime dateTime);
    void deleteByEmail(String email);
    void deleteByLogoutTimeBefore(LocalDateTime dateTime);
}

