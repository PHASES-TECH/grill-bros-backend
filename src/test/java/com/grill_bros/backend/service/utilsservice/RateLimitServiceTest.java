package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.filter.RateLimitFilter;
import com.grill_bros.backend.records.RateLimitPolicy;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class RateLimitServiceTest {

    private  RateLimiterService rateLimiterService;
    private  RateLimitPolicyProvider policyProvider;
    private  RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        // Use the real service so we test actual token-bucket behaviour
        rateLimiterService = new RateLimiterService();
        policyProvider     = mock(RateLimitPolicyProvider.class);
        filter             = new RateLimitFilter(rateLimiterService, policyProvider);

        when(policyProvider.getPolicy(anyString()))
                .thenReturn(new RateLimitPolicy(3, Duration.ofMinutes(1)));
    }

    @Test
    void requestsBelowLimitPassThrough() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = doRequest("1.2.3.4", "/auth/login");
            assertThat(response.getStatus())
                    .as("request %d should pass through", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    void requestOverLimitReturns429() throws Exception {
        for (int i = 0; i < 3; i++) {
            doRequest("1.2.3.4", "/auth/login");
        }

        MockHttpServletResponse response = doRequest("1.2.3.4", "/auth/login");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void blockedResponseHasRetryAfterHeader() throws Exception {
        for (int i = 0; i < 3; i++) {
            doRequest("1.2.3.4", "/auth/login");
        }

        MockHttpServletResponse response = doRequest("1.2.3.4", "/auth/login");
        assertThat(response.getHeader("Retry-After"))
                .as("429 response must include Retry-After header")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void blockedResponseBodyContainsErrorMessage() throws Exception {
        for (int i = 0; i < 3; i++) {
            doRequest("1.2.3.4", "/auth/login");
        }

        MockHttpServletResponse response = doRequest("1.2.3.4", "/auth/login");
        assertThat(response.getContentAsString())
                .contains("TOO_MANY_REQUESTS");
    }

    @Test
    void differentIpsHaveIndependentLimits() throws Exception {
        // Exhaust IP A
        for (int i = 0; i < 3; i++) {
            doRequest("1.1.1.1", "/auth/login");
        }

        // IP B must still be allowed
        MockHttpServletResponse response = doRequest("2.2.2.2", "/auth/login");
        assertThat(response.getStatus())
                .as("different IP should have its own bucket")
                .isNotEqualTo(429);
    }

    @Test
    void xForwardedForIsUsedWhenPresent() throws Exception {
        // Exhaust using real client IP via X-Forwarded-For
        for (int i = 0; i < 3; i++) {
            doRequestWithXff("proxy-ip", "real-client-ip", "/auth/login");
        }

        // Same real client IP should be blocked
        MockHttpServletResponse response =
                doRequestWithXff("proxy-ip", "real-client-ip", "/auth/login");
        assertThat(response.getStatus()).isEqualTo(429);

        // A different real client behind the same proxy must not be blocked
        MockHttpServletResponse otherResponse =
                doRequestWithXff("proxy-ip", "other-client-ip", "/auth/login");
        assertThat(otherResponse.getStatus())
                .as("different real client should have its own bucket")
                .isNotEqualTo(429);
    }

    @Test
    void actuatorPathIsExcluded() throws Exception {
        // Drive way past any limit
        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse response = doRequest("1.2.3.4", "/actuator/health");
            assertThat(response.getStatus())
                    .as("actuator must never be rate-limited")
                    .isNotEqualTo(429);
        }
    }

    @Test
    void swaggerPathIsExcluded() throws Exception {
        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse response = doRequest("1.2.3.4", "/swagger-ui/index.html");
            assertThat(response.getStatus()).isNotEqualTo(429);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MockHttpServletResponse doRequest(String remoteAddr, String path) throws Exception {
        MockHttpServletRequest request  = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr(remoteAddr);

        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse doRequestWithXff(
            String remoteAddr, String realClientIp, String path) throws Exception {

        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", realClientIp + ", " + remoteAddr);

        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
