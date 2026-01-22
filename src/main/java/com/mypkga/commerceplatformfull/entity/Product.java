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

    // Image file names (instead of URLs)
    @Column(name = "image_original", length = 255)
    private String imageOriginal;

    @Column(name = "image_thumbnail", length = 255)
    private String imageThumbnail;

    @Column(name = "image_medium", length = 255)
    private String imageMedium;

    @Column(name = "image_large", length = 255)
    private String imageLarge;

    // Video file name (instead of URL)
    @Column(name = "video_filename", length = 255)
    private String videoFilename;

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

    // Helper methods to get image URLs
    @Transient
    public String getThumbnailUrl() {
        return imageThumbnail != null ? "/files/images/" + imageThumbnail : null;
    }

    @Transient
    public String getMediumUrl() {
        return imageMedium != null ? "/files/images/" + imageMedium : null;
    }

    @Transient
    public String getLargeUrl() {
        return imageLarge != null ? "/files/images/" + imageLarge : null;
    }

    @Transient
    public String getOriginalUrl() {
        return imageOriginal != null ? "/files/images/" + imageOriginal : null;
    }

    @Transient
    public String getVideoUrl() {
        return videoFilename != null ? "/files/videos/" + videoFilename : null;
    }
}
