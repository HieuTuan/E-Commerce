package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import com.mypkga.commerceplatformfull.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for validating return eligibility based on business rules.
 * Implements time-based validation and order status checks according to requirements 1.1 and 1.3.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnEligibilityService {
    
    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;
    
    /**
     * Check if an order is eligible for return request.
     * 
     * Requirements:
     * - Order must exist
     * - Order status must be DELIVERED (Requirement 1.1)
     * - Return request must be within 2 days of delivery (Requirement 1.3)
     * - Order must not already have a return request
     * 
     * @param orderId the ID of the order to check
     * @return true if the order is eligible for return, false otherwise
     */
    public boolean isEligibleForReturn(Long orderId) {
        log.debug("Checking return eligibility for order ID: {}", orderId);
        
        if (orderId == null) {
            log.debug("Order ID is null, not eligible for return");
            return false;
        }
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.debug("Order with ID {} not found, not eligible for return", orderId);
            return false;
        }
        
        Order order = orderOpt.get();
        return isEligibleForReturn(order);
    }
    
    /**
     * Check if an order is eligible for return request.
     * 
     * @param order the order to check
     * @return true if the order is eligible for return, false otherwise
     */
    public boolean isEligibleForReturn(Order order) {
        if (order == null) {
            log.debug("Order is null, not eligible for return");
            return false;
        }
        
        log.debug("Checking return eligibility for order: {} with status: {}", 
                 order.getId(), order.getCurrentStatus());
        
        // Check if order status is DELIVERED (Requirement 1.1)
        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            log.debug("Order {} status is {}, must be DELIVERED for return eligibility", 
                     order.getId(), order.getCurrentStatus());
            return false;
        }
        
        // Check if order already has a return request
        if (returnRequestRepository.existsByOrderId(order.getId())) {
            log.debug("Order {} already has a return request, not eligible for another", order.getId());
            return false;
        }
        
        // Check if within 2-day eligibility window (Requirement 1.3)
        if (!isWithinEligibilityWindow(order)) {
            log.debug("Order {} is outside the 2-day eligibility window", order.getId());
            return false;
        }
        
        log.debug("Order {} is eligible for return", order.getId());
        return true;
    }
    
    /**
     * Check if the order is within the 2-day eligibility window.
     * Uses the order's updatedDate as the delivery date reference.
     * 
     * @param order the order to check
     * @return true if within 2 days of delivery, false otherwise
     */
    public boolean isWithinEligibilityWindow(Order order) {
        if (order == null || order.getUpdatedDate() == null) {
            log.debug("Order or updated date is null, not within eligibility window");
            return false;
        }
        
        LocalDateTime deliveryDate = order.getUpdatedDate();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eligibilityDeadline = deliveryDate.plusDays(2);
        
        boolean withinWindow = !now.isAfter(eligibilityDeadline);
        
        log.debug("Order {} delivery date: {}, current time: {}, deadline: {}, within window: {}", 
                 order.getId(), deliveryDate, now, eligibilityDeadline, withinWindow);
        
        return withinWindow;
    }
    
    /**
     * Get the remaining time in hours for return eligibility.
     * 
     * @param orderId the ID of the order
     * @return remaining hours for eligibility, or -1 if not eligible or order not found
     */
    public long getRemainingEligibilityHours(Long orderId) {
        if (orderId == null) {
            return -1;
        }
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return -1;
        }
        
        return getRemainingEligibilityHours(orderOpt.get());
    }
    
    /**
     * Get the remaining time in hours for return eligibility.
     * 
     * @param order the order to check
     * @return remaining hours for eligibility, or -1 if not eligible
     */
    public long getRemainingEligibilityHours(Order order) {
        if (order == null || order.getCurrentStatus() != OrderStatus.DELIVERED) {
            return -1;
        }
        
        if (returnRequestRepository.existsByOrderId(order.getId())) {
            return -1; // Already has return request
        }
        
        LocalDateTime deliveryDate = order.getUpdatedDate();
        if (deliveryDate == null) {
            return -1;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eligibilityDeadline = deliveryDate.plusDays(2);
        
        if (now.isAfter(eligibilityDeadline)) {
            return 0; // Expired
        }
        
        // Calculate remaining hours
        long remainingHours = java.time.Duration.between(now, eligibilityDeadline).toHours();
        return Math.max(0, remainingHours);
    }
    
    /**
     * Get a detailed eligibility result with reason if not eligible.
     * 
     * @param orderId the ID of the order to check
     * @return EligibilityResult containing eligibility status and reason
     */
    public EligibilityResult getEligibilityResult(Long orderId) {
        if (orderId == null) {
            return new EligibilityResult(false, "Order ID cannot be null");
        }
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return new EligibilityResult(false, "Order not found");
        }
        
        return getEligibilityResult(orderOpt.get());
    }
    
    /**
     * Get a detailed eligibility result with reason if not eligible.
     * 
     * @param order the order to check
     * @return EligibilityResult containing eligibility status and reason
     */
    public EligibilityResult getEligibilityResult(Order order) {
        if (order == null) {
            return new EligibilityResult(false, "Order cannot be null");
        }
        
        // Check order status
        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            return new EligibilityResult(false, 
                String.format("Order status must be DELIVERED, current status: %s", 
                             order.getCurrentStatus().getDisplayName()));
        }
        
        // Check for existing return request
        if (returnRequestRepository.existsByOrderId(order.getId())) {
            return new EligibilityResult(false, "Order already has a return request");
        }
        
        // Check eligibility window
        if (!isWithinEligibilityWindow(order)) {
            return new EligibilityResult(false, 
                "Return request must be submitted within 2 days of delivery");
        }
        
        long remainingHours = getRemainingEligibilityHours(order);
        return new EligibilityResult(true, 
            String.format("Eligible for return. %d hours remaining.", remainingHours));
    }
    
    /**
     * Result class for detailed eligibility information.
     */
    public static class EligibilityResult {
        private final boolean eligible;
        private final String reason;
        
        public EligibilityResult(boolean eligible, String reason) {
            this.eligible = eligible;
            this.reason = reason;
        }
        
        public boolean isEligible() {
            return eligible;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return String.format("EligibilityResult{eligible=%s, reason='%s'}", eligible, reason);
        }
    }
}