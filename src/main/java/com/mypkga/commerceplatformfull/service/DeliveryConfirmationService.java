package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.ConfirmationStatus;
import com.mypkga.commerceplatformfull.entity.DeliveryConfirmation;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.repository.DeliveryConfirmationRepository;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing delivery confirmation operations.
 * Handles confirmation requests, customer confirmations, and rejections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryConfirmationService {

    private final DeliveryConfirmationRepository deliveryConfirmationRepository;
    private final OrderRepository orderRepository;
    private final OrderTimelineService orderTimelineService;
    private final AuditLogService auditLogService;

    /**
     * Create a delivery confirmation request for an order
     */
    @Transactional
    public DeliveryConfirmation createConfirmationRequest(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        // Validate order is in DELIVERED status
        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot create confirmation request for order not in DELIVERED status");
        }

        // Check if confirmation already exists
        if (deliveryConfirmationRepository.existsByOrderId(orderId)) {
            throw new IllegalStateException("Delivery confirmation already exists for order: " + orderId);
        }

        DeliveryConfirmation confirmation = new DeliveryConfirmation(order);
        DeliveryConfirmation savedConfirmation = deliveryConfirmationRepository.save(confirmation);

        // Create timeline entry
        orderTimelineService.createTimelineEntry(orderId, OrderStatus.DELIVERED, "SYSTEM", 
            "Delivery confirmation request created");

        log.info("Created delivery confirmation request for order {}", orderId);
        return savedConfirmation;
    }

    /**
     * Confirm delivery by customer
     */
    @Transactional
    public void confirmDelivery(Long orderId, Long customerId) {
        DeliveryConfirmation confirmation = getConfirmationForOrder(orderId);
        
        // Validate ownership
        if (!canConfirmDelivery(orderId, customerId)) {
            throw new IllegalArgumentException("Customer does not have permission to confirm this delivery");
        }

        // Validate status
        if (!confirmation.isPending()) {
            throw new IllegalStateException("Delivery confirmation is not in pending status");
        }

        confirmation.confirmDelivery();
        deliveryConfirmationRepository.save(confirmation);

        // Create timeline entry
        orderTimelineService.createTimelineEntry(orderId, OrderStatus.DELIVERED, "CUSTOMER", 
            "Delivery confirmed by customer");

        // Audit log the confirmation
        auditLogService.logDeliveryConfirmation(orderId, "CONFIRMED", customerId.toString(), null);

        log.info("Customer {} confirmed delivery for order {}", customerId, orderId);
    }

    /**
     * Reject delivery by customer with reason
     */
    @Transactional
    public void rejectDelivery(Long orderId, Long customerId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        DeliveryConfirmation confirmation = getConfirmationForOrder(orderId);
        
        // Validate ownership
        if (!canConfirmDelivery(orderId, customerId)) {
            throw new IllegalArgumentException("Customer does not have permission to reject this delivery");
        }

        // Validate status
        if (!confirmation.isPending()) {
            throw new IllegalStateException("Delivery confirmation is not in pending status");
        }

        confirmation.rejectDelivery(reason);
        deliveryConfirmationRepository.save(confirmation);

        // Create timeline entry
        orderTimelineService.createTimelineEntry(orderId, OrderStatus.DELIVERED, "CUSTOMER", 
            "Delivery rejected by customer: " + reason);

        // Audit log the rejection
        auditLogService.logDeliveryConfirmation(orderId, "REJECTED", customerId.toString(), reason);

        log.info("Customer {} rejected delivery for order {} with reason: {}", customerId, orderId, reason);
    }

    /**
     * Check if customer can confirm/reject delivery for this order
     */
    public boolean canConfirmDelivery(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        return order.getUser().getId().equals(customerId);
    }

    /**
     * Get delivery confirmation status for an order
     */
    public DeliveryConfirmation getConfirmationStatus(Long orderId) {
        return deliveryConfirmationRepository.findByOrderId(orderId).orElse(null);
    }

    /**
     * Get all pending delivery confirmations
     */
    public List<DeliveryConfirmation> getAllPendingConfirmations() {
        return deliveryConfirmationRepository.findAllPending();
    }

    /**
     * Get all rejected confirmations with reasons
     */
    public List<DeliveryConfirmation> getAllRejectedConfirmations() {
        return deliveryConfirmationRepository.findAllRejectedWithReasons();
    }

    /**
     * Get confirmations by status
     */
    public List<DeliveryConfirmation> getConfirmationsByStatus(ConfirmationStatus status) {
        return deliveryConfirmationRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Check if order requires delivery confirmation
     */
    public boolean requiresDeliveryConfirmation(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        return order.requiresDeliveryConfirmation();
    }

    /**
     * Auto-create confirmation requests for delivered orders without confirmations
     */
    @Transactional
    public void createMissingConfirmationRequests() {
        List<Order> deliveredOrders = orderRepository.findByCurrentStatus(OrderStatus.DELIVERED);
        
        for (Order order : deliveredOrders) {
            if (!deliveryConfirmationRepository.existsByOrderId(order.getId())) {
                try {
                    createConfirmationRequest(order.getId());
                } catch (Exception e) {
                    log.warn("Failed to create confirmation request for order {}: {}", 
                        order.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Get delivery confirmation for order (internal helper)
     */
    private DeliveryConfirmation getConfirmationForOrder(Long orderId) {
        return deliveryConfirmationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery confirmation not found for order: " + orderId));
    }
}