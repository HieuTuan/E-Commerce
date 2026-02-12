package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.CategoryRepository;
import com.mypkga.commerceplatformfull.service.CartService;
import com.mypkga.commerceplatformfull.service.CategoryService;
import com.mypkga.commerceplatformfull.service.ProductService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping("/products")
    public String productList(@RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            Model model, Authentication authentication) {
        List<Product> products;

        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search);
        } else if (categoryId != null) {
            products = productService.getProductsByCategory(categoryId);
        } else {
            products = productService.getAllProducts();
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

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);

        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        model.addAttribute("product", product);
        return "products/detail";
    }
}
