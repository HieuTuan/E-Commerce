package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.ProductImage;
import com.mypkga.commerceplatformfull.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {
    
    private final ProductImageRepository productImageRepository;
    
    @Override
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
    }
    
    @Override
    public Optional<ProductImage> getImageById(Long imageId) {
        return productImageRepository.findById(imageId);
    }
    
    @Override
    @Transactional
    public void deleteImage(Long imageId) {
        productImageRepository.deleteById(imageId);
    }
    
    @Override
    @Transactional
    public void setPrimaryImage(Long productId, Long imageId) {
        // Get all images for this product
        List<ProductImage> productImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        
        // Set isPrimary=false for all images
        productImages.forEach(img -> img.setIsPrimary(false));
        productImageRepository.saveAll(productImages);
        
        // Set isPrimary=true for the specified image
        ProductImage primaryImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        primaryImage.setIsPrimary(true);
        productImageRepository.save(primaryImage);
    }
    
    @Override
    @Transactional
    public ProductImage saveImage(ProductImage productImage) {
        return productImageRepository.save(productImage);
    }
    
    @Override
    @Transactional
    public List<ProductImage> saveAll(List<ProductImage> productImages) {
        return productImageRepository.saveAll(productImages);
    }
}
