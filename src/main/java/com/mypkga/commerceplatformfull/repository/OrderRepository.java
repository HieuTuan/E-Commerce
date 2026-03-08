package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByCreatedDateDesc(Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCurrentStatus(OrderStatus currentStatus);

    List<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus);

    List<Order> findAllByOrderByCreatedDateDesc();

    Page<Order> findAllByOrderByCreatedDateDesc(Pageable pageable);

    // Staff workflow methods
    Page<Order> findByStatusOrderByCreatedDateDesc(OrderStatus status, Pageable pageable);

    Page<Order> findByCurrentStatusOrderByCreatedDateDesc(OrderStatus currentStatus, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByCurrentStatus(OrderStatus currentStatus);

    List<Order> findTop5ByOrderByCreatedDateDesc();

    // Find COD orders with specific status
    List<Order> findByPaymentMethodAndStatus(String paymentMethod, OrderStatus status);

    // Find order by ID with return request and delivery issues eagerly loaded
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.returnRequest LEFT JOIN FETCH o.deliveryIssues WHERE o.id = :id")
    Optional<Order> findByIdWithReturnRequest(@Param("id") Long id);

    // Delivery issue methods
    List<Order> findByHasDeliveryIssueTrue();

    List<Order> findByHasDeliveryIssueTrueOrderByUpdatedDateDesc();

    // Check if a user has purchased a specific product (via order items) and order
    // is delivered/received
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i WHERE o.user.id = :userId AND i.product.id = :productId AND (o.currentStatus = com.mypkga.commerceplatformfull.entity.OrderStatus.DELIVERED OR o.currentStatus = com.mypkga.commerceplatformfull.entity.OrderStatus.CONFIRMED_BY_CUSTOMER)")
    boolean existsDeliveredOrderContainingProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    // Get the first delivered orderId containing product (for review anti-spam)
    @Query("SELECT o.id FROM Order o JOIN o.items i WHERE o.user.id = :userId AND i.product.id = :productId AND (o.currentStatus = com.mypkga.commerceplatformfull.entity.OrderStatus.DELIVERED OR o.currentStatus = com.mypkga.commerceplatformfull.entity.OrderStatus.CONFIRMED_BY_CUSTOMER) ORDER BY o.updatedDate DESC")
    List<Long> findDeliveredOrderIdsContainingProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
