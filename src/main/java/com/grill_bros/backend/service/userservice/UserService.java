package com.grill_bros.backend.service.userservice;

import com.grill_bros.backend.dto.AdminRequestDto;
import com.grill_bros.backend.dto.UserEmailLoginDto;
import com.grill_bros.backend.dto.authentication.OtpResponse;
import com.grill_bros.backend.dto.authentication.UserAuthenticationRequest;
import com.grill_bros.backend.dto.authentication.VerifyOtpRequest;
import com.grill_bros.backend.dto.usersdto.UsersDto;
import com.grill_bros.backend.exceptions.*;
import com.grill_bros.backend.exceptions.passwordexceptions.InvalidOtpException;
import com.grill_bros.backend.model.UserAuthenticationOtp;
import com.grill_bros.backend.model.UserPrincipal;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.GoogleLoginRequest;
import com.grill_bros.backend.records.VerifyGoogleOtpRequest;
import com.grill_bros.backend.repository.OtpRepository;
import com.grill_bros.backend.repository.UserRepository;
import com.grill_bros.backend.service.authenticationservice.AuthenticationOtpService;
import com.grill_bros.backend.service.authenticationservice.GoogleAuthService;
import com.grill_bros.backend.service.jwtservice.JWTService;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import com.grill_bros.backend.service.utilsservice.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final SmsProviderService smsProviderService;
    private final GoogleAuthService googleAuthService;
    private final EmailService emailService;


    @Autowired
    private AuthenticationManager authManager;

    private final AuthenticationOtpService authenticationOtpService;

    private final JWTService jwtService;
    private final AuthContextService authContextService;

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);

    public Users toEntity(UsersDto dto) {
        Users user = new Users();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPassword());
        return user;
    }


    @Transactional
    public OtpResponse registerUser(AdminRequestDto request) {
        userRepository.findByPhoneNumber(request.getPhoneNumber())
                .ifPresent(user -> {
                    throw new DuplicateResourceException("User already exists");
                });

        Users newUser = new Users();
        newUser.setEmail(request.getEmail());
        newUser.setFullName(request.getFullName());
        newUser.setPhoneNumber(request.getPhoneNumber());
        newUser.setVerified(false);
        newUser.setRole(request.getRole());
        newUser.setPasswordHash(bCryptPasswordEncoder.encode(request.getPassword()));
        newUser.setPhoneNumberVerified(false);

        userRepository.save(newUser);

        UserAuthenticationRequest authRequest = new UserAuthenticationRequest();
        authRequest.setPhoneNumber(request.getPhoneNumber());

        String message = String.format(
                "Welcome to Grill Bros! You’ve been added as an admin. Sign in using your phone number (%s) to start managing orders and operations.",
                request.getPhoneNumber()
        );

        smsProviderService.sendSms(List.of(request.getPhoneNumber()), message);

        return authenticationOtpService.requestUserAuthentication(authRequest);
    }

    public void verifyUserOtp(VerifyOtpRequest request) {
        authenticationOtpService.verifyOtp(request);

        Users user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPhoneNumberVerified(true);

        userRepository.save(user);
    }

    public String verifyUserOtpGoogleLogin(VerifyGoogleOtpRequest request) {
        authenticationOtpService.verifyGoogleLoginOtp(request);

        Users user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return jwtService.generateToken(user);
    }


    public void verifyNewUser(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setVerified(true);
        userRepository.save(user);
    }

//    public List<AdminResponseDto> getAllUsers() {
//        Users admin = authContextService.getCurrentUser();
//
//        return switch (admin.getRole()) {
//
//            case SUPER_ADMIN ->
//                    userRepository.findAllAdmins()
//                            .stream()
//                            .map(UserMapper::toDto)
//                            .toList();
//
//            default -> throw new AccessDeniedException("Not allowed");
//        };
//    }

    public Users findUserWithContext(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user;
    }

    public Users findUserbyPhone(String phoneNumber) {
        Users user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return user;
    }

    public Users findUserbyEmail(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return user;
    }

    @Transactional
    public String verifyOtpAndLogin(VerifyOtpRequest request) {

        authenticationOtpService.verifyOtp(request);

        Users user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        UserAuthenticationOtp otpRecord =
                otpRepository.findByOtpAndPhoneNumberAndIsUsedFalseAndIsLockedFalse(request.getOtp(), request.getPhoneNumber())
                        .orElseThrow(() -> new InvalidOtpException("Invalid OTP"));

        otpRecord.setIsUsed(true);
        otpRepository.save(otpRecord);

        return jwtService.generateToken(user);
    }

    @Transactional
    public String verify(UserEmailLoginDto loginRequest) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Users user = userPrincipal.getUser();

            // Check if user account is locked or disabled
//            if (!user.isEnabled()) {
//                throw new AccountDisabledException();
//            }
//
//            if (user.isLocked()) {
//                throw new AccountLockedException();
//            }

            return jwtService.generateToken(user);

        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        } catch (DisabledException e) {
            throw new AccountDisabledException();
        } catch (LockedException e) {
            throw new AccountLockedException();
        } catch (AuthenticationException e) {
            // Catch any other authentication exceptions
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public String googleLoginSendOtp(GoogleLoginRequest request) throws Exception {

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

        UserAuthenticationOtp loginOtp = new UserAuthenticationOtp();
        loginOtp.setEmail(email);
        loginOtp.setOtp(otp);
        loginOtp.setExpiresAt(Instant.now().plusSeconds(600));

        otpRepository.save(loginOtp);

        emailService.sendOtpEmail(email, user.getFullName(), otp);

        return "OTP sent successfully";
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}

