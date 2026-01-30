package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.service.OrderService;
import com.mypkga.commerceplatformfull.service.VNPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final VNPayService vnPayService;
    private final OrderService orderService;

    @GetMapping("/vnpay/callback")
    public String vnpayCallback(@RequestParam Map<String, String> params, Model model) {
        try {
            boolean isValid = vnPayService.verifyPaymentResponse(params);

            if (isValid) {
                String orderNumber = params.get("vnp_TxnRef");
                String responseCode = params.get("vnp_ResponseCode");

                Order order = orderService.getOrderByOrderNumber(orderNumber)
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                if ("00".equals(responseCode)) {
                    // Payment successful
                    orderService.updatePaymentStatus(order.getId(), Order.PaymentStatus.PAID);
                    log.info("VNPay payment successful for order: {}", orderNumber);
                    // Redirect to timeline page instead of success page
                    return "redirect:/orders/" + order.getId() + "/timeline?success=true&payment=vnpay";
                } else {
                    // Payment failed
                    orderService.updatePaymentStatus(order.getId(), Order.PaymentStatus.FAILED);
                    log.warn("VNPay payment failed for order: {} with response code: {}", orderNumber, responseCode);
                    model.addAttribute("error", "Payment failed");
                    return "checkout/payment-failed";
                }
            } else {
                log.error("Invalid VNPay payment response");
                model.addAttribute("error", "Invalid payment response");
                return "checkout/payment-failed";
            }
        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            model.addAttribute("error", "Error processing payment");
            return "checkout/payment-failed";
        }
    }
}
