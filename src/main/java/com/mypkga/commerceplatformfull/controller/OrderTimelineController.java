package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.StatusUpdateRequest;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.entity.OrderTimelineEntry;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.OrderTimelineService;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for handling order timeline operations.
 * Provides endpoints for viewing timeline and updating order status.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderTimelineController {

    private final OrderTimelineService orderTimelineService;
    private final OrderService orderService;
    private final UserService userService;

    /**
     * Display order timeline page for customers
     */
    @GetMapping("/{orderId}/timeline")
    public String getOrderTimeline(@PathVariable Long orderId, Model model, Authentication auth) {
        try {
            // Security check for customers
            if (auth != null && !hasAdminOrStaffRole(auth)) {
                User user = getCurrentUser(auth);
                Order order = orderService.getOrderById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"));
                
                // Ensure user can only view their own orders
                if (!order.getUser().getId().equals(user.getId())) {
                    throw new RuntimeException("Access denied");
                }
                
                model.addAttribute("order", order);
            }
            
            List<OrderTimelineEntry> timeline = orderTimelineService.getOrderTimeline(orderId);
            model.addAttribute("timeline", timeline);
            model.addAttribute("orderId", orderId);
            
            return "orders/timeline";
        } catch (Exception e) {
            log.error("Error retrieving timeline for order {}: {}", orderId, e.getMessage());
            model.addAttribute("error", "Không thể tải timeline đơn hàng");
            return "error/500";
        }
    }

    /**
     * API endpoint for real-time timeline updates
     */
    @GetMapping("/{orderId}/timeline/api")
    @ResponseBody
    public ResponseEntity<List<OrderTimelineEntry>> getTimelineApi(@PathVariable Long orderId) {
        try {
            List<OrderTimelineEntry> timeline = orderTimelineService.getOrderTimeline(orderId);
            return ResponseEntity.ok(timeline);
        } catch (Exception e) {
            log.error("Error retrieving timeline API for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update order status (Admin/Staff only)
     */
    @PostMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public String updateOrderStatus(@PathVariable Long orderId, 
                                   @RequestParam OrderStatus status,
                                   @RequestParam(required = false) String notes,
                                   Authentication auth) {
        try {
            String updatedBy = auth.getName();
            orderTimelineService.updateOrderStatus(orderId, status, updatedBy, notes);
            
            // Sync status update
            orderTimelineService.syncStatusUpdate(orderId, status);
            
            return "redirect:/orders/" + orderId + "/timeline?success=true";
        } catch (IllegalStateException e) {
            log.warn("Invalid status update for order {}: {}", orderId, e.getMessage());
            return "redirect:/orders/" + orderId + "/timeline?error=invalid_status";
        } catch (Exception e) {
            log.error("Error updating status for order {}: {}", orderId, e.getMessage());
            return "redirect:/orders/" + orderId + "/timeline?error=update_failed";
        }
    }

    /**
     * API endpoint for updating order status
     */
    @PostMapping("/{orderId}/status/api")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatusApi(@PathVariable Long orderId,
                                                 @Valid @RequestBody StatusUpdateRequest request,
                                                 Authentication auth) {
        try {
            String updatedBy = auth.getName();
            orderTimelineService.updateOrderStatus(orderId, request.getStatus(), updatedBy, request.getNotes());
            
            // Sync status update
            orderTimelineService.syncStatusUpdate(orderId, request.getStatus());
            
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Invalid status transition: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error updating status for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to update status");
        }
    }

    /**
     * Get timeline entries by status (Admin/Staff only)
     */
    @GetMapping("/timeline/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public String getTimelineByStatus(@PathVariable OrderStatus status, Model model) {
        try {
            List<OrderTimelineEntry> timeline = orderTimelineService.getTimelineByStatus(status);
            model.addAttribute("timeline", timeline);
            model.addAttribute("status", status);
            
            return "admin/timeline-by-status";
        } catch (Exception e) {
            log.error("Error retrieving timeline by status {}: {}", status, e.getMessage());
            model.addAttribute("error", "Không thể tải timeline theo trạng thái");
            return "error/500";
        }
    }

    /**
     * Check if status can be updated
     */
    @GetMapping("/{orderId}/status/can-update")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @ResponseBody
    public ResponseEntity<Boolean> canUpdateStatus(@PathVariable Long orderId,
                                                  @RequestParam OrderStatus currentStatus,
                                                  @RequestParam OrderStatus newStatus) {
        boolean canUpdate = orderTimelineService.canUpdateStatus(currentStatus, newStatus);
        return ResponseEntity.ok(canUpdate);
    }

    /**
     * Customer confirms delivery receipt
     */
    @PostMapping("/{orderId}/confirm-delivery")
    public String confirmDelivery(@PathVariable Long orderId, Authentication auth) {
        try {
            String customerEmail = auth.getName();
            orderTimelineService.confirmDeliveryByCustomer(orderId, customerEmail);
            
            return "redirect:/orders/" + orderId + "?confirmed=true";
        } catch (IllegalStateException e) {
            log.warn("Invalid delivery confirmation for order {}: {}", orderId, e.getMessage());
            return "redirect:/orders/" + orderId + "?error=invalid_confirmation";
        } catch (Exception e) {
            log.error("Error confirming delivery for order {}: {}", orderId, e.getMessage());
            return "redirect:/orders/" + orderId + "?error=confirmation_failed";
        }
    }

    /**
     * Customer rejects delivery
     */
    @PostMapping("/{orderId}/reject-delivery")
    public String rejectDelivery(@PathVariable Long orderId, 
                                @RequestParam String reason,
                                @RequestParam(required = false) String customReason,
                                Authentication auth) {
        try {
            String customerEmail = auth.getName();
            
            // Combine reason and custom reason
            String fullReason = reason;
            if (customReason != null && !customReason.trim().isEmpty()) {
                fullReason += " - " + customReason.trim();
            }
            
            orderTimelineService.rejectDeliveryByCustomer(orderId, customerEmail, fullReason);
            
            return "redirect:/orders/" + orderId + "?rejected=true";
        } catch (IllegalStateException e) {
            log.warn("Invalid delivery rejection for order {}: {}", orderId, e.getMessage());
            return "redirect:/orders/" + orderId + "?error=invalid_rejection";
        } catch (Exception e) {
            log.error("Error rejecting delivery for order {}: {}", orderId, e.getMessage());
            return "redirect:/orders/" + orderId + "?error=rejection_failed";
        }
    }

    /**
     * Request DTO for status updates
     */
    public static class StatusUpdateRequest {
        private OrderStatus status;
        private String notes;

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    private User getCurrentUser(Authentication authentication) {
        // authentication.getName() returns email (from CustomUserDetailsService)
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean hasAdminOrStaffRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> 
                    authority.getAuthority().equals("ROLE_ADMIN") || 
                    authority.getAuthority().equals("ROLE_STAFF"));
    }
}