package com.mypkga.commerceplatformfull.entity;

/**
 * Enum representing the status of an order in the timeline system.
 * This enum defines the standard order statuses used throughout the application.
 */
public enum OrderStatus {
    PENDING("Chờ xử lý"),
    CONFIRMED("Đã xác nhận"), 
    SHIPPING("Đang giao hàng"),
    AWAITING_CONFIRMATION("Chờ xác nhận giao hàng"), // Status for delivery confirmation
    DELIVERED("Đã giao hàng"),
    CONFIRMED_BY_CUSTOMER("Đã xác nhận nhận hàng"), // Customer confirmed receipt
    CANCELLED("Đã hủy"),
    
    // Return and refund related statuses
    REFUND_REQUESTED("Yêu cầu hoàn trả"),
    RETURN_APPROVED("Chấp nhận hoàn trả"),
    RETURNING("Đang hoàn trả"),
    RETURN_RECEIVED("Đã nhận hàng hoàn trả"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this status is a final state (no further transitions allowed)
     */
    public boolean isFinalState() {
        return this == CONFIRMED_BY_CUSTOMER || this == CANCELLED || this == REFUNDED;
    }
    
    /**
     * Check if this status allows return request
     */
    public boolean allowsReturnRequest() {
        return this == DELIVERED;
    }
    
    /**
     * Get valid next statuses for state machine transitions
     */
    public OrderStatus[] getValidNextStatuses() {
        return switch (this) {
            case PENDING -> new OrderStatus[]{CONFIRMED, CANCELLED};
            case CONFIRMED -> new OrderStatus[]{SHIPPING, CANCELLED};
            case SHIPPING -> new OrderStatus[]{AWAITING_CONFIRMATION, CANCELLED};
            case AWAITING_CONFIRMATION -> new OrderStatus[]{DELIVERED, CANCELLED};
            case DELIVERED -> new OrderStatus[]{CONFIRMED_BY_CUSTOMER, REFUND_REQUESTED};
            case REFUND_REQUESTED -> new OrderStatus[]{RETURN_APPROVED, DELIVERED}; // Can be rejected back to DELIVERED
            case RETURN_APPROVED -> new OrderStatus[]{RETURNING};
            case RETURNING -> new OrderStatus[]{RETURN_RECEIVED};
            case RETURN_RECEIVED -> new OrderStatus[]{REFUNDED};
            case CONFIRMED_BY_CUSTOMER, CANCELLED, REFUNDED -> new OrderStatus[]{}; // Final states
        };
    }
    
    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        OrderStatus[] validNext = getValidNextStatuses();
        for (OrderStatus status : validNext) {
            if (status == newStatus) {
                return true;
            }
        }
        return false;
    }
}