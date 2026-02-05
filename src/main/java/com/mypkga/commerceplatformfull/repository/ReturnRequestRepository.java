package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReturnRequest entity operations.
 * Provides data access methods for return request management.
 */
@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    /**
     * Find return request by return code
     */
    Optional<ReturnRequest> findByReturnCode(String returnCode);

    /**
     * Find return request by order ID
     */
    Optional<ReturnRequest> findByOrderId(Long orderId);

    /**
     * Find all return requests with specific status
     */
    List<ReturnRequest> findByStatus(ReturnStatus status);

    /**
     * Find all return requests with specific status and sort
     */
    List<ReturnRequest> findByStatus(ReturnStatus status, org.springframework.data.domain.Sort sort);

    /**
     * Find all pending return requests (REFUND_REQUESTED status)
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.status = 'REFUND_REQUESTED' ORDER BY rr.createdAt ASC")
    List<ReturnRequest> findPendingReturnRequests();

    /**
     * Find all return requests in RETURNING status for shipping tracking
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.status = 'RETURNING' ORDER BY rr.updatedAt DESC")
    List<ReturnRequest> findReturningRequests();

    /**
     * Find all approved return requests waiting for customer to send items
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.status = 'RETURN_APPROVED' ORDER BY rr.updatedAt DESC")
    List<ReturnRequest> findApprovedRequests();

    /**
     * Find return requests by customer (user)
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.order.user.id = :userId ORDER BY rr.createdAt DESC")
    List<ReturnRequest> findByCustomerId(@Param("userId") Long userId);

    /**
     * Find return requests created within a date range
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.createdAt BETWEEN :startDate AND :endDate ORDER BY rr.createdAt DESC")
    List<ReturnRequest> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Check if an order already has a return request
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Count return requests by status
     */
    long countByStatus(ReturnStatus status);

    /**
     * Find return requests processed by specific staff member
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.processedByStaff.id = :staffId ORDER BY rr.processedAt DESC")
    List<ReturnRequest> findByProcessedByStaffId(@Param("staffId") Long staffId);

    /**
     * Find return request by GHN order code
     */
    Optional<ReturnRequest> findByGhnOrderCode(String ghnOrderCode);

    /**
     * Find return requests with GHN tracking numbers
     */
    @Query("SELECT rr FROM ReturnRequest rr WHERE rr.ghnOrderCode IS NOT NULL ORDER BY rr.updatedAt DESC")
    List<ReturnRequest> findWithGHNTracking();

    /**
     * Find return requests by GHN status
     */
    List<ReturnRequest> findByGhnStatus(String ghnStatus);
}