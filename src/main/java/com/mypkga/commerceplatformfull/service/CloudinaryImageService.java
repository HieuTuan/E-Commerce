package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.ProductImage;
import com.mypkga.commerceplatformfull.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageService {

    private final ProductImageRepository productImageRepository;
    private final FileService fileService;

    /**
     * Save image to Cloudinary
     */
    public ProductImage saveImageToCloudinary(MultipartFile file, Product product, Integer displayOrder, Boolean isPrimary) throws IOException {
        // Upload to Cloudinary - Cloudinary will automatically optimize the image
        FileService.FileUploadResult uploadResult = fileService.uploadImage(file, "products");

        // Create ProductImage entity
        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setOriginalFilename(file.getOriginalFilename());
        productImage.setContentType(file.getContentType());
        productImage.setFileSize(file.getSize());
        productImage.setDisplayOrder(displayOrder);
        productImage.setIsPrimary(isPrimary);
        productImage.setIsVideo(false);

        // Store Cloudinary URLs - Cloudinary handles all transformations automatically
        productImage.setCloudinaryPublicId(uploadResult.getCloudinaryPublicId());
        productImage.setImageUrl(uploadResult.getPublicUrl());
        
        // Generate different sizes using Cloudinary transformations
        String baseUrl = uploadResult.getPublicUrl();
        productImage.setThumbnailUrl(baseUrl.replace("/upload/", "/upload/c_thumb,w_150,h_150/"));
        productImage.setMediumUrl(baseUrl.replace("/upload/", "/upload/c_fit,w_400,h_400/"));
        productImage.setLargeUrl(baseUrl.replace("/upload/", "/upload/c_fit,w_800,h_800/"));

        // Save to database
        ProductImage savedImage = productImageRepository.save(productImage);
        log.info("Image saved to Cloudinary with ID: {} for Product ID: {}", savedImage.getId(), product.getId());

        return savedImage;
    }

    /**
     * Save video to Cloudinary
     */
    public ProductImage saveVideoToCloudinary(MultipartFile file, Product product, Integer displayOrder) throws IOException {
        // Upload to Cloudinary - Cloudinary will automatically optimize the video
        FileService.FileUploadResult uploadResult = fileService.uploadVideo(file, "products/videos");

        // Create ProductImage entity for video
        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setOriginalFilename(file.getOriginalFilename());
        productImage.setContentType(file.getContentType());
        productImage.setFileSize(file.getSize());
        productImage.setDisplayOrder(displayOrder);
        productImage.setIsPrimary(false);
        productImage.setIsVideo(true);

        // Store Cloudinary URLs - Cloudinary handles video processing automatically
        productImage.setCloudinaryPublicId(uploadResult.getCloudinaryPublicId());
        productImage.setVideoUrl(uploadResult.getPublicUrl());
        
        // Generate video thumbnail using Cloudinary auto-generated thumbnail
        String videoThumbnailUrl = uploadResult.getPublicUrl()
                .replace("/upload/", "/upload/so_0,c_thumb,w_150,h_150/")
                .replace(".mp4", ".jpg");
        productImage.setVideoThumbnailUrl(videoThumbnailUrl);

        // Save to database
        ProductImage savedVideo = productImageRepository.save(productImage);
        log.info("Video saved to Cloudinary with ID: {} for Product ID: {}", savedVideo.getId(), product.getId());

        return savedVideo;
    }

    /**
     * Delete image from Cloudinary
     */
    public void deleteImage(Long imageId) {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));

        // Delete from Cloudinary
        if (productImage.getCloudinaryPublicId() != null) {
            try {
                if (productImage.getIsVideo() != null && productImage.getIsVideo()) {
                    fileService.deleteVideo(productImage.getCloudinaryPublicId());
                } else {
                    // For images, we can use the same delete method
                    fileService.deleteVideo(productImage.getCloudinaryPublicId());
                }
                log.info("File deleted from Cloudinary: {}", productImage.getCloudinaryPublicId());
            } catch (Exception e) {
                log.error("Failed to delete file from Cloudinary: {}", productImage.getCloudinaryPublicId(), e);
            }
        }

        // Delete from database
        productImageRepository.deleteById(imageId);
        log.info("Image deleted from database with ID: {}", imageId);
    }
}