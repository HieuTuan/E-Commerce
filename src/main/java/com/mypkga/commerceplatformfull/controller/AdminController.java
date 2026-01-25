package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Category;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.ProductImage;
import com.mypkga.commerceplatformfull.repository.CategoryRepository;
import com.mypkga.commerceplatformfull.repository.ProductImageRepository;
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
                            productImage.setDisplayOrder(displayOrder++);
                            productImage.setIsPrimary(displayOrder == 1); // First image is primary
                            
                            productImageRepository.save(productImage);
                        } catch (Exception e) {
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
            orderService.updateOrderStatus(id, Order.OrderStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Order status updated!");
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
}
