package com.mypkga.commerceplatformfull.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {

    @Value("${app.image.upload-dir}")
    private String imageUploadDir;

    @Value("${app.video.upload-dir}")
    private String videoUploadDir;

    /**
     * Serve image files
     */
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(imageUploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                        .body(resource);
            } else {
                log.warn("Image file not found: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error serving image file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Serve video files
     */
    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> serveVideo(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(videoUploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=7200") // Cache for 2 hours
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                log.warn("Video file not found: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error serving video file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get image info (for debugging/admin purposes)
     */
    @GetMapping("/images/{filename:.+}/info")
    public ResponseEntity<FileInfo> getImageInfo(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(imageUploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                FileInfo info = FileInfo.builder()
                        .filename(filename)
                        .size(resource.contentLength())
                        .contentType(determineContentType(filename))
                        .path(filePath.toString())
                        .build();
                
                return ResponseEntity.ok(info);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting image info: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Determine content type based on file extension
     */
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/avi";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";
            case "webm":
                return "video/webm";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * File info response
     */
    public static class FileInfo {
        private String filename;
        private long size;
        private String contentType;
        private String path;

        public static FileInfoBuilder builder() {
            return new FileInfoBuilder();
        }

        public static class FileInfoBuilder {
            private String filename;
            private long size;
            private String contentType;
            private String path;

            public FileInfoBuilder filename(String filename) {
                this.filename = filename;
                return this;
            }

            public FileInfoBuilder size(long size) {
                this.size = size;
                return this;
            }

            public FileInfoBuilder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public FileInfoBuilder path(String path) {
                this.path = path;
                return this;
            }

            public FileInfo build() {
                FileInfo info = new FileInfo();
                info.filename = this.filename;
                info.size = this.size;
                info.contentType = this.contentType;
                info.path = this.path;
                return info;
            }
        }

        // Getters
        public String getFilename() { return filename; }
        public long getSize() { return size; }
        public String getContentType() { return contentType; }
        public String getPath() { return path; }
    }
}