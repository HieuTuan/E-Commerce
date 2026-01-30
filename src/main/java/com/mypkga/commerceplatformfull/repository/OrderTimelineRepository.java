package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.OrderTimelineEntry;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for OrderTimelineEntry entity.
 * Provides methods to query timeline entries by various criteria including orderId, status, and date ranges.
 * 
 * Requirements: 1.1, 1.3
 */
@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimelineEntry, Long> {

    /**
     * Find all timeline entries for a specific order, ordered by update time descending (newest first).
     * This is the primary method for displaying order timeline to customers and admins.
     * 
     * @param orderId the ID of the order
     * @return list of timeline entries ordered by updatedAt DESC
     */
    List<OrderTimelineEntry> findByOrderIdOrderByUpdatedAtDesc(Long orderId);

    /**
     * Find all timeline entries for a specific order, ordered by update time ascending (oldest first).
     * Useful for displaying chronological progression of order status.
     * 
     * @param orderId the ID of the order
     * @return list of timeline entries ordered by updatedAt ASC
     */
    List<OrderTimelineEntry> findByOrderIdOrderByUpdatedAtAsc(Long orderId);

    /**
     * Find timeline entries by order ID with pagination support.
     * Useful for orders with many status changes.
     * 
     * @param orderId the ID of the order
     * @param pageable pagination information
     * @return page of timeline entries
     */
    Page<OrderTimelineEntry> findByOrderId(Long orderId, Pageable pageable);

    /**
     * Find timeline entries by specific status across all orders.
     * Useful for admin reporting and analytics.
     * 
     * @param status the order status to search for
     * @return list of timeline entries with the specified status
     */
    List<OrderTimelineEntry> findByStatus(OrderStatus status);

    /**
     * Find timeline entries by status with pagination support.
     * 
     * @param status the order status to search for
     * @param pageable pagination information
     * @return page of timeline entries with the specified status
     */
    Page<OrderTimelineEntry> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find timeline entries by status ordered by update time descending.
     * 
     * @param status the order status to search for
     * @return list of timeline entries ordered by updatedAt DESC
     */
    List<OrderTimelineEntry> findByStatusOrderByUpdatedAtDesc(OrderStatus status);

    /**
     * Find timeline entries within a specific date range.
     * Useful for reporting and analytics over time periods.
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of timeline entries within the date range
     */
    List<OrderTimelineEntry> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find timeline entries within a date range with pagination support.
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @param pageable pagination information
     * @return page of timeline entries within the date range
     */
    Page<OrderTimelineEntry> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find timeline entries by status within a specific date range.
     * Combines status filtering with date range for detailed reporting.
     * 
     * @param status the order status to search for
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of timeline entries matching both status and date criteria
     */
    List<OrderTimelineEntry> findByStatusAndUpdatedAtBetween(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find timeline entries by status and date range with pagination support.
     * 
     * @param status the order status to search for
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @param pageable pagination information
     * @return page of timeline entries matching both status and date criteria
     */
    Page<OrderTimelineEntry> findByStatusAndUpdatedAtBetween(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find timeline entries for a specific order within a date range.
     * Useful for tracking order progress over a specific time period.
     * 
     * @param orderId the ID of the order
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of timeline entries for the order within the date range
     */
    List<OrderTimelineEntry> findByOrderIdAndUpdatedAtBetween(Long orderId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find timeline entries by who updated them.
     * Useful for tracking changes made by specific users or systems.
     * 
     * @param updatedBy the username or identifier of who made the update
     * @return list of timeline entries updated by the specified user
     */
    List<OrderTimelineEntry> findByUpdatedBy(String updatedBy);

    /**
     * Find timeline entries by updatedBy within a date range.
     * 
     * @param updatedBy the username or identifier of who made the update
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of timeline entries updated by the specified user within the date range
     */
    List<OrderTimelineEntry> findByUpdatedByAndUpdatedAtBetween(String updatedBy, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count timeline entries by status.
     * Useful for dashboard statistics and reporting.
     * 
     * @param status the order status to count
     * @return number of timeline entries with the specified status
     */
    long countByStatus(OrderStatus status);

    /**
     * Count timeline entries within a date range.
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return number of timeline entries within the date range
     */
    long countByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count timeline entries by status within a date range.
     * 
     * @param status the order status to count
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return number of timeline entries matching both status and date criteria
     */
    long countByStatusAndUpdatedAtBetween(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find the most recent timeline entry for a specific order.
     * Useful for getting the current status of an order.
     * 
     * @param orderId the ID of the order
     * @return the most recent timeline entry for the order, or null if none exists
     */
    @Query("SELECT ote FROM OrderTimelineEntry ote WHERE ote.order.id = :orderId ORDER BY ote.updatedAt DESC LIMIT 1")
    OrderTimelineEntry findLatestByOrderId(@Param("orderId") Long orderId);

    /**
     * Find timeline entries for multiple orders.
     * Useful for bulk operations and reporting across multiple orders.
     * 
     * @param orderIds list of order IDs
     * @return list of timeline entries for the specified orders
     */
    @Query("SELECT ote FROM OrderTimelineEntry ote WHERE ote.order.id IN :orderIds ORDER BY ote.updatedAt DESC")
    List<OrderTimelineEntry> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);

    /**
     * Find timeline entries with notes containing specific text.
     * Useful for searching through timeline notes and comments.
     * 
     * @param searchText the text to search for in notes (case-insensitive)
     * @return list of timeline entries with notes containing the search text
     */
    @Query("SELECT ote FROM OrderTimelineEntry ote WHERE LOWER(ote.notes) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<OrderTimelineEntry> findByNotesContainingIgnoreCase(@Param("searchText") String searchText);

    /**
     * Get timeline statistics for a specific date range.
     * Returns count of entries grouped by status within the date range.
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return list of objects containing status and count
     */
    @Query("SELECT ote.status as status, COUNT(ote) as count FROM OrderTimelineEntry ote " +
           "WHERE ote.updatedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY ote.status ORDER BY COUNT(ote) DESC")
    List<Object[]> getStatusStatisticsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find orders that have been in a specific status for longer than specified hours.
     * Useful for identifying orders that may need attention.
     * 
     * @param status the status to check
     * @param hoursAgo number of hours ago to check from current time
     * @return list of timeline entries for orders stuck in status
     */
    @Query("SELECT ote FROM OrderTimelineEntry ote WHERE ote.status = :status " +
           "AND ote.updatedAt < :cutoffTime " +
           "AND ote.id = (SELECT MAX(ote2.id) FROM OrderTimelineEntry ote2 WHERE ote2.order.id = ote.order.id)")
    List<OrderTimelineEntry> findOrdersStuckInStatus(@Param("status") OrderStatus status, 
                                                     @Param("cutoffTime") LocalDateTime cutoffTime);
}