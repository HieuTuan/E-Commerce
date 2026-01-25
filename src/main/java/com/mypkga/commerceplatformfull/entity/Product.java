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
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity = 0;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 100)
    private String aiCategory;

    @Column(nullable = false)
    private Boolean featured = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> productImages = new ArrayList<>();

    @Transient
    public Double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    @Transient
    public Integer getReviewCount() {
        return reviews != null ? reviews.size() : 0;
    }




    // Helper methods for product images
    @Transient
    public ProductImage getPrimaryImage() {
        if (productImages == null || productImages.isEmpty()) {
            return null;
        }
        return productImages.stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .orElse(productImages.get(0));
    }

    @Transient
    public List<ProductImage> getSortedImages() {
        if (productImages == null) {
            return new ArrayList<>();
        }
        return productImages.stream()
                .sorted((a, b) -> {
                    // Primary image first
                    if (a.getIsPrimary() && !b.getIsPrimary()) return -1;
                    if (!a.getIsPrimary() && b.getIsPrimary()) return 1;
                    // Then by display order
                    return Integer.compare(
                        a.getDisplayOrder() != null ? a.getDisplayOrder() : 0,
                        b.getDisplayOrder() != null ? b.getDisplayOrder() : 0
                    );
                })
                .toList();
    }
}
