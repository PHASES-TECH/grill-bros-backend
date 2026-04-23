package com.grill_bros.backend.service.userservice;

import com.grill_bros.backend.model.UserPrincipal;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthContextService {
    private final UserRepository userRepository;

    public Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getUser();
    }
}
