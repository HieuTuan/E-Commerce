package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.dto.CreateReturnRequestDto;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.ReturnRequestHistory;

import java.util.List;

public interface ReturnService {
    

    ReturnRequest createReturnRequest(Long orderId, CreateReturnRequestDto dto);
    
    /**
     * Get all pending return requests for staff review.
     * Returns requests with status REFUND_REQUESTED ordered by creation date.
     * 
     * @return list of pending return requests
     */
    List<ReturnRequest> getPendingReturnRequests();
    

    ReturnRequest approveReturnRequest(Long requestId, Long staffId);
    

    ReturnRequest rejectReturnRequest(Long requestId, String reason, Long staffId);
    

    ReturnRequest confirmShipping(String returnCode);
    

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
     * Get history of a return request
     * 
     * @param returnRequestId the ID of the return request
     * @return list of history records ordered by creation date
     */
    List<ReturnRequestHistory> getReturnRequestHistory(Long returnRequestId);
    
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
    
    /**
     * Get all return requests with optional status filter.
     * 
     * @param status the status to filter by (null for all)
     * @param sort the sort order
     * @return list of return requests
     */
    List<ReturnRequest> getAllReturnRequests(com.mypkga.commerceplatformfull.entity.ReturnStatus status, org.springframework.data.domain.Sort sort);
    
    /**
     * Update return request with refund proof image and complete refund.
     * 
     * @param requestId the ID of the return request
     * @param file the refund proof image file
     * @return the updated ReturnRequest entity
     * @throws IllegalArgumentException if return request is not found
     * @throws java.io.IOException if file upload fails
     */
    ReturnRequest uploadRefundProofAndComplete(Long requestId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;
}