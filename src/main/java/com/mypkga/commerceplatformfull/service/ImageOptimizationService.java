package com.mypkga.commerceplatformfull.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class ImageOptimizationService {

    // Recommended image sizes for e-commerce
    public static final int THUMBNAIL_SIZE = 150;      // 150x150 for thumbnails
    public static final int MEDIUM_SIZE = 400;         // 400x400 for product lists
    public static final int LARGE_SIZE = 800;          // 800x800 for product details
    public static final int MAX_ORIGINAL_SIZE = 1200;  // 1200x1200 max original

    // File size limits (in bytes)
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final long OPTIMIZED_TARGET_SIZE = 500 * 1024; // 500KB target

    /**
     * Validates and optimizes product image
     * @param file Original image file
     * @return Optimized image data
     */
    public ImageData optimizeProductImage(MultipartFile file) throws IOException {
        validateImageFile(file);
        
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid image file");
        }

        // Create different sizes
        BufferedImage thumbnail = resizeImage(originalImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        BufferedImage medium = resizeImage(originalImage, MEDIUM_SIZE, MEDIUM_SIZE);
        BufferedImage large = resizeImage(originalImage, LARGE_SIZE, LARGE_SIZE);
        BufferedImage original = resizeImage(originalImage, MAX_ORIGINAL_SIZE, MAX_ORIGINAL_SIZE);

        return ImageData.builder()
                .thumbnail(imageToByteArray(thumbnail, "jpg"))
                .medium(imageToByteArray(medium, "jpg"))
                .large(imageToByteArray(large, "jpg"))
                .original(imageToByteArray(original, "jpg"))
                .originalFileName(file.getOriginalFilename())
                .contentType("image/jpeg")
                .build();
    }

    /**
     * Resize image maintaining aspect ratio
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions maintaining aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth, newHeight;

        if (originalWidth > originalHeight) {
            newWidth = Math.min(maxWidth, originalWidth);
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newHeight = Math.min(maxHeight, originalHeight);
            newWidth = (int) (newHeight * aspectRatio);
        }

        // Create optimized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // High quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * Convert BufferedImage to byte array
     */
    private byte[] imageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image file too large. Maximum size is 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Check supported formats
        String[] supportedFormats = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        boolean isSupported = false;
        for (String format : supportedFormats) {
            if (format.equals(contentType)) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            throw new IllegalArgumentException("Unsupported image format. Supported: JPEG, PNG, GIF, WebP");
        }
    }

    /**
     * Get recommended image dimensions for different use cases
     */
    public static class ImageDimensions {
        public static final String THUMBNAIL = "150x150"; // Product grid thumbnails
        public static final String MEDIUM = "400x400";    // Product list items
        public static final String LARGE = "800x800";     // Product detail main image
        public static final String ZOOM = "1200x1200";    // Zoom/lightbox view
        
        // Banner/Hero images
        public static final String BANNER_DESKTOP = "1920x600";
        public static final String BANNER_MOBILE = "768x400";
        
        // Category images
        public static final String CATEGORY = "300x200";
    }

    /**
     * Image data container
     */
    public static class ImageData {
        private byte[] thumbnail;
        private byte[] medium;
        private byte[] large;
        private byte[] original;
        private String originalFileName;
        private String contentType;

        public static ImageDataBuilder builder() {
            return new ImageDataBuilder();
        }

        // Builder pattern implementation
        public static class ImageDataBuilder {
            private byte[] thumbnail;
            private byte[] medium;
            private byte[] large;
            private byte[] original;
            private String originalFileName;
            private String contentType;

            public ImageDataBuilder thumbnail(byte[] thumbnail) {
                this.thumbnail = thumbnail;
                return this;
            }

            public ImageDataBuilder medium(byte[] medium) {
                this.medium = medium;
                return this;
            }

            public ImageDataBuilder large(byte[] large) {
                this.large = large;
                return this;
            }

            public ImageDataBuilder original(byte[] original) {
                this.original = original;
                return this;
            }

            public ImageDataBuilder originalFileName(String originalFileName) {
                this.originalFileName = originalFileName;
                return this;
            }

            public ImageDataBuilder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public ImageData build() {
                ImageData imageData = new ImageData();
                imageData.thumbnail = this.thumbnail;
                imageData.medium = this.medium;
                imageData.large = this.large;
                imageData.original = this.original;
                imageData.originalFileName = this.originalFileName;
                imageData.contentType = this.contentType;
                return imageData;
            }
        }

        // Getters
        public byte[] getThumbnail() { return thumbnail; }
        public byte[] getMedium() { return medium; }
        public byte[] getLarge() { return large; }
        public byte[] getOriginal() { return original; }
        public String getOriginalFileName() { return originalFileName; }
        public String getContentType() { return contentType; }
    }
}