package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.service.ChatbotService;
import com.mypkga.commerceplatformfull.service.CartService;
import com.mypkga.commerceplatformfull.service.UserService;
import com.mypkga.commerceplatformfull.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final CartService cartService;
    private final UserService userService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> postMessage(@RequestBody ChatRequest request) {
        String reply = chatbotService.processMessage(request.getMessage());
        ChatResponse response = new ChatResponse(reply);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/action")
    public ResponseEntity<Map<String, Object>> handleAction(@RequestParam String action, @RequestParam Long product) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null &&
                authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        if (!isAuthenticated) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
            response.put("requireLogin", true);
            return ResponseEntity.ok(response);
        }

        if ("add-to-cart".equals(action)) {
            try {
                String username = authentication.getName();
                User user = userService.findByUsername(username).orElse(null);
                if (user == null) {
                    response.put("success", false);
                    response.put("message", "Không tìm thấy thông tin người dùng");
                    return ResponseEntity.ok(response);
                }
                
                cartService.addToCart(user, product, 1); // Thêm 1 sản phẩm
                
                response.put("success", true);
                response.put("message", "Đã thêm sản phẩm vào giỏ hàng thành công!");
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Có lỗi xảy ra khi thêm sản phẩm vào giỏ hàng: " + e.getMessage());
                return ResponseEntity.ok(response);
            }
        }

        response.put("success", false);
        response.put("message", "Action không được hỗ trợ");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth-status")
    public ResponseEntity<Map<String, Boolean>> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null &&
                authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser");

        Map<String, Boolean> response = new HashMap<>();
        response.put("authenticated", isAuthenticated);
        return ResponseEntity.ok(response);
    }
}
