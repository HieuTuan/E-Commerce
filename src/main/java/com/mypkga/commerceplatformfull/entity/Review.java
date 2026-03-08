package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "order_id", "product_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Track which specific order this review belongs to (1 review per order per
     * product).
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(columnDefinition = "NVARCHAR(500)")
    private String comment;

    @Column(nullable = false)
    private Boolean approved = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedDate;

    /**
     * Check if this review is still editable (within 7 days of creation).
     */
    public boolean isEditable() {
        return createdDate != null && createdDate.plusDays(7).isAfter(LocalDateTime.now());
    }
}
