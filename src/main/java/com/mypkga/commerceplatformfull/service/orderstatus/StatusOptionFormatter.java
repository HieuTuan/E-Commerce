package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for formatting status options for different UI contexts.
 * Provides specialized formatting logic for various display scenarios.
 * 
 * Requirements: 4.1 - UI Integration for status options display
 */
@Component
@Slf4j
public class StatusOptionFormatter {
    
    /**
     * Format status options for dropdown/select UI components
     * 
     * @param statusOptions List of status options to format
     * @return Map of status value to display text
     */
    public Map<String, String> formatForDropdown(List<StatusOption> statusOptions) {
        if (statusOptions == null || statusOptions.isEmpty()) {
            return Map.of();
        }
        
        return statusOptions.stream()
            .collect(Collectors.toMap(
                option -> option.getStatus().name(),
                StatusOption::getDisplayName,
                (existing, replacement) -> existing // Keep first value in case of duplicates
            ));
    }
    
    /**
     * Format status options for button UI components
     * Includes special handling for customer action buttons
     * 
     * @param statusOptions List of status options to format
     * @return List of formatted button configurations
     */
    public List<ButtonConfig> formatForButtons(List<StatusOption> statusOptions) {
        if (statusOptions == null || statusOptions.isEmpty()) {
            return List.of();
        }
        
        return statusOptions.stream()
            .map(this::createButtonConfig)
            .collect(Collectors.toList());
    }
    
    /**
     * Format status options for timeline/progress UI components
     * 
     * @param statusOptions List of status options to format
     * @return List of timeline step configurations
     */
    public List<TimelineStep> formatForTimeline(List<StatusOption> statusOptions) {
        if (statusOptions == null || statusOptions.isEmpty()) {
            return List.of();
        }
        
        return statusOptions.stream()
            .map(this::createTimelineStep)
            .collect(Collectors.toList());
    }
    
    /**
     * Get customer-specific status options with appropriate messaging
     * 
     * @param statusOptions List of status options to filter and format
     * @return List of customer-facing status options
     */
    public List<StatusOption> formatForCustomer(List<StatusOption> statusOptions) {
        if (statusOptions == null || statusOptions.isEmpty()) {
            return List.of();
        }
        
        return statusOptions.stream()
            .filter(this::isCustomerRelevant)
            .map(this::enhanceForCustomer)
            .collect(Collectors.toList());
    }
    
    /**
     * Get admin-specific status options with detailed information
     * 
     * @param statusOptions List of status options to format
     * @return List of admin-facing status options
     */
    public List<StatusOption> formatForAdmin(List<StatusOption> statusOptions) {
        if (statusOptions == null || statusOptions.isEmpty()) {
            return List.of();
        }
        
        return statusOptions.stream()
            .map(this::enhanceForAdmin)
            .collect(Collectors.toList());
    }
    
    /**
     * Create button configuration from status option
     */
    private ButtonConfig createButtonConfig(StatusOption option) {
        ButtonConfig config = new ButtonConfig();
        config.setValue(option.getStatus().name());
        config.setText(option.getFormattedDisplayText());
        config.setCssClass(option.getCssClass());
        config.setDisabled(option.isFinalState());
        config.setRequiresConfirmation(option.isNegativeOutcome());
        config.setIsCustomerAction(option.isRequiresCustomerAction());
        
        return config;
    }
    
    /**
     * Create timeline step from status option
     */
    private TimelineStep createTimelineStep(StatusOption option) {
        TimelineStep step = new TimelineStep();
        step.setStatus(option.getStatus().name());
        step.setTitle(option.getDisplayName());
        step.setDescription(option.getDescription());
        step.setCompleted(false); // This would be set based on actual order status
        step.setActive(false); // This would be set based on current order status
        step.setIcon(getIconForStatus(option.getStatus()));
        
        return step;
    }
    
    /**
     * Check if status option is relevant for customer view
     */
    private boolean isCustomerRelevant(StatusOption option) {
        // Hide internal statuses from customers
        return option.getStatus() != OrderStatus.AWAITING_CONFIRMATION;
    }
    
    /**
     * Enhance status option for customer display
     */
    private StatusOption enhanceForCustomer(StatusOption option) {
        StatusOption enhanced = new StatusOption();
        enhanced.setStatus(option.getStatus());
        enhanced.setDisplayName(option.getDisplayName());
        enhanced.setDescription(getCustomerFriendlyDescription(option.getStatus()));
        enhanced.setRequiresCustomerAction(option.isRequiresCustomerAction());
        enhanced.setCssClass(option.getCssClass());
        enhanced.setActionButtonText(option.getActionButtonText());
        
        return enhanced;
    }
    
    /**
     * Enhance status option for admin display
     */
    private StatusOption enhanceForAdmin(StatusOption option) {
        StatusOption enhanced = new StatusOption();
        enhanced.setStatus(option.getStatus());
        enhanced.setDisplayName(option.getDisplayName() + " (" + option.getStatus().name() + ")");
        enhanced.setDescription(option.getDescription() + " [Admin: " + getAdminNotes(option.getStatus()) + "]");
        enhanced.setRequiresCustomerAction(option.isRequiresCustomerAction());
        enhanced.setCssClass(option.getCssClass());
        enhanced.setActionButtonText(option.getActionButtonText());
        
        return enhanced;
    }
    
    /**
     * Get customer-friendly description for status
     */
    private String getCustomerFriendlyDescription(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Chúng tôi đang xử lý đơn hàng của bạn";
            case CONFIRMED -> "Đơn hàng đã được xác nhận và đang chuẩn bị";
            case SHIPPING -> "Đơn hàng đang trên đường giao đến bạn";
            case AWAITING_CONFIRMATION -> "Đơn hàng đã được giao, chờ xác nhận";
            case DELIVERED -> "Đơn hàng đã được giao thành công";
            case CONFIRMED_BY_CUSTOMER -> "Cảm ơn bạn đã xác nhận nhận hàng";
            case CANCELLED -> "Đơn hàng đã được hủy";
            case REFUND_REQUESTED -> "Yêu cầu hoàn trả đang được xử lý";
            case RETURN_APPROVED -> "Yêu cầu hoàn trả đã được chấp nhận";
            case RETURNING -> "Hàng đang được hoàn trả";
            case RETURN_RECEIVED -> "Hàng hoàn trả đã được nhận";
            case REFUNDED -> "Đã hoàn tiền thành công";
        };
    }
    
    /**
     * Get admin notes for status
     */
    private String getAdminNotes(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Awaiting manual confirmation";
            case CONFIRMED -> "Ready for fulfillment";
            case SHIPPING -> "In transit to customer";
            case AWAITING_CONFIRMATION -> "Delivered, waiting for customer confirmation";
            case DELIVERED -> "Delivery completed";
            case CONFIRMED_BY_CUSTOMER -> "Customer confirmed receipt";
            case CANCELLED -> "Order cancelled";
            case REFUND_REQUESTED -> "Return request submitted, awaiting review";
            case RETURN_APPROVED -> "Return approved, awaiting shipment";
            case RETURNING -> "Item being returned to warehouse";
            case RETURN_RECEIVED -> "Return received, processing refund";
            case REFUNDED -> "Refund completed";
        };
    }
    
    /**
     * Get icon class for status
     */
    private String getIconForStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> "fa-clock";
            case CONFIRMED -> "fa-check-circle";
            case SHIPPING -> "fa-truck";
            case AWAITING_CONFIRMATION -> "fa-hourglass-half";
            case DELIVERED -> "fa-box-open";
            case CONFIRMED_BY_CUSTOMER -> "fa-thumbs-up";
            case CANCELLED -> "fa-times-circle";
            case REFUND_REQUESTED -> "fa-undo";
            case RETURN_APPROVED -> "fa-check";
            case RETURNING -> "fa-shipping-fast";
            case RETURN_RECEIVED -> "fa-warehouse";
            case REFUNDED -> "fa-money-bill-wave";
        };
    }
    
    /**
     * Configuration for button UI components
     */
    public static class ButtonConfig {
        private String value;
        private String text;
        private String cssClass;
        private boolean disabled;
        private boolean requiresConfirmation;
        private boolean isCustomerAction;
        
        // Getters and setters
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getCssClass() { return cssClass; }
        public void setCssClass(String cssClass) { this.cssClass = cssClass; }
        
        public boolean isDisabled() { return disabled; }
        public void setDisabled(boolean disabled) { this.disabled = disabled; }
        
        public boolean isRequiresConfirmation() { return requiresConfirmation; }
        public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
        
        public boolean isIsCustomerAction() { return isCustomerAction; }
        public void setIsCustomerAction(boolean isCustomerAction) { this.isCustomerAction = isCustomerAction; }
    }
    
    /**
     * Configuration for timeline UI components
     */
    public static class TimelineStep {
        private String status;
        private String title;
        private String description;
        private boolean completed;
        private boolean active;
        private String icon;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
}