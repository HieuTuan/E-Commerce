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
    
    // Staff workflow methods
    Page<Order> findByStatusOrderByCreatedDateDesc(OrderStatus status, Pageable pageable);
    
    Page<Order> findByCurrentStatusOrderByCreatedDateDesc(OrderStatus currentStatus, Pageable pageable);
    
    long countByStatus(OrderStatus status);
    
    long countByCurrentStatus(OrderStatus currentStatus);
    
    List<Order> findTop5ByOrderByCreatedDateDesc();

    // Find COD orders with specific status
    List<Order> findByPaymentMethodAndStatus(String paymentMethod, OrderStatus status);
    
    // Find order by ID with return request eagerly loaded
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.returnRequest WHERE o.id = :id")
    Optional<Order> findByIdWithReturnRequest(@Param("id") Long id);
    
    // Delivery issue methods
    List<Order> findByHasDeliveryIssueTrue();
    
    List<Order> findByHasDeliveryIssueTrueOrderByUpdatedDateDesc();
}
