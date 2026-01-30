package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.CartService;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.UserService;
import com.mypkga.commerceplatformfull.service.VNPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final UserService userService;
    private final CartService cartService;
    private final VNPayService vnPayService;

    @GetMapping
    public String checkoutPage(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        Cart cart = cartService.getCartByUser(user);
        
        // Check if cart is empty
        if (cart == null || cart.getItems().isEmpty()) {
            model.addAttribute("error", "Your cart is empty");
            return "redirect:/cart";
        }
        
        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartService.getCartTotal(user.getId()));
        return "checkout/checkout";
    }

    @PostMapping("/process")
    public String processCheckout(@RequestParam String shippingAddress,
            @RequestParam String customerName,
            @RequestParam String customerPhone,
            @RequestParam String paymentMethod,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            Order order = orderService.createOrder(user, shippingAddress, customerName,
                    customerPhone, paymentMethod);

            // Handle payment method
            if ("VNPAY".equals(paymentMethod)) {
                String paymentUrl = vnPayService.createPaymentUrl(order);
                return "redirect:" + paymentUrl;
            } else if ("COD".equals(paymentMethod)) {
                redirectAttributes.addFlashAttribute("success", "Đơn hàng đã được tạo thành công!");
                // Redirect to timeline instead of success page
                return "redirect:/orders/" + order.getId() + "/timeline?success=true";
            }

            redirectAttributes.addFlashAttribute("success", "Đơn hàng đã được tạo thành công!");
            // Redirect to timeline instead of success page
            return "redirect:/orders/" + order.getId() + "/timeline?success=true";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Checkout failed: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/success")
    public String checkoutSuccess(@RequestParam String orderNumber, Model model) {
        Order order = orderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        model.addAttribute("order", order);
        return "checkout/success";
    }

    private User getCurrentUser(Authentication authentication) {
        // authentication.getName() returns email (from CustomUserDetailsService)
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
