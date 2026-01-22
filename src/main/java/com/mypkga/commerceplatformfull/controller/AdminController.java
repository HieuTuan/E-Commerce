package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Category;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.repository.CategoryRepository;
import com.mypkga.commerceplatformfull.service.FileUploadService;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.ProductService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final FileUploadService fileUploadService;

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("totalProducts", productService.getAllProducts().size());
        model.addAttribute("totalOrders", orderService.getAllOrders().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("recentOrders", orderService.getAllOrders());
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String manageProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
            @RequestParam Long categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
            RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);

            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    FileUploadService.ImageUploadResult imageResult = fileUploadService.uploadImage(imageFile);
                    product.setImageOriginal(imageResult.getOriginalFileName());
                    product.setImageThumbnail(imageResult.getThumbnailFileName());
                    product.setImageMedium(imageResult.getMediumFileName());
                    product.setImageLarge(imageResult.getLargeFileName());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Error uploading image: " + e.getMessage());
                    return "redirect:/admin/products";
                }
            }

            // Handle video upload
            if (videoFile != null && !videoFile.isEmpty()) {
                try {
                    FileUploadService.VideoUploadResult videoResult = fileUploadService.uploadVideo(videoFile);
                    product.setVideoFilename(videoResult.getFileName());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Error uploading video: " + e.getMessage());
                    return "redirect:/admin/products";
                }
            }

            if (product.getId() == null) {
                productService.createProduct(product);
                redirectAttributes.addFlashAttribute("success", "Product created successfully!");
            } else {
                productService.updateProduct(product);
                redirectAttributes.addFlashAttribute("success", "Product updated successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Get product to delete associated files
            Product product = productService.getProductById(id).orElse(null);
            if (product != null) {
                // Delete associated files
                if (product.getImageOriginal() != null) {
                    fileUploadService.deleteImageFiles(
                        product.getImageOriginal(),
                        product.getImageThumbnail(),
                        product.getImageMedium(),
                        product.getImageLarge()
                    );
                }
                if (product.getVideoFilename() != null) {
                    fileUploadService.deleteVideoFile(product.getVideoFilename());
                }
            }
            
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String manageOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, Order.OrderStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Order status updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating order: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}
