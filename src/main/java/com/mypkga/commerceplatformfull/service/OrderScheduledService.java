package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling scheduled order operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderScheduledService {

    private final OrderRepository orderRepository;
    private final OrderTimelineService orderTimelineService;

    /**
     * Auto-confirm delivery for orders that have been awaiting confirmation for more than 24 hours
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void autoConfirmExpiredDeliveries() {
        log.info("Running auto-confirm delivery task...");
        
        try {
            // Find all orders with AWAITING_CONFIRMATION status
            List<Order> awaitingOrders = orderRepository.findByCurrentStatus(OrderStatus.AWAITING_CONFIRMATION);
            
            for (Order order : awaitingOrders) {
                try {
                    orderTimelineService.autoConfirmDeliveryIfExpired(order.getId());
                } catch (Exception e) {
                    log.error("Error auto-confirming delivery for order {}: {}", order.getId(), e.getMessage());
                }
            }
            
            log.info("Auto-confirm delivery task completed. Processed {} orders", awaitingOrders.size());
        } catch (Exception e) {
            log.error("Error in auto-confirm delivery task: {}", e.getMessage());
        }
    }
}