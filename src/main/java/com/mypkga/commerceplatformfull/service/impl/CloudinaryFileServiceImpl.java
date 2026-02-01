package com.mypkga.commerceplatformfull.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mypkga.commerceplatformfull.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Cloudinary implementation of FileService for video and image storage
 */
@Service
@Primary
@ConditionalOnProperty(name = "app.file-storage.type", havingValue = "cloudinary")
@RequiredArgsConstructor
@Slf4j
public class CloudinaryFileServiceImpl implements FileService {

    private final Cloudinary cloudinary;

    @Override
    public FileUploadResult uploadVideo(MultipartFile file, String folder) {
        try {
            log.info("Uploading video to Cloudinary: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
            
            // Generate unique filename
            String publicId = folder + "/" + UUID.randomUUID().toString();
            
            // Upload video to Cloudinary - SIMPLE VERSION WITHOUT TRANSFORMATION
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "video",
                    "folder", folder,
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false
                )
            );

            String publicUrl = (String) uploadResult.get("secure_url");
            String cloudinaryPublicId = (String) uploadResult.get("public_id");
            
            log.info("Video uploaded successfully to Cloudinary: {}", publicUrl);
            
            return FileUploadResult.builder()
                .publicUrl(publicUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .cloudinaryPublicId(cloudinaryPublicId)
                .build();
                
        } catch (IOException e) {
            log.error("Failed to upload video to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video to Cloudinary", e);
        }
    }

    @Override
    public FileUploadResult uploadImage(MultipartFile file, String folder) {
        try {
            log.info("Uploading image to Cloudinary: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
            
            // Generate unique filename
            String publicId = folder + "/" + UUID.randomUUID().toString();
            
            // Upload image to Cloudinary - SIMPLE VERSION WITHOUT TRANSFORMATION
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "folder", folder,
                    "use_filename", false,
                    "unique_filename", true,
                    "overwrite", false
                )
            );

            String publicUrl = (String) uploadResult.get("secure_url");
            String cloudinaryPublicId = (String) uploadResult.get("public_id");
            
            log.info("Image uploaded successfully to Cloudinary: {}", publicUrl);
            
            return FileUploadResult.builder()
                .publicUrl(publicUrl)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .cloudinaryPublicId(cloudinaryPublicId)
                .build();
                
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }

    @Override
    public void deleteFile(String publicUrl) {
        try {
            // Extract public_id from Cloudinary URL
            String publicId = extractPublicIdFromUrl(publicUrl);
            if (publicId != null) {
                log.info("Deleting file from Cloudinary: {}", publicId);
                
                // Try to delete as video first, then as image
                Map<String, Object> result = cloudinary.uploader().destroy(publicId, 
                    ObjectUtils.asMap("resource_type", "video"));
                
                if (!"ok".equals(result.get("result"))) {
                    // Try as image
                    result = cloudinary.uploader().destroy(publicId, 
                        ObjectUtils.asMap("resource_type", "image"));
                }
                
                log.info("File deleted from Cloudinary: {} (result: {})", publicId, result.get("result"));
            }
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage(), e);
            // Don't throw exception for delete failures
        }
    }

    @Override
    public boolean fileExists(String publicUrl) {
        try {
            String publicId = extractPublicIdFromUrl(publicUrl);
            if (publicId != null) {
                // Try to get resource info
                Map<String, Object> result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
                return result != null && result.containsKey("public_id");
            }
        } catch (Exception e) {
            log.debug("File does not exist or error checking: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract public_id from Cloudinary URL
     * Example: https://res.cloudinary.com/demo/video/upload/v1234567890/folder/filename.mp4
     * Returns: folder/filename
     */
    private String extractPublicIdFromUrl(String url) {
        if (url == null || !url.contains("cloudinary.com")) {
            return null;
        }
        
        try {
            // Find the part after /upload/v{version}/
            String[] parts = url.split("/upload/v\\d+/");
            if (parts.length > 1) {
                String publicIdWithExtension = parts[1];
                // Remove file extension
                int lastDotIndex = publicIdWithExtension.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    return publicIdWithExtension.substring(0, lastDotIndex);
                }
                return publicIdWithExtension;
            }
        } catch (Exception e) {
            log.warn("Failed to extract public_id from URL: {}", url, e);
        }
        
        return null;
    }
}