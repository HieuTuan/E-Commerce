package com.mypkga.commerceplatformfull.config;

import com.mypkga.commerceplatformfull.security.CustomUserDetailsService;
import com.mypkga.commerceplatformfull.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import jakarta.servlet.ServletException;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Add rate limiting filter
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            
            // CSRF Protection
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**", "/payment/**") // API endpoints can use other auth methods
            )
            
            // Security Headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            
            // Session Management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(3) // Allow max 3 concurrent sessions per user
                .maxSessionsPreventsLogin(false) // Don't prevent login, invalidate oldest session
                .sessionRegistry(sessionRegistry())
            )
            .sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.changeSessionId()) // Prevent session fixation attacks
                .invalidSessionUrl("/login?expired")
            )
            
            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/files/**", "/favicon.ico").permitAll()
                .requestMatchers("/login", "/register", "/verify-email", "/api/resend-otp", "/api/otp-status", "/error/**").permitAll()
                // Forgot password endpoints
                .requestMatchers("/forgot-password", "/forgot-password-otp", "/verify-reset-otp", "/reset-password-otp", "/reset-password").permitAll()
                .requestMatchers("/api/resend-reset-otp", "/api/reset-otp-status").permitAll()
                .requestMatchers("/payment/**").permitAll() // Payment callbacks
                .requestMatchers("/products/**", "/api/chatbot/**").permitAll()
                .requestMatchers("/", "/home").permitAll() // Cho phép tất cả, HomeController sẽ xử lý redirect

                // Admin and Staff dashboards
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/staff/**").hasRole("STAFF")


                // Shopping features - Customer and Admin can access
                .requestMatchers("/cart/**", "/checkout/**").hasRole("CUSTOMER")
                .requestMatchers("/orders/**").hasAnyRole("CUSTOMER", "ADMIN", "STAFF")

                .anyRequest().authenticated()
            )
            
            // Login configuration
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    String role = authentication.getAuthorities().iterator().next().getAuthority();
                    switch (role) {
                        case "ROLE_ADMIN":
                            response.sendRedirect("/admin");
                            break;
                        case "ROLE_STAFF":
                            response.sendRedirect("/staff");
                            break;
                        case "ROLE_CUSTOMER":
                        default:
                            response.sendRedirect("/");
                            break;
                    }
                })
                .failureUrl("/login?error")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            
            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )
            
            // Exception handling
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    try {
                        // Set error attributes for CustomErrorController
                        request.setAttribute("jakarta.servlet.error.status_code", 403);
                        request.setAttribute("jakarta.servlet.error.message", "Access Denied");
                        request.setAttribute("jakarta.servlet.error.request_uri", request.getRequestURI());
                        request.getRequestDispatcher("/error").forward(request, response);
                    } catch (ServletException | IOException e) {
                        response.sendError(403, "Access Denied");
                    }
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect("/login");
                })
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strong encryption
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false); // For better error handling
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}