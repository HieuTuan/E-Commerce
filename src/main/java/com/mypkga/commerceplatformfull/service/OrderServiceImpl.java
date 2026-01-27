package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.CartRepository;
import com.mypkga.commerceplatformfull.repository.OrderItemRepository;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import com.mypkga.commerceplatformfull.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Order createOrder(User user, String shippingAddress, String customerName,
            String customerPhone, String paymentMethod) {
        // Get user's cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Validate stock availability before creating order
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() + 
                    ". Available: " + product.getStockQuantity() + ", Requested: " + cartItem.getQuantity());
            }
        }

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setTotalAmount(cart.getTotalAmount());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setShippingAddress(shippingAddress);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);

        // Set payment status and reduce stock based on payment method
        if ("COD".equals(paymentMethod)) {
            // For COD, reduce stock immediately and set payment as pending
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setStatus(Order.OrderStatus.PROCESSING);
            
            // Reduce stock immediately for COD orders
            for (CartItem cartItem : cart.getItems()) {
                Product product = cartItem.getProduct();
                int newStock = product.getStockQuantity() - cartItem.getQuantity();
                product.setStockQuantity(newStock);
                productRepository.save(product);
                System.out.println("Reduced stock for product: " + product.getName() + 
                    " from " + (newStock + cartItem.getQuantity()) + " to " + newStock);
            }
        } else {
            // For online payment methods, stock will be reduced when payment is confirmed
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
        }

        Order savedOrder = orderRepository.save(order);

        // Create order items from cart
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItemRepository.save(orderItem);
        }

        // Clear cart
        cartService.clearCart(user.getId());

        return savedOrder;
    }

    @Override
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Override
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedDateDesc(userId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedDateDesc();
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updatePaymentStatus(Long orderId, Order.PaymentStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        Order.PaymentStatus oldStatus = order.getPaymentStatus();
        order.setPaymentStatus(status);

        // If payment is successful and was not paid before, reduce stock (only for non-COD orders)
        if (status == Order.PaymentStatus.PAID && oldStatus != Order.PaymentStatus.PAID) {
            order.setStatus(Order.OrderStatus.PROCESSING);
            
            // Only reduce stock for non-COD orders (COD stock is already reduced in createOrder)
            if (!"COD".equals(order.getPaymentMethod())) {
                // Reduce stock for each order item
                for (OrderItem orderItem : order.getItems()) {
                    Product product = orderItem.getProduct();
                    int newStock = product.getStockQuantity() - orderItem.getQuantity();
                    
                    if (newStock < 0) {
                        throw new RuntimeException("Insufficient stock for product: " + product.getName());
                    }
                    
                    product.setStockQuantity(newStock);
                    productRepository.save(product);
                    System.out.println("Reduced stock for product: " + product.getName() + 
                        " from " + (newStock + orderItem.getQuantity()) + " to " + newStock);
                }
            }
        }

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Only restore stock if order was not already cancelled and stock was previously reduced
        if (order.getStatus() != Order.OrderStatus.CANCELLED) {
            // Restore stock for cancelled orders
            boolean shouldRestoreStock = false;
            
            // Restore stock if:
            // 1. COD order (stock was reduced on creation)
            // 2. Paid order (stock was reduced on payment)
            if ("COD".equals(order.getPaymentMethod()) || 
                order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                shouldRestoreStock = true;
            }
            
            if (shouldRestoreStock) {
                for (OrderItem orderItem : order.getItems()) {
                    Product product = orderItem.getProduct();
                    int restoredStock = product.getStockQuantity() + orderItem.getQuantity();
                    product.setStockQuantity(restoredStock);
                    productRepository.save(product);
                    System.out.println("Restored stock for product: " + product.getName() + 
                        " from " + (restoredStock - orderItem.getQuantity()) + " to " + restoredStock);
                }
            }
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    public String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }
    
    // Staff workflow methods
    @Override
    public Page<Order> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedDateDesc(status, pageable);
    }
    
    @Override
    public long countOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
    
    @Override
    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findTop5ByOrderByCreatedDateDesc();
    }

    /**
     * Fix existing COD orders that haven't had their stock reduced
     * This is a one-time fix for orders created before the stock reduction logic was implemented
     */
    @Transactional
    public void fixExistingCODOrders() {
        // Find all COD orders with PENDING status (these likely haven't had stock reduced)
        List<Order> codOrders = orderRepository.findByPaymentMethodAndStatus("COD", Order.OrderStatus.PENDING);
        
        for (Order order : codOrders) {
            try {
                // Check if we can reduce stock for this order
                boolean canReduceStock = true;
                for (OrderItem orderItem : order.getItems()) {
                    Product product = orderItem.getProduct();
                    if (product.getStockQuantity() < orderItem.getQuantity()) {
                        canReduceStock = false;
                        System.out.println("Cannot reduce stock for order " + order.getOrderNumber() + 
                            " - insufficient stock for product: " + product.getName());
                        break;
                    }
                }
                
                if (canReduceStock) {
                    // Reduce stock and update order status
                    for (OrderItem orderItem : order.getItems()) {
                        Product product = orderItem.getProduct();
                        int newStock = product.getStockQuantity() - orderItem.getQuantity();
                        product.setStockQuantity(newStock);
                        productRepository.save(product);
                        System.out.println("Fixed COD order - Reduced stock for product: " + product.getName() + 
                            " from " + (newStock + orderItem.getQuantity()) + " to " + newStock);
                    }
                    
                    // Update order status to PROCESSING
                    order.setStatus(Order.OrderStatus.PROCESSING);
                    orderRepository.save(order);
                    System.out.println("Updated COD order " + order.getOrderNumber() + " status to PROCESSING");
                }
            } catch (Exception e) {
                System.err.println("Error fixing COD order " + order.getOrderNumber() + ": " + e.getMessage());
            }
        }
    }
}
