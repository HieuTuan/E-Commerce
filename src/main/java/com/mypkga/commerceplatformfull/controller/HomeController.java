package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.CartService;
import com.mypkga.commerceplatformfull.service.ProductService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping({ "/", "/home" })
    public String home(Model model, Authentication authentication) {
        // Redirect authenticated ADMIN/STAFF to their respective dashboards
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role)) {
                    return "redirect:/admin";
                } else if ("ROLE_STAFF".equals(role)) {
                    return "redirect:/staff";
                }
            }
        }

        // Add cart information for authenticated users
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                User user = userService.findByEmail(authentication.getName()).orElse(null);
                if (user != null) {
                    Cart cart = cartService.getCartByUser(user);
                    model.addAttribute("cartItemCount", cart != null ? cart.getTotalItems() : 0);
                }
            } catch (Exception e) {
                model.addAttribute("cartItemCount", 0);
            }
        } else {
            model.addAttribute("cartItemCount", 0);
        }

        // For CUSTOMER or anonymous users, show home page
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        model.addAttribute("latestProducts", productService.getLatestProducts());
        return "index";
    }
}
