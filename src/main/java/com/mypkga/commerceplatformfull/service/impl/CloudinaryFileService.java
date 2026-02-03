package com.mypkga.commerceplatformfull.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mypkga.commerceplatformfull.exception.VideoUploadException;
import com.mypkga.commerceplatformfull.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cloudinary implementation of FileService for video upload handling.
 * Provides cloud storage with automatic optimization and secure URLs.
 * 
 * Note: This class is not used as a Spring service. Use CloudinaryFileServiceImpl instead.
 */
@Slf4j
public class CloudinaryFileService implements FileService {
    
    @Value("${cloudinary.cloud-name}")
    private String cloudName;
    
    @Value("${cloudinary.api-key}")
    private String apiKey;
    
    @Value("${cloudinary.api-secret}")
    private String apiSecret;
    
    @Value("${app.video.max-size:50MB}")
    private String videoMaxSize;
    
    @Value("${app.video.allowed-types:video/mp4,video/avi,video/mov,video/wmv,video/webm}")
    private String videoAllowedTypes;
    
    private Cloudinary cloudinary;
    
    @PostConstruct
    public void initializeCloudinary() {
        try {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
            
            log.info("Cloudinary client initialized successfully for cloud: {}", cloudName);
        } catch (Exception e) {
            log.error("Failed to initialize Cloudinary client", e);
            throw new VideoUploadException("Failed to initialize cloud storage client", e);
        }
    }
    
    @Override
    public FileUploadResult uploadVideo(MultipartFile file, String folder) {
        validateVideoFile(file);
        
        try {
            // Generate unique public ID
            String originalFilename = file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID().toString();
            String publicId = folder + "/" + uniqueFileName;
            
            // Upload options
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "video",
                    "folder", folder,
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false,
                    "quality", "auto",
                    "format", "mp4" // Convert to MP4 for better compatibility
            );
            
            // Upload file
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            
            String publicUrl = (String) uploadResult.get("secure_url");
            String fileKey = (String) uploadResult.get("public_id");
            
            log.info("Video uploaded successfully to Cloudinary: {} -> {}", originalFilename, fileKey);
            
            return new FileUploadResult(
                    fileKey,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    publicUrl
            );
            
        } catch (IOException e) {
            log.error("Failed to upload video to Cloudinary: {}", file.getOriginalFilename(), e);
            throw new VideoUploadException("Failed to upload video to cloud storage", e);
        } catch (Exception e) {
            log.error("Cloudinary error during video upload: {}", e.getMessage(), e);
            throw new VideoUploadException("Cloud storage error: " + e.getMessage(), e);
        }
    }
    
    @Override
    public FileUploadResult uploadImage(MultipartFile file, String folder) {
        validateImageFile(file);
        
        try {
            // Generate unique public ID
            String originalFilename = file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID().toString();
            String publicId = folder + "/" + uniqueFileName;
            
            // Upload options for images
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "folder", folder,
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false,
                    "quality", "auto",
                    "format", "jpg" // Convert to JPG for better compatibility
            );
            
            // Upload file
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            
            String publicUrl = (String) uploadResult.get("secure_url");
            String fileKey = (String) uploadResult.get("public_id");
            
            log.info("Image uploaded successfully to Cloudinary: {} -> {}", originalFilename, fileKey);
            
            return new FileUploadResult(
                    fileKey,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    publicUrl
            );
            
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload image to cloud storage", e);
        } catch (Exception e) {
            log.error("Cloudinary error during image upload: {}", e.getMessage(), e);
            throw new RuntimeException("Cloud storage error: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generateSecureUrl(String fileKey, int expirationMinutes) {
        try {
            // Generate basic signed URL for security
            // Note: Time-based expiration requires Cloudinary's advanced features
            // For now, we use signed URLs for access control
            String signedUrl = cloudinary.url()
                    .resourceType("video")
                    .publicId(fileKey)
                    .signed(true)
                    .generate();
            
            log.debug("Generated secure URL for file: {} (signed for access control)", fileKey);
            return signedUrl;
            
        } catch (Exception e) {
            log.error("Failed to generate secure URL for file: {}", fileKey, e);
            throw new VideoUploadException("Failed to generate secure access URL", e);
        }
    }
    
    @Override
    public boolean deleteVideo(String fileKey) {
        try {
            Map<String, Object> deleteOptions = ObjectUtils.asMap(
                    "resource_type", "video"
            );
            
            Map<String, Object> result = cloudinary.uploader().destroy(fileKey, deleteOptions);
            String resultStatus = (String) result.get("result");
            
            boolean success = "ok".equals(resultStatus);
            if (success) {
                log.info("Video deleted successfully from Cloudinary: {}", fileKey);
            } else {
                log.warn("Video deletion from Cloudinary returned status: {} for file: {}", resultStatus, fileKey);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to delete video from Cloudinary: {}", fileKey, e);
            return false;
        }
    }
    
    @Override
    public void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file is required and cannot be empty");
        }
        
        // Check file size
        long maxSizeBytes = parseFileSize(videoMaxSize);
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Video file too large. Maximum size is " + videoMaxSize);
        }
        
        // Check content type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(videoAllowedTypes.split(","));
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid video format. Allowed formats: " + videoAllowedTypes);
        }
        
        // Check file extension matches content type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidVideoExtension(originalFilename)) {
            throw new IllegalArgumentException("Invalid video file extension");
        }
        
        log.debug("Video file validation passed: {} ({})", originalFilename, contentType);
    }
    
    /**
     * Validate image file
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required and cannot be empty");
        }
        
        // Check file size (max 10MB for images)
        long maxSizeBytes = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Image file too large. Maximum size is 10MB");
        }
        
        // Check content type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image format. Allowed formats: JPEG, PNG, GIF, WebP");
        }
        
        // Check file extension matches content type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidImageExtension(originalFilename)) {
            throw new IllegalArgumentException("Invalid image file extension");
        }
        
        log.debug("Image file validation passed: {} ({})", originalFilename, contentType);
    }
    
    /**
     * Parse file size string (e.g., "50MB") to bytes
     */
    private long parseFileSize(String sizeStr) {
        sizeStr = sizeStr.toUpperCase().trim();
        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024;
        } else if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024L * 1024L * 1024L;
        }
        return Long.parseLong(sizeStr); // Assume bytes
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
    
    /**
     * Check if filename has a valid video extension
     */
    private boolean hasValidVideoExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        List<String> validExtensions = Arrays.asList(".mp4", ".avi", ".mov", ".wmv", ".webm");
        return validExtensions.contains(extension);
    }
    
    /**
     * Check if filename has a valid image extension
     */
    private boolean hasValidImageExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        List<String> validExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
        return validExtensions.contains(extension);
    }
}