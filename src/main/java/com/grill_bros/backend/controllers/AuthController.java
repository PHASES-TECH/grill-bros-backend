package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.dto.*;
import com.grill_bros.backend.dto.authentication.OtpResponse;
import com.grill_bros.backend.dto.authentication.UserAuthenticationRequest;
import com.grill_bros.backend.dto.authentication.VerifyOtpRequest;
import com.grill_bros.backend.model.RefreshToken;
import com.grill_bros.backend.model.UserPrincipal;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.service.authenticationservice.AuthenticationOtpService;
import com.grill_bros.backend.service.jwtservice.JWTService;
import com.grill_bros.backend.service.jwtservice.RefreshTokenService;
import com.grill_bros.backend.service.userservice.UserService;
import com.grill_bros.backend.service.usersessionservice.UserSessionService;
import com.grill_bros.backend.utils.CookieUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserSessionService userSessionService;
    private final AuthenticationOtpService authenticationOtpService;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody UserEmailLoginDto loginRequest) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getEmail(),
                                loginRequest.getPassword()
                        )
                );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = userService.verify(loginRequest);
        Users user = userPrincipal.getUser();
        Users loggedInUser = userService.findUserWithContext(user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        userSessionService.createSession(user.getEmail(), accessToken);

        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
//                .sameSite("SameSite")
                .path("/")
//                .domain("localhost")
                .maxAge(Duration.ofMinutes(30))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
//                .sameSite("SameSite")
                .path("/")
//                .domain("localhost")
                .maxAge(Duration.ofHours(24))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(AuthResponse.builder()
                        .message("Login successful")
                        .user(UserResponseDto.from(loggedInUser))
                        .build()
                );

//        return ResponseEntity.ok(
//                new LoginResponse(accessToken, refreshToken.getToken(), UserResponseDto.from(user))
//        );
    }

    @PostMapping("auth/register/admin")
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OtpResponse>> registerAdmin(@RequestBody AdminRequestDto adminRequestDto) {
        OtpResponse otpResponse = userService.registerUser(adminRequestDto);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        otpResponse,
                        "OTP sent"
                )
        );
    }

    @PostMapping("auth/register/verify-phone")
    public ResponseEntity<?> verifyRegisteredUserPhoneNumber(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        userService.verifyUserOtp(verifyOtpRequest);
        return ResponseEntity.ok("Phone number verified successfully");
    }

    @PostMapping("auth/login/request-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> requestOtp(
            @RequestBody UserAuthenticationRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        authenticationOtpService.requestUserAuthentication(request),
                        "OTP sent"
                )
        );
    }

    @PostMapping("auth/login/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        String accessToken = userService.verifyOtpAndLogin(request);
        Users user = userService.findUserbyPhone(request.getPhoneNumber());
        user.setLastLoginAt(Instant.now());

        Users loggedInUser = userService.findUserWithContext(user.getId());

        userSessionService.createSession(user.getEmail(), accessToken);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
//                .sameSite("SameSite")
                .path("/")
//                .domain("localhost")
                .maxAge(Duration.ofHours(24))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
//                .sameSite("SameSite")
                .path("/")
//                .domain("localhost")
                .maxAge(Duration.ofDays(30))
                .build();


        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(AuthResponse.builder()
                        .message("Login successful")
                        .user(UserResponseDto.from(loggedInUser))
                        .build()
                );
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request, Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).body("No authentication");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Users user = userPrincipal.getUser();
        Users loggedInUser = userService.findUserWithContext(user.getId());
        return ResponseEntity.ok(UserResponseDto.from(loggedInUser));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {

//        String accessToken = cookieUtil.extractToken(request);

        String refreshTokenValue = cookieUtil.extractRefreshToken(request);

        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            throw new RuntimeException("Refresh token is missing");
        }

        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

        Users user = refreshToken.getUser();

        refreshTokenService.delete(refreshToken);

        String newAccessToken = jwtService.generateToken(user);

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

//        userSessionService.rotateSession(accessToken, newAccessToken);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Token refreshed successfully"));
    }

    @PermitAll
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String accessToken = cookieUtil.extractToken(request);
        String refreshTokenValue = cookieUtil.extractRefreshToken(request);

        if (accessToken != null && !accessToken.isEmpty()) {
            userSessionService.endSession(accessToken);
        }

        ResponseCookie deleteAccess = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
//                .sameSite("SameSite")
                .path("/")
//                .domain("localhost")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
//                .sameSite("SameSite")
                .path("/")
//                .domain("localhost")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());

        return ResponseEntity.noContent().build();
    }
}
