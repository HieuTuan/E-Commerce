package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.*;
import com.mypkga.commerceplatformfull.entity.PostOffice;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.PostOfficeRepository;
import com.mypkga.commerceplatformfull.service.ReturnEligibilityService;
import com.mypkga.commerceplatformfull.service.ReturnService;
import com.mypkga.commerceplatformfull.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for customer return operations.
 * Handles return request creation, eligibility checks, and post office lookups.
 * 
 * Requirements: 1.1, 1.3, 2.8
 */
@RestController
@RequestMapping("/api/returns")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Slf4j
public class ReturnController {
    
    private final ReturnService returnService;
    private final ReturnEligibilityService eligibilityService;
    private final PostOfficeRepository postOfficeRepository;
    private final UserService userService;
    
    /**
     * Create a new return request for an order.
     * 
     * @param orderId the ID of the order to create a return request for
     * @param dto the return request data
     * @param authentication current user authentication
     * @return the created return request
     */
    @PostMapping(value = "/orders/{orderId}", consumes = "multipart/form-data")
    public ResponseEntity<ReturnRequestDto> createReturnRequest(
            @PathVariable Long orderId,
            @Valid @ModelAttribute CreateReturnRequestDto dto,
            Authentication authentication) {
        
        log.info("Creating return request for order {} by user {}", orderId, authentication.getName());
        
        try {
            // Verify the order belongs to the current user
            User currentUser = getCurrentUser(authentication);
            
            // Create the return request
            ReturnRequest returnRequest = returnService.createReturnRequest(orderId, dto);
            
            log.info("Successfully created return request {} for order {}", 
                    returnRequest.getId(), orderId);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ReturnRequestDto.from(returnRequest));
                    
        } catch (Exception e) {
            log.error("Error creating return request for order {}: {}", orderId, e.getMessage(), e);
            throw e; // Let global exception handler deal with it
        }
    }
    
    /**
     * Check if an order is eligible for return request.
     * 
     * @param orderId the ID of the order to check
     * @param authentication current user authentication
     * @return eligibility information
     */
    @GetMapping("/orders/{orderId}/eligibility")
    public ResponseEntity<EligibilityDto> checkEligibility(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        log.debug("Checking return eligibility for order {} by user {}", 
                orderId, authentication.getName());
        
        try {
            // Verify the order belongs to the current user (this will be handled by the service)
            User currentUser = getCurrentUser(authentication);
            
            // Get detailed eligibility result
            ReturnEligibilityService.EligibilityResult result = 
                    eligibilityService.getEligibilityResult(orderId);
            
            // Get remaining hours if eligible
            Long remainingHours = result.isEligible() ? 
                    eligibilityService.getRemainingEligibilityHours(orderId) : null;
            
            EligibilityDto eligibilityDto = EligibilityDto.from(result, remainingHours);
            
            log.debug("Eligibility check for order {}: eligible={}, reason={}", 
                    orderId, result.isEligible(), result.getReason());
            
            return ResponseEntity.ok(eligibilityDto);
            
        } catch (Exception e) {
            log.error("Error checking eligibility for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get list of available post offices for return drop-off.
     * Optionally filter by address/location.
     * 
     * @param address optional address filter for finding nearest post offices
     * @return list of available post offices
     */
    @GetMapping("/post-offices")
    public ResponseEntity<List<PostOfficeDto>> getNearestPostOffices(
            @RequestParam(required = false) String address) {
        
        log.debug("Getting post offices with address filter: {}", address);
        
        try {
            List<PostOffice> postOffices;
            
            if (address != null && !address.trim().isEmpty()) {
                // Search by address
                postOffices = postOfficeRepository.findByAddressContainingIgnoreCase(address.trim());
                
                // If no results by address, try by city/area
                if (postOffices.isEmpty()) {
                    postOffices = postOfficeRepository.findByCityContainingIgnoreCase(address.trim());
                }
                
                log.debug("Found {} post offices matching address filter '{}'", 
                        postOffices.size(), address);
            } else {
                // Get all active post offices
                postOffices = postOfficeRepository.findByActiveTrue();
                log.debug("Retrieved {} active post offices", postOffices.size());
            }
            
            List<PostOfficeDto> postOfficeDtos = postOffices.stream()
                    .map(PostOfficeDto::from)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(postOfficeDtos);
            
        } catch (Exception e) {
            log.error("Error retrieving post offices: {}", e.getMessage(), e);
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