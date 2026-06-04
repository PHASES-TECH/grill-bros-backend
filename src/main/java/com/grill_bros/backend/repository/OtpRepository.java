package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.UserAuthenticationOtp;
import com.grill_bros.backend.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<UserAuthenticationOtp, UUID> {

    Optional<UserAuthenticationOtp> findByOtpAndPhoneNumberAndIsUsedFalseAndIsLockedFalse(
            String otp, String phoneNumber
    );

    Optional<UserAuthenticationOtp> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(
            String email
    );

    List<UserAuthenticationOtp> findByUserAndIsUsedFalseAndExpiresAtAfter(Users user, Instant instant);

    void deleteByExpiresAtBefore(Instant instant);

    long countByUserAndCreatedAtAfter(Users user, Instant instant);
}
