package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.ReturnRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestHistoryRepository extends JpaRepository<ReturnRequestHistory, Long> {
    
    /**
     * Find all history records for a specific return request, ordered by creation date
     */
    List<ReturnRequestHistory> findByReturnRequestIdOrderByCreatedAtDesc(Long returnRequestId);
    
    /**
     * Find history records by action type
     */
    List<ReturnRequestHistory> findByActionTypeOrderByCreatedAtDesc(String actionType);
    
    /**
     * Find history records by GHN order code
     */
    List<ReturnRequestHistory> findByGhnOrderCode(String ghnOrderCode);
    
    /**
     * Get latest history record for a return request
     */
    @Query("SELECT h FROM ReturnRequestHistory h WHERE h.returnRequest.id = :returnRequestId ORDER BY h.createdAt DESC LIMIT 1")
    ReturnRequestHistory findLatestByReturnRequestId(@Param("returnRequestId") Long returnRequestId);
}