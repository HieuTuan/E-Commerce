package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.User;
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
    private final UserService userService;

    /**
     * Submit a new review for a product.
     * Enforces: must be logged in, must have purchased, 1 review per product,
     * comment 10-500 chars.
     */
    @PostMapping("/products/{id}/reviews")
    public String submitReview(@PathVariable("id") Long productId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "comment", required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            reviewService.submitReview(user.getId(), productId, rating, comment);
            redirectAttributes.addFlashAttribute("message", "Đã gửi đánh giá của bạn. Cảm ơn bạn!");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/products/" + productId + "#reviews";
    }

    /**
     * Edit an existing review.
     * Enforces: must be review owner, must be within 7 days.
     */
    @PostMapping("/products/{id}/reviews/{reviewId}/edit")
    public String editReview(@PathVariable("id") Long productId,
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "comment", required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            reviewService.editReview(reviewId, user.getId(), rating, comment);
            redirectAttributes.addFlashAttribute("message", "Đã cập nhật đánh giá của bạn.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/products/" + productId + "#reviews";
    }
}
