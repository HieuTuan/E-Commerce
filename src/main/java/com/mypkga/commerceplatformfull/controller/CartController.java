package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.exception.CartUpdateException;
import com.mypkga.commerceplatformfull.exception.OutOfStockException;
import com.mypkga.commerceplatformfull.service.CartService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            RedirectAttributes redirectAttributes) {

        // Guest user handling - redirect to login
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("Guest user attempted to add to cart - redirecting to login");
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
            return "redirect:/login";
        }

        try {
            User user = getCurrentUser(authentication);
            cartService.addToCart(user, productId, quantity);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng!");
            log.info("Product {} added to cart for user {}", productId, user.getUsername());
        } catch (OutOfStockException e) {
            log.warn("Out of stock error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage() +
                " (Bạn yêu cầu: " + e.getRequestedQuantity() + ")");
            return returnUrl != null && !returnUrl.isEmpty() ? "redirect:" + returnUrl : "redirect:/products/" + productId;
        } catch (CartUpdateException e) {
            log.error("Cart update error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return returnUrl != null && !returnUrl.isEmpty() ? "redirect:" + returnUrl : "redirect:/products/" + productId;
        } catch (Exception e) {
            log.error("Error adding to cart", e);
            redirectAttributes.addFlashAttribute("error", "Không thể thêm sản phẩm vào giỏ hàng");
            return returnUrl != null && !returnUrl.isEmpty() ? "redirect:" + returnUrl : "redirect:/products/" + productId;
        }

        // Redirect to return URL if provided, otherwise to cart
        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/cart";
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
