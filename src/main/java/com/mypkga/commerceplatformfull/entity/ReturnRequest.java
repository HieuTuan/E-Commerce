package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a return request submitted by a customer.
 * This contains all information needed to process a return and refund.
 */
@Entity
@Table(name = "return_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exclude from toString to prevent circular reference with Order
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull(message = "Return reason is required")
    private ReturnReason reason;

    @NotBlank(message = "Detailed description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    @Column(name = "detailed_description", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String detailedDescription;

    @Column(name = "evidence_video_url", length = 500)
    private String evidenceVideoUrl;

    @Column(name = "return_code", unique = true, length = 50)
    private String returnCode;

    @Embedded
    @Valid
    @NotNull(message = "Bank information is required")
    private RefundBankInfo bankInfo;

    // GHN Integration fields
    @Column(name = "ghn_order_code", length = 50)
    private String ghnOrderCode;

    @Column(name = "ghn_tracking_number", length = 50)
    private String ghnTrackingNumber;

    @Column(name = "ghn_status", length = 50)
    private String ghnStatus;

    @Column(name = "ghn_fee")
    private Integer ghnFee;

    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.REFUND_REQUESTED;

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(500)")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Staff who processed the request (for approval/rejection)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_staff_id")
    private User processedByStaff;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Check if the return request can be updated to a new status
     */
    public boolean canUpdateStatus(ReturnStatus newStatus) {
        return this.status.canTransitionTo(newStatus);
    }

    /**
     * Update the status and set processing information
     */
    public void updateStatus(ReturnStatus newStatus, User staff) {
        if (!canUpdateStatus(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", this.status, newStatus));
        }
        this.status = newStatus;
        this.processedByStaff = staff;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Check if this return request is in a final state
     */
    public boolean isFinalState() {
        return this.status.isFinalState();
    }

    /**
     * Get the customer who made this return request
     */
    public User getCustomer() {
        return this.order != null ? this.order.getUser() : null;
    }
}