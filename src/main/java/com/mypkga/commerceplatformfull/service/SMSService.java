package com.mypkga.commerceplatformfull.service;

/**
 * Service interface for SMS operations
 */
public interface SMSService {

    /**
     * Send SMS message to a phone number
     * @param phoneNumber The recipient phone number
     * @param message The message content
     * @return true if message was sent successfully, false otherwise
     */
    boolean sendMessage(String phoneNumber, String message);

    /**
     * Validate phone number format
     * @param phoneNumber The phone number to validate
     * @return true if phone number is valid, false otherwise
     */
    boolean isValidPhoneNumber(String phoneNumber);

    /**
     * Format phone number to international format
     * @param phoneNumber The phone number to format
     * @return Formatted phone number or null if invalid
     */
    String formatPhoneNumber(String phoneNumber);

    /**
     * Send OTP message to phone number
     * @param phoneNumber The recipient phone number
     * @param otp The OTP code
     * @return true if OTP was sent successfully, false otherwise
     */
    boolean sendOTP(String phoneNumber, String otp);
}