package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.service.ChatbotService;
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

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> postMessage(@RequestBody ChatRequest request) {
        String reply = chatbotService.processMessage(request.getMessage());
        ChatResponse response = new ChatResponse(reply);
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
