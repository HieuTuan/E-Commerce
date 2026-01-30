package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a status option for UI display.
 * Contains display information for order status transitions.
 * 
 * Requirements: 4.1 - UI Integration for status options display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusOption {
    
    /**
     * The order status
     */
    private OrderStatus status;
    
    /**
     * Display name for the UI
     */
    private String displayName;
    
    /**
     * Description of what this status means
     */
    private String description;
    
    /**
     * Indicates if this option requires special customer action (e.g., delivery confirmation)
     */
    private boolean requiresCustomerAction;
    
    /**
     * CSS class for styling the status option in UI
     */
    private String cssClass;
    
    /**
     * Button text for customer actions (e.g., "Xác nhận đã nhận hàng")
     */
    private String actionButtonText;
    
    /**
     * Create a StatusOption from an OrderStatus with default descriptions
     * 
     * @param status The OrderStatus to create option for
     * @return StatusOption with default display properties
     */
    public static StatusOption fromOrderStatus(OrderStatus status) {
        String displayName = getDisplayName(status);
        String description = getDefaultDescription(status);
        boolean requiresCustomerAction = requiresCustomerAction(status);
        String cssClass = getCssClass(status);
        String actionButtonText = getActionButtonText(status);
        
        StatusOption option = new StatusOption();
        option.setStatus(status);
        option.setDisplayName(displayName);
        option.setDescription(description);
        option.setRequiresCustomerAction(requiresCustomerAction);
        option.setCssClass(cssClass);
        option.setActionButtonText(actionButtonText);
        
        return option;
    }
    
    /**
     * Create a StatusOption for customer delivery confirmation
     * Special factory method for DELIVERED status when customer needs to confirm
     * 
     * @return StatusOption configured for customer delivery confirmation
     */
    public static StatusOption createCustomerDeliveryConfirmation() {
        StatusOption option = new StatusOption();
        option.setStatus(OrderStatus.CONFIRMED_BY_CUSTOMER);
        option.setDisplayName("Xác nhận đã nhận hàng");
        option.setDescription("Xác nhận rằng bạn đã nhận được đơn hàng và hài lòng với sản phẩm");
        option.setRequiresCustomerAction(true);
        option.setCssClass("btn-success customer-confirm");
        option.setActionButtonText("Xác nhận đã nhận hàng");
        
        return option;
    }
    
    /**
     * Get display name for each status with Vietnamese localization
     */
    private static String getDisplayName(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case SHIPPING -> "Đang giao hàng";
            case AWAITING_CONFIRMATION -> "Chờ xác nhận nhận hàng";
            case DELIVERED -> "Đã giao hàng";
            case CONFIRMED_BY_CUSTOMER -> "Đã xác nhận nhận hàng";
            case CANCELLED -> "Đã hủy";
        };
    }
    
    /**
     * Get default description for each status with Vietnamese localization
     */
    private static String getDefaultDescription(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Đơn hàng đang chờ được xác nhận";
            case CONFIRMED -> "Đơn hàng đã được xác nhận và đang được chuẩn bị";
            case SHIPPING -> "Đơn hàng đang được vận chuyển đến khách hàng";
            case AWAITING_CONFIRMATION -> "Đơn hàng đã được giao, đang chờ khách hàng xác nhận";
            case DELIVERED -> "Đơn hàng đã được giao đến khách hàng";
            case CONFIRMED_BY_CUSTOMER -> "Khách hàng đã xác nhận nhận được đơn hàng";
            case CANCELLED -> "Đơn hàng đã bị hủy";
        };
    }
    
    /**
     * Determine if status requires customer action
     */
    private static boolean requiresCustomerAction(OrderStatus status) {
        return status == OrderStatus.CONFIRMED_BY_CUSTOMER;
    }
    
    /**
     * Get CSS class for styling status options
     */
    private static String getCssClass(OrderStatus status) {
        return switch (status) {
            case PENDING -> "btn-warning status-pending";
            case CONFIRMED -> "btn-info status-confirmed";
            case SHIPPING -> "btn-primary status-shipping";
            case AWAITING_CONFIRMATION -> "btn-secondary status-awaiting";
            case DELIVERED -> "btn-success status-delivered";
            case CONFIRMED_BY_CUSTOMER -> "btn-success status-confirmed-customer";
            case CANCELLED -> "btn-danger status-cancelled";
        };
    }
    
    /**
     * Get action button text for customer actions
     */
    private static String getActionButtonText(OrderStatus status) {
        return switch (status) {
            case CONFIRMED_BY_CUSTOMER -> "Xác nhận đã nhận hàng";
            case CANCELLED -> "Hủy đơn hàng";
            default -> null;
        };
    }
    
    /**
     * Check if this status option is for a final state
     */
    public boolean isFinalState() {
        return status != null && status.isFinalState();
    }
    
    /**
     * Check if this status option represents a positive outcome
     */
    public boolean isPositiveOutcome() {
        return status == OrderStatus.CONFIRMED || 
               status == OrderStatus.DELIVERED || 
               status == OrderStatus.CONFIRMED_BY_CUSTOMER;
    }
    
    /**
     * Check if this status option represents a negative outcome
     */
    public boolean isNegativeOutcome() {
        return status == OrderStatus.CANCELLED;
    }
    
    /**
     * Get formatted display text for UI components
     */
    public String getFormattedDisplayText() {
        if (requiresCustomerAction && actionButtonText != null) {
            return actionButtonText;
        }
        return displayName;
    }
}
