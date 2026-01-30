package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.entity.OrderTimelineEntry;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for ensuring data consistency between database and UI
 * Implements conflict resolution logic and synchronization mechanisms
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSynchronizationService {

    private final OrderRepository orderRepository;
    private final OrderTimelineService orderTimelineService;
    private final AuditLogService auditLogService;

    /**
     * Synchronize order status with database - ensures UI consistency
     * Returns the authoritative status from database
     */
    @Transactional(readOnly = true)
    public OrderStatus syncOrderStatus(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

            OrderStatus dbStatus = order.getCurrentStatus();
            
            // Log synchronization event
            auditLogService.logDataSynchronization("ORDER_STATUS_SYNC", orderId, 
                "Synchronized order status: " + dbStatus, true);
            
            log.debug("Synchronized order {} status: {}", orderId, dbStatus);
            return dbStatus;
            
        } catch (Exception e) {
            auditLogService.logSystemError("SYNC_ERROR", orderId, 
                "Failed to sync order status", e);
            throw e;
        }
    }

    /**
     * Resolve conflicts when multiple updates occur simultaneously
     * Database timestamp is the source of truth
     */
    @Transactional
    public OrderStatus resolveStatusConflict(Long orderId, OrderStatus clientStatus, LocalDateTime clientTimestamp) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

            OrderStatus dbStatus = order.getCurrentStatus();
            
            // Get latest timeline entry to compare timestamps
            OrderTimelineEntry latestEntry = orderTimelineService.getLatestTimelineEntry(orderId);
            
            if (latestEntry == null) {
                // No timeline entries, use database status
                auditLogService.logDataSynchronization("CONFLICT_RESOLUTION", orderId, 
                    "No timeline entries found, using database status: " + dbStatus, true);
                return dbStatus;
            }

            LocalDateTime dbTimestamp = latestEntry.getUpdatedAt();
            
            // Compare timestamps - database wins in case of conflict
            if (dbTimestamp.isAfter(clientTimestamp)) {
                // Database is newer, reject client update
                auditLogService.logDataSynchronization("CONFLICT_RESOLUTION", orderId, 
                    String.format("Database newer (DB: %s, Client: %s), using DB status: %s", 
                        dbTimestamp, clientTimestamp, dbStatus), true);
                
                log.info("Conflict resolved for order {}: Database status {} is newer than client status {}", 
                    orderId, dbStatus, clientStatus);
                
                return dbStatus;
            } else {
                // Client is newer or same, accept client status
                auditLogService.logDataSynchronization("CONFLICT_RESOLUTION", orderId, 
                    String.format("Client newer or same (DB: %s, Client: %s), accepting client status: %s", 
                        dbTimestamp, clientTimestamp, clientStatus), true);
                
                log.info("Conflict resolved for order {}: Accepting client status {}", orderId, clientStatus);
                return clientStatus;
            }
            
        } catch (Exception e) {
            auditLogService.logSystemError("CONFLICT_RESOLUTION_ERROR", orderId, 
                "Failed to resolve status conflict", e);
            
            // In case of error, return database status as fallback
            return syncOrderStatus(orderId);
        }
    }

    /**
     * Validate data consistency across related entities
     */
    @Transactional(readOnly = true)
    public boolean validateDataConsistency(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

            // Check if order status matches latest timeline entry
            OrderTimelineEntry latestEntry = orderTimelineService.getLatestTimelineEntry(orderId);
            
            if (latestEntry == null) {
                // No timeline entries - this might be inconsistent
                auditLogService.logDataSynchronization("CONSISTENCY_CHECK", orderId, 
                    "No timeline entries found for order", false);
                return false;
            }

            boolean isConsistent = order.getCurrentStatus() == latestEntry.getStatus();
            
            auditLogService.logDataSynchronization("CONSISTENCY_CHECK", orderId, 
                String.format("Order status: %s, Latest timeline: %s, Consistent: %s", 
                    order.getCurrentStatus(), latestEntry.getStatus(), isConsistent), isConsistent);
            
            if (!isConsistent) {
                log.warn("Data inconsistency detected for order {}: Order status {} != Timeline status {}", 
                    orderId, order.getCurrentStatus(), latestEntry.getStatus());
            }
            
            return isConsistent;
            
        } catch (Exception e) {
            auditLogService.logSystemError("CONSISTENCY_CHECK_ERROR", orderId, 
                "Failed to validate data consistency", e);
            return false;
        }
    }

    /**
     * Fix data inconsistencies by making database the source of truth
     */
    @Transactional
    public void fixDataInconsistency(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

            OrderTimelineEntry latestEntry = orderTimelineService.getLatestTimelineEntry(orderId);
            
            if (latestEntry == null) {
                // Create initial timeline entry based on current order status
                orderTimelineService.createTimelineEntry(orderId, order.getCurrentStatus(), "SYSTEM", 
                    "Timeline entry created during consistency fix");
                
                auditLogService.logDataSynchronization("CONSISTENCY_FIX", orderId, 
                    "Created missing timeline entry for status: " + order.getCurrentStatus(), true);
                
                log.info("Fixed missing timeline entry for order {}", orderId);
                return;
            }

            // If statuses don't match, update order to match latest timeline
            if (order.getCurrentStatus() != latestEntry.getStatus()) {
                OrderStatus oldStatus = order.getCurrentStatus();
                order.updateCurrentStatus(latestEntry.getStatus());
                orderRepository.save(order);
                
                auditLogService.logDataSynchronization("CONSISTENCY_FIX", orderId, 
                    String.format("Updated order status from %s to %s to match timeline", 
                        oldStatus, latestEntry.getStatus()), true);
                
                log.info("Fixed order {} status inconsistency: {} -> {}", 
                    orderId, oldStatus, latestEntry.getStatus());
            }
            
        } catch (Exception e) {
            auditLogService.logSystemError("CONSISTENCY_FIX_ERROR", orderId, 
                "Failed to fix data inconsistency", e);
            throw e;
        }
    }

    /**
     * Bulk synchronization for all orders - maintenance operation
     */
    @Transactional
    public void syncAllOrders() {
        try {
            List<Order> allOrders = orderRepository.findAll();
            int fixedCount = 0;
            int errorCount = 0;
            
            for (Order order : allOrders) {
                try {
                    if (!validateDataConsistency(order.getId())) {
                        fixDataInconsistency(order.getId());
                        fixedCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to sync order {}: {}", order.getId(), e.getMessage());
                }
            }
            
            auditLogService.logDataSynchronization("BULK_SYNC", null, 
                String.format("Bulk sync completed: %d orders processed, %d fixed, %d errors", 
                    allOrders.size(), fixedCount, errorCount), errorCount == 0);
            
            log.info("Bulk synchronization completed: {} orders processed, {} fixed, {} errors", 
                allOrders.size(), fixedCount, errorCount);
                
        } catch (Exception e) {
            auditLogService.logSystemError("BULK_SYNC_ERROR", null, 
                "Failed to perform bulk synchronization", e);
            throw e;
        }
    }

    /**
     * Get synchronization status for an order
     */
    @Transactional(readOnly = true)
    public SyncStatus getSyncStatus(Long orderId) {
        try {
            boolean isConsistent = validateDataConsistency(orderId);
            OrderStatus currentStatus = syncOrderStatus(orderId);
            OrderTimelineEntry latestEntry = orderTimelineService.getLatestTimelineEntry(orderId);
            
            return new SyncStatus(orderId, isConsistent, currentStatus, 
                latestEntry != null ? latestEntry.getUpdatedAt() : null);
                
        } catch (Exception e) {
            auditLogService.logSystemError("SYNC_STATUS_ERROR", orderId, 
                "Failed to get sync status", e);
            throw e;
        }
    }

    /**
     * Data class for synchronization status
     */
    public static class SyncStatus {
        private final Long orderId;
        private final boolean isConsistent;
        private final OrderStatus currentStatus;
        private final LocalDateTime lastUpdated;

        public SyncStatus(Long orderId, boolean isConsistent, OrderStatus currentStatus, LocalDateTime lastUpdated) {
            this.orderId = orderId;
            this.isConsistent = isConsistent;
            this.currentStatus = currentStatus;
            this.lastUpdated = lastUpdated;
        }

        // Getters
        public Long getOrderId() { return orderId; }
        public boolean isConsistent() { return isConsistent; }
        public OrderStatus getCurrentStatus() { return currentStatus; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }
}