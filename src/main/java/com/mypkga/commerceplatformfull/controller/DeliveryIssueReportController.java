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
            
            reportService.updateReportStatus(id, reportStatus, resolvedBy, adminNotes);
            
            redirectAttributes.addFlashAttribute("success", 
                "Cập nhật trạng thái báo cáo thành công: " + reportStatus.getDisplayName());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Trạng thái không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật báo cáo: " + e.getMessage());
        }
        
        return "redirect:/admin/delivery-issues/" + id;
    }
}