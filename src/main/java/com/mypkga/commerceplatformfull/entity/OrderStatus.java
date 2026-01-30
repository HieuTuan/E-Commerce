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
    CANCELLED("Đã hủy");

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
        return this == CONFIRMED_BY_CUSTOMER || this == CANCELLED;
    }
}