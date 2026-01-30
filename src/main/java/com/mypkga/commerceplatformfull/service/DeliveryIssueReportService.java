package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.DeliveryIssueReport;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.repository.DeliveryIssueReportRepository;
import com.mypkga.commerceplatformfull.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryIssueReportService {
    
    private final DeliveryIssueReportRepository reportRepository;
    private final OrderRepository orderRepository;
    
    /**
     * Create a new delivery issue report
     */
    @Transactional
    public DeliveryIssueReport createReport(Long orderId, String customerEmail, String issueType, String description) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
        
        DeliveryIssueReport report = new DeliveryIssueReport();
        report.setOrder(order);
        report.setCustomerEmail(customerEmail);
        report.setIssueType(issueType);
        report.setDescription(description);
        report.setStatus(DeliveryIssueReport.ReportStatus.PENDING);
        report.setReportedAt(LocalDateTime.now());
        
        DeliveryIssueReport savedReport = reportRepository.save(report);
        log.info("Created delivery issue report {} for order {} by customer {}", 
                savedReport.getId(), orderId, customerEmail);
        
        return savedReport;
    }
    
    /**
     * Get all reports
     */
    public List<DeliveryIssueReport> getAllReports() {
        return reportRepository.findAllByOrderByReportedAtDesc();
    }
    
    /**
     * Get reports by status
     */
    public List<DeliveryIssueReport> getReportsByStatus(DeliveryIssueReport.ReportStatus status) {
        return reportRepository.findByStatusOrderByReportedAtDesc(status);
    }
    
    /**
     * Get pending reports count
     */
    public long getPendingReportsCount() {
        return reportRepository.countByStatus(DeliveryIssueReport.ReportStatus.PENDING);
    }
    
    /**
     * Get recent reports for dashboard
     */
    public List<DeliveryIssueReport> getRecentReports() {
        return reportRepository.findTop10ByOrderByReportedAtDesc();
    }
    
    /**
     * Get report by ID
     */
    public Optional<DeliveryIssueReport> getReportById(Long id) {
        return reportRepository.findById(id);
    }
    
    /**
     * Update report status
     */
    @Transactional
    public DeliveryIssueReport updateReportStatus(Long reportId, DeliveryIssueReport.ReportStatus status, 
                                                  String resolvedBy, String adminNotes) {
        DeliveryIssueReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));
        
        report.setStatus(status);
        report.setResolvedBy(resolvedBy);
        report.setAdminNotes(adminNotes);
        
        if (status == DeliveryIssueReport.ReportStatus.RESOLVED || 
            status == DeliveryIssueReport.ReportStatus.REJECTED) {
            report.setResolvedAt(LocalDateTime.now());
        }
        
        DeliveryIssueReport savedReport = reportRepository.save(report);
        log.info("Updated report {} status to {} by {}", reportId, status, resolvedBy);
        
        return savedReport;
    }
    
    /**
     * Get reports for a specific order
     */
    public List<DeliveryIssueReport> getReportsForOrder(Long orderId) {
        return reportRepository.findByOrderIdOrderByReportedAtDesc(orderId);
    }
}