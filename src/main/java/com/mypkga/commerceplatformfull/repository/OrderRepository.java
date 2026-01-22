package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByCreatedDateDesc(Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus);

    List<Order> findAllByOrderByCreatedDateDesc();
    
    // Staff workflow methods
    Page<Order> findByStatusOrderByCreatedDateDesc(Order.OrderStatus status, Pageable pageable);
    
    long countByStatus(Order.OrderStatus status);
    
    List<Order> findTop5ByOrderByCreatedDateDesc();
}
