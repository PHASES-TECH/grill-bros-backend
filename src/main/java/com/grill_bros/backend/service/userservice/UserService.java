package com.grill_bros.backend.service.userservice;

import com.grill_bros.backend.dto.AdminRequestDto;
import com.grill_bros.backend.dto.UserEmailLoginDto;
import com.grill_bros.backend.dto.authentication.OtpResponse;
import com.grill_bros.backend.dto.authentication.UserAuthenticationRequest;
import com.grill_bros.backend.dto.authentication.VerifyOtpRequest;
import com.grill_bros.backend.dto.usersdto.UsersDto;
import com.grill_bros.backend.exceptions.AccountDisabledException;
import com.grill_bros.backend.exceptions.AccountLockedException;
import com.grill_bros.backend.exceptions.InvalidCredentialsException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.exceptions.passwordexceptions.InvalidOtpException;
import com.grill_bros.backend.model.UserAuthenticationOtp;
import com.grill_bros.backend.model.UserPrincipal;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.repository.OtpRepository;
import com.grill_bros.backend.repository.UserRepository;
import com.grill_bros.backend.service.authenticationservice.AuthenticationOtpService;
import com.grill_bros.backend.service.jwtservice.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;

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
        Users existingUser = userRepository.findByPhoneNumber(request.getPhoneNumber());

        if (existingUser != null) {
            throw new IllegalArgumentException("Account already exists");
        }

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

        return authenticationOtpService.requestUserAuthentication(authRequest);
    }

    public void verifyUserOtp(VerifyOtpRequest request) {
        authenticationOtpService.verifyOtp(request);

        Users user = userRepository.findByPhoneNumber(request.getPhoneNumber());

        user.setPhoneNumberVerified(true);

        userRepository.save(user);
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

    public Users findUserWithContext(UUID userId){
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user;
    }

    public Users findUserbyPhone(String phoneNumber) {
        Users user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        return user;
    }

    @Transactional
    public String verifyOtpAndLogin(VerifyOtpRequest request) {

        authenticationOtpService.verifyOtp(request);

        Users user = userRepository.findByPhoneNumber(request.getPhoneNumber());

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
}

