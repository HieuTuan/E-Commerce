package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.DeliveryRejectionRequest;
import com.mypkga.commerceplatformfull.entity.DeliveryConfirmation;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.DeliveryConfirmationService;
import com.mypkga.commerceplatformfull.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling delivery confirmation operations.
 * Provides endpoints for customers to confirm or reject deliveries.
 */
@Controller
@RequestMapping("/delivery")
@RequiredArgsConstructor
@Slf4j
public class DeliveryConfirmationController {

    private final DeliveryConfirmationService deliveryConfirmationService;
    private final UserService userService;

    /**
     * Display delivery confirmation page for customers
     */
    @GetMapping("/confirm/{orderId}")
    public String showConfirmationPage(@PathVariable Long orderId, Model model, Authentication auth) {
        try {
            User currentUser = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if user can confirm this delivery
            if (!deliveryConfirmationService.canConfirmDelivery(orderId, currentUser.getId())) {
                model.addAttribute("error", "Bạn không có quyền xác nhận đơn hàng này");
                return "error/403";
            }

            DeliveryConfirmation confirmation = deliveryConfirmationService.getConfirmationStatus(orderId);
            if (confirmation == null) {
                model.addAttribute("error", "Không tìm thấy yêu cầu xác nhận giao hàng");
                return "error/404";
            }

            model.addAttribute("orderId", orderId);
            model.addAttribute("confirmation", confirmation);
            model.addAttribute("order", confirmation.getOrder());
            
            return "delivery/confirm";
        } catch (Exception e) {
            log.error("Error showing confirmation page for order {}: {}", orderId, e.getMessage());
            model.addAttribute("error", "Không thể tải trang xác nhận");
            return "error/500";
        }
    }

    /**
     * Confirm delivery
     */
    @PostMapping("/confirm/{orderId}")
    public String confirmDelivery(@PathVariable Long orderId, Authentication auth) {
        try {
            User currentUser = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            deliveryConfirmationService.confirmDelivery(orderId, currentUser.getId());
            
            return "redirect:/orders/" + orderId + "/timeline?confirmed=true";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid delivery confirmation for order {}: {}", orderId, e.getMessage());
            return "redirect:/delivery/confirm/" + orderId + "?error=invalid";
        } catch (IllegalStateException e) {
            log.warn("Invalid state for delivery confirmation for order {}: {}", orderId, e.getMessage());
            return "redirect:/delivery/confirm/" + orderId + "?error=invalid_state";
        } catch (Exception e) {
            log.error("Error confirming delivery for order {}: {}", orderId, e.getMessage());
            return "redirect:/delivery/confirm/" + orderId + "?error=failed";
        }
    }

    /**
     * Reject delivery with reason
     */
    @PostMapping("/reject/{orderId}")
    public String rejectDelivery(@PathVariable Long orderId, 
                                @Valid @ModelAttribute DeliveryRejectionRequest request,
                                Authentication auth) {
        try {
            User currentUser = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            deliveryConfirmationService.rejectDelivery(orderId, currentUser.getId(), request.getReason());
            
            return "redirect:/orders/" + orderId + "/timeline?rejected=true";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid delivery rejection for order {}: {}", orderId, e.getMessage());
            return "redirect:/delivery/confirm/" + orderId + "?error=invalid";
        } catch (IllegalStateException e) {
            log.warn("Invalid state for delivery rejection for order {}: {}", orderId, e.getMessage());
            return "redirect:/delivery/confirm/" + orderId + "?error=invalid_state";
        } catch (Exception e) {
            log.error("Error rejecting delivery for order {}: {}", orderId, e.getMessage());
            return "redirect:/delivery/confirm/" + orderId + "?error=failed";
        }
    }

    /**
     * Admin view for all pending confirmations
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public String viewPendingConfirmations(Model model) {
        try {
            var pendingConfirmations = deliveryConfirmationService.getAllPendingConfirmations();
            model.addAttribute("confirmations", pendingConfirmations);
            
            return "admin/delivery-confirmations-pending";
        } catch (Exception e) {
            log.error("Error retrieving pending confirmations: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải danh sách xác nhận chờ");
            return "error/500";
        }
    }

    /**
     * Admin view for all rejected confirmations
     */
    @GetMapping("/admin/rejected")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public String viewRejectedConfirmations(Model model) {
        try {
            var rejectedConfirmations = deliveryConfirmationService.getAllRejectedConfirmations();
            model.addAttribute("confirmations", rejectedConfirmations);
            
            return "admin/delivery-confirmations-rejected";
        } catch (Exception e) {
            log.error("Error retrieving rejected confirmations: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải danh sách xác nhận bị từ chối");
            return "error/500";
        }
    }

    /**
     * Admin view for confirmation details
     */
    @GetMapping("/admin/details/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public String viewConfirmationDetails(@PathVariable Long orderId, Model model) {
        try {
            DeliveryConfirmation confirmation = deliveryConfirmationService.getConfirmationStatus(orderId);
            if (confirmation == null) {
                model.addAttribute("error", "Không tìm thấy thông tin xác nhận");
                return "error/404";
            }

            model.addAttribute("confirmation", confirmation);
            model.addAttribute("order", confirmation.getOrder());
            
            return "admin/delivery-confirmation-details";
        } catch (Exception e) {
            log.error("Error retrieving confirmation details for order {}: {}", orderId, e.getMessage());
            model.addAttribute("error", "Không thể tải chi tiết xác nhận");
            return "error/500";
        }
    }

    /**
     * Create missing confirmation requests (Admin utility)
     */
    @PostMapping("/admin/create-missing")
    @PreAuthorize("hasRole('ADMIN')")
    public String createMissingConfirmationRequests() {
        try {
            deliveryConfirmationService.createMissingConfirmationRequests();
            return "redirect:/delivery/admin/pending?created=true";
        } catch (Exception e) {
            log.error("Error creating missing confirmation requests: {}", e.getMessage());
            return "redirect:/delivery/admin/pending?error=creation_failed";
        }
    }
}