package com.mypkga.commerceplatformfull.entity;

/**
 * Enum representing the status of delivery confirmation.
 * Used to track whether a customer has confirmed or rejected delivery.
 */
public enum ConfirmationStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    REJECTED("Đã từ chối");

    private final String displayName;

    ConfirmationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}