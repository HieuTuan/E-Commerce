package com.mypkga.commerceplatformfull.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving uploaded files with proper security
 * Only authorized users can access video evidence files
 */
@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {
    
    @Value("${app.video.upload-dir:uploads/videos}")
    private String videoUploadDir;
    
    /**
     * Serve video files to authorized users only
     * Staff members can view evidence videos for return requests
     */
    @GetMapping("/{folder}/{filename:.+}")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<Resource> serveFile(@PathVariable String folder, @PathVariable String filename) {
        try {
            Path filePath = Paths.get(videoUploadDir).resolve(folder).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                log.warn("File not found or not readable: {}/{}", folder, filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error serving file: {}/{}", folder, filename, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Serve files with secure token (for time-limited access)
     * This is a simplified implementation - in production, you'd want proper token validation
     */
    @GetMapping("/secure/{folder}/{filename:.+}")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<Resource> serveSecureFile(
            @PathVariable String folder, 
            @PathVariable String filename,
            @RequestParam String token,
            @RequestParam long expires) {
        
        // Check if token is still valid (simplified check)
        if (System.currentTimeMillis() > expires) {
            log.warn("Expired token used for file access: {}/{}", folder, filename);
            return ResponseEntity.status(410).build(); // Gone
        }
        
        // For now, just serve the file - in production, validate the token properly
        return serveFile(folder, filename);
    }
    
    /**
     * Determine content type based on file extension
     */
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "wmv" -> "video/x-ms-wmv";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }
}