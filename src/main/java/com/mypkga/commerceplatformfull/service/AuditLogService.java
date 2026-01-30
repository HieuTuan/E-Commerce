package com.mypkga.commerceplatformfull.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for structured audit logging of order status changes
 * Implements JSON-formatted logging for better parsing and analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final ObjectMapper objectMapper;

    /**
     * Log order status change with structured JSON format
     */
    public void logOrderStatusChange(Long orderId, OrderStatus oldStatus, OrderStatus newStatus, 
                                   String updatedBy, String reason) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("eventType", "ORDER_STATUS_CHANGE");
            auditLog.put("timestamp", LocalDateTime.now().toString());
            auditLog.put("orderId", orderId);
            auditLog.put("oldStatus", oldStatus != null ? oldStatus.name() : null);
            auditLog.put("newStatus", newStatus.name());
            auditLog.put("updatedBy", updatedBy);
            auditLog.put("reason", reason);
            auditLog.put("sessionId", getCurrentSessionId());
            
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.info("AUDIT_LOG: {}", jsonLog);
            
        } catch (Exception e) {
            log.error("Failed to create audit log for order status change", e);
        }
    }

    /**
     * Log delivery confirmation events
     */
    public void logDeliveryConfirmation(Long orderId, String action, String customerId, String reason) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("eventType", "DELIVERY_CONFIRMATION");
            auditLog.put("timestamp", LocalDateTime.now().toString());
            auditLog.put("orderId", orderId);
            auditLog.put("action", action); // CONFIRMED, REJECTED
            auditLog.put("customerId", customerId);
            auditLog.put("reason", reason);
            auditLog.put("sessionId", getCurrentSessionId());
            
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.info("AUDIT_LOG: {}", jsonLog);
            
        } catch (Exception e) {
            log.error("Failed to create audit log for delivery confirmation", e);
        }
    }

    /**
     * Log timeline entry creation
     */
    public void logTimelineEntryCreation(Long orderId, OrderStatus status, String updatedBy, String notes) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("eventType", "TIMELINE_ENTRY_CREATED");
            auditLog.put("timestamp", LocalDateTime.now().toString());
            auditLog.put("orderId", orderId);
            auditLog.put("status", status.name());
            auditLog.put("updatedBy", updatedBy);
            auditLog.put("notes", notes);
            auditLog.put("sessionId", getCurrentSessionId());
            
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.info("AUDIT_LOG: {}", jsonLog);
            
        } catch (Exception e) {
            log.error("Failed to create audit log for timeline entry creation", e);
        }
    }

    /**
     * Log data synchronization events
     */
    public void logDataSynchronization(String syncType, Long orderId, String details, boolean success) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("eventType", "DATA_SYNCHRONIZATION");
            auditLog.put("timestamp", LocalDateTime.now().toString());
            auditLog.put("syncType", syncType);
            auditLog.put("orderId", orderId);
            auditLog.put("details", details);
            auditLog.put("success", success);
            auditLog.put("sessionId", getCurrentSessionId());
            
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.info("AUDIT_LOG: {}", jsonLog);
            
        } catch (Exception e) {
            log.error("Failed to create audit log for data synchronization", e);
        }
    }

    /**
     * Log system errors and conflicts
     */
    public void logSystemError(String errorType, Long orderId, String errorMessage, Exception exception) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("eventType", "SYSTEM_ERROR");
            auditLog.put("timestamp", LocalDateTime.now().toString());
            auditLog.put("errorType", errorType);
            auditLog.put("orderId", orderId);
            auditLog.put("errorMessage", errorMessage);
            auditLog.put("exceptionClass", exception != null ? exception.getClass().getSimpleName() : null);
            auditLog.put("stackTrace", exception != null ? exception.getMessage() : null);
            auditLog.put("sessionId", getCurrentSessionId());
            
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.error("AUDIT_LOG: {}", jsonLog);
            
        } catch (Exception e) {
            log.error("Failed to create audit log for system error", e);
        }
    }

    /**
     * Log system errors without exception
     */
    public void logSystemError(String errorType, Long orderId, String errorMessage) {
        logSystemError(errorType, orderId, errorMessage, null);
    }

    /**
     * Log invalid status transition attempts
     */
    public void logInvalidStatusTransition(Long orderId, OrderStatus currentStatus, OrderStatus attemptedStatus, 
                                         String userId, String errorMessage) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("eventType", "INVALID_STATUS_TRANSITION");
            auditLog.put("timestamp", LocalDateTime.now().toString());
            auditLog.put("orderId", orderId);
            auditLog.put("currentStatus", currentStatus != null ? currentStatus.name() : null);
            auditLog.put("attemptedStatus", attemptedStatus != null ? attemptedStatus.name() : null);
            auditLog.put("userId", userId);
            auditLog.put("errorMessage", errorMessage);
            auditLog.put("sessionId", getCurrentSessionId());
            
            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.warn("AUDIT_LOG: {}", jsonLog);
            
        } catch (Exception e) {
            log.error("Failed to create audit log for invalid status transition", e);
        }
    }

    /**
     * Get current session ID for tracking
     * In a real implementation, this would get the actual session ID
     */
    private String getCurrentSessionId() {
        // For now, return a placeholder
        // In production, this would integrate with Spring Security to get actual session
        return "SESSION_" + System.currentTimeMillis();
    }
}