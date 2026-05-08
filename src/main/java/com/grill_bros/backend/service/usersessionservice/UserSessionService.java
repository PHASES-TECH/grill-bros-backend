package com.grill_bros.backend.service.usersessionservice;

import com.grill_bros.backend.model.UserSession;
import com.grill_bros.backend.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSessionService implements IUserSessionService {

    private final UserSessionRepository sessionRepository;
    private final HttpServletRequest request;

    @Transactional
    @Override
    public UserSession createSession(String email, String token) {
        sessionRepository.findByEmailAndIsActiveTrue(email)
                .forEach(existing -> {
                    existing.setIsActive(false);
                    existing.setLogoutTime(LocalDateTime.now());
                });

        UserSession session = new UserSession();
        session.setEmail(email);
        session.setToken(token);
        session.setIpAddress(getClientIp());
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setLoginTime(LocalDateTime.now());
        session.setLastActivityTime(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        session.setIsActive(true);

        return sessionRepository.save(session);
    }

    @Override
    public void updateLastActivity(String token) {
        sessionRepository.findByTokenAndIsActiveTrue(token)
                .ifPresent(session -> {
                    session.setLastActivityTime(LocalDateTime.now());
                    sessionRepository.save(session);
                });
    }

    @Override
    @Transactional
    public UserSession rotateSession(String oldAccessToken, String newAccessToken) {

        UserSession session = sessionRepository
                .findByTokenAndIsActiveTrue(oldAccessToken)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setToken(newAccessToken);
        session.setLastActivityTime(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    @Override
    public void endSession(String token) {
        sessionRepository.findByTokenAndIsActiveTrue(token)
                .ifPresent(session -> {
                    session.setIsActive(false);
                    session.setLogoutTime(LocalDateTime.now());
                    sessionRepository.save(session);
                });
    }

    public boolean isSessionValid(String token) {
        return sessionRepository.findByTokenAndIsActiveTrue(token)
                .map(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public int endExpiredSessions() {
        List<UserSession> expiredSessions = sessionRepository
                .findByIsActiveTrueAndExpiresAtBefore(LocalDateTime.now());

        expiredSessions.forEach(session -> {
            session.setIsActive(false);
            session.setLogoutTime(LocalDateTime.now());
        });

        sessionRepository.saveAll(expiredSessions);

        return expiredSessions.size();
    }

    @Override
    public List<UserSession> getActiveSessions(String email) {
        return sessionRepository.findByEmailAndIsActiveTrue(email);
    }

    @Override
    public void endAllUserSessions(String email) {
        List<UserSession> sessions = sessionRepository.findByEmailAndIsActiveTrue(email);
        sessions.forEach(session -> {
            session.setIsActive(false);
            session.setLogoutTime(LocalDateTime.now());
        });
        sessionRepository.saveAll(sessions);
    }

    @Override
    public String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

