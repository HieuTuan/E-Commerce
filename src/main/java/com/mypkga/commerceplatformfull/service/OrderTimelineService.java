package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.entity.OrderTimelineEntry;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import com.mypkga.commerceplatformfull.repository.OrderTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing order timeline operations.
 * Handles timeline entry creation, status updates, and timeline retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimelineService {

    private final OrderTimelineRepository orderTimelineRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    /**
     * Create a new timeline entry for an order
     */
    @Transactional
    public OrderTimelineEntry createTimelineEntry(Long orderId, OrderStatus status, String updatedBy) {
        return createTimelineEntry(orderId, status, updatedBy, null);
    }

    /**
     * Create a new timeline entry for an order with notes
     */
    @Transactional
    public OrderTimelineEntry createTimelineEntry(Long orderId, OrderStatus status, String updatedBy, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        OrderTimelineEntry entry = new OrderTimelineEntry(order, status, updatedBy, notes);
        OrderTimelineEntry savedEntry = orderTimelineRepository.save(entry);
        
        // Audit log the timeline entry creation
        auditLogService.logTimelineEntryCreation(orderId, status, updatedBy, notes);
        
        log.info("Created timeline entry for order {}: {} by {}", orderId, status, updatedBy);
        return savedEntry;
    }

    /**
     * Get complete timeline for an order, sorted by timestamp (newest first)
     */
    public List<OrderTimelineEntry> getOrderTimeline(Long orderId) {
        return orderTimelineRepository.findByOrderIdOrderByUpdatedAtDesc(orderId);
    }

    /**
     * Update order status and create timeline entry
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String updatedBy) {
        updateOrderStatus(orderId, newStatus, updatedBy, null);
    }

    /**
     * Update order status and create timeline entry with notes
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String updatedBy, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        OrderStatus oldStatus = order.getCurrentStatus();
        
        // Validate status transition
        if (!canUpdateStatus(oldStatus, newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot update order %d from %s to %s", 
                    orderId, oldStatus, newStatus));
        }

        // Update order status
        order.updateCurrentStatus(newStatus);
        orderRepository.save(order);

        // Create timeline entry
        createTimelineEntry(orderId, newStatus, updatedBy, notes);

        // Audit log the status change
        auditLogService.logOrderStatusChange(orderId, oldStatus, newStatus, updatedBy, notes);

        log.info("Updated order {} status from {} to {} by {}", 
            orderId, oldStatus, newStatus, updatedBy);
    }

    /**
     * Check if order status can be updated to new status
     */
    public boolean canUpdateStatus(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        // Cannot update from final states
        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.DELIVERED) {
            return false;
        }

        // Cannot go backwards in normal flow (except to CANCELLED)
        if (newStatus == OrderStatus.CANCELLED) {
            return true; // Can always cancel (except from final states above)
        }

        // Normal progression validation
        return switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.SHIPPING || newStatus == OrderStatus.CANCELLED;
            case SHIPPING -> newStatus == OrderStatus.AWAITING_CONFIRMATION || newStatus == OrderStatus.CANCELLED;
            case AWAITING_CONFIRMATION -> newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED;
            default -> false;
        };
    }

    /**
     * Get timeline entries by status
     */
    public List<OrderTimelineEntry> getTimelineByStatus(OrderStatus status) {
        return orderTimelineRepository.findByStatusOrderByUpdatedAtDesc(status);
    }

    /**
     * Get latest timeline entry for an order
     */
    public OrderTimelineEntry getLatestTimelineEntry(Long orderId) {
        List<OrderTimelineEntry> timeline = getOrderTimeline(orderId);
        return timeline.isEmpty() ? null : timeline.get(0);
    }

    /**
     * Sync status update across all clients (placeholder for real-time updates)
     */
    public void syncStatusUpdate(Long orderId, OrderStatus status) {
        // Log synchronization event
        auditLogService.logDataSynchronization("STATUS_UPDATE_SYNC", orderId, 
            "Status update synchronized: " + status, true);
        
        // TODO: Implement real-time synchronization (WebSocket, Server-Sent Events, etc.)
        // This could broadcast to connected clients about the status change
        log.info("Syncing status update for order {}: {}", orderId, status);
    }

    /**
     * Auto-confirm delivery after 24 hours if customer doesn't respond
     */
    @Transactional
    public void autoConfirmDeliveryIfExpired(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (order.getCurrentStatus() == OrderStatus.AWAITING_CONFIRMATION) {
            // Check if 24 hours have passed since AWAITING_CONFIRMATION status
            OrderTimelineEntry latestEntry = getLatestTimelineEntry(orderId);
            if (latestEntry != null && latestEntry.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                long hoursSinceAwaitingConfirmation = java.time.Duration.between(
                    latestEntry.getUpdatedAt(), 
                    java.time.LocalDateTime.now()
                ).toHours();

                if (hoursSinceAwaitingConfirmation >= 24) {
                    // Auto-confirm delivery
                    updateOrderStatus(orderId, OrderStatus.DELIVERED, "SYSTEM", 
                        "Tự động xác nhận giao hàng sau 24 giờ không có phản hồi từ khách hàng");
                    
                    log.info("Auto-confirmed delivery for order {} after 24 hours", orderId);
                }
            }
        }
    }

    /**
     * Customer confirms delivery receipt
     */
    @Transactional
    public void confirmDeliveryByCustomer(Long orderId, String customerEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order is not in delivered status");
        }

        // Verify customer owns this order
        if (!order.getUser().getEmail().equals(customerEmail)) {
            throw new IllegalStateException("Customer not authorized to confirm this order");
        }

        // Update to CONFIRMED_BY_CUSTOMER status
        order.updateCurrentStatus(OrderStatus.CONFIRMED_BY_CUSTOMER);
        orderRepository.save(order);
        
        // Create timeline entry
        createTimelineEntry(orderId, OrderStatus.CONFIRMED_BY_CUSTOMER, customerEmail, 
            "Khách hàng xác nhận đã nhận hàng thành công");
        
        log.info("Customer {} confirmed delivery for order {}", customerEmail, orderId);
    }

    /**
     * Customer rejects delivery
     */
    @Transactional
    public void rejectDeliveryByCustomer(Long orderId, String customerEmail, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order is not in delivered status");
        }

        // Verify customer owns this order
        if (!order.getUser().getEmail().equals(customerEmail)) {
            throw new IllegalStateException("Customer not authorized to reject this order");
        }
        
        // Mark order as having delivery issue
        order.setHasDeliveryIssue(true);
        
        // Update to CANCELLED status
        order.updateCurrentStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // Create timeline entry
        createTimelineEntry(orderId, OrderStatus.CANCELLED, customerEmail, 
            "Khách hàng từ chối nhận hàng: " + reason);
        
        log.info("Customer {} rejected delivery for order {}: {}", customerEmail, orderId, reason);
    }
}