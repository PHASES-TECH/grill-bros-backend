package com.grill_bros.backend.filter;

import com.grill_bros.backend.records.RateLimitPolicy;
import com.grill_bros.backend.service.utilsservice.RateLimitPolicyProvider;
import com.grill_bros.backend.service.utilsservice.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final RateLimitPolicyProvider policyProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/static")
                || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip   = resolveClientIp(request);
        String path = request.getRequestURI();

        RateLimitPolicy policy = policyProvider.getPolicy(path);
        String key = ip + ":" + path;

        boolean allowed = rateLimiterService.isAllowed(
                key,
                policy.maxRequests(),
                policy.window()
        );

        if (!allowed) {
            long retryAfterSec = policy.window().toSeconds();

            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSec));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {
                  "error": "TOO_MANY_REQUESTS",
                  "message": "Too many requests. Please try again later.",
                  "retryAfterSeconds": %d
                }
                """.formatted(retryAfterSec));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
