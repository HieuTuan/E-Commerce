package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Review;
import com.mypkga.commerceplatformfull.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    
    private final ReviewRepository reviewRepository;
    
    @Override
    @Transactional
    public Review saveReview(Review review) {
        return reviewRepository.save(review);
    }
    
    @Override
    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }
    
    @Override
    public Page<Review> getPendingReviews(Pageable pageable) {
        // Assuming reviews need moderation - you might need to add a status field to Review entity
        return reviewRepository.findAll(pageable);
    }
    
    @Override
    public long countPendingReviews() {
        // For now, return 0 - implement when Review entity has moderation status
        return 0;
    }
    
    @Override
    public long countAllReviews() {
        return reviewRepository.count();
    }
    
    @Override
    @Transactional
    public void approveReview(Long id) {
        // Implement when Review entity has moderation status
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        // review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);
    }
    
    @Override
    @Transactional
    public void rejectReview(Long id) {
        // For now, just delete the review
        reviewRepository.deleteById(id);
    }
}