package com.mypkga.commerceplatformfull.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for handling file uploads to cloud storage
 * Supports multiple storage providers: Local, Cloudinary
 */
public interface FileService {

    FileUploadResult uploadVideo(MultipartFile file, String folder);
    
    /**
     * Upload image file to cloud storage
     * 
     * @param file The image file to upload
     * @param folder The folder path in cloud storage
     * @return Upload result containing file information
     */
    FileUploadResult uploadImage(MultipartFile file, String folder);
    
    /**
     * Delete a file from cloud storage
     * 
     * @param publicUrl The public URL or file key
     */
    default void deleteFile(String publicUrl) {
        // Default implementation - can be overridden by specific implementations
    }
    
    /**
     * Check if a file exists in cloud storage
     * 
     * @param publicUrl The public URL or file key
     * @return true if file exists, false otherwise
     */
    default boolean fileExists(String publicUrl) {
        return false; // Default implementation
    }
    
    /**
     * Generate a secure, time-limited URL for accessing a file
     * 
     * @param fileKey The file key/path in cloud storage
     * @param expirationMinutes How long the URL should be valid (in minutes)
     * @return Secure URL for accessing the file
     */
    default String generateSecureUrl(String fileKey, int expirationMinutes) {
        return fileKey; // Default implementation returns the key as-is
    }
    
    /**
     * Delete a video file from cloud storage
     * 
     * @param fileKey The file key/path in cloud storage
     * @return true if deletion was successful, false otherwise
     */
    default boolean deleteVideo(String fileKey) {
        deleteFile(fileKey);
        return true;
    }
    
    /**
     * Validate that the uploaded file is a valid video format and size
     * 
     * @param file The file to validate
     * @throws IllegalArgumentException if validation fails
     */
    default void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file cannot be empty");
        }
        
        // Check file size (max 50MB)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("Video file size cannot exceed 50MB");
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("File must be a video format");
        }
    }
    
    /**
     * Result object containing upload information
     */
    @Data
    @Builder
    class FileUploadResult {
        private final String fileKey;
        private final String fileName;
        private final String contentType;
        private final long fileSize;
        private final String publicUrl;
        private final String cloudinaryPublicId; // For Cloudinary-specific operations
        
        // Backward compatibility constructor
        public FileUploadResult(String fileKey, String fileName, String contentType, long fileSize, String publicUrl) {
            this.fileKey = fileKey;
            this.fileName = fileName;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.publicUrl = publicUrl;
            this.cloudinaryPublicId = null;
        }
        
        // Full constructor for Lombok @Builder
        public FileUploadResult(String fileKey, String fileName, String contentType, long fileSize, String publicUrl, String cloudinaryPublicId) {
            this.fileKey = fileKey;
            this.fileName = fileName;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.publicUrl = publicUrl;
            this.cloudinaryPublicId = cloudinaryPublicId;
        }
    }
}