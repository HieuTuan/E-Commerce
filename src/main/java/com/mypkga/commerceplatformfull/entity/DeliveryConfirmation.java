package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing delivery confirmation from customers.
 * Tracks whether a customer has confirmed or rejected delivery of their order.
 */
@Entity
@Table(name = "delivery_confirmations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfirmationStatus status = ConfirmationStatus.PENDING;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(500)")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Constructor for creating a pending delivery confirmation
     */
    public DeliveryConfirmation(Order order) {
        this.order = order;
        this.status = ConfirmationStatus.PENDING;
    }

    /**
     * Confirm the delivery
     */
    public void confirmDelivery() {
        this.status = ConfirmationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.rejectionReason = null; // Clear any previous rejection reason
    }

    /**
     * Reject the delivery with a reason
     */
    public void rejectDelivery(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason cannot be empty");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("Rejection reason cannot exceed 500 characters");
        }
        
        this.status = ConfirmationStatus.REJECTED;
        this.confirmedAt = LocalDateTime.now();
        this.rejectionReason = reason.trim();
    }

    /**
     * Check if the confirmation is still pending
     */
    public boolean isPending() {
        return status == ConfirmationStatus.PENDING;
    }

    /**
     * Check if the delivery was confirmed
     */
    public boolean isConfirmed() {
        return status == ConfirmationStatus.CONFIRMED;
    }

    /**
     * Check if the delivery was rejected
     */
    public boolean isRejected() {
        return status == ConfirmationStatus.REJECTED;
    }
}