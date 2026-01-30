package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.ConfirmationStatus;
import com.mypkga.commerceplatformfull.entity.DeliveryConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DeliveryConfirmation entity.
 * Provides methods to query delivery confirmations by various criteria.
 */
@Repository
public interface DeliveryConfirmationRepository extends JpaRepository<DeliveryConfirmation, Long> {

    /**
     * Find delivery confirmation by order ID
     */
    Optional<DeliveryConfirmation> findByOrderId(Long orderId);

    /**
     * Find all delivery confirmations by status
     */
    List<DeliveryConfirmation> findByStatus(ConfirmationStatus status);

    /**
     * Find all delivery confirmations by status ordered by creation date
     */
    List<DeliveryConfirmation> findByStatusOrderByCreatedAtDesc(ConfirmationStatus status);

    /**
     * Find all pending delivery confirmations
     */
    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.status = 'PENDING'")
    List<DeliveryConfirmation> findAllPending();

    /**
     * Find delivery confirmations created within a date range
     */
    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.createdAt BETWEEN :startDate AND :endDate ORDER BY dc.createdAt DESC")
    List<DeliveryConfirmation> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find delivery confirmations by status and date range
     */
    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.status = :status AND dc.createdAt BETWEEN :startDate AND :endDate ORDER BY dc.createdAt DESC")
    List<DeliveryConfirmation> findByStatusAndCreatedAtBetween(@Param("status") ConfirmationStatus status,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Check if delivery confirmation exists for order
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Count confirmations by status
     */
    long countByStatus(ConfirmationStatus status);

    /**
     * Find all rejected confirmations with reasons
     */
    @Query("SELECT dc FROM DeliveryConfirmation dc WHERE dc.status = 'REJECTED' AND dc.rejectionReason IS NOT NULL ORDER BY dc.confirmedAt DESC")
    List<DeliveryConfirmation> findAllRejectedWithReasons();
}