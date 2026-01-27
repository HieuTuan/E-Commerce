package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.OTPVerification;
import com.mypkga.commerceplatformfull.repository.OTPRepository;
import com.mypkga.commerceplatformfull.service.impl.OTPServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OTPServiceTest {

    @Mock
    private OTPRepository otpRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OTPServiceImpl otpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set the maxAttempts field to 5
        ReflectionTestUtils.setField(otpService, "maxAttempts", 5);
    }

    @Test
    void testRemainingAttemptsDecrement() {
        String email = "test@example.com";
        String otp = "123456";
        
        // Create OTP verification with different attempt counts
        OTPVerification otpVerification = new OTPVerification();
        otpVerification.setEmail(email);
        otpVerification.setOtpCode("hashedOTP");
        otpVerification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpVerification.setAttempts(0);
        otpVerification.setVerified(false);
//        otpVerification.setBlocked(false);

        when(emailService.isValidEmail(email)).thenReturn(true);
        when(otpRepository.findActiveOTPByEmail(eq(email), any(LocalDateTime.class)))
            .thenReturn(Optional.of(otpVerification));
        when(passwordEncoder.matches(eq(otp), eq("hashedOTP"))).thenReturn(false);

        // Test attempts: 0 -> should have 5 remaining
        int remaining = otpService.getRemainingAttempts(email);
        assertEquals(5, remaining, "Should have 5 attempts initially");

        // Validate wrong OTP (this will increment attempts to 1)
        boolean result = otpService.validateOTP(email, otp);
        assertFalse(result);

        // Now attempts should be 1, remaining should be 4
        otpVerification.setAttempts(1);
        remaining = otpService.getRemainingAttempts(email);
        assertEquals(4, remaining, "Should have 4 attempts after 1 failed attempt");

        // Test with 2 attempts
        otpVerification.setAttempts(2);
        remaining = otpService.getRemainingAttempts(email);
        assertEquals(3, remaining, "Should have 3 attempts after 2 failed attempts");

        // Test with 3 attempts
        otpVerification.setAttempts(3);
        remaining = otpService.getRemainingAttempts(email);
        assertEquals(2, remaining, "Should have 2 attempts after 3 failed attempts");

        // Test with 4 attempts
        otpVerification.setAttempts(4);
        remaining = otpService.getRemainingAttempts(email);
        assertEquals(1, remaining, "Should have 1 attempt after 4 failed attempts");

        // Test with 5 attempts (max reached)
        otpVerification.setAttempts(5);
        remaining = otpService.getRemainingAttempts(email);
        assertEquals(0, remaining, "Should have 0 attempts after 5 failed attempts");
    }

    @Test
    void testMaxAttemptsReached() {
        OTPVerification otpVerification = new OTPVerification();
        
        // Test with different attempt counts
        otpVerification.setAttempts(3);
        assertFalse(otpVerification.isMaxAttemptsReached(5), "Should not be max with 3/5 attempts");
        
        otpVerification.setAttempts(4);
        assertFalse(otpVerification.isMaxAttemptsReached(5), "Should not be max with 4/5 attempts");
        
        otpVerification.setAttempts(5);
        assertTrue(otpVerification.isMaxAttemptsReached(5), "Should be max with 5/5 attempts");
        
        otpVerification.setAttempts(6);
        assertTrue(otpVerification.isMaxAttemptsReached(5), "Should be max with 6/5 attempts");
    }
}