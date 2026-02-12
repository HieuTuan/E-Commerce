package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.ghn.GHNFeeResponse;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.ReturnRequestHistory;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.ReturnService;
import com.mypkga.commerceplatformfull.service.UserService;
import com.mypkga.commerceplatformfull.service.EmailService;
import com.mypkga.commerceplatformfull.service.GHNReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Staff Controller for Return & Refund Management
 */
@Controller
@RequestMapping("/staff/returns")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class StaffReturnController {

    private final ReturnService returnService;
    private final UserService userService;
    private final EmailService emailService;
    private final GHNReturnService ghnReturnService;

    /**
     * Staff return requests dashboard
     */
    @GetMapping
    public String showReturnsDashboard(Model model) {
        try {
            List<ReturnRequest> pendingRequests = returnService.getPendingReturnRequests();
            List<ReturnRequest> shippingRequests = returnService.getReturningRequests();

            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("shippingRequests", shippingRequests);

            return "staff/returns/dashboard";

        } catch (Exception e) {
            log.error("Error loading returns dashboard: {}", e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "error";
        }
    }

    /**
     * View all return request history
     */
    @GetMapping("/history")
    public String showReturnHistory(Model model, @RequestParam(required = false) String status) {
        try {
            List<ReturnRequest> returnRequests;
            org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort
                    .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");

            if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                try {
                    com.mypkga.commerceplatformfull.entity.ReturnStatus returnStatus = com.mypkga.commerceplatformfull.entity.ReturnStatus
                            .valueOf(status);
                    returnRequests = returnService.getAllReturnRequests(returnStatus, sort);
                } catch (IllegalArgumentException e) {
                    // Invalid status, fallback to all
                    returnRequests = returnService.getAllReturnRequests(null, sort);
                }
            } else {
                returnRequests = returnService.getAllReturnRequests(null, sort);
            }

            model.addAttribute("returnRequests", returnRequests);
            model.addAttribute("statuses", com.mypkga.commerceplatformfull.entity.ReturnStatus.values());
            model.addAttribute("currentStatus", status);

            return "staff/returns/history";
        } catch (Exception e) {
            log.error("Error loading return history: {}", e.getMessage());
            model.addAttribute("error", "Không thể tải lịch sử xử lý");
            return "error";
        }
    }

    /**
     * View return request details with history
     */
    @GetMapping("/{requestId}")
    public String showReturnRequestDetail(@PathVariable Long requestId, Model model) {
        try {
            ReturnRequest returnRequest = returnService.findById(requestId);

            // Get return request history
            List<ReturnRequestHistory> history = returnService.getReturnRequestHistory(requestId);

            // Calculate GHN shipping fee if not already calculated
            GHNFeeResponse ghnFee = null;
            try {
                // Temporarily disable GHN API call to avoid errors
                log.info("GHN fee calculation temporarily disabled for return request {}", requestId);

                if (returnRequest.getGhnFee() == null) {
                    ghnFee = ghnReturnService.calculateReturnShippingFee(returnRequest);
                }

            } catch (Exception e) {
                log.warn("Could not calculate GHN fee for return request {}: {}", requestId, e.getMessage());
            }

            model.addAttribute("returnRequest", returnRequest);
            model.addAttribute("history", history);
            model.addAttribute("ghnFee", ghnFee);

            return "staff/returns/detail";

        } catch (Exception e) {
            log.error("Error loading return request {}: {}", requestId, e.getMessage());
            model.addAttribute("error", "Không tìm thấy yêu cầu hoàn tiền");
            return "error";
        }
    }

    /**
     * Get GHN shipping fee for return request (AJAX endpoint)
     */
    @GetMapping("/{requestId}/ghn-fee")
    @ResponseBody
    public ResponseEntity<?> getGHNShippingFee(@PathVariable Long requestId) {
        try {
            ReturnRequest returnRequest = returnService.findById(requestId);

            GHNFeeResponse feeResponse = ghnReturnService.calculateReturnShippingFee(returnRequest);

            if (feeResponse != null && feeResponse.getCode() == 200) {
                return ResponseEntity.ok(feeResponse.getData());
            } else {
                return ResponseEntity.badRequest().body("Không thể tính phí vận chuyển GHN");
            }

        } catch (Exception e) {
            log.error("Error calculating GHN fee for return request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi khi tính phí vận chuyển: " + e.getMessage());
        }
    }

    /**
     * Approve return request
     */
    @PostMapping("/{requestId}/approve")
    public String approveReturnRequest(@PathVariable Long requestId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentStaff = getCurrentUser(authentication);
            ReturnRequest approved = returnService.approveReturnRequest(requestId, currentStaff.getId());

            // Gửi email thông báo cho khách hàng
            emailService.sendReturnApprovalNotification(approved);

            redirectAttributes.addFlashAttribute("success",
                    "Đã duyệt yêu cầu hoàn tiền #" + approved.getId()
                            + " thành công! Đã gửi email thông báo cho khách hàng.");

        } catch (Exception e) {
            log.error("Error approving return request {}: {}", requestId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/staff/returns";
    }

    /**
     * Reject return request
     */
    @PostMapping("/{requestId}/reject")
    public String rejectReturnRequest(@PathVariable Long requestId,
            @RequestParam String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentStaff = getCurrentUser(authentication);
            ReturnRequest rejected = returnService.rejectReturnRequest(requestId, reason, currentStaff.getId());

            redirectAttributes.addFlashAttribute("success",
                    "Đã từ chối yêu cầu hoàn tiền #" + rejected.getId());

        } catch (Exception e) {
            log.error("Error rejecting return request {}: {}", requestId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/staff/returns";
    }

    /**
     * Confirm shipping
     */
    @PostMapping("/shipping/{returnCode}/confirm")
    public String confirmShipping(@PathVariable String returnCode,
            RedirectAttributes redirectAttributes) {
        try {
            ReturnRequest confirmed = returnService.confirmShipping(returnCode);

            redirectAttributes.addFlashAttribute("success",
                    "Đã xác nhận giao hàng cho mã #" + returnCode);

        } catch (Exception e) {
            log.error("Error confirming shipping for code {}: {}", returnCode, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/staff/returns";
    }

    /**
     * Confirm receipt
     */
    @PostMapping("/{requestId}/receipt/confirm")
    public String confirmReceipt(@PathVariable Long requestId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentStaff = getCurrentUser(authentication);
            ReturnRequest confirmed = returnService.confirmReceipt(requestId, currentStaff.getId());

            redirectAttributes.addFlashAttribute("success",
                    "Đã xác nhận nhận hàng cho yêu cầu #" + confirmed.getId());

        } catch (Exception e) {
            log.error("Error confirming receipt for request {}: {}", requestId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/staff/returns/" + requestId;
    }

    /**
     * Complete refund
     */
    @PostMapping("/{requestId}/refund/complete")
    public String completeRefund(@PathVariable Long requestId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentStaff = getCurrentUser(authentication);
            ReturnRequest completed = returnService.completeRefund(requestId, currentStaff.getId());

            redirectAttributes.addFlashAttribute("success",
                    "Đã hoàn tiền thành công cho yêu cầu #" + completed.getId());

        } catch (Exception e) {
            log.error("Error completing refund for request {}: {}", requestId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/staff/returns";
    }

    /**
     * Upload refund proof image
     */
    @PostMapping("/{requestId}/upload-refund-proof")
    public String uploadRefundProof(@PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file ảnh!");
                return "redirect:/staff/returns/" + requestId;
            }

            returnService.uploadRefundProofAndComplete(requestId, file);

            redirectAttributes.addFlashAttribute("success",
                    "✅ Đã upload chứng từ và hoàn tiền thành công! Đơn hàng đã chuyển sang trạng thái HỦY.");

        } catch (Exception e) {
            log.error("Error uploading refund proof for request {}: {}", requestId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Có lỗi khi upload chứng từ: " + e.getMessage());
        }

        return "redirect:/staff/returns/" + requestId;
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}