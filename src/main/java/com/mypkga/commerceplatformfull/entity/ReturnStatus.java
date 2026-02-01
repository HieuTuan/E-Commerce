package com.mypkga.commerceplatformfull.entity;

/**
 * Enum representing the status of a return request.
 * This tracks the lifecycle of a return request from submission to completion.
 */
public enum ReturnStatus {
    REFUND_REQUESTED("Yêu cầu hoàn trả"),
    RETURN_APPROVED("Chấp nhận hoàn trả"),
    RETURNING("Đang hoàn trả"),
    RETURN_RECEIVED("Đã nhận hàng hoàn trả"),
    REFUNDED("Đã hoàn tiền"),
    REFUND_REJECTED("Từ chối hoàn trả"),
    RETURN_FAILED("Hoàn trả thất bại");

    private final String displayName;

    ReturnStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFinalState() {
        return this == REFUNDED || this == REFUND_REJECTED || this == RETURN_FAILED;
    }
    
    /**
     * Get valid next statuses for state machine transitions
     */
    public ReturnStatus[] getValidNextStatuses() {
        return switch (this) {
            case REFUND_REQUESTED -> new ReturnStatus[]{RETURN_APPROVED, REFUND_REJECTED};
            case RETURN_APPROVED -> new ReturnStatus[]{RETURNING};
            case RETURNING -> new ReturnStatus[]{RETURN_RECEIVED, RETURN_FAILED};
            case RETURN_RECEIVED -> new ReturnStatus[]{REFUNDED};
            case REFUNDED, REFUND_REJECTED, RETURN_FAILED -> new ReturnStatus[]{}; // Final states
        };
    }
    
    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(ReturnStatus newStatus) {
        ReturnStatus[] validNext = getValidNextStatuses();
        for (ReturnStatus status : validNext) {
            if (status == newStatus) {
                return true;
            }
        }
        return false;
    }
}