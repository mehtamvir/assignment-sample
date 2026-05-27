/**
 * Perform AI Backend — Rate Limit Filter
 *
 * Enforces per-IP rate limiting on unauthenticated public endpoints
 * to prevent abuse, brute force, and DoS attacks.
 *
 * OWASP A04 — Insecure Design: rate limiting as a compensating control
 * for unauthenticated endpoints.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter that applies a token bucket rate limit per client IP address.
 *
 * Policy: 20 requests per minute per IP.
 * Exceeding the limit returns 429 Too Many Requests with a Retry-After header.
 * Only applies to /api/v1/ paths — all other paths pass through unaffected.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Max requests allowed per IP per minute. */
    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    /** One bucket per client IP. */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only rate-limit the public API paths
        if (!request.getRequestURI().startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", "60");
            response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Max "
                + MAX_REQUESTS_PER_MINUTE + " requests per minute.\"}"
            );
        }
    }

    /**
     * Creates a new token bucket that refills to MAX_REQUESTS_PER_MINUTE every 60 seconds.
     *
     * @return configured Bucket instance
     */
    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS_PER_MINUTE)
                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the real client IP, respecting X-Forwarded-For for reverse proxy deployments.
     * Falls back to getRemoteAddr() if the header is absent.
     *
     * @param request the incoming HTTP request
     * @return resolved client IP address
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // first entry is the original client IP
        }
        return request.getRemoteAddr();
    }
}
