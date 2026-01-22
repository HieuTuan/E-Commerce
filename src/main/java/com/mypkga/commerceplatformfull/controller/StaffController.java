package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.ProductService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

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
            User staff = getCurrentUser(authentication);
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
                                @RequestParam String deliveryInfo,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User staff = getCurrentUser(authentication);
            
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
                              @RequestParam(defaultValue = "10") int size) {
        
        Page<Product> products = productService.getAllProducts(PageRequest.of(page, size));
        
        model.addAttribute("products", products);
        
        return "staff/products";
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}