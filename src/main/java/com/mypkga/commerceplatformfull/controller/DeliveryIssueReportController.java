package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.DeliveryIssueReport;
import com.mypkga.commerceplatformfull.service.DeliveryIssueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/delivery-issues")
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@RequiredArgsConstructor
public class DeliveryIssueReportController {

    private final DeliveryIssueReportService reportService;

    /**
     * Display all delivery issue reports
     */
    @GetMapping
    public String listReports(@RequestParam(required = false) String status, Model model) {
        List<DeliveryIssueReport> reports;

        if (status != null && !status.isEmpty()) {
            try {
                DeliveryIssueReport.ReportStatus reportStatus = DeliveryIssueReport.ReportStatus.valueOf(status);
                reports = reportService.getReportsByStatus(reportStatus);
                model.addAttribute("selectedStatus", status);
            } catch (IllegalArgumentException e) {
                reports = reportService.getAllReports();
            }
        } else {
            reports = reportService.getAllReports();
        }

        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", reportService.getPendingReportsCount());
        model.addAttribute("statuses", DeliveryIssueReport.ReportStatus.values());

        return "admin/delivery-issues";
    }

    /**
     * View report details
     */
    @GetMapping("/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        DeliveryIssueReport report = reportService.getReportById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        model.addAttribute("report", report);
        model.addAttribute("statuses", DeliveryIssueReport.ReportStatus.values());

        return "admin/delivery-issue-detail";
    }

    /**
     * View reports for a specific order
     */
    @GetMapping("/order/{orderId}")
    public String viewOrderReports(@PathVariable Long orderId, Model model) {
        List<DeliveryIssueReport> reports = reportService.getReportsByOrderId(orderId);

        if (reports.isEmpty()) {
            throw new RuntimeException("No reports found for order: " + orderId);
        }

        // If only one report, redirect to detail view
        if (reports.size() == 1) {
            return "redirect:/admin/delivery-issues/" + reports.get(0).getId();
        }

        // Multiple reports - show list filtered by order
        model.addAttribute("reports", reports);
        model.addAttribute("orderId", orderId);
        model.addAttribute("pendingCount", reportService.getPendingReportsCount());
        model.addAttribute("statuses", DeliveryIssueReport.ReportStatus.values());

        return "admin/delivery-issues";
    }

    /**
     * Update report status
     */
    @PostMapping("/{id}/update-status")
    public String updateReportStatus(@PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String adminNotes,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            DeliveryIssueReport.ReportStatus reportStatus = DeliveryIssueReport.ReportStatus.valueOf(status);
            String resolvedBy = auth.getName();

            if (reportStatus == DeliveryIssueReport.ReportStatus.RESOLVED) {
                reportService.resolveReport(id, resolvedBy, adminNotes);
            } else if (reportStatus == DeliveryIssueReport.ReportStatus.REJECTED) {
                reportService.rejectReport(id, resolvedBy, adminNotes);
            }

            redirectAttributes.addFlashAttribute("success",
                    "Cập nhật trạng thái báo cáo thành công: " + reportStatus.getDisplayName());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Trạng thái không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật báo cáo: " + e.getMessage());
        }

        return "redirect:/admin/delivery-issues/" + id;
    }

    /**
     * Display delivery issue statistics
     */
    @GetMapping("/reports")
    public String deliveryIssueReports(Model model) {
        // Get statistics
        long pendingCount = reportService.getPendingReportsCount();
        long resolvedCount = reportService.getReportsByStatus(DeliveryIssueReport.ReportStatus.RESOLVED).size();
        long rejectedCount = reportService.getReportsByStatus(DeliveryIssueReport.ReportStatus.REJECTED).size();
        long totalCount = pendingCount + resolvedCount + rejectedCount;

        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("recentReports", reportService.getRecentReports());

        return "admin/delivery-issue-reports";
    }
}