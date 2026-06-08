package com.grill_bros.backend.service.authenticationservice;

import com.grill_bros.backend.dto.authentication.OtpResponse;
import com.grill_bros.backend.dto.authentication.UserAuthenticationRequest;
import com.grill_bros.backend.dto.authentication.VerifyOtpRequest;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.exceptions.passwordexceptions.*;
import com.grill_bros.backend.model.UserAuthenticationOtp;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.GoogleLoginRequest;
import com.grill_bros.backend.records.VerifyGoogleOtpRequest;
import com.grill_bros.backend.repository.OtpRepository;
import com.grill_bros.backend.repository.UserRepository;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import com.grill_bros.backend.service.utilsservice.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationOtpService {

    private final UserRepository userRepository;
    private final SmsProviderService smsProviderService;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final GoogleAuthService googleAuthService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RATE_LIMIT_MINUTES = 60; // 1 hour
    private static final int MAX_OTP_PER_HOUR = 10;

    @Transactional
    public OtpResponse requestUserAuthentication(UserAuthenticationRequest request) {
        // Find user by phone number
        Users user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//                .orElseThrow(() -> new ResourceNotFoundException("No account found with this phone number"));

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            throw new BadRequestException("No phone number associated with this account. Please contact support.");
        }

        String phoneNumber = formatPhoneNumber(user.getPhoneNumber());

        checkRateLimit(user);

        invalidatePreviousOtps(user);

        String otp = generateOtp();

        UserAuthenticationOtp otpRecord = UserAuthenticationOtp.builder()
                .user(user)
                .otp(otp)
                .phoneNumber(phoneNumber)
                .expiresAt(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .isUsed(false)
                .attemptCount(0)
                .isLocked(false)
                .build();

        otpRepository.save(otpRecord);

        // Send OTP via SMS
        String message = String.format(
                "Your authentication OTP is: %s. Valid for %d minutes. Do not share this code with anyone.",
                otp,
                OTP_EXPIRY_MINUTES
        );

        smsProviderService.sendSms(List.of(phoneNumber), message);

//        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);

//        log.info("Authentication OTP sent to user: {} (phone: {})", user.getId(), phoneNumber);

        return OtpResponse.builder()
                .message("OTP sent successfully to your phone number")
                .phoneNumber(maskPhoneNumber(phoneNumber))
                .email(user.getEmail())
                .expiresAt(otpRecord.getExpiresAt())
                .build();
    }

    @Transactional
    public OtpResponse googleLoginSendOtp(GoogleLoginRequest request) throws Exception {

        var payload = googleAuthService.verify(request.idToken());

        String email = payload.getEmail();
        String googleId = payload.getSubject();

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // OPTIONAL: remove this unless admin-only login is intended
        // if (!user.isAdminRole()) {
        //     throw new AccessDeniedException("Unauthorized");
        // }

        // Link Google account if not already linked
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }

        // Only enforce if already linked AND mismatch (security check)
        if (user.getGoogleId() != null && !user.getGoogleId().equals(googleId)) {
            throw new AccessDeniedException("Google account mismatch");
        }

        String otp = generateOtp();

        UserAuthenticationOtp otpRecord = UserAuthenticationOtp.builder()
                .user(user)
                .otp(otp)
                .email(user.getEmail())
                .expiresAt(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .isUsed(false)
                .attemptCount(0)
                .isLocked(false)
                .build();

        otpRepository.save(otpRecord);

        emailService.sendOtpEmail(email, user.getFullName(), otp);

        return OtpResponse.builder()
                .message("OTP sent successfully to your phone number")
                .phoneNumber(maskPhoneNumber(user.getPhoneNumber()))
                .email(user.getEmail())
                .expiresAt(otpRecord.getExpiresAt())
                .build();
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {

        Users user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String phoneNumber = formatPhoneNumber(user.getPhoneNumber());

        UserAuthenticationOtp otpRecord = otpRepository.findByOtpAndPhoneNumberAndIsUsedFalseAndIsLockedFalse(request.getOtp(), request.getPhoneNumber())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP"));

        if (!otpRecord.getPhoneNumber().equals(phoneNumber)) {
            throw new InvalidOtpException("Invalid OTP");
        }

        if (otpRecord.getExpiresAt().isBefore(Instant.now())) {
            throw new OtpExpiredException("OTP has expired. Please request a new one.");
        }

        otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);

        if (otpRecord.getAttemptCount() >= MAX_ATTEMPTS) {
            otpRecord.setIsLocked(true);
            otpRepository.save(otpRecord);
            throw new OtpLockedException("Too many failed attempts. Please request a new OTP.");
        }

        otpRepository.save(otpRecord);

        log.info("OTP verified successfully for user: {}", otpRecord.getUser().getId());
    }

    @Transactional
    public void verifyGoogleLoginOtp(VerifyGoogleOtpRequest request) {

        UserAuthenticationOtp otpRecord = otpRepository.findByOtpAndEmailAndIsUsedFalseAndIsLockedFalse(request.email(), request.otp())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP"));

        if (otpRecord.isUsed()) {
            throw new RuntimeException(
                    "OTP already used"
            );
        }

        if (Instant.now().isAfter(
                otpRecord.getExpiresAt()
        )) {
            throw new RuntimeException(
                    "OTP expired"
            );
        }

        // Verify phone number matches
        if (!otpRecord.getEmail().equals(request.email())) {
            throw new InvalidOtpException("Invalid OTP");
        }

        if (otpRecord.getExpiresAt().isBefore(Instant.now())) {
            throw new OtpExpiredException("OTP has expired. Please request a new one.");
        }

        otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);

        if (otpRecord.getAttemptCount() >= MAX_ATTEMPTS) {
            otpRecord.setIsLocked(true);
            otpRepository.save(otpRecord);
            throw new OtpLockedException("Too many failed attempts. Please request a new OTP.");
        }

        otpRecord.setIsUsed(true);

        otpRepository.save(otpRecord);

        log.info("OTP verified successfully for user: {}", otpRecord.getUser().getId());
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private String formatPhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        if (cleaned.startsWith("233") && cleaned.length() >= 12) {
            return cleaned;
        }

        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
            return cleaned;
        }

        return "233" + cleaned;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() < 8) {
            return phoneNumber;
        }

        String prefix = phoneNumber.substring(0, 3);
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "*****" + suffix;
    }

    private void invalidatePreviousOtps(Users user) {
        List<UserAuthenticationOtp> previousOtps = otpRepository
                .findByUserAndIsUsedFalseAndExpiresAtAfter(user, Instant.now());

        previousOtps.forEach(otp -> {
            otp.setIsUsed(true);
            otp.setUsedAt(Instant.now());
        });

        otpRepository.saveAll(previousOtps);
    }

    private void checkRateLimit(Users user) {
        Instant oneHourAgo = Instant.now().minus(RATE_LIMIT_MINUTES, ChronoUnit.MINUTES);
        long recentOtpCount = otpRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);

        if (recentOtpCount >= MAX_OTP_PER_HOUR) {
            throw new RateLimitExceededException(
                    "Too many OTP requests. Please try again after 1 hour."
            );
        }
    }


    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Password must contain at least one digit");
        }
    }
}
