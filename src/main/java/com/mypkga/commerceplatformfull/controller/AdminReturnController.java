package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.RejectRequestDto;
import com.mypkga.commerceplatformfull.dto.ReturnRequestDto;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.ReturnService;
import com.mypkga.commerceplatformfull.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for admin return operations.
 * Handles staff review, approval, rejection, shipping confirmation, receipt confirmation,
 * and refund completion for return requests.
 * 
 * Requirements: 4.1, 4.4, 4.6, 5.1, 5.3, 5.4, 6.2
 */
@RestController
@RequestMapping("/api/admin/returns")
@PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminReturnController {
    
    private final ReturnService returnService;
    private final UserService userService;
    
    /**
     * Get all pending return requests for staff review.
     * Returns requests with status REFUND_REQUESTED ordered by creation date.
     * 
     * Requirements: 4.1
     * 
     * @return list of pending return requests
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ReturnRequestDto>> getPendingRequests() {
        log.info("Retrieving pending return requests for staff review");
        
        try {
            List<ReturnRequest> pendingRequests = returnService.getPendingReturnRequests();
            
            List<ReturnRequestDto> requestDtos = pendingRequests.stream()
                    .map(ReturnRequestDto::from)
                    .collect(Collectors.toList());
            
            log.info("Retrieved {} pending return requests", requestDtos.size());
            
            return ResponseEntity.ok(requestDtos);
            
        } catch (Exception e) {
            log.error("Error retrieving pending return requests: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get all return requests currently in shipping (RETURNING status).
     * Used for tracking items being returned to warehouse.
     * 
     * Requirements: 5.3
     * 
     * @return list of return requests in shipping
     */
    @GetMapping("/shipping")
    public ResponseEntity<List<ReturnRequestDto>> getShippingRequests() {
        log.info("Retrieving return requests in shipping status");
        
        try {
            List<ReturnRequest> shippingRequests = returnService.getReturningRequests();
            
            List<ReturnRequestDto> requestDtos = shippingRequests.stream()
                    .map(ReturnRequestDto::from)
                    .collect(Collectors.toList());
            
            log.info("Retrieved {} return requests in shipping", requestDtos.size());
            
            return ResponseEntity.ok(requestDtos);
            
        } catch (Exception e) {
            log.error("Error retrieving shipping return requests: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Approve a return request.
     * Updates status to RETURN_APPROVED, changes order status, and sends notification email.
     * 
     * Requirements: 4.4
     * 
     * @param requestId the ID of the return request to approve
     * @param authentication current staff authentication
     * @return the updated return request
     */
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ReturnRequestDto> approveRequest(
            @PathVariable Long requestId,
            Authentication authentication) {
        
        log.info("Approving return request {} by staff {}", requestId, authentication.getName());
        
        try {
            User currentStaff = getCurrentUser(authentication);
            
            ReturnRequest approvedRequest = returnService.approveReturnRequest(requestId, currentStaff.getId());
            
            log.info("Successfully approved return request {} by staff {}", 
                    requestId, currentStaff.getEmail());
            
            return ResponseEntity.ok(ReturnRequestDto.from(approvedRequest));
            
        } catch (Exception e) {
            log.error("Error approving return request {}: {}", requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Reject a return request with a reason.
     * Updates status to REFUND_REJECTED, reverts order status to DELIVERED, and sends notification email.
     * 
     * Requirements: 4.6
     * 
     * @param requestId the ID of the return request to reject
     * @param rejectDto the rejection reason
     * @param authentication current staff authentication
     * @return the updated return request
     */
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ReturnRequestDto> rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectRequestDto rejectDto,
            Authentication authentication) {
        
        log.info("Rejecting return request {} by staff {} with reason: {}", 
                requestId, authentication.getName(), rejectDto.getReason());
        
        try {
            User currentStaff = getCurrentUser(authentication);
            
            ReturnRequest rejectedRequest = returnService.rejectReturnRequest(
                    requestId, rejectDto.getReason(), currentStaff.getId());
            
            log.info("Successfully rejected return request {} by staff {}", 
                    requestId, currentStaff.getEmail());
            
            return ResponseEntity.ok(ReturnRequestDto.from(rejectedRequest));
            
        } catch (Exception e) {
            log.error("Error rejecting return request {}: {}", requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Confirm shipping when return code is scanned at post office.
     * Updates status to RETURNING when customer drops off the item.
     * 
     * Requirements: 5.1
     * 
     * @param returnCode the return code scanned at pickup
     * @return the updated return request
     */
    @PostMapping("/shipping/{returnCode}/confirm")
    public ResponseEntity<ReturnRequestDto> confirmShipping(@PathVariable String returnCode) {
        
        log.info("Confirming shipping for return code: {}", returnCode);
        
        try {
            ReturnRequest shippingRequest = returnService.confirmShipping(returnCode);
            
            log.info("Successfully confirmed shipping for return request {} with code {}", 
                    shippingRequest.getId(), returnCode);
            
            return ResponseEntity.ok(ReturnRequestDto.from(shippingRequest));
            
        } catch (Exception e) {
            log.error("Error confirming shipping for return code {}: {}", returnCode, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Confirm receipt of returned item at warehouse.
     * Updates status to RETURN_RECEIVED when staff confirm physical receipt.
     * 
     * Requirements: 5.4
     * 
     * @param requestId the ID of the return request
     * @param authentication current staff authentication
     * @return the updated return request
     */
    @PostMapping("/{requestId}/receipt/confirm")
    public ResponseEntity<ReturnRequestDto> confirmReceipt(
            @PathVariable Long requestId,
            Authentication authentication) {
        
        log.info("Confirming receipt for return request {} by staff {}", 
                requestId, authentication.getName());
        
        try {
            User currentStaff = getCurrentUser(authentication);
            
            ReturnRequest receivedRequest = returnService.confirmReceipt(requestId, currentStaff.getId());
            
            log.info("Successfully confirmed receipt for return request {} by staff {}", 
                    requestId, currentStaff.getEmail());
            
            return ResponseEntity.ok(ReturnRequestDto.from(receivedRequest));
            
        } catch (Exception e) {
            log.error("Error confirming receipt for return request {}: {}", requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Complete refund processing using customer's bank information.
     * Updates status to REFUNDED, changes order status, and sends confirmation email.
     * 
     * Requirements: 6.2
     * 
     * @param requestId the ID of the return request
     * @param authentication current staff authentication
     * @return the updated return request
     */
    @PostMapping("/{requestId}/refund/complete")
    public ResponseEntity<ReturnRequestDto> completeRefund(
            @PathVariable Long requestId,
            Authentication authentication) {
        
        log.info("Completing refund for return request {} by staff {}", 
                requestId, authentication.getName());
        
        try {
            User currentStaff = getCurrentUser(authentication);
            
            ReturnRequest refundedRequest = returnService.completeRefund(requestId, currentStaff.getId());
            
            log.info("Successfully completed refund for return request {} by staff {}", 
                    requestId, currentStaff.getEmail());
            
            return ResponseEntity.ok(ReturnRequestDto.from(refundedRequest));
            
        } catch (Exception e) {
            log.error("Error completing refund for return request {}: {}", requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get current user from authentication.
     * 
     * @param authentication the authentication object
     * @return the current user
     */
    private User getCurrentUser(Authentication authentication) {
        // authentication.getName() returns email (from CustomUserDetailsService)
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}