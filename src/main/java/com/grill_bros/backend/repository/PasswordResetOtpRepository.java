package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.PasswordResetOtp;
import com.grill_bros.backend.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, UUID> {

    Optional<PasswordResetOtp> findByOtpAndIsUsedFalseAndIsLockedFalse(String otp);

    List<PasswordResetOtp> findByUserAndIsUsedFalseAndExpiresAtAfter(Users user, Instant instant);

    void deleteByExpiresAtBefore(Instant instant);

    long countByUserAndCreatedAtAfter(Users user, Instant instant);
}
