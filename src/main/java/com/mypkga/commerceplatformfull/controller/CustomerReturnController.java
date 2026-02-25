package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.ReturnRequestHistory;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.entity.WalletTransaction;
import com.mypkga.commerceplatformfull.service.ReturnService;
import com.mypkga.commerceplatformfull.service.UserService;
import com.mypkga.commerceplatformfull.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer Controller for viewing return request details and history
 */
@Controller
@RequestMapping("/customer/returns")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Slf4j
public class CustomerReturnController {

    private final ReturnService returnService;
    private final UserService userService;
    private final WalletService walletService;

    /**
     * View customer's return requests
     */
    @GetMapping
    public String viewMyReturnRequests(Model model, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            List<ReturnRequest> returnRequests = returnService.getReturnRequestsByCustomer(currentUser.getId());

            model.addAttribute("returnRequests", returnRequests);
            return "customer/returns/list";

        } catch (Exception e) {
            log.error("Error loading customer return requests: {}", e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra khi tải danh sách yêu cầu hoàn hàng");
            return "error";
        }
    }

    /**
     * View return request details with history
     */
    @GetMapping("/{requestId}")
    public String viewReturnRequestDetail(@PathVariable Long requestId,
            Model model,
            Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            ReturnRequest returnRequest = returnService.findById(requestId);

            // Verify the return request belongs to the current user
            if (!returnRequest.getCustomer().getId().equals(currentUser.getId())) {
                log.warn("User {} attempted to access return request {} belonging to another user",
                        currentUser.getId(), requestId);
                model.addAttribute("error", "Bạn không có quyền xem yêu cầu hoàn hàng này");
                return "error";
            }

            // Get return request history
            List<ReturnRequestHistory> history = returnService.getReturnRequestHistory(requestId);

            model.addAttribute("returnRequest", returnRequest);
            model.addAttribute("history", history);

            return "customer/returns/detail";

        } catch (Exception e) {
            log.error("Error loading return request {}: {}", requestId, e.getMessage());
            model.addAttribute("error", "Không tìm thấy yêu cầu hoàn hàng");
            return "error";
        }
    }

    /**
     * View customer wallet and transaction history
     */
    @GetMapping("/wallet")
    public String viewWallet(Model model, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            java.math.BigDecimal balance = walletService.getBalance(currentUser.getId());
            java.util.List<WalletTransaction> transactions = walletService.getTransactionHistory(currentUser.getId());
            model.addAttribute("balance", balance);
            model.addAttribute("transactions", transactions);
            return "customer/wallet";
        } catch (Exception e) {
            log.error("Error loading wallet: {}", e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra khi tải thông tin ví");
            return "error";
        }
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}