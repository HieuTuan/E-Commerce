package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.ReturnRequest;

/**
 * Service interface for handling email notifications related to return requests.
 * This service manages all email communications with customers throughout the return workflow,
 * including approval, rejection, and completion notifications.
 */
public interface NotificationService {
    
    /**
     * Send approval notification email to customer when return request is approved.
     * Email contains post office name, 48-hour deadline, and tracking code.
     * 
     * @param returnRequest the approved return request
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendApprovalNotification(ReturnRequest returnRequest);
    
    /**
     * Send rejection notification email to customer when return request is rejected.
     * Email contains rejection reason and hotline contact information.
     * 
     * @param returnRequest the rejected return request
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendRejectionNotification(ReturnRequest returnRequest);
    
    /**
     * Send completion notification email to customer when refund is processed.
     * Email contains account information and thank you message.
     * 
     * @param returnRequest the completed return request
     * @return true if email was sent successfully, false otherwise
     */
    boolean sendCompletionNotification(ReturnRequest returnRequest);
    
    /**
     * Validate customer email address from return request.
     * Ensures the customer has a valid email address for notifications.
     * 
     * @param returnRequest the return request to validate
     * @return true if customer email is valid, false otherwise
     */
    boolean validateCustomerEmail(ReturnRequest returnRequest);
    
    /**
     * Get customer email address from return request.
     * Extracts the email from the associated order's customer.
     * 
     * @param returnRequest the return request
     * @return customer email address, or null if not available
     */
    String getCustomerEmail(ReturnRequest returnRequest);
}