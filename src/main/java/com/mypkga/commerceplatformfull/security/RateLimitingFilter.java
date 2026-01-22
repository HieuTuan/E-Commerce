package com.mypkga.commerceplatformfull.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypkga.commerceplatformfull.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 20;
    private static final long WINDOW_SIZE_MILLIS = 60 * 1000; // 1 minute

    private final ConcurrentHashMap<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitingFilter() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {

        // Temporarily disable rate limiting to fix login/logout issues
        filterChain.doFilter(request, response);
        return;
        
        /*
        String clientId = getClientIdentifier(request);
        String requestUri = request.getRequestURI();
        
        // Skip rate limiting for static resources
        if (isStaticResource(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Different limits for different endpoints
        int maxRequests = getMaxRequestsForEndpoint(requestUri);
        
        if (isRateLimited(clientId, maxRequests)) {
            handleRateLimitExceeded(request, response, clientId);
            return;
        }

        filterChain.doFilter(request, response);
        */
    }

    private boolean isStaticResource(String uri) {
        return uri.startsWith("/css/") || 
               uri.startsWith("/js/") || 
               uri.startsWith("/images/") || 
               uri.startsWith("/files/") ||
               uri.equals("/favicon.ico") ||
               uri.startsWith("/logout") ||
               uri.startsWith("/error");
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Use IP address as client identifier
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private int getMaxRequestsForEndpoint(String uri) {
        if (uri.contains("/login") || uri.contains("/register")) {
            return MAX_LOGIN_ATTEMPTS_PER_MINUTE;
        }
        return MAX_REQUESTS_PER_MINUTE;
    }

    private boolean isRateLimited(String clientId, int maxRequests) {
        long currentTime = System.currentTimeMillis();
        
        requestCounts.compute(clientId, (key, window) -> {
            if (window == null || currentTime - window.windowStart > WINDOW_SIZE_MILLIS) {
                return new RequestWindow(currentTime, new AtomicInteger(1));
            } else {
                window.requestCount.incrementAndGet();
                return window;
            }
        });

        RequestWindow window = requestCounts.get(clientId);
        return window != null && window.requestCount.get() > maxRequests;
    }

    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
            String clientId) throws IOException {
        
        log.warn("Rate limit exceeded for client: {} on path: {}", clientId, request.getRequestURI());

        try {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now().toString())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("Too Many Requests")
                    .message("Rate limit exceeded. Please try again later.")
                    .path(request.getRequestURI())
                    .errorCode("RATE_LIMIT_EXCEEDED")
                    .build();

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } catch (Exception e) {
            log.error("Error writing rate limit response", e);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }

    private static class RequestWindow {
        final long windowStart;
        final AtomicInteger requestCount;

        RequestWindow(long windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}