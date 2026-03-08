package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    // Basic CRUD operations
    Review saveReview(Review review);

    void deleteReview(Long id);

    // Moderation methods
    Page<Review> getPendingReviews(Pageable pageable);

    long countPendingReviews();

    long countAllReviews();

    void approveReview(Long id);

    void rejectReview(Long id);

    // Business logic methods
    /**
     * Submit a new review. Enforces:
     * - User must have a delivered order containing this product
     * - 1 review per order per product (anti-spam)
     * - Comment length: 10–500 chars
     */
    Review submitReview(Long userId, Long productId, Integer rating, String comment);

    /**
     * Edit an existing review. Enforces:
     * - Review must belong to the user
     * - Review must be within 7 days of creation
     * - Comment length: 10–500 chars
     */
    Review editReview(Long reviewId, Long userId, Integer rating, String comment);

    /**
     * Find the user's existing review for a product (if any).
     */
    Optional<Review> findUserReviewForProduct(Long userId, Long productId);

    // --- Product detail page queries ---

    /** Get all approved reviews for a product, sorted newest first. */
    List<Review> getApprovedReviews(Long productId);

    /**
     * Get average star rating for a product (approved reviews). Returns 0.0 if
     * none.
     */
    double getAverageRating(Long productId);

    /** Count approved reviews for a product. */
    long countApprovedReviews(Long productId);

    /**
     * Check if the user has at least one delivered order containing this product.
     */
    boolean hasPurchasedProduct(Long userId, Long productId);
}