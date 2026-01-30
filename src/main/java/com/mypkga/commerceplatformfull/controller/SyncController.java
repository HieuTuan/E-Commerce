package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.service.DataSynchronizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST Controller for data synchronization operations
 * Provides endpoints for real-time status synchronization and conflict resolution
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final DataSynchronizationService dataSynchronizationService;

    /**
     * Get current synchronized status for an order
     */
    @GetMapping("/order/{orderId}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable Long orderId) {
        try {
            OrderStatus status = dataSynchronizationService.syncOrderStatus(orderId);
            DataSynchronizationService.SyncStatus syncStatus = dataSynchronizationService.getSyncStatus(orderId);
            
            return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", status.name(),
                "displayName", status.getDisplayName(),
                "isConsistent", syncStatus.isConsistent(),
                "lastUpdated", syncStatus.getLastUpdated(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Failed to get order status for sync: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get order status",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Resolve status conflict between client and server
     */
    @PostMapping("/order/{orderId}/resolve-conflict")
    public ResponseEntity<Map<String, Object>> resolveConflict(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> conflictData) {
        
        try {
            String clientStatusStr = (String) conflictData.get("clientStatus");
            String clientTimestampStr = (String) conflictData.get("clientTimestamp");
            
            if (clientStatusStr == null || clientTimestampStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: clientStatus, clientTimestamp"
                ));
            }
            
            OrderStatus clientStatus = OrderStatus.valueOf(clientStatusStr);
            LocalDateTime clientTimestamp = LocalDateTime.parse(clientTimestampStr);
            
            OrderStatus resolvedStatus = dataSynchronizationService.resolveStatusConflict(
                orderId, clientStatus, clientTimestamp);
            
            return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "resolvedStatus", resolvedStatus.name(),
                "displayName", resolvedStatus.getDisplayName(),
                "clientStatus", clientStatus.name(),
                "wasConflict", !resolvedStatus.equals(clientStatus),
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Failed to resolve conflict for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to resolve conflict",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Validate data consistency for an order
     */
    @GetMapping("/order/{orderId}/validate")
    public ResponseEntity<Map<String, Object>> validateConsistency(@PathVariable Long orderId) {
        try {
            boolean isConsistent = dataSynchronizationService.validateDataConsistency(orderId);
            DataSynchronizationService.SyncStatus syncStatus = dataSynchronizationService.getSyncStatus(orderId);
            
            return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "isConsistent", isConsistent,
                "currentStatus", syncStatus.getCurrentStatus().name(),
                "lastUpdated", syncStatus.getLastUpdated(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Failed to validate consistency for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to validate consistency",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Fix data inconsistency for an order (Admin only)
     */
    @PostMapping("/order/{orderId}/fix")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> fixInconsistency(@PathVariable Long orderId) {
        try {
            dataSynchronizationService.fixDataInconsistency(orderId);
            DataSynchronizationService.SyncStatus syncStatus = dataSynchronizationService.getSyncStatus(orderId);
            
            return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "fixed", true,
                "currentStatus", syncStatus.getCurrentStatus().name(),
                "isConsistent", syncStatus.isConsistent(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Failed to fix inconsistency for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fix inconsistency",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Bulk synchronization for all orders (Admin only)
     */
    @PostMapping("/bulk-sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> bulkSync() {
        try {
            dataSynchronizationService.syncAllOrders();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bulk synchronization completed successfully",
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Failed to perform bulk sync: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to perform bulk synchronization",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check for synchronization service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "service", "DataSynchronizationService",
            "status", "healthy",
            "timestamp", LocalDateTime.now()
        ));
    }
}