package com.mypkga.commerceplatformfull.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.image.upload-dir}")
    private String imageUploadDir;

    @Value("${app.video.upload-dir}")
    private String videoUploadDir;

    @Value("${app.image.max-size}")
    private String imageMaxSize;

    @Value("${app.video.max-size}")
    private String videoMaxSize;

    @Value("${app.image.allowed-types}")
    private String imageAllowedTypes;

    @Value("${app.video.allowed-types}")
    private String videoAllowedTypes;

    // Image size constants
    public static final String THUMBNAIL_SUFFIX = "_thumbnail";
    public static final String MEDIUM_SUFFIX = "_medium";
    public static final String LARGE_SUFFIX = "_large";

    /**
     * Upload image file and create multiple sizes
     */
    public ImageUploadResult uploadImage(MultipartFile file) throws IOException {
        validateImageFile(file);
        
        // Create upload directory if not exists
        Path uploadPath = Paths.get(imageUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString();
        
        // Save original file
        String originalFileName = uniqueFilename + "_original" + fileExtension;
        Path originalFilePath = uploadPath.resolve(originalFileName);
        Files.copy(file.getInputStream(), originalFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Use ImageOptimizationService to create different sizes
        ImageOptimizationService imageService = new ImageOptimizationService();
        ImageOptimizationService.ImageData imageData = imageService.optimizeProductImage(file);

        // Save different sizes
        String thumbnailFileName = uniqueFilename + THUMBNAIL_SUFFIX + ".jpg";
        String mediumFileName = uniqueFilename + MEDIUM_SUFFIX + ".jpg";
        String largeFileName = uniqueFilename + LARGE_SUFFIX + ".jpg";

        Path thumbnailPath = uploadPath.resolve(thumbnailFileName);
        Path mediumPath = uploadPath.resolve(mediumFileName);
        Path largePath = uploadPath.resolve(largeFileName);

        Files.write(thumbnailPath, imageData.getThumbnail());
        Files.write(mediumPath, imageData.getMedium());
        Files.write(largePath, imageData.getLarge());

        log.info("Image uploaded successfully: {}", originalFilename);

        return ImageUploadResult.builder()
                .originalFileName(originalFileName)
                .thumbnailFileName(thumbnailFileName)
                .mediumFileName(mediumFileName)
                .largeFileName(largeFileName)
                .originalPath(originalFilePath.toString())
                .thumbnailPath(thumbnailPath.toString())
                .mediumPath(mediumPath.toString())
                .largePath(largePath.toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
    }

    /**
     * Upload video file
     */
    public VideoUploadResult uploadVideo(MultipartFile file) throws IOException {
        validateVideoFile(file);
        
        // Create upload directory if not exists
        Path uploadPath = Paths.get(videoUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Save video file
        Path videoFilePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), videoFilePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Video uploaded successfully: {}", originalFilename);

        return VideoUploadResult.builder()
                .fileName(uniqueFilename)
                .originalFileName(originalFilename)
                .filePath(videoFilePath.toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
    }

    /**
     * Delete uploaded files
     */
    public void deleteImageFiles(String originalFileName, String thumbnailFileName, 
                                String mediumFileName, String largeFileName) {
        try {
            Path uploadPath = Paths.get(imageUploadDir);
            Files.deleteIfExists(uploadPath.resolve(originalFileName));
            Files.deleteIfExists(uploadPath.resolve(thumbnailFileName));
            Files.deleteIfExists(uploadPath.resolve(mediumFileName));
            Files.deleteIfExists(uploadPath.resolve(largeFileName));
            log.info("Image files deleted successfully");
        } catch (IOException e) {
            log.error("Error deleting image files", e);
        }
    }

    public void deleteVideoFile(String fileName) {
        try {
            Path uploadPath = Paths.get(videoUploadDir);
            Files.deleteIfExists(uploadPath.resolve(fileName));
            log.info("Video file deleted successfully: {}", fileName);
        } catch (IOException e) {
            log.error("Error deleting video file: {}", fileName, e);
        }
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        // Check file size (convert from string like "5MB" to bytes)
        long maxSizeBytes = parseFileSize(imageMaxSize);
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Image file too large. Maximum size is " + imageMaxSize);
        }

        // Check content type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(imageAllowedTypes.split(","));
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Invalid image format. Allowed: " + imageAllowedTypes);
        }
    }

    /**
     * Validate video file
     */
    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Video file is empty");
        }

        // Check file size
        long maxSizeBytes = parseFileSize(videoMaxSize);
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Video file too large. Maximum size is " + videoMaxSize);
        }

        // Check content type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(videoAllowedTypes.split(","));
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Invalid video format. Allowed: " + videoAllowedTypes);
        }
    }

    /**
     * Parse file size string (e.g., "5MB") to bytes
     */
    private long parseFileSize(String sizeStr) {
        sizeStr = sizeStr.toUpperCase().trim();
        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024;
        } else if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024 * 1024;
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
     * Image upload result
     */
    public static class ImageUploadResult {
        private String originalFileName;
        private String thumbnailFileName;
        private String mediumFileName;
        private String largeFileName;
        private String originalPath;
        private String thumbnailPath;
        private String mediumPath;
        private String largePath;
        private long fileSize;
        private String contentType;

        public static ImageUploadResultBuilder builder() {
            return new ImageUploadResultBuilder();
        }

        // Builder pattern
        public static class ImageUploadResultBuilder {
            private String originalFileName;
            private String thumbnailFileName;
            private String mediumFileName;
            private String largeFileName;
            private String originalPath;
            private String thumbnailPath;
            private String mediumPath;
            private String largePath;
            private long fileSize;
            private String contentType;

            public ImageUploadResultBuilder originalFileName(String originalFileName) {
                this.originalFileName = originalFileName;
                return this;
            }

            public ImageUploadResultBuilder thumbnailFileName(String thumbnailFileName) {
                this.thumbnailFileName = thumbnailFileName;
                return this;
            }

            public ImageUploadResultBuilder mediumFileName(String mediumFileName) {
                this.mediumFileName = mediumFileName;
                return this;
            }

            public ImageUploadResultBuilder largeFileName(String largeFileName) {
                this.largeFileName = largeFileName;
                return this;
            }

            public ImageUploadResultBuilder originalPath(String originalPath) {
                this.originalPath = originalPath;
                return this;
            }

            public ImageUploadResultBuilder thumbnailPath(String thumbnailPath) {
                this.thumbnailPath = thumbnailPath;
                return this;
            }

            public ImageUploadResultBuilder mediumPath(String mediumPath) {
                this.mediumPath = mediumPath;
                return this;
            }

            public ImageUploadResultBuilder largePath(String largePath) {
                this.largePath = largePath;
                return this;
            }

            public ImageUploadResultBuilder fileSize(long fileSize) {
                this.fileSize = fileSize;
                return this;
            }

            public ImageUploadResultBuilder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public ImageUploadResult build() {
                ImageUploadResult result = new ImageUploadResult();
                result.originalFileName = this.originalFileName;
                result.thumbnailFileName = this.thumbnailFileName;
                result.mediumFileName = this.mediumFileName;
                result.largeFileName = this.largeFileName;
                result.originalPath = this.originalPath;
                result.thumbnailPath = this.thumbnailPath;
                result.mediumPath = this.mediumPath;
                result.largePath = this.largePath;
                result.fileSize = this.fileSize;
                result.contentType = this.contentType;
                return result;
            }
        }

        // Getters
        public String getOriginalFileName() { return originalFileName; }
        public String getThumbnailFileName() { return thumbnailFileName; }
        public String getMediumFileName() { return mediumFileName; }
        public String getLargeFileName() { return largeFileName; }
        public String getOriginalPath() { return originalPath; }
        public String getThumbnailPath() { return thumbnailPath; }
        public String getMediumPath() { return mediumPath; }
        public String getLargePath() { return largePath; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
    }

    /**
     * Video upload result
     */
    public static class VideoUploadResult {
        private String fileName;
        private String originalFileName;
        private String filePath;
        private long fileSize;
        private String contentType;

        public static VideoUploadResultBuilder builder() {
            return new VideoUploadResultBuilder();
        }

        // Builder pattern
        public static class VideoUploadResultBuilder {
            private String fileName;
            private String originalFileName;
            private String filePath;
            private long fileSize;
            private String contentType;

            public VideoUploadResultBuilder fileName(String fileName) {
                this.fileName = fileName;
                return this;
            }

            public VideoUploadResultBuilder originalFileName(String originalFileName) {
                this.originalFileName = originalFileName;
                return this;
            }

            public VideoUploadResultBuilder filePath(String filePath) {
                this.filePath = filePath;
                return this;
            }

            public VideoUploadResultBuilder fileSize(long fileSize) {
                this.fileSize = fileSize;
                return this;
            }

            public VideoUploadResultBuilder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public VideoUploadResult build() {
                VideoUploadResult result = new VideoUploadResult();
                result.fileName = this.fileName;
                result.originalFileName = this.originalFileName;
                result.filePath = this.filePath;
                result.fileSize = this.fileSize;
                result.contentType = this.contentType;
                return result;
            }
        }

        // Getters
        public String getFileName() { return fileName; }
        public String getOriginalFileName() { return originalFileName; }
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
    }
}