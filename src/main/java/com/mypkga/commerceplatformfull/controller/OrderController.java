package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping
    public String myOrders(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        List<Order> orders = orderService.getUserOrders(user.getId());

        model.addAttribute("orders", orders);
        return "orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
            Authentication authentication,
            Model model) {
        User user = getCurrentUser(authentication);
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security check: ensure user can only view their own orders
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        model.addAttribute("order", order);
        return "orders/detail";
    }

    private User getCurrentUser(Authentication authentication) {
        // authentication.getName() returns email (from CustomUserDetailsService)
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
