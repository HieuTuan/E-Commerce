package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.service.orderstatus.ConfigurationHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for system health monitoring.
 * Provides endpoints for checking system configuration health.
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
public class SystemHealthController {
    
    private final ConfigurationHealthService configurationHealthService;
    
    /**
     * Get system health status
     * Only accessible by admin users
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        log.debug("System health check requested");
        
        try {
            Map<String, Object> healthStatus = configurationHealthService.getHealthStatus();
            
            // Determine HTTP status based on health
            boolean isHealthy = (Boolean) healthStatus.get("healthy");
            
            if (isHealthy) {
                return ResponseEntity.ok(healthStatus);
            } else {
                return ResponseEntity.status(503).body(healthStatus); // Service Unavailable
            }
            
        } catch (Exception e) {
            log.error("Error checking system health", e);
            return ResponseEntity.status(500).body(Map.of(
                "healthy", false,
                "error", "Failed to check system health: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Perform on-demand health check
     * Only accessible by admin users
     */
    @GetMapping("/health/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performHealthCheck() {
        log.info("On-demand system health check requested");
        
        try {
            boolean isHealthy = configurationHealthService.performHealthCheck();
            Map<String, Object> healthStatus = configurationHealthService.getHealthStatus();
            
            if (isHealthy) {
                return ResponseEntity.ok(healthStatus);
            } else {
                return ResponseEntity.status(503).body(healthStatus);
            }
            
        } catch (Exception e) {
            log.error("Error performing system health check", e);
            return ResponseEntity.status(500).body(Map.of(
                "healthy", false,
                "error", "Failed to perform health check: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get simple health status string
     * Only accessible by admin users
     */
    @GetMapping("/health/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getHealthStatus() {
        try {
            String status = configurationHealthService.getHealthStatusString();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting health status", e);
            return ResponseEntity.status(500).body("ERROR");
        }
    }
}