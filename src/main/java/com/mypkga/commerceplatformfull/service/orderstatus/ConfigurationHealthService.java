package com.mypkga.commerceplatformfull.service.orderstatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring the health of order status configuration.
 * Provides runtime health checks and configuration status information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationHealthService {
    
    private final SystemStartupValidator systemStartupValidator;
    private final StateTransitionRules stateTransitionRules;
    private final StateTransitionValidator stateTransitionValidator;
    
    private LocalDateTime lastValidationTime;
    private boolean lastValidationResult = false;
    private String lastValidationError;
    
    /**
     * Perform runtime health check of the configuration.
     * This can be called periodically or on-demand to verify system health.
     * 
     * @return true if configuration is healthy, false otherwise
     */
    public boolean performHealthCheck() {
        log.debug("Performing configuration health check...");
        
        try {
            // Perform basic validation
            systemStartupValidator.validateSystemIntegrity();
            
            lastValidationTime = LocalDateTime.now();
            lastValidationResult = true;
            lastValidationError = null;
            
            log.debug("Configuration health check passed");
            return true;
            
        } catch (Exception e) {
            lastValidationTime = LocalDateTime.now();
            lastValidationResult = false;
            lastValidationError = e.getMessage();
            
            log.warn("Configuration health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get detailed health status information
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("healthy", lastValidationResult);
        status.put("lastCheckTime", lastValidationTime);
        status.put("lastError", lastValidationError);
        
        // Add configuration details
        status.put("totalStates", com.mypkga.commerceplatformfull.entity.OrderStatus.values().length);
        status.put("totalRules", stateTransitionRules.getAllRules().size());
        
        // Add system information
        status.put("configurationReport", systemStartupValidator.getSystemConfigurationReport());
        
        return status;
    }
    
    /**
     * Get a simple health status string
     */
    public String getHealthStatusString() {
        if (lastValidationTime == null) {
            return "NOT_CHECKED";
        }
        
        return lastValidationResult ? "HEALTHY" : "UNHEALTHY";
    }
    
    /**
     * Check if configuration validation has been performed
     */
    public boolean hasBeenValidated() {
        return lastValidationTime != null;
    }
    
    /**
     * Get the last validation error message
     */
    public String getLastValidationError() {
        return lastValidationError;
    }
    
    /**
     * Get the time of last validation
     */
    public LocalDateTime getLastValidationTime() {
        return lastValidationTime;
    }
}