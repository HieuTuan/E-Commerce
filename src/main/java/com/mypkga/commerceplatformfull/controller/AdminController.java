package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final UserService userService;
    private final CloudinaryImageService cloudinaryImageService; // Changed to CloudinaryImageService
    private final ProductImageService productImageService;
    private final RoleService roleService;
    private final DeliveryIssueReportService deliveryIssueReportService;

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("totalProducts", productService.getAllProducts().size());
        model.addAttribute("totalOrders", orderService.getAllOrders().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("pendingIssues", deliveryIssueReportService.getPendingReportsCount());
        model.addAttribute("recentOrders", orderService.getAllOrders());
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String manageProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
            @RequestParam Long categoryId,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
            RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.getCategoryById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);

            // Save product first
            Product savedProduct;
            if (product.getId() == null) {
                savedProduct = productService.createProduct(product);
            } else {
                savedProduct = productService.updateProduct(product);
            }

            // Handle multiple image uploads
            if (imageFiles != null && imageFiles.length > 0) {
                int displayOrder = 0;
                for (MultipartFile imageFile : imageFiles) {
                    if (imageFile != null && !imageFile.isEmpty()) {
                        try {
                            // Save image to Cloudinary
                            ProductImage savedImage = cloudinaryImageService.saveImageToCloudinary(
                                imageFile, savedProduct, displayOrder, displayOrder == 0);
                            
                            System.out.println("Saved ProductImage with ID: " + savedImage.getId() + " for Product ID: " + savedProduct.getId());
                            displayOrder++;
                        } catch (Exception e) {
                            System.err.println("Error saving ProductImage: " + e.getMessage());
                            e.printStackTrace();
                            redirectAttributes.addFlashAttribute("error", "Error uploading image: " + e.getMessage());
                            return "redirect:/admin/products";
                        }
                    }
                }
            }

            // Handle video upload - store in Cloudinary
            if (videoFile != null && !videoFile.isEmpty()) {
                try {
                    // Save video to Cloudinary
                    ProductImage savedVideo = cloudinaryImageService.saveVideoToCloudinary(
                        videoFile, savedProduct, 999);
                    
                    System.out.println("Saved Video ProductImage with ID: " + savedVideo.getId() + " for Product ID: " + savedProduct.getId());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Error uploading video: " + e.getMessage());
                    return "redirect:/admin/products";
                }
            }

            redirectAttributes.addFlashAttribute("success", 
                product.getId() == null ? "Product created successfully!" : "Product updated successfully!");
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
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/product-form";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Get product to delete associated files
            Product product = productService.getProductById(id).orElse(null);
            if (product != null) {
                // Delete associated product images and their files
                List<ProductImage> productImages = productImageService.getImagesByProductId(id);
                for (ProductImage productImage : productImages) {
                    // Images are stored in Cloudinary, deletion handled by cascade delete
                    // Just delete the database records (handled by cascade delete)
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
            OrderStatus newStatus = OrderStatus.valueOf(status);
            
            // Admin bypass: Use the service method to update status (no validation)
            orderService.updateOrderStatus(id, newStatus);
            
            redirectAttributes.addFlashAttribute("success", 
                "Order status updated successfully: " + newStatus.getDisplayName());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid status value: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating order: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/products/{productId}/images/{imageId}/delete")
    public String deleteProductImage(@PathVariable Long productId, 
                                   @PathVariable Long imageId,
                                   RedirectAttributes redirectAttributes) {
        try {
            ProductImage productImage = productImageService.getImageById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image not found"));
            
            // Images are stored in Cloudinary, deletion handled by CloudinaryImageService
            // Just delete the database record
            productImageService.deleteImage(imageId);
            redirectAttributes.addFlashAttribute("success", "Image deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting image: " + e.getMessage());
        }
        return "redirect:/admin/products/edit/" + productId;
    }

    @PostMapping("/products/{productId}/images/{imageId}/set-primary")
    public String setPrimaryImage(@PathVariable Long productId, 
                                @PathVariable Long imageId,
                                RedirectAttributes redirectAttributes) {
        try {
            // Use service to handle the primary image logic
            productImageService.setPrimaryImage(productId, imageId);
            
            redirectAttributes.addFlashAttribute("success", "Primary image updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating primary image: " + e.getMessage());
        }
        return "redirect:/admin/products/edit/" + productId;
    }

    // ==================== User Management ====================

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setEnabled(!user.getEnabled());
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success",
                "User status updated to " + (user.getEnabled() ? "Enabled" : "Disabled"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/change-role")
    public String changeUserRole(@PathVariable Long id,
                                @RequestParam Long roleId,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Role role = roleService.getRoleById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole(role);
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "User role updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user role: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== Category Management ====================

    @GetMapping("/categories")
    public String manageCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        try {
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("success",
                category.getId() == null ? "Category created successfully!" : "Category updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        model.addAttribute("category", category);
        return "admin/category-form";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ==================== Reports ====================

    @GetMapping("/reports")
    public String viewReports(Model model) {
        model.addAttribute("totalOrders", orderService.getAllOrders().size());
        model.addAttribute("totalProducts", productService.getAllProducts().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("recentOrders", orderService.getRecentOrders(10));
        return "admin/reports";
    }

    @PostMapping("/fix-cod-orders")
    public String fixCODOrders(RedirectAttributes redirectAttributes) {
        try {
            orderService.fixExistingCODOrders();
            redirectAttributes.addFlashAttribute("success", "Fixed existing COD orders successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error fixing COD orders: " + e.getMessage());
        }
        return "redirect:/admin/reports";
    }
}
