package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.dto.CreateReturnRequestDto;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;

import java.util.List;

/**
 * Service interface for managing return requests and the complete return workflow.
 * This service handles the core business logic for return processing including
 * eligibility validation, request creation, status transitions, and integration
 * with file upload and return code generation services.
 */
public interface ReturnService {
    
    /**
     * Create a new return request for an order.
     * This method performs eligibility validation, uploads evidence video,
     * generates return code, and updates order status transactionally.
     * 
     * @param orderId the ID of the order to create a return request for
     * @param dto the return request data including reason, description, video, and bank info
     * @return the created ReturnRequest entity
     * @throws com.mypkga.commerceplatformfull.exception.ReturnNotEligibleException if order is not eligible
     * @throws com.mypkga.commerceplatformfull.exception.VideoUploadException if video upload fails
     */
    ReturnRequest createReturnRequest(Long orderId, CreateReturnRequestDto dto);
    
    /**
     * Get all pending return requests for staff review.
     * Returns requests with status REFUND_REQUESTED ordered by creation date.
     * 
     * @return list of pending return requests
     */
    List<ReturnRequest> getPendingReturnRequests();
    
    /**
     * Approve a return request by authorized staff.
     * Updates status to RETURN_APPROVED, changes order status, and sends notification email.
     * 
     * @param requestId the ID of the return request to approve
     * @param staffId the ID of the staff member approving the request
     * @return the updated ReturnRequest entity
     * @throws com.mypkga.commerceplatformfull.exception.InvalidReturnStatusException if status transition is invalid
     */
    ReturnRequest approveReturnRequest(Long requestId, Long staffId);
    
    /**
     * Reject a return request by authorized staff.
     * Updates status to REFUND_REJECTED, reverts order status to DELIVERED, and sends notification email.
     * 
     * @param requestId the ID of the return request to reject
     * @param reason the reason for rejection
     * @param staffId the ID of the staff member rejecting the request
     * @return the updated ReturnRequest entity
     * @throws com.mypkga.commerceplatformfull.exception.InvalidReturnStatusException if status transition is invalid
     */
    ReturnRequest rejectReturnRequest(Long requestId, String reason, Long staffId);
    
    /**
     * Confirm shipping when return code is scanned at post office.
     * Updates status to RETURNING when customer drops off the item.
     * 
     * @param returnCode the return code scanned at pickup
     * @return the updated ReturnRequest entity
     * @throws IllegalArgumentException if return code is invalid or not found
     */
    ReturnRequest confirmShipping(String returnCode);
    
    /**
     * Confirm receipt of returned item at warehouse.
     * Updates status to RETURN_RECEIVED when staff confirm physical receipt.
     * 
     * @param requestId the ID of the return request
     * @param staffId the ID of the staff member confirming receipt
     * @return the updated ReturnRequest entity
     * @throws com.mypkga.commerceplatformfull.exception.InvalidReturnStatusException if status transition is invalid
     */
    ReturnRequest confirmReceipt(Long requestId, Long staffId);
    
    /**
     * Complete refund processing using customer's bank information.
     * Updates status to REFUNDED, changes order status, and sends confirmation email.
     * 
     * @param requestId the ID of the return request
     * @param staffId the ID of the staff member completing the refund
     * @return the updated ReturnRequest entity
     * @throws com.mypkga.commerceplatformfull.exception.InvalidReturnStatusException if status transition is invalid
     */
    ReturnRequest completeRefund(Long requestId, Long staffId);
    
    /**
     * Check if an order is eligible for return request.
     * Validates order status, time window, and existing return requests.
     * 
     * @param orderId the ID of the order to check
     * @return true if eligible for return, false otherwise
     */
    boolean isEligibleForReturn(Long orderId);
    
    /**
     * Get all return requests in RETURNING status for shipping tracking.
     * 
     * @return list of return requests currently being shipped back
     */
    List<ReturnRequest> getReturningRequests();
    
    /**
     * Get all approved return requests waiting for customer to send items.
     * 
     * @return list of approved return requests
     */
    List<ReturnRequest> getApprovedReturnRequests();
    
    /**
     * Get return requests by customer ID.
     * 
     * @param customerId the ID of the customer
     * @return list of return requests for the customer
     */
    List<ReturnRequest> getReturnRequestsByCustomer(Long customerId);
    
    /**
     * Find return request by return code.
     * 
     * @param returnCode the return code to search for
     * @return the return request if found
     * @throws IllegalArgumentException if return code is not found
     */
    ReturnRequest findByReturnCode(String returnCode);
    
    /**
     * Find return request by ID.
     * 
     * @param id the ID of the return request
     * @return the return request if found
     * @throws IllegalArgumentException if return request is not found
     */
    ReturnRequest findById(Long id);
    
    /**
     * Confirm package receipt at post office with photo upload.
     * Updates status to RETURNING and saves receipt photo.
     * 
     * @param returnCode the return code
     * @param receiptPhoto the photo of received package
     * @return the updated ReturnRequest entity
     * @throws IllegalArgumentException if return code is invalid or not found
     */
    ReturnRequest confirmPackageReceiptWithPhoto(String returnCode, org.springframework.web.multipart.MultipartFile receiptPhoto);
}