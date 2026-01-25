package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.service.ProductService;
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

        // For CUSTOMER or anonymous users, show home page
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        model.addAttribute("latestProducts", productService.getLatestProducts());
        return "index";
    }
}
