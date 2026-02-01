package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Category;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
public interface ProductService {

    List<Product> getAllProducts();
    
    Page<Product> getAllProducts(Pageable pageable);

    Optional<Product> getProductById(Long id);

    Product createProduct(Product product);

    Product updateProduct(Product product);
    
    Product saveProduct(Product product);

    void deleteProduct(Long id);

    List<Product> searchProducts(String keyword);

    List<Product> getProductsByCategory(Long categoryId);

    List<Product> getFeaturedProducts();

    List<Product> getLatestProducts();

    String saveProductImage(MultipartFile file);

    void classifyProduct(Product product);
    
    long countAllProducts();
}
