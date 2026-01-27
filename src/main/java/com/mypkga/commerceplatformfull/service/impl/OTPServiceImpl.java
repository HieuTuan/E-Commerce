package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.entity.OTPVerification;
import com.mypkga.commerceplatformfull.repository.OTPRepository;
import com.mypkga.commerceplatformfull.service.OTPService;
import com.mypkga.commerceplatformfull.service.EmailService;
import com.mypkga.commerceplatformfull.service.SMSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OTPServiceImpl implements OTPService {

    private final OTPRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit-minutes:1}")
    private int rateLimitMinutes;

    @Value("${app.otp.max-requests-per-period:3}")
    private int maxRequestsPerPeriod;

    @Value("${app.otp.block-duration-minutes:15}")
    private int blockDurationMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public boolean generateAndSendOTP(String email) {
        // Validate email format
        if (!emailService.isValidEmail(email)) {
            log.error("Invalid email format: {}", maskEmail(email));
            return false;
        }

        String normalizedEmail = email.toLowerCase().trim();

        // Check if email is blocked
        if (isBlocked(normalizedEmail)) {
            log.warn("Email is blocked: {}", maskEmail(normalizedEmail));
            throw new IllegalStateException("Email is temporarily blocked due to too many failed attempts");
        }

        // Check rate limiting
        if (isRateLimited(normalizedEmail)) {
            log.warn("Rate limit exceeded for email: {}", maskEmail(normalizedEmail));
            throw new IllegalStateException("Too many OTP requests. Please wait before requesting again.");
        }

        // Clean up any existing OTPs for this email to avoid duplicates
        otpRepository.deleteByEmail(normalizedEmail);

        // Generate OTP
        String otp = generateOTP();
        String hashedOTP = passwordEncoder.encode(otp);

        // Create OTP verification record
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        OTPVerification otpVerification = new OTPVerification(normalizedEmail, hashedOTP, expiresAt);

        // Save to database
        otpRepository.save(otpVerification);

        // Send OTP via email
        boolean sent = emailService.sendOTP(normalizedEmail, otp);

        if (!sent) {
            log.error("Failed to send OTP to email: {}", maskEmail(normalizedEmail));
            return false;
        }

        log.info("OTP generated and sent to email: {}", maskEmail(normalizedEmail));
        return true;
    }

    @Override
    public boolean validateOTP(String email, String otp) {
        if (!emailService.isValidEmail(email) || otp == null || otp.trim().isEmpty()) {
            return false;
        }

        String normalizedEmail = email.toLowerCase().trim();

        Optional<OTPVerification> otpVerificationOpt = otpRepository.findActiveOTPByEmail(
            normalizedEmail, LocalDateTime.now()
        );

        if (otpVerificationOpt.isEmpty()) {
            log.warn("No active OTP found for email: {}", maskEmail(normalizedEmail));
            return false;
        }

        OTPVerification otpVerification = otpVerificationOpt.get();

        // Check if already verified
        if (otpVerification.isVerified()) {
            log.warn("OTP already verified for email: {}", maskEmail(normalizedEmail));
            return false;
        }

        // Check if expired
        if (otpVerification.isExpired()) {
            log.warn("OTP expired for email: {}", maskEmail(normalizedEmail));
            return false;
        }

        // Increment attempts
        otpVerification.incrementAttempts();
        log.debug("Incremented attempts for email: {}, current attempts: {}", maskEmail(normalizedEmail), otpVerification.getAttempts());

        // Validate OTP
        boolean isValid = passwordEncoder.matches(otp, otpVerification.getOtpCode());

        if (isValid) {
            // Mark as verified
            otpVerification.markAsVerified();
            otpRepository.save(otpVerification);
            log.info("OTP verified successfully for email: {}", maskEmail(normalizedEmail));
            return true;
        } else {
            // Save the incremented attempts
            otpRepository.save(otpVerification);
            log.warn("Invalid OTP for email: {}", maskEmail(normalizedEmail));
            return false;
        }
    }

    @Override
    public boolean isOTPExpired(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        Optional<OTPVerification> otpVerificationOpt = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail);
        return otpVerificationOpt.map(OTPVerification::isExpired).orElse(true);
    }

    @Override
    public void invalidateOTP(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        otpRepository.deleteByEmail(normalizedEmail);
        log.info("OTP invalidated for email: {}", maskEmail(normalizedEmail));
    }

    @Override
    public boolean isRateLimited(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        LocalDateTime since = LocalDateTime.now().minusMinutes(rateLimitMinutes);
        long requestCount = otpRepository.countOTPRequestsSince(normalizedEmail, since);
        return requestCount >= maxRequestsPerPeriod;
    }

    @Override
    public boolean isBlocked(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        // Check if there's an active OTP with max attempts reached
        Optional<OTPVerification> otpVerificationOpt = otpRepository.findActiveOTPByEmail(
            normalizedEmail, LocalDateTime.now()
        );

        if (otpVerificationOpt.isPresent()) {
            OTPVerification otpVerification = otpVerificationOpt.get();
            return otpVerification.isMaxAttemptsReached(maxAttempts);
        }

        // Also check if there are recent failed attempts within block duration
        LocalDateTime since = LocalDateTime.now().minusMinutes(blockDurationMinutes);
        return otpRepository.isEmailBlocked(normalizedEmail, since);
    }

    @Override
    public OTPVerification getLatestOTP(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        return otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail).orElse(null);
    }

    @Override
    public int cleanupExpiredOTPs() {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.deleteExpiredOTPs(now);
        log.info("Cleaned up expired OTPs");
        return 0; // Return value would need to be tracked if needed
    }

    @Override
    public boolean resendOTP(String email) {
        // Simple resend - just generate new OTP
        String normalizedEmail = email.toLowerCase().trim();
        
        // Check rate limiting first
        if (isRateLimited(normalizedEmail)) {
            log.warn("Rate limit exceeded for resend OTP: {}", maskEmail(normalizedEmail));
            return false;
        }
        
        // Clean up existing OTPs and generate new one
        otpRepository.deleteByEmail(normalizedEmail);
        return generateAndSendOTP(normalizedEmail);
    }

    @Override
    public int getRemainingAttempts(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        Optional<OTPVerification> otpVerificationOpt = otpRepository.findActiveOTPByEmail(
            normalizedEmail, LocalDateTime.now()
        );

        if (otpVerificationOpt.isEmpty()) {
            log.debug("No active OTP found for email: {}, returning max attempts: {}", maskEmail(normalizedEmail), maxAttempts);
            return maxAttempts;
        }

        OTPVerification otpVerification = otpVerificationOpt.get();
        int remaining = Math.max(0, maxAttempts - otpVerification.getAttempts());
        log.debug("Email: {}, Current attempts: {}, Max attempts: {}, Remaining: {}", 
                 maskEmail(normalizedEmail), otpVerification.getAttempts(), maxAttempts, remaining);
        return remaining;
    }

    @Override
    public long getSecondsUntilNextRequest(String email) {
        if (!isRateLimited(email)) {
            return 0;
        }

        String normalizedEmail = email.toLowerCase().trim();

        Optional<OTPVerification> latestOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail);
        if (latestOtp.isEmpty()) {
            return 0;
        }

        LocalDateTime nextAllowedTime = latestOtp.get().getCreatedAt().plusMinutes(rateLimitMinutes);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(nextAllowedTime)) {
            return java.time.Duration.between(now, nextAllowedTime).getSeconds();
        }
        
        return 0;
    }

    @Override
    public long getRemainingOTPSeconds(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        Optional<OTPVerification> latestOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail);
        if (latestOtp.isEmpty()) {
            return 0;
        }

        OTPVerification otp = latestOtp.get();
        LocalDateTime expiryTime = otp.getExpiresAt();
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(expiryTime)) {
            return java.time.Duration.between(now, expiryTime).getSeconds();
        }
        
        return 0;
    }

    /**
     * Generate a 6-digit OTP
     */
    private String generateOTP() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Mask email for logging (show only first 2 chars and domain)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domain;
        } else {
            return localPart.substring(0, 2) + "***" + domain;
        }
    }
}