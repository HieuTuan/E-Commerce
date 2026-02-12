package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.CreateReturnRequestDto;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.PostOfficeService;
import com.mypkga.commerceplatformfull.service.ReturnEligibilityService;
import com.mypkga.commerceplatformfull.service.ReturnService;
import com.mypkga.commerceplatformfull.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web Controller for Return & Refund pages
 */
@Controller
@RequestMapping("/returns")
@RequiredArgsConstructor
@Slf4j
public class ReturnWebController {
    
    private final ReturnService returnService;
    private final ReturnEligibilityService eligibilityService;
    private final PostOfficeService postOfficeService;
    private final OrderService orderService;
    private final UserService userService;
    
    /**
     * Customer return request form page
     */
    @GetMapping("/orders/{orderId}/request")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String showReturnRequestForm(@PathVariable Long orderId, Model model, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Get order and verify ownership using OrderService
            Order order = orderService.getOrderByIdWithOwnershipCheck(orderId, currentUser.getId());
            
            // Check if order already has a return request
            if (orderService.orderHasReturnRequest(orderId)) {
                log.info("Order {} already has return request, redirecting to my-requests", orderId);
                return "redirect:/returns/my-requests?existing=true";
            }
            
            // Check eligibility
            ReturnEligibilityService.EligibilityResult eligibility = 
                eligibilityService.getEligibilityResult(orderId);
            
            if (!eligibility.isEligible()) {
                model.addAttribute("error", eligibility.getReason());
                model.addAttribute("order", order);
                return "returns/not-eligible";
            }
            
            // Get remaining hours
            Long remainingHours = eligibilityService.getRemainingEligibilityHours(orderId);
            
            model.addAttribute("order", order);
            model.addAttribute("remainingHours", remainingHours);
            model.addAttribute("postOffices", postOfficeService.getActivePostOffices());
            
            return "returns/request-form";
            
        } catch (Exception e) {
            log.error("Error showing return request form for order {}: {}", orderId, e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Customer return requests list
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String showMyReturnRequests(Model model, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            List<ReturnRequest> returnRequests = returnService.getReturnRequestsByCustomer(currentUser.getId());
            
            model.addAttribute("returnRequests", returnRequests);
            return "returns/my-requests";
            
        } catch (Exception e) {
            log.error("Error showing return requests for user {}: {}", authentication.getName(), e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Handle form submission for return request creation
     */
    @PostMapping("/orders/{orderId}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String submitReturnRequest(@PathVariable Long orderId,
                                    @Valid @ModelAttribute CreateReturnRequestDto dto,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Create the return request
            ReturnRequest returnRequest = returnService.createReturnRequest(orderId, dto);
            
            log.info("Successfully created return request {} for order {} by user {}", 
                    returnRequest.getId(), orderId, authentication.getName());
            
            // Add success message with return code
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu hoàn tiền đã được gửi thành công! Mã trả hàng của bạn là: " + returnRequest.getReturnCode());
            redirectAttributes.addFlashAttribute("returnCode", returnRequest.getReturnCode());
            redirectAttributes.addFlashAttribute("returnRequest", returnRequest);
            
            return "redirect:/returns/my-requests";
            
        } catch (Exception e) {
            log.error("Error creating return request for order {}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/returns/orders/" + orderId + "/request";
        }
    }
    
    /**
     * Customer confirms shipping of return item
     */
    @PostMapping("/confirm-shipping/{returnCode}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String confirmShipping(@PathVariable String returnCode,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Confirm shipping
            ReturnRequest returnRequest = returnService.confirmShipping(returnCode);
            
            // Verify ownership
            if (!returnRequest.getOrder().getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied");
            }
            
            log.info("Customer {} confirmed shipping for return code {}", 
                    authentication.getName(), returnCode);
            
            redirectAttributes.addFlashAttribute("success", 
                "Đã xác nhận gửi hàng thành công! Bưu điện sẽ nhận và xử lý hàng trả của bạn.");
            
            return "redirect:/returns/my-requests";
            
        } catch (Exception e) {
            log.error("Error confirming shipping for return code {}: {}", returnCode, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/returns/my-requests";
        }
    }
    
    private User getCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}