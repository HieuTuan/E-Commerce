package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.DeliveryIssueReport;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.exception.DeliveryIssueException;
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
    private final EmailService emailService;

    /**
     * Create a new delivery issue report
     */
    @Transactional
    public DeliveryIssueReport createReport(Long orderId, String customerEmail, String issueType, String description) {
        // Validate order exists and has DELIVERED status
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new DeliveryIssueException("Order not found with id: " + orderId));

        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new DeliveryIssueException("Can only report issues for delivered orders");
        }

        // Check for duplicate reports
        if (reportRepository.existsByOrderId(orderId)) {
            throw new DeliveryIssueException("Báo cáo đã tồn tại cho đơn hàng này");
        }

        // Create report
        DeliveryIssueReport report = new DeliveryIssueReport();
        report.setOrderId(orderId);
        report.setCustomerEmail(customerEmail);
        report.setIssueType(issueType);
        report.setDescription(description);
        report.setStatus(DeliveryIssueReport.ReportStatus.PENDING);
        report.setReportedAt(LocalDateTime.now());

        DeliveryIssueReport savedReport = reportRepository.save(report);

        // Update order flag
        order.setHasDeliveryIssue(true);
        orderRepository.save(order);

        log.info("Created delivery issue report {} for order {} by customer {}",
                savedReport.getId(), orderId, customerEmail);

        return savedReport;
    }

    /**
     * Get all pending reports for admin
     */
    public List<DeliveryIssueReport> getPendingReports() {
        return reportRepository.findByStatusOrderByReportedAtDesc(DeliveryIssueReport.ReportStatus.PENDING);
    }

    /**
     * Resolve a delivery issue report
     */
    @Transactional
    public DeliveryIssueReport resolveReport(Long reportId, String adminEmail, String notes) {
        DeliveryIssueReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));

        // Update report status
        report.setStatus(DeliveryIssueReport.ReportStatus.RESOLVED);
        report.setResolvedBy(adminEmail);
        report.setAdminNotes(notes);
        report.setResolvedAt(LocalDateTime.now());

        DeliveryIssueReport savedReport = reportRepository.save(report);

        // Update order flag - resolved means no more issue
        Order order = orderRepository.findById(report.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setHasDeliveryIssue(false);
        orderRepository.save(order);

        // Send notification email
        try {
            emailService.sendDeliveryIssueResolvedNotification(
                    report.getCustomerEmail(),
                    report.getOrderId().toString(),
                    notes);
        } catch (Exception e) {
            log.error("Failed to send email notification for resolved report {}", reportId, e);
        }

        log.info("Resolved delivery issue report {} by admin {}", reportId, adminEmail);

        return savedReport;
    }

    /**
     * Reject a delivery issue report
     */
    @Transactional
    public DeliveryIssueReport rejectReport(Long reportId, String adminEmail, String notes) {
        DeliveryIssueReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with id: " + reportId));

        // Update report status
        report.setStatus(DeliveryIssueReport.ReportStatus.REJECTED);
        report.setResolvedBy(adminEmail);
        report.setAdminNotes(notes);
        report.setResolvedAt(LocalDateTime.now());

        DeliveryIssueReport savedReport = reportRepository.save(report);

        // Order flag remains true for rejected reports

        // Send notification email
        try {
            emailService.sendDeliveryIssueRejectedNotification(
                    report.getCustomerEmail(),
                    report.getOrderId().toString(),
                    notes);
        } catch (Exception e) {
            log.error("Failed to send email notification for rejected report {}", reportId, e);
        }

        log.info("Rejected delivery issue report {} by admin {}", reportId, adminEmail);

        return savedReport;
    }

    /**
     * Get reports for a specific order
     */
    public List<DeliveryIssueReport> getReportsByOrderId(Long orderId) {
        return reportRepository.findByOrderIdOrderByReportedAtDesc(orderId);
    }

    /**
     * Get report by ID
     */
    public Optional<DeliveryIssueReport> getReportById(Long id) {
        return reportRepository.findById(id);
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
}