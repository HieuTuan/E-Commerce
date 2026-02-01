package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.ghn.GHNWebhookPayload;
import com.mypkga.commerceplatformfull.service.GHNService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to handle webhooks from Giao Hàng Nhanh (GHN)
 * Receives status updates for return shipments
 */
@RestController
@RequestMapping("/api/ghn/webhook")
@RequiredArgsConstructor
@Slf4j
public class GHNWebhookController {
    
    private final GHNService ghnService;
    
    /**
     * Handle status update webhook from GHN
     * This endpoint receives notifications when shipment status changes
     */
    @PostMapping("/status-update")
    public ResponseEntity<String> handleStatusUpdate(@RequestBody GHNWebhookPayload payload) {
        log.info("Received GHN webhook for order: {} with status: {}", 
            payload.getOrderCode(), payload.getStatus());
        
        try {
            // Process the status update
            ghnService.processStatusUpdate(payload);
            
            log.info("Successfully processed GHN webhook for order: {}", payload.getOrderCode());
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing GHN webhook for order: {}", payload.getOrderCode(), e);
            // Return OK to prevent GHN from retrying
            // Log the error for manual investigation
            return ResponseEntity.ok("ERROR_LOGGED");
        }
    }
    
    /**
     * Health check endpoint for GHN webhook
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("GHN Webhook endpoint is healthy");
    }
}