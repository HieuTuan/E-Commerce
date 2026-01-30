package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a timeline entry for order status changes.
 * Each entry records when an order status was changed, by whom, and any additional notes.
 */
@Entity
@Table(name = "order_timeline")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private OrderStatus status;

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "notes", columnDefinition = "NVARCHAR(500)")
    private String notes;

    /**
     * Constructor for creating a timeline entry with basic information
     */
    public OrderTimelineEntry(Order order, OrderStatus status, String updatedBy) {
        this.order = order;
        this.status = status;
        this.updatedBy = updatedBy;
    }

    /**
     * Constructor for creating a timeline entry with notes
     */
    public OrderTimelineEntry(Order order, OrderStatus status, String updatedBy, String notes) {
        this.order = order;
        this.status = status;
        this.updatedBy = updatedBy;
        this.notes = notes;
    }
}