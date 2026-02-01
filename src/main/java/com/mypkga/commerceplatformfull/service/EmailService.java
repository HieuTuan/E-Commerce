package com.mypkga.commerceplatformfull.service;

/**
 * Service interface for email operations
 */
public interface EmailService {

    /**
     * Send email message to an email address
     * @param email The recipient email address
     * @param subject The email subject
     * @param message The message content
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendEmail(String email, String subject, String message);

    /**
     * Validate email format
     * @param email The email to validate
     * @return true if email is valid, false otherwise
     */
    boolean isValidEmail(String email);

    /**
     * Send OTP email to email address
     * @param email The recipient email address
     * @param otp The OTP code
     * @return true if OTP was sent successfully, false otherwise
     */
    boolean sendOTP(String email, String otp);

    /**
     * Send HTML email
     * @param email The recipient email address
     * @param subject The email subject
     * @param htmlContent The HTML content
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendHtmlEmail(String email, String subject, String htmlContent);
    
    /**
     * Send return approval notification to customer
     * @param returnRequest The approved return request
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendReturnApprovalNotification(com.mypkga.commerceplatformfull.entity.ReturnRequest returnRequest);
    
    /**
     * Send return package received notification to staff
     * @param returnRequest The return request with received package
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendReturnPackageReceivedNotification(com.mypkga.commerceplatformfull.entity.ReturnRequest returnRequest);
    
    /**
     * Send delivery issue resolved notification to customer
     * @param customerEmail The customer email
     * @param orderNumber The order number
     * @param adminNotes Admin notes about resolution
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendDeliveryIssueResolvedNotification(String customerEmail, String orderNumber, String adminNotes);
    
    /**
     * Send delivery issue rejected notification to customer
     * @param customerEmail The customer email
     * @param orderNumber The order number
     * @param adminNotes Admin notes about rejection
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendDeliveryIssueRejectedNotification(String customerEmail, String orderNumber, String adminNotes);
}