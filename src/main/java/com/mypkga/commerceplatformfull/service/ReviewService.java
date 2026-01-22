package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}