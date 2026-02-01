package com.mypkga.commerceplatformfull.entity;

import com.mypkga.commerceplatformfull.util.HtmlUtilsHelper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    // Current status field for timeline tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private OrderStatus currentStatus = OrderStatus.PENDING;

    @Column(length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String shippingAddress;

    @Column(length = 100,columnDefinition = "NVARCHAR(100)")
    private String customerName;

    @Column(length = 20)
    private String customerPhone;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    // New fields for timeline and delivery confirmation
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("updatedAt DESC")
    private List<OrderTimelineEntry> timeline = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DeliveryConfirmation deliveryConfirmation;

    // Return request relationship
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReturnRequest returnRequest;

    // Delivery issue tracking
    @Column(name = "has_delivery_issue", nullable = false)
    private Boolean hasDeliveryIssue = false;
    
    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DeliveryIssueReport> deliveryIssues = new ArrayList<>();

    // Helper methods for timeline and status management
    
    /**
     * Add a timeline entry to this order
     */
    public void addTimelineEntry(OrderTimelineEntry entry) {
        timeline.add(entry);
        entry.setOrder(this);
    }

    /**
     * Check if the order status can be updated to a new status
     */
    public boolean canUpdateStatus(OrderStatus newStatus) {
        return !currentStatus.isFinalState();
    }

    /**
     * Update the current status without affecting the legacy status field
     * This avoids CHECK constraint violations for return-related statuses
     */
    public void updateCurrentStatus(OrderStatus newStatus) {
        this.currentStatus = newStatus;
        // Don't update the legacy status field to avoid CHECK constraint issues
        // The currentStatus field is the source of truth for order state
    }

    /**
     * Get the current status
     */
    public OrderStatus getCurrentStatus() {
        return this.currentStatus;
    }

    /**
     * Get the most recent timeline entry
     */
    public OrderTimelineEntry getLatestTimelineEntry() {
        return timeline.isEmpty() ? null : timeline.get(0);
    }

    /**
     * Check if delivery confirmation is required (order is delivered but not confirmed)
     */
    public boolean requiresDeliveryConfirmation() {
        return currentStatus == OrderStatus.DELIVERED && 
               (deliveryConfirmation == null || deliveryConfirmation.isPending());
    }
    
    /**
     * Check if this order is eligible for return request
     */
    public boolean isEligibleForReturn() {
        // Must be delivered and within 2 days of delivery
        if (currentStatus != OrderStatus.DELIVERED) {
            return false;
        }
        
        // Check if already has a return request
        if (returnRequest != null) {
            return false;
        }
        
        // Check delivery date (assuming updatedDate reflects delivery date for DELIVERED status)
        LocalDateTime deliveryDate = updatedDate;
        LocalDateTime now = LocalDateTime.now();
        return deliveryDate.plusDays(2).isAfter(now);
    }
    
    /**
     * Check if this order has a return request
     */
    public boolean hasReturnRequest() {
        return returnRequest != null;
    }
    
    /**
     * Get the return request for this order
     */
    public ReturnRequest getReturnRequest() {
        return returnRequest;
    }
    
    /**
     * Check if this order has delivery issues
     */
    public boolean hasDeliveryIssue() {
        return hasDeliveryIssue != null && hasDeliveryIssue;
    }
    
    /**
     * Set delivery issue flag
     */
    public void setHasDeliveryIssue(boolean hasDeliveryIssue) {
        this.hasDeliveryIssue = hasDeliveryIssue;
    }
    
    /**
     * Get all delivery issue reports for this order
     */
    public List<DeliveryIssueReport> getDeliveryIssues() {
        return deliveryIssues != null ? deliveryIssues : new ArrayList<>();
    }
    
    /**
     * Get the latest delivery issue report
     */
    public DeliveryIssueReport getLatestDeliveryIssue() {
        return deliveryIssues.isEmpty() ? null : 
               deliveryIssues.stream()
                   .max((r1, r2) -> r1.getReportedAt().compareTo(r2.getReportedAt()))
                   .orElse(null);
    }

    /**
     * Get decoded shipping address for display
     */
    public String getDecodedShippingAddress() {
        return HtmlUtilsHelper.decodeHtml(shippingAddress);
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }
}
