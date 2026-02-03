package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final AIClassificationService aiClassificationService;

    // Removed uploadDir - no longer needed since images are stored in database

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAllWithImages();
    }
    
    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        // AI classification disabled for performance
        // classifyProduct(product);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
    
    @Override
    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        return productRepository.searchProductsWithImages(keyword);
    }

    @Override
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdWithImages(categoryId);
    }

    @Override
    public List<Product> getFeaturedProducts() {
        return productRepository.findByFeaturedTrue();
    }

    @Override
    public List<Product> getLatestProducts() {
        return productRepository.findTop10ByOrderByCreatedDateDesc();
    }

    // Removed saveProductImage method - now using CloudinaryImageService

    @Override
    public void classifyProduct(Product product) {
        // AI classification disabled for performance
        // String aiCategory = aiClassificationService.classifyProduct(
        //         product.getName(),
        //         product.getDescription());
        // product.setAiCategory(aiCategory);
        
        // Set default category to avoid null
        product.setAiCategory("General");
    }
    
    @Override
    public long countAllProducts() {
        return productRepository.count();
    }
}
