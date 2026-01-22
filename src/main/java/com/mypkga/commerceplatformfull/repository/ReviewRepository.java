package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.Review;
import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct(Product product);

    List<Review> findByProductId(Long productId);

    List<Review> findByProductIdOrderByCreatedDateDesc(Long productId);

    List<Review> findByProductIdAndApprovedTrue(Long productId);

    List<Review> findByProductIdAndApprovedTrueOrderByCreatedDateDesc(Long productId);

    List<Review> findByUser(User user);

    List<Review> findByUserId(Long userId);

    List<Review> findByApprovedFalse();

    Long countByProductId(Long productId);

    Long countByProductIdAndApprovedTrue(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);
}
