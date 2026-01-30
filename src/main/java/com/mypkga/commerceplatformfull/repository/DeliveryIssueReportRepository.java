package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.DeliveryIssueReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryIssueReportRepository extends JpaRepository<DeliveryIssueReport, Long> {
    
    /**
     * Find all reports ordered by reported date (newest first)
     */
    List<DeliveryIssueReport> findAllByOrderByReportedAtDesc();
    
    /**
     * Find reports by status
     */
    List<DeliveryIssueReport> findByStatusOrderByReportedAtDesc(DeliveryIssueReport.ReportStatus status);
    
    /**
     * Find reports for a specific order
     */
    List<DeliveryIssueReport> findByOrderIdOrderByReportedAtDesc(Long orderId);
    
    /**
     * Count pending reports
     */
    long countByStatus(DeliveryIssueReport.ReportStatus status);
    
    /**
     * Find recent reports (for dashboard)
     */
    @Query("SELECT r FROM DeliveryIssueReport r ORDER BY r.reportedAt DESC")
    List<DeliveryIssueReport> findTop10ByOrderByReportedAtDesc();
}