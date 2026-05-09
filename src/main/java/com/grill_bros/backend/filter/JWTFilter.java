package com.grill_bros.backend.filter;

import com.grill_bros.backend.service.jwtservice.JWTService;
import com.grill_bros.backend.service.userservice.MyUserDetailsService;
import com.grill_bros.backend.service.usersessionservice.UserSessionService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    @Autowired
    private JWTService jwtService;

    private final UserSessionService userSessionService;

    @Autowired
    private ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = null;
        String username = null;

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();

        if (path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/login/request-otp")
                || path.equals("/api/v1/auth/login/verify-otp")
                || path.equals("/api/v1/auth/register/admin")
                || path.equals("/api/v1/auth/logout")
                || path.equals("/api/v1/auth/register/verify-phone")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/forgot-password")
                || path.equals("/api/v1/auth/password/verify-otp")
                || path.equals("/api/v1/auth/reset-password")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT from cookie
        token = extractTokenFromCookie(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenExpired(token)) {
                ResponseCookie clear = ResponseCookie.from("access_token", "")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("None")
                        .path("/")
                        .maxAge(0)
                        .build();

                response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("""
                            {
                              "error": "TOKEN_EXPIRED",
                              "message": "Session expired. Please sign in again."
                            }
                        """);
                return;
            }

        } catch (JwtException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                        {
                          "error": "INVALID_TOKEN",
                          "message": "Invalid authentication token."
                        }
                    """);

            response.getWriter().flush();
            return;
        }

        username = jwtService.extractUserName(token);

        userSessionService.updateLastActivity(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = context.getBean(MyUserDetailsService.class).loadUserByUsername(username);
            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}


