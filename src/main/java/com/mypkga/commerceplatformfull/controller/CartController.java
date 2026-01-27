package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.exception.CartUpdateException;
import com.mypkga.commerceplatformfull.exception.OutOfStockException;
import com.mypkga.commerceplatformfull.service.CartService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    public String viewCart(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        // Guest user handling - redirect to login
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("Guest user attempted to access cart - redirecting to login");
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem giỏ hàng");
            return "redirect:/login";
        }

        try {
            User user = getCurrentUser(authentication);
            Cart cart = cartService.getCartByUser(user);
            model.addAttribute("cart", cart);
            return "cart/view";
        } catch (Exception e) {
            log.error("Error viewing cart", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi tải giỏ hàng");
            return "redirect:/";
        }
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // Guest user handling - redirect to login
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("Guest user attempted to add to cart - redirecting to login");
            
            // Handle AJAX requests
            if (isAjaxRequest(request)) {
                return "redirect:/login"; // AJAX will handle this
            }
            
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
            return "redirect:/login";
        }

        try {
            User user = getCurrentUser(authentication);
            cartService.addToCart(user, productId, quantity);
            
            String successMessage = "Đã thêm sản phẩm vào giỏ hàng!";
            log.info("Product {} added to cart for user {}", productId, user.getUsername());
            
            // Handle AJAX requests
            if (isAjaxRequest(request)) {
                // For AJAX requests, we'll return a success response
                // The JavaScript will handle the animation
                return "redirect:/cart/add/success"; // This will be handled by JS
            }
            
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (OutOfStockException e) {
            log.warn("Out of stock error: {}", e.getMessage());
            String errorMessage = e.getMessage() + " (Bạn yêu cầu: " + e.getRequestedQuantity() + ")";
            
            if (isAjaxRequest(request)) {
                redirectAttributes.addFlashAttribute("error", errorMessage);
                return "redirect:/products/" + productId;
            }
            
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return returnUrl != null && !returnUrl.isEmpty() ? "redirect:" + returnUrl : "redirect:/products/" + productId;
        } catch (CartUpdateException e) {
            log.error("Cart update error: {}", e.getMessage());
            
            if (isAjaxRequest(request)) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/products/" + productId;
            }
            
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return returnUrl != null && !returnUrl.isEmpty() ? "redirect:" + returnUrl : "redirect:/products/" + productId;
        } catch (Exception e) {
            log.error("Error adding to cart", e);
            String errorMessage = "Không thể thêm sản phẩm vào giỏ hàng";
            
            if (isAjaxRequest(request)) {
                redirectAttributes.addFlashAttribute("error", errorMessage);
                return "redirect:/products/" + productId;
            }
            
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return returnUrl != null && !returnUrl.isEmpty() ? "redirect:" + returnUrl : "redirect:/products/" + productId;
        }

        // Redirect to return URL if provided, otherwise to cart
        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/cart";
    }

    @GetMapping("/add/success")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCartSuccess() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã thêm sản phẩm vào giỏ hàng!");
        return ResponseEntity.ok(response);
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    @PostMapping("/update/{itemId}")
    public String updateCartItem(@PathVariable Long itemId,
            @RequestParam Integer quantity,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // Guest user handling
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("Guest user attempted to update cart - redirecting to login");
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            cartService.updateCartItemQuantity(itemId, quantity);
            if (quantity > 0) {
                redirectAttributes.addFlashAttribute("success", "Đã cập nhật số lượng!");
                log.info("Cart item {} updated to quantity {}", itemId, quantity);
            } else {
                redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng!");
                log.info("Cart item {} removed (quantity 0)", itemId);
            }
        } catch (OutOfStockException e) {
            log.warn("Out of stock during update: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage() +
                " (Bạn yêu cầu: " + e.getRequestedQuantity() + ")");
        } catch (Exception e) {
            log.error("Error updating cart", e);
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật giỏ hàng");
        }

        return "redirect:/cart";
    }

    @PostMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        // Guest user handling
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("Guest user attempted to remove from cart - redirecting to login");
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        try {
            cartService.removeFromCart(itemId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng!");
            log.info("Cart item {} removed", itemId);
        } catch (Exception e) {
            log.error("Error removing from cart", e);
            redirectAttributes.addFlashAttribute("error", "Không thể xóa sản phẩm");
        }

        return "redirect:/cart";
    }

    private User getCurrentUser(Authentication authentication) {
        // authentication.getName() returns email (from CustomUserDetailsService)
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
