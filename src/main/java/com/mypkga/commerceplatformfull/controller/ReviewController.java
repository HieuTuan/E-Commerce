package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.Review;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.ProductService;
import com.mypkga.commerceplatformfull.service.ReviewService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ProductService productService;
    private final UserService userService;

    @PostMapping("/products/{id}/reviews")
    public String submitReview(@PathVariable("id") Long productId,
                               @RequestParam("rating") Integer rating,
                               @RequestParam(value = "comment", required = false) String comment,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Validate rating bounds (1-5)
        if (rating == null || rating < 1) rating = 1;
        if (rating > 5) rating = 5;

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);
        // Auto-approve for now so it appears immediately
        review.setApproved(true);

        reviewService.saveReview(review);

        redirectAttributes.addFlashAttribute("message", "Đã gửi đánh giá của bạn. Cảm ơn bạn!");
        return "redirect:/products/" + productId + "#reviews";
    }
}
