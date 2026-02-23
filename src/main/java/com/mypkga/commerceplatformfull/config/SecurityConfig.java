package com.mypkga.commerceplatformfull.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

/**
 * Security Configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/", "/home", "/products/**", "/register", "/login").permitAll()
                        .requestMatchers("/forgot-password/**", "/forgot-password-otp/**", "/verify-reset-otp/**",
                                "/reset-password-otp/**", "/verify-email/**", "/reset-password/**")
                        .permitAll()
                        .requestMatchers("/api/resend-otp/**", "/api/otp-status/**", "/api/resend-reset-otp/**",
                                "/api/reset-otp-status/**")
                        .permitAll()
                        .requestMatchers("/checkout/**", "/cart/**").permitAll() // Allow cart and checkout access
                        .requestMatchers("/staff/**").hasAnyRole("STAFF", "MODERATOR", "ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/api/ghn/**").permitAll() // Allow GHN webhooks and master data APIs
                        .requestMatchers("/api/chatbot/**").permitAll() // Allow public access to chatbot API
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            // Lấy tất cả authorities và tìm role
                            String redirectUrl = "/";

                            for (var authority : authentication.getAuthorities()) {
                                String auth = authority.getAuthority();
                                if (auth.equals("ROLE_STAFF")) {
                                    redirectUrl = "/staff/returns";
                                    break;
                                } else if (auth.equals("ROLE_ADMIN")) {
                                    redirectUrl = "/admin";
                                    break;
                                }
                            }

                            System.out.println("User authorities: " + authentication.getAuthorities());
                            System.out.println("Redirecting to: " + redirectUrl);
                            response.sendRedirect(redirectUrl);
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll())
                .csrf(csrf -> csrf.disable()); // Tạm thời disable CSRF cho test

        return http.build();
    }
}