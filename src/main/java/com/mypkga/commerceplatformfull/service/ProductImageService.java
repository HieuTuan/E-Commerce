package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.ProductImage;

import java.util.List;
import java.util.Optional;

public interface ProductImageService {
    
    List<ProductImage> getImagesByProductId(Long productId);
    
    Optional<ProductImage> getImageById(Long imageId);
    
    void deleteImage(Long imageId);
    
    void setPrimaryImage(Long productId, Long imageId);
    
    ProductImage saveImage(ProductImage productImage);
    
    List<ProductImage> saveAll(List<ProductImage> productImages);
}
