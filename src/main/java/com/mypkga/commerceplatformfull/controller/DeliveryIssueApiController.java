package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.DeliveryIssueReportRequest;
import com.mypkga.commerceplatformfull.dto.ResolveReportRequest;
import com.mypkga.commerceplatformfull.dto.RejectReportRequest;
import com.mypkga.commerceplatformfull.entity.DeliveryIssueReport;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.DeliveryIssueReportService;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery-issues")
@RequiredArgsConstructor
@Slf4j
public class DeliveryIssueApiController {
    
    private final DeliveryIssueReportService reportService;
    private final OrderService orderService;
    private final UserService userService;
    
    /**
     * Create a new delivery issue report (Customer endpoint)
     */
    @PostMapping("/report")
    public ResponseEntity<?> reportIssue(@Valid @RequestBody DeliveryIssueReportRequest request, 
                                        Authentication auth) {
        try {
            // Get current user
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate order belongs to user
            Order order = orderService.getOrderById(request.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (!order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền báo cáo vấn đề cho đơn hàng này"));
            }
            
            // Create report
            DeliveryIssueReport report = reportService.createReport(
                    request.getOrderId(),
                    user.getEmail(),
                    request.getIssueType(),
                    request.getDescription()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Báo cáo vấn đề giao hàng đã được tạo thành công",
                    "reportId", report.getId()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating delivery issue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Có lỗi xảy ra khi tạo báo cáo"));
        }
    }
    
    /**
     * Get pending reports for admin
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<List<DeliveryIssueReport>> getPendingReports() {
        try {
            List<DeliveryIssueReport> reports = reportService.getPendingReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error getting pending reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Resolve a delivery issue report (Admin endpoint)
     */
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<?> resolveReport(@PathVariable Long id,
                                          @Valid @RequestBody ResolveReportRequest request,
                                          Authentication auth) {
        try {
            DeliveryIssueReport report = reportService.resolveReport(
                    id,
                    auth.getName(),
                    request.getNotes()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Báo cáo đã được giải quyết thành công",
                    "report", report
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error resolving delivery issue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Có lỗi xảy ra khi giải quyết báo cáo"));
        }
    }
    
    /**
     * Reject a delivery issue report (Admin endpoint)
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<?> rejectReport(@PathVariable Long id,
                                         @Valid @RequestBody RejectReportRequest request,
                                         Authentication auth) {
        try {
            DeliveryIssueReport report = reportService.rejectReport(
                    id,
                    auth.getName(),
                    request.getReason()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Báo cáo đã được từ chối",
                    "report", report
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting delivery issue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Có lỗi xảy ra khi từ chối báo cáo"));
        }
    }
    
    /**
     * Get reports for a specific order (Customer endpoint)
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrderReports(@PathVariable Long orderId, Authentication auth) {
        try {
            // Get current user
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate order belongs to user (unless admin/staff)
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
            
            if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền xem báo cáo của đơn hàng này"));
            }
            
            List<DeliveryIssueReport> reports = reportService.getReportsByOrderId(orderId);
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            log.error("Error getting order reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Có lỗi xảy ra khi lấy báo cáo"));
        }
    }
}