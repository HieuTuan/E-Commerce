package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.Category;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(Category category);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);
    
    Optional<Product> findByName(String name);

    List<Product> findByFeaturedTrue();

    List<Product> findByAiCategory(String aiCategory);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    List<Product> findTop10ByOrderByCreatedDateDesc();
    
    // Eager load product images to fix display issues
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productImages")
    List<Product> findAllWithImages();
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productImages WHERE p.category.id = :categoryId")
    List<Product> findByCategoryIdWithImages(@Param("categoryId") Long categoryId);
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productImages WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchProductsWithImages(@Param("keyword") String keyword);
}
