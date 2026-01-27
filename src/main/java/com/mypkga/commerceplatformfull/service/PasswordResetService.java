package com.mypkga.commerceplatformfull.service;

public interface PasswordResetService {

    /**
     * Generate and send password reset token to email
     * @param email The email address
     * @return true if token was generated and sent successfully
     */
    boolean generateAndSendResetToken(String email);

    /**
     * Validate password reset token
     * @param token The reset token
     * @return true if token is valid and not expired
     */
    boolean validateResetToken(String token);

    /**
     * Reset password using token
     * @param token The reset token
     * @param newPassword The new password
     * @return true if password was reset successfully
     */
    boolean resetPassword(String token, String newPassword);

    /**
     * Get email associated with reset token
     * @param token The reset token
     * @return email address or null if token not found
     */
    String getEmailByToken(String token);
}