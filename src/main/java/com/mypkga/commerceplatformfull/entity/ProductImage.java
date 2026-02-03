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

    // Store Cloudinary URLs for different image sizes
    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "medium_url", length = 500)
    private String mediumUrl;

    @Column(name = "large_url", length = 500)
    private String largeUrl;

    // Video URLs for Cloudinary
    @Column(name = "video_url", length = 500)
    private String videoUrl;
    
    @Column(name = "video_thumbnail_url", length = 500)
    private String videoThumbnailUrl;

    // Metadata fields
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "is_video")
    private Boolean isVideo = false;

    @Column(length = 255, columnDefinition = "NVARCHAR(200)")
    private String altText;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // Helper methods to get image URLs from Cloudinary
    @Transient
    public String getThumbnailUrl() {
        if (thumbnailUrl != null) return thumbnailUrl;
        if (imageUrl != null && cloudinaryPublicId != null) {
            // Generate thumbnail URL from Cloudinary
            return imageUrl.replace("/upload/", "/upload/c_thumb,w_150,h_150/");
        }
        return null;
    }

    @Transient
    public String getMediumUrl() {
        if (mediumUrl != null) return mediumUrl;
        if (imageUrl != null && cloudinaryPublicId != null) {
            // Generate medium URL from Cloudinary
            return imageUrl.replace("/upload/", "/upload/c_fit,w_400,h_400/");
        }
        return imageUrl;
    }

    @Transient
    public String getLargeUrl() {
        if (largeUrl != null) return largeUrl;
        if (imageUrl != null && cloudinaryPublicId != null) {
            // Generate large URL from Cloudinary
            return imageUrl.replace("/upload/", "/upload/c_fit,w_800,h_800/");
        }
        return imageUrl;
    }

    @Transient
    public String getOriginalUrl() {
        return imageUrl;
    }

    @Transient
    public String getVideoUrl() {
        return (isVideo != null && isVideo) ? videoUrl : null;
    }
    
    @Transient
    public String getVideoThumbnailUrl() {
        if (isVideo != null && isVideo) {
            if (videoThumbnailUrl != null) return videoThumbnailUrl;
            if (videoUrl != null) {
                // Generate video thumbnail from Cloudinary
                return videoUrl.replace("/upload/", "/upload/so_0,c_thumb,w_150,h_150/").replace(".mp4", ".jpg");
            }
        }
        return "/images/video-thumbnail.svg";
    }
}