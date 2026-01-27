package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.CategoryRepository;
import com.mypkga.commerceplatformfull.repository.ProductImageRepository;
import com.mypkga.commerceplatformfull.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;
    private final ProductImageRepository productImageRepository;

    @GetMapping("")
    public String staffHome() {
        return "redirect:/staff/dashboard";
    }

    @GetMapping("/dashboard")
    public String staffDashboard(Model model) {
        // Staff Dashboard - Hỗ trợ khách hàng và xử lý đơn hàng
        model.addAttribute("pendingOrders", orderService.countOrdersByStatus(Order.OrderStatus.PENDING));
        model.addAttribute("processingOrders", orderService.countOrdersByStatus(Order.OrderStatus.PROCESSING));
        model.addAttribute("totalProducts", productService.countAllProducts());
        model.addAttribute("recentOrders", orderService.getRecentOrders(5));
        
        return "staff/dashboard";
    }

    @GetMapping("/orders")
    public String manageOrders(Model model,
                              @RequestParam(defaultValue = "PENDING") String status,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
        
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status);
        Page<Order> orders = orderService.getOrdersByStatus(orderStatus, PageRequest.of(page, size));
        
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", Order.OrderStatus.values());
        
        return "staff/orders";
    }

    @GetMapping("/orders/{id}")
    public String viewOrderDetail(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        model.addAttribute("order", order);
        return "staff/order-detail";
    }

    @PostMapping("/orders/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
                                   @RequestParam String status,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
            
            orderService.updateOrderStatus(id, newStatus);
            
            redirectAttributes.addFlashAttribute("success", 
                "Order status updated to " + newStatus + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to update order status: " + e.getMessage());
        }
        
        return "redirect:/staff/orders";
    }

    @PostMapping("/orders/{id}/assign-delivery")
    public String assignDelivery(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            // Logic để assign delivery (có thể mở rộng sau)
            orderService.updateOrderStatus(id, Order.OrderStatus.PROCESSING);
            
            redirectAttributes.addFlashAttribute("success", 
                "Delivery assigned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to assign delivery: " + e.getMessage());
        }
        
        return "redirect:/staff/orders/" + id;
    }

    @GetMapping("/support")
    public String customerSupport(Model model) {
        // Customer Support Interface - Chat support
        model.addAttribute("activeChats", 0); // Placeholder for chat system
        return "staff/support";
    }

    @GetMapping("/products")
    public String viewProducts(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) Long categoryId) {

        Page<Product> products;
        Pageable pageable = PageRequest.of(page, size);

        if (search != null && !search.isEmpty()) {
            // Convert List to Page manually for search results
            List<Product> searchResults = productService.searchProducts(search);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), searchResults.size());
            List<Product> pageContent = searchResults.subList(start, end);
            products = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, searchResults.size());
        } else if (categoryId != null) {
            // Convert List to Page manually for category filter
            List<Product> categoryResults = productService.getProductsByCategory(categoryId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), categoryResults.size());
            List<Product> pageContent = categoryResults.subList(start, end);
            products = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, categoryResults.size());
        } else {
            products = productService.getAllProducts(pageable);
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);

        return "staff/products";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        return "staff/product-form";
    }

    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());
        return "staff/product-form";
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
                            redirectAttributes.addFlashAttribute("error", "Lỗi khi tải ảnh lên: " + e.getMessage());
                            return "redirect:/staff/products";
                        }
                    }
                }
            }

            // Handle video upload
            if (videoFile != null && !videoFile.isEmpty()) {
                try {
                    FileUploadService.VideoUploadResult videoResult = fileUploadService.uploadVideo(videoFile);

                    ProductImage videoImage = new ProductImage();
                    videoImage.setProduct(savedProduct);
                    videoImage.setVideoFilename(videoResult.getFileName());
                    videoImage.setDisplayOrder(999);
                    videoImage.setIsPrimary(false);

                    productImageRepository.save(videoImage);
                    System.out.println("Saved Video ProductImage with ID: " + videoImage.getId() + " for Product ID: " + savedProduct.getId());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Lỗi khi tải video lên: " + e.getMessage());
                    return "redirect:/staff/products";
                }
            }

            redirectAttributes.addFlashAttribute("success",
                product.getId() == null ? "Tạo sản phẩm thành công!" : "Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu sản phẩm: " + e.getMessage());
        }
        return "redirect:/staff/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(id).orElse(null);
            if (product != null) {
                List<ProductImage> productImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id);
                for (ProductImage productImage : productImages) {
                    if (productImage.getImageOriginal() != null) {
                        fileUploadService.deleteImageFiles(
                            productImage.getImageOriginal(),
                            productImage.getImageThumbnail(),
                            productImage.getImageMedium(),
                            productImage.getImageLarge()
                        );
                    }
                    if (productImage.getVideoFilename() != null) {
                        fileUploadService.deleteVideoFile(productImage.getVideoFilename());
                    }
                }
            }

            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/staff/products";
    }

    @PostMapping("/products/{productId}/images/{imageId}/delete")
    public String deleteProductImage(@PathVariable Long productId,
                                   @PathVariable Long imageId,
                                   RedirectAttributes redirectAttributes) {
        try {
            ProductImage productImage = productImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image not found"));

            if (productImage.getImageOriginal() != null) {
                fileUploadService.deleteImageFiles(
                    productImage.getImageOriginal(),
                    productImage.getImageThumbnail(),
                    productImage.getImageMedium(),
                    productImage.getImageLarge()
                );
            }

            productImageRepository.delete(productImage);
            redirectAttributes.addFlashAttribute("success", "Xóa ảnh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa ảnh: " + e.getMessage());
        }
        return "redirect:/staff/products/edit/" + productId;
    }

    @PostMapping("/products/{productId}/images/{imageId}/set-primary")
    public String setPrimaryImage(@PathVariable Long productId,
                                @PathVariable Long imageId,
                                RedirectAttributes redirectAttributes) {
        try {
            List<ProductImage> productImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
            productImages.forEach(img -> img.setIsPrimary(false));
            productImageRepository.saveAll(productImages);

            ProductImage primaryImage = productImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image not found"));
            primaryImage.setIsPrimary(true);
            productImageRepository.save(primaryImage);

            redirectAttributes.addFlashAttribute("success", "Đặt ảnh chính thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi đặt ảnh chính: " + e.getMessage());
        }
        return "redirect:/staff/products/edit/" + productId;
    }

    @GetMapping("/categories")
    public String manageCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "staff/categories";
    }


    private User getCurrentUser(Authentication authentication) {
        // authentication.getName() returns email (from CustomUserDetailsService)
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}