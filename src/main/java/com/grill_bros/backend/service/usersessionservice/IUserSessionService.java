package com.grill_bros.backend.service.usersessionservice;

import com.grill_bros.backend.model.UserSession;

import java.util.List;

public interface IUserSessionService {
    UserSession createSession(String email, String token);
    void updateLastActivity(String token);
    void endSession(String token);
    List<UserSession> getActiveSessions(String email);
    void endAllUserSessions(String email);
    String getClientIp();

}
