package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> processMessage(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String response = chatbotService.processMessage(userMessage);

        Map<String, String> result = new HashMap<>();
        result.put("response", response);

        return ResponseEntity.ok(result);
    }
}
