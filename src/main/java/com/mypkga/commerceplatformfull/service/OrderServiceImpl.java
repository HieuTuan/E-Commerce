package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.CartRepository;
import com.mypkga.commerceplatformfull.repository.OrderItemRepository;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import com.mypkga.commerceplatformfull.repository.ProductRepository;
import com.mypkga.commerceplatformfull.util.HtmlUtilsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final OrderTimelineService orderTimelineService;
    private final DeliveryConfirmationService deliveryConfirmationService;

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
        order.setStatus(OrderStatus.PENDING);
        order.setCurrentStatus(OrderStatus.PENDING); // Set current status for timeline
        order.setPaymentMethod(paymentMethod);
        order.setShippingAddress(HtmlUtilsHelper.decodeHtml(shippingAddress));
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);

        // Set payment status and reduce stock based on payment method
        if ("COD".equals(paymentMethod)) {
            // For COD, reduce stock immediately and set payment as pending
            order.setPaymentStatus(Order.PaymentStatus.PENDING);
            order.setStatus(OrderStatus.CONFIRMED); // Set to CONFIRMED for COD
            order.setCurrentStatus(OrderStatus.CONFIRMED);

            // Reduce stock immediately for COD orders
            for (CartItem cartItem : cart.getItems()) {
                Product product = cartItem.getProduct();
                int newStock = product.getStockQuantity() - cartItem.getQuantity();
                product.setStockQuantity(newStock);
                productRepository.save(product);
                System.out.println("Reduced stock for product: " + product.getName() +
                        " from " + (newStock + cartItem.getQuantity()) + " to " + newStock);
            }

            // Clear cart immediately for COD orders
            cartService.clearCart(user.getId());
        } else {
            // For online payment methods, stock will be reduced when payment is confirmed
            // Cart will also be cleared when payment is confirmed
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

        // Don't clear cart here for online payments - will be cleared when payment is
        // confirmed

        // Create initial timeline entry
        try {
            orderTimelineService.createTimelineEntry(savedOrder.getId(), OrderStatus.PENDING, "SYSTEM",
                    "Đơn hàng được tạo với phương thức thanh toán: " + paymentMethod);
        } catch (Exception e) {
            log.warn("Failed to create initial timeline entry for order {}: {}", savedOrder.getId(), e.getMessage());
        }

        return savedOrder;
    }

    @Override
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findByIdWithReturnRequest(id);
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
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getCurrentStatus();
        order.setStatus(status);
        order.updateCurrentStatus(status);

        Order savedOrder = orderRepository.save(order);

        // Create timeline entry for status change
        try {
            orderTimelineService.createTimelineEntry(orderId, status, "ADMIN",
                    "Trạng thái đơn hàng được cập nhật từ <b>" + oldStatus.getDisplayName() + "</b> => <b>"
                            + status.getDisplayName() + "</b>");

            // Create delivery confirmation request when order is awaiting confirmation
            if (status == OrderStatus.AWAITING_CONFIRMATION
                    && deliveryConfirmationService.getConfirmationStatus(orderId) == null) {
                deliveryConfirmationService.createConfirmationRequest(orderId);
            }
        } catch (Exception e) {
            log.warn("Failed to create timeline entry for order {} status update: {}", orderId, e.getMessage());
        }

        return savedOrder;
    }

    @Override
    @Transactional
    public Order updatePaymentStatus(Long orderId, Order.PaymentStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        Order.PaymentStatus oldStatus = order.getPaymentStatus();
        order.setPaymentStatus(status);

        // If payment is successful and was not paid before, reduce stock (only for
        // non-COD orders)
        if (status == Order.PaymentStatus.PAID && oldStatus != Order.PaymentStatus.PAID) {
            // Keep order status as PENDING after successful payment
            // Admin/Staff will manually change to CONFIRMED

            // Create timeline entry for payment confirmation
            try {
                orderTimelineService.createTimelineEntry(orderId, OrderStatus.PENDING, "SYSTEM",
                        "Thanh toán thành công - Đơn hàng đang chờ xác nhận từ admin/staff");
            } catch (Exception e) {
                log.warn("Failed to create timeline entry for payment confirmation: {}", e.getMessage());
            }

            // Clear cart when payment is successful (for online payment methods)
            if (!"COD".equals(order.getPaymentMethod())) {
                try {
                    cartService.clearCart(order.getUser().getId());
                    log.info("Cart cleared for user {} after successful payment for order {}",
                            order.getUser().getId(), orderId);
                } catch (Exception e) {
                    log.warn("Failed to clear cart for user {} after payment: {}",
                            order.getUser().getId(), e.getMessage());
                }
            }

            // Only reduce stock for non-COD orders (COD stock is already reduced in
            // createOrder)
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

        boolean shouldRestoreStock = false;

        // Only restore stock if order was not already cancelled and stock was
        // previously reduced
        if (order.getStatus() != OrderStatus.CANCELLED) {
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

        order.setStatus(OrderStatus.CANCELLED);
        order.updateCurrentStatus(OrderStatus.CANCELLED);

        Order savedOrder = orderRepository.save(order);

        // Create timeline entry for cancellation
        try {
            orderTimelineService.createTimelineEntry(orderId, OrderStatus.CANCELLED, "SYSTEM",
                    "Đơn hàng bị hủy" + (shouldRestoreStock ? " - Đã hoàn trả kho" : ""));
        } catch (Exception e) {
            log.warn("Failed to create timeline entry for order cancellation: {}", e.getMessage());
        }
    }

    @Override
    public String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }

    // Staff workflow methods
    @Override
    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedDateDesc(status, pageable);
    }

    @Override
    public long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findTop5ByOrderByCreatedDateDesc();
    }

    /**
     * Fix existing COD orders that haven't had their stock reduced
     * This is a one-time fix for orders created before the stock reduction logic
     * was implemented
     */
    @Transactional
    public void fixExistingCODOrders() {
        // Find all COD orders with PENDING status (these likely haven't had stock
        // reduced)
        List<Order> codOrders = orderRepository.findByPaymentMethodAndStatus("COD", OrderStatus.PENDING);

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
                    order.setStatus(OrderStatus.PENDING);
                    orderRepository.save(order);
                    System.out.println("Updated COD order " + order.getOrderNumber() + " status to PROCESSING");
                }
            } catch (Exception e) {
                System.err.println("Error fixing COD order " + order.getOrderNumber() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public List<Order> getOrdersWithDeliveryIssues() {
        return orderRepository.findByHasDeliveryIssueTrue();
    }

    @Override
    @Transactional
    public void updateDeliveryIssueFlag(Long orderId, boolean hasIssue) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setHasDeliveryIssue(hasIssue);
        orderRepository.save(order);

        log.info("Updated delivery issue flag for order {} to {}", orderId, hasIssue);
    }

    @Override
    public boolean hasDeliveryIssue(Long orderId) {
        return orderRepository.findById(orderId)
                .map(Order::hasDeliveryIssue)
                .orElse(false);
    }
}
