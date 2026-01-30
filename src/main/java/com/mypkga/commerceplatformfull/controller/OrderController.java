package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.DeliveryConfirmation;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderTimelineEntry;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.DeliveryConfirmationService;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.OrderTimelineService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final OrderTimelineService orderTimelineService;
    private final DeliveryConfirmationService deliveryConfirmationService;

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

        // Security check: ensure user can only view their own orders (except ADMIN/STAFF)
        boolean isAdminOrStaff = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                auth.getAuthority().equals("ROLE_STAFF"));
        
        if (!isAdminOrStaff && !order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Get timeline for the order
        List<OrderTimelineEntry> timeline = orderTimelineService.getOrderTimeline(id);
        
        // Get delivery confirmation status
        DeliveryConfirmation deliveryConfirmation = deliveryConfirmationService.getConfirmationStatus(id);

        model.addAttribute("order", order);
        model.addAttribute("timeline", timeline);
        model.addAttribute("deliveryConfirmation", deliveryConfirmation);
        
        return "orders/detail";
    }

    private User getCurrentUser(Authentication authentication) {
        // authentication.getName() returns email (from CustomUserDetailsService)
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
