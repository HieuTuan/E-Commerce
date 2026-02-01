package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(User user, String shippingAddress, String customerName, String customerPhone,
            String paymentMethod);

    Optional<Order> getOrderById(Long id);

    Optional<Order> getOrderByOrderNumber(String orderNumber);

    List<Order> getUserOrders(Long userId);

    List<Order> getAllOrders();

    Order updateOrderStatus(Long orderId, OrderStatus status);

    Order updatePaymentStatus(Long orderId, Order.PaymentStatus status);

    void cancelOrder(Long orderId);

    String generateOrderNumber();
    
    // Staff workflow methods
    Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable);
    
    long countOrdersByStatus(OrderStatus status);
    
    List<Order> getRecentOrders(int limit);

    // Fix method for existing COD orders
    void fixExistingCODOrders();
    
    // Delivery issue methods
    List<Order> getOrdersWithDeliveryIssues();
    
    void updateDeliveryIssueFlag(Long orderId, boolean hasIssue);
    
    boolean hasDeliveryIssue(Long orderId);
}
