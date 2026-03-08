package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Review;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import com.mypkga.commerceplatformfull.repository.ProductRepository;
import com.mypkga.commerceplatformfull.repository.ReviewRepository;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

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
        return reviewRepository.findAll(pageable);
    }

    @Override
    public long countPendingReviews() {
        return 0;
    }

    @Override
    public long countAllReviews() {
        return reviewRepository.count();
    }

    @Override
    @Transactional
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setApproved(true);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void rejectReview(Long id) {
        reviewRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Review submitReview(Long userId, Long productId, Integer rating, String comment) {
        // 1. Validate comment length
        validateComment(comment);

        // 2. Check user has purchased this product (delivered order)
        boolean hasPurchased = orderRepository.existsDeliveredOrderContainingProduct(userId, productId);
        if (!hasPurchased) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sau khi đã mua và nhận sản phẩm này.");
        }

        // 3. Get the orderId to use for this review (latest delivered order)
        List<Long> orderIds = orderRepository.findDeliveredOrderIdsContainingProduct(userId, productId);
        if (orderIds.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy đơn hàng hợp lệ.");
        }

        // 4. Anti-spam: check if review already exists for this user + product
        // (regardless of order)
        Optional<Review> existing = reviewRepository.findFirstByUserIdAndProductIdOrderByCreatedDateDesc(userId,
                productId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi.");
        }

        // 5. Anti-spam: check if any orderId already has a review
        Long orderId = null;
        for (Long oid : orderIds) {
            if (!reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, oid, productId)) {
                orderId = oid;
                break;
            }
        }
        if (orderId == null) {
            throw new IllegalStateException("Mỗi đơn hàng chỉ được đánh giá 1 lần.");
        }

        // 6. Validate rating
        if (rating == null || rating < 1)
            rating = 1;
        if (rating > 5)
            rating = 5;

        // 7. Build and save review
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setApproved(true);

        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public Review editReview(Long reviewId, Long userId, Integer rating, String comment) {
        // 1. Validate comment
        validateComment(comment);

        // 2. Find review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá."));

        // 3. Check ownership
        if (!review.getUser().getId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa đánh giá này.");
        }

        // 4. Check 7-day window
        if (!review.isEditable()) {
            throw new IllegalStateException("Chỉ có thể chỉnh sửa đánh giá trong vòng 7 ngày sau khi gửi.");
        }

        // 5. Validate rating
        if (rating == null || rating < 1)
            rating = 1;
        if (rating > 5)
            rating = 5;

        // 6. Apply changes
        review.setRating(rating);
        review.setComment(comment.trim());

        return reviewRepository.save(review);
    }

    @Override
    public Optional<Review> findUserReviewForProduct(Long userId, Long productId) {
        return reviewRepository.findFirstByUserIdAndProductIdOrderByCreatedDateDesc(userId, productId);
    }

    @Override
    public List<Review> getApprovedReviews(Long productId) {
        return reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedDateDesc(productId);
    }

    @Override
    public double getAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public long countApprovedReviews(Long productId) {
        return reviewRepository.countByProductIdAndApprovedTrue(productId);
    }

    @Override
    public boolean hasPurchasedProduct(Long userId, Long productId) {
        return orderRepository.existsDeliveredOrderContainingProduct(userId, productId);
    }

    // --- Private helpers ---

    private void validateComment(String comment) {
        if (comment == null || comment.trim().length() < 10) {
            throw new IllegalArgumentException("Bình luận phải có ít nhất 10 ký tự.");
        }
        if (comment.trim().length() > 500) {
            throw new IllegalArgumentException("Bình luận không được vượt quá 500 ký tự.");
        }
    }

}