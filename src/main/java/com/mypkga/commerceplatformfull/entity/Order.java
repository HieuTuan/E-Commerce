package com.mypkga.commerceplatformfull.entity;

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

    @Column(columnDefinition = "NVARCHAR(100)")
    private String shippingAddress;

    @Column(length = 100)
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
     * Update the current status and sync with the legacy status field
     */
    public void updateCurrentStatus(OrderStatus newStatus) {
        this.currentStatus = newStatus;
        this.status = newStatus; // Keep legacy field in sync
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

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }
}
