package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_original", length = 255)
    private String imageOriginal;

    @Column(name = "image_thumbnail", length = 255)
    private String imageThumbnail;

    @Column(name = "image_medium", length = 255)
    private String imageMedium;

    @Column(name = "image_large", length = 255)
    private String imageLarge;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    // Video file name (instead of URL)
    @Column(name = "video_filename", length = 255)
    private String videoFilename;
    
    // Video thumbnail image
    @Column(name = "video_thumbnail", length = 255)
    private String videoThumbnail;
    @Column(length = 255,columnDefinition = "NVARCHAR(200)")
    private String altText;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

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
    public String getVideoUrl() {
        return videoFilename != null ? "/files/videos/" + videoFilename : null;
    }
    
    @Transient
    public String getVideoThumbnailUrl() {
        return videoThumbnail != null ? "/files/images/" + videoThumbnail : "/images/video-thumbnail.svg";
    }
    @Transient
    public String getOriginalUrl() {
        return imageOriginal != null ? "/files/images/" + imageOriginal : null;
    }
}