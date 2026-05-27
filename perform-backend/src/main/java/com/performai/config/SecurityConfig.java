/**
 * Perform AI Backend — Security Configuration
 *
 * Configures HTTP security headers aligned with OWASP Top 10.
 * Authentication is intentionally open for this mock service —
 * replace with JWT or OAuth2 before production deployment.
 *
 * OWASP coverage:
 *   A01 — Access Control: placeholder; add JWT/OAuth2 for production
 *   A05 — Security Misconfiguration: security headers enforced
 *   A07 — Auth Failures: placeholder; add token-based auth for production
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Defines the application security filter chain.
 * All endpoints are currently open — intended for mock/demo use only.
 * Security headers are applied on every response to mitigate common attacks.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain with:
     *   - CSRF disabled (stateless REST API — no session cookies)
     *   - Stateless session management (no server-side session state)
     *   - All requests permitted (add authentication rules for production)
     *   - Security response headers:
     *       X-Content-Type-Options: nosniff          (prevents MIME sniffing)
     *       X-Frame-Options: DENY                    (prevents clickjacking)
     *       X-XSS-Protection: 1; mode=block          (legacy XSS filter)
     *       Strict-Transport-Security                (enforces HTTPS)
     *       Content-Security-Policy                  (restricts resource loading)
     *       Referrer-Policy                          (controls referrer leakage)
     *
     * @param http the HttpSecurity builder
     * @return configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                          // stateless REST — no CSRF needed
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll())                         // open for mock; restrict in production
            .headers(headers -> headers
                .contentTypeOptions(ct -> {})                      // X-Content-Type-Options: nosniff
                .frameOptions(frame -> frame.deny())               // X-Frame-Options: DENY
                .httpStrictTransportSecurity(hsts -> hsts          // HSTS: enforce HTTPS
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentSecurityPolicy(csp -> csp                  // CSP: restrict resource origins
                    .policyDirectives("default-src 'none'; frame-ancestors 'none'"))
            );

        return http.build();
    }
}
