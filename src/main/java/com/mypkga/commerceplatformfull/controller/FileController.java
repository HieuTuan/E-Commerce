package com.mypkga.commerceplatformfull.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
     * Serve files (including nested sub-folders) to authorized users only.
     * Supports paths like /files/return-evidence/8/uuid.mp4
     */
    @GetMapping("/**")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        // Extract the sub-path after /files/
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String subPath = requestURI.substring((contextPath + "/files/").length());

        // Normalize any double slashes
        subPath = subPath.replaceAll("//+", "/");

        String filename = subPath.contains("/") ? subPath.substring(subPath.lastIndexOf('/') + 1) : subPath;

        try {
            Path filePath = Paths.get(videoUploadDir).resolve(subPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                log.warn("File not found or not readable: {}", subPath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error serving file: {}", subPath, e);
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

        // Serve the file directly
        String subPath = folder + "/" + filename;
        try {
            Path filePath = Paths.get(videoUploadDir).resolve(subPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error serving secure file: {}/{}", folder, filename, e);
            return ResponseEntity.badRequest().build();
        }
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