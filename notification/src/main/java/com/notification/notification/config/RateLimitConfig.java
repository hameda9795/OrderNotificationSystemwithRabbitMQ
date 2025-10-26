package com.notification.notification.config;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rate limiting configuration to prevent abuse and DoS attacks.
 * Uses Guava's RateLimiter for in-memory rate limiting.
 *
 * For distributed systems, consider using Redis-based rate limiting.
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    /**
     * Global rate limiter allowing 100 requests per second.
     * Adjust based on your system capacity and requirements.
     *
     * @return Configured RateLimiter instance
     */
    @Bean
    public RateLimiter globalRateLimiter() {
        return RateLimiter.create(100.0); // 100 requests per second
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitingInterceptor(globalRateLimiter()));
    }
}

/**
 * Interceptor that enforces rate limiting on HTTP requests.
 */
@Component
class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    public RateLimitingInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Skip rate limiting for health checks
        if (request.getRequestURI().contains("/actuator/health")) {
            return true;
        }

        if (!rateLimiter.tryAcquire()) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
            response.setContentType("application/json");
            return false;
        }

        return true;
    }
}
