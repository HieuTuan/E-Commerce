package com.mypkga.commerceplatformfull.util;

import java.util.regex.Pattern;

/**
 * Utility class for phone number validation and formatting
 */
public class PhoneNumberValidator {

    // Vietnamese phone number patterns
    private static final Pattern VIETNAM_MOBILE_PATTERN = Pattern.compile(
        "^(\\+84|84|0)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$"
    );

    // International phone number pattern (basic)
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile(
        "^\\+[1-9]\\d{1,14}$"
    );

    /**
     * Validate Vietnamese phone number
     * @param phoneNumber The phone number to validate
     * @return true if valid Vietnamese phone number
     */
    public static boolean isValidVietnamesePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String cleanNumber = phoneNumber.replaceAll("\\s+", "");
        return VIETNAM_MOBILE_PATTERN.matcher(cleanNumber).matches();
    }

    /**
     * Validate international phone number
     * @param phoneNumber The phone number to validate
     * @return true if valid international phone number
     */
    public static boolean isValidInternationalPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String cleanNumber = phoneNumber.replaceAll("\\s+", "");
        return INTERNATIONAL_PATTERN.matcher(cleanNumber).matches();
    }

    /**
     * Validate phone number (Vietnamese or international)
     * @param phoneNumber The phone number to validate
     * @return true if valid phone number
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return isValidVietnamesePhoneNumber(phoneNumber) || 
               isValidInternationalPhoneNumber(phoneNumber);
    }

    /**
     * Format Vietnamese phone number to international format
     * @param phoneNumber The phone number to format
     * @return Formatted phone number or null if invalid
     */
    public static String formatToInternational(String phoneNumber) {
        if (!isValidVietnamesePhoneNumber(phoneNumber)) {
            return null;
        }

        String cleanNumber = phoneNumber.replaceAll("\\s+", "");
        
        // Convert to +84 format
        if (cleanNumber.startsWith("0")) {
            return "+84" + cleanNumber.substring(1);
        } else if (cleanNumber.startsWith("84")) {
            return "+" + cleanNumber;
        } else if (cleanNumber.startsWith("+84")) {
            return cleanNumber;
        }
        
        return null;
    }

    /**
     * Clean and normalize phone number
     * @param phoneNumber The phone number to clean
     * @return Cleaned phone number
     */
    public static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[^+\\d]", "");
    }

    /**
     * Mask phone number for display (show only last 4 digits)
     * @param phoneNumber The phone number to mask
     * @return Masked phone number
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return "****" + lastFour;
    }
}