package com.grill_bros.backend.service.jwtservice;

import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.RefreshToken;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.repository.RefreshTokenRepository;
import com.grill_bros.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepo;
    private final UserRepository userRepo;

    public RefreshToken createRefreshToken(UUID id) {
        Users user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(30, ChronoUnit.DAYS));

        return refreshTokenRepo.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepo.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    public void delete(RefreshToken token) {
        refreshTokenRepo.delete(token);
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {

        if (request.getCookies() == null) {
            throw new RuntimeException("No cookies found in request");
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("Refresh token missing")
                );

    }


    public RefreshToken verifyExpiration(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepo.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    @Transactional
    public void deleteByUser(Users user) {
        refreshTokenRepo.deleteByUser(user);
    }
}
