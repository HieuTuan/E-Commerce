package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.OTPVerification;

/**
 * Service interface for OTP operations
 */
public interface OTPService {

    /**
     * Generate and send OTP to email address
     * @param email The email address to send OTP to
     * @return true if OTP was generated and sent successfully
     * @throws IllegalStateException if rate limit exceeded or email blocked
     */
    boolean generateAndSendOTP(String email);

    /**
     * Validate OTP for an email address
     * @param email The email address
     * @param otp The OTP code to validate
     * @return true if OTP is valid and verified successfully
     */
    boolean validateOTP(String email, String otp);

    /**
     * Check if OTP is expired for an email address
     * @param email The email address
     * @return true if the latest OTP is expired
     */
    boolean isOTPExpired(String email);

    /**
     * Invalidate OTP for an email address
     * @param email The email address
     */
    void invalidateOTP(String email);

    /**
     * Check if email address is rate limited
     * @param email The email address
     * @return true if rate limit exceeded
     */
    boolean isRateLimited(String email);

    /**
     * Check if email address is blocked due to too many failed attempts
     * @param email The email address
     * @return true if email address is blocked
     */
    boolean isBlocked(String email);

    /**
     * Get the latest OTP verification record for an email address
     * @param email The email address
     * @return OTPVerification record or null if not found
     */
    OTPVerification getLatestOTP(String email);

    /**
     * Clean up expired OTP records
     * @return number of records cleaned up
     */
    int cleanupExpiredOTPs();

    /**
     * Resend OTP to email address (if allowed)
     * @param email The email address
     * @return true if OTP was resent successfully
     */
    boolean resendOTP(String email);

    /**
     * Get remaining attempts for an email address
     * @param email The email address
     * @return number of remaining attempts
     */
    int getRemainingAttempts(String email);

    /**
     * Get time until next OTP request is allowed (for rate limiting)
     * @param email The email address
     * @return seconds until next request allowed, 0 if allowed now
     */
    long getSecondsUntilNextRequest(String email);

    /**
     * Get remaining seconds until current OTP expires
     * @param email The email address
     * @return seconds until OTP expires, 0 if already expired
     */
    long getRemainingOTPSeconds(String email);
}