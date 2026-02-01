package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.exception.VideoUploadException;
import com.mypkga.commerceplatformfull.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Local file system implementation of FileService
 * Used as fallback when cloud storage is not configured
 */
@Slf4j
public class LocalFileService implements FileService {
    
    @Value("${app.video.upload-dir:uploads/videos}")
    private String videoUploadDir;
    
    @Value("${app.video.max-size:50MB}")
    private String videoMaxSize;
    
    @Value("${app.video.allowed-types:video/mp4,video/avi,video/mov,video/wmv,video/webm}")
    private String videoAllowedTypes;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Override
    public FileUploadResult uploadVideo(MultipartFile file, String folder) {
        validateVideoFile(file);
        
        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(videoUploadDir, folder);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            String fileKey = folder + "/" + uniqueFileName;
            
            // Save video file
            Path videoFilePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), videoFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Generate public URL for local access
            String publicUrl = String.format("http://localhost:%s%s/files/%s", 
                    serverPort, contextPath, fileKey);
            
            log.info("Video uploaded successfully to local storage: {} -> {}", originalFilename, fileKey);
            
            return new FileUploadResult(
                    fileKey,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    publicUrl
            );
            
        } catch (IOException e) {
            log.error("Failed to upload video to local storage: {}", file.getOriginalFilename(), e);
            throw new VideoUploadException("Failed to upload video to local storage", e);
        }
    }
    
    @Override
    public FileUploadResult uploadImage(MultipartFile file, String folder) {
        validateImageFile(file);
        
        try {
            // Create upload directory if not exists (use images directory)
            Path uploadPath = Paths.get("uploads/images", folder);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            String fileKey = folder + "/" + uniqueFileName;
            
            // Save image file
            Path imageFilePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), imageFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Generate public URL for local access
            String publicUrl = String.format("http://localhost:%s%s/images/%s", 
                    serverPort, contextPath, fileKey);
            
            log.info("Image uploaded successfully to local storage: {} -> {}", originalFilename, fileKey);
            
            return new FileUploadResult(
                    fileKey,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    publicUrl
            );
            
        } catch (IOException e) {
            log.error("Failed to upload image to local storage: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload image to local storage", e);
        }
    }
    
    @Override
    public String generateSecureUrl(String fileKey, int expirationMinutes) {
        // For local storage, we'll generate a simple URL with a token
        // In a real implementation, you'd want to implement proper token-based security
        String token = UUID.randomUUID().toString();
        String secureUrl = String.format("http://localhost:%s%s/files/secure/%s?token=%s&expires=%d", 
                serverPort, contextPath, fileKey, token, System.currentTimeMillis() + (expirationMinutes * 60 * 1000));
        
        log.debug("Generated secure URL for local file: {} (expires in {} minutes)", fileKey, expirationMinutes);
        return secureUrl;
    }
    
    @Override
    public boolean deleteVideo(String fileKey) {
        try {
            Path filePath = Paths.get(videoUploadDir, fileKey);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Video deleted successfully from local storage: {}", fileKey);
            } else {
                log.warn("Video file not found for deletion: {}", fileKey);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete video from local storage: {}", fileKey, e);
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