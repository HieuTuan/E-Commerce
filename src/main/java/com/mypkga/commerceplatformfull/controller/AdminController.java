package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.CategoryRepository;
import com.mypkga.commerceplatformfull.repository.ProductImageRepository;
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
    private final CategoryRepository categoryRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final FileUploadService fileUploadService;
    private final ProductImageRepository productImageRepository;
    private final RoleService roleService;

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
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
            RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(categoryId)
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
                            FileUploadService.ImageUploadResult imageResult = fileUploadService.uploadImage(imageFile);
                            
                            ProductImage productImage = new ProductImage();
                            productImage.setProduct(savedProduct);
                            productImage.setImageOriginal(imageResult.getOriginalFileName());
                            productImage.setImageThumbnail(imageResult.getThumbnailFileName());
                            productImage.setImageMedium(imageResult.getMediumFileName());
                            productImage.setImageLarge(imageResult.getLargeFileName());
                            productImage.setDisplayOrder(displayOrder);
                            productImage.setIsPrimary(displayOrder == 0); // First image is primary
                            displayOrder++;
                            
                            ProductImage savedImage = productImageRepository.save(productImage);
                            System.out.println("Saved ProductImage with ID: " + savedImage.getId() + " for Product ID: " + savedProduct.getId());
                        } catch (Exception e) {
                            System.err.println("Error saving ProductImage: " + e.getMessage());
                            e.printStackTrace();
                            redirectAttributes.addFlashAttribute("error", "Error uploading image: " + e.getMessage());
                            return "redirect:/admin/products";
                        }
                    }
                }
            }

            // Handle video upload - store in ProductImage
            if (videoFile != null && !videoFile.isEmpty()) {
                try {
                    FileUploadService.VideoUploadResult videoResult = fileUploadService.uploadVideo(videoFile);
                    
                    // Create a ProductImage entry for video
                    ProductImage videoImage = new ProductImage();
                    videoImage.setProduct(savedProduct);
                    videoImage.setVideoFilename(videoResult.getFileName());
                    videoImage.setDisplayOrder(999); // Put video at the end
                    videoImage.setIsPrimary(false);
                    
                    productImageRepository.save(videoImage);
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
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Get product to delete associated files
            Product product = productService.getProductById(id).orElse(null);
            if (product != null) {
                // Delete associated product images and their files
                List<ProductImage> productImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id);
                for (ProductImage productImage : productImages) {
                    // Delete image files
                    if (productImage.getImageOriginal() != null) {
                        fileUploadService.deleteImageFiles(
                            productImage.getImageOriginal(),
                            productImage.getImageThumbnail(),
                            productImage.getImageMedium(),
                            productImage.getImageLarge()
                        );
                    }
                    // Delete video files
                    if (productImage.getVideoFilename() != null) {
                        fileUploadService.deleteVideoFile(productImage.getVideoFilename());
                    }
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
            Order order = orderService.getOrderById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            Order.OrderStatus currentStatus = order.getStatus();
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
            
            // Validate status transition
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                redirectAttributes.addFlashAttribute("error", 
                    "Không thể chuyển từ trạng thái " + currentStatus + " sang " + newStatus);
                return "redirect:/admin/orders";
            }
            
            orderService.updateOrderStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("success", 
                "Cập nhật trạng thái đơn hàng thành công: " + newStatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating order: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    /**
     * Validate if status transition is allowed
     */
    private boolean isValidStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        // If same status, allow (no change)
        if (currentStatus == newStatus) {
            return true;
        }
        
        // Once delivered, cannot change to any other status
        if (currentStatus == Order.OrderStatus.DELIVERED) {
            return false;
        }
        
        // Once cancelled, cannot change to any other status except back to cancelled
        if (currentStatus == Order.OrderStatus.CANCELLED && newStatus != Order.OrderStatus.CANCELLED) {
            return false;
        }
        
        // Cannot go back to previous statuses (except cancellation)
        if (newStatus == Order.OrderStatus.CANCELLED) {
            // Can cancel from any status except DELIVERED
            return currentStatus != Order.OrderStatus.DELIVERED;
        }
        
        // Define allowed forward transitions
        switch (currentStatus) {
            case PENDING:
                return newStatus == Order.OrderStatus.PROCESSING || 
                       newStatus == Order.OrderStatus.CANCELLED;
            case PROCESSING:
                return newStatus == Order.OrderStatus.SHIPPED || 
                       newStatus == Order.OrderStatus.CANCELLED;
            case SHIPPED:
                return newStatus == Order.OrderStatus.DELIVERED || 
                       newStatus == Order.OrderStatus.CANCELLED;
            case DELIVERED:
                return false; // Already handled above
            case CANCELLED:
                return false; // Already handled above
            default:
                return false;
        }
    }

    @PostMapping("/products/{productId}/images/{imageId}/delete")
    public String deleteProductImage(@PathVariable Long productId, 
                                   @PathVariable Long imageId,
                                   RedirectAttributes redirectAttributes) {
        try {
            ProductImage productImage = productImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image not found"));
            
            // Delete physical files
            if (productImage.getImageOriginal() != null) {
                fileUploadService.deleteImageFiles(
                    productImage.getImageOriginal(),
                    productImage.getImageThumbnail(),
                    productImage.getImageMedium(),
                    productImage.getImageLarge()
                );
            }
            
            productImageRepository.delete(productImage);
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
            // Remove primary flag from all images of this product
            List<ProductImage> productImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
            productImages.forEach(img -> img.setIsPrimary(false));
            productImageRepository.saveAll(productImages);
            
            // Set new primary image
            ProductImage primaryImage = productImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image not found"));
            primaryImage.setIsPrimary(true);
            productImageRepository.save(primaryImage);
            
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
        model.addAttribute("categories", categoryRepository.findAll());
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
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("success",
                category.getId() == null ? "Category created successfully!" : "Category updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        model.addAttribute("category", category);
        return "admin/category-form";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryRepository.deleteById(id);
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
