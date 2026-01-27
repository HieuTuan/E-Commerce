package com.mypkga.commerceplatformfull.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ProductRepository productRepository;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.api-url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public String processMessage(String userMessage) {
        // Check if API is configured


        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENAI_API_KEY")) {
            return processMessageWithRules(userMessage);
        }

        try {
            return processMessageWithAI(userMessage);
        } catch (Exception e) {
            log.error("AI chatbot failed, using rule-based response", e);
            return processMessageWithRules(userMessage);
        }
    }

    private String processMessageWithRules(String userMessage) {
        String msg = userMessage.toLowerCase();

        // Product search
        if (msg.contains("show") || msg.contains("find") || msg.contains("search") || msg.contains("looking for")) {
            List<Product> products = searchProductsFromMessage(msg);
            if (!products.isEmpty()) {
                StringBuilder response = new StringBuilder("I found these products for you:\n\n");
                for (Product product : products) {
                    response.append("• ").append(product.getName())
                            .append(" - $").append(product.getPrice())
                            .append("\n");
                }
                return response.toString();
            }
            return "I couldn't find any products matching your request. Could you try different keywords?";
        }

        // FAQ responses
        if (msg.contains("return") || msg.contains("refund")) {
            return "Our return policy allows returns within 30 days of purchase. Items must be unused and in original packaging. Please contact our support team for assistance.";
        }

        if (msg.contains("shipping") || msg.contains("delivery")) {
            return "We offer free shipping on orders over $50. Standard delivery takes 3-5 business days. Express shipping is available for an additional fee.";
        }

        if (msg.contains("payment") || msg.contains("pay")) {
            return "We accept VNPay and Cash on Delivery. All payment methods are secure and encrypted.";
        }

        if (msg.contains("recommend") || msg.contains("suggestion")) {
            List<Product> featured = productRepository.findByFeaturedTrue();
            if (!featured.isEmpty()) {
                StringBuilder response = new StringBuilder("Here are our recommended products:\n\n");
                for (int i = 0; i < Math.min(5, featured.size()); i++) {
                    Product product = featured.get(i);
                    response.append("• ").append(product.getName())
                            .append(" - $").append(product.getPrice())
                            .append("\n");
                }
                return response.toString();
            }
        }

        return "Hello! I can help you find products, answer questions about shipping, returns, and payments. What would you like to know?";
    }

    private List<Product> searchProductsFromMessage(String message) {
        String[] words = message.split("\\s+");
        List<Product> results = new ArrayList<>();

        for (String word : words) {
            if (word.length() > 3) {
                List<Product> found = productRepository.searchProducts(word);
                results.addAll(found);
            }
        }

        return results.stream().distinct().limit(5).toList();
    }

    private String processMessageWithAI(String userMessage) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", "Bearer " + apiKey);

            // Get product context
            List<Product> products = productRepository.findTop10ByOrderByCreatedDateDesc();
            StringBuilder productContext = new StringBuilder("Available products:\n");
            for (Product p : products) {
                productContext.append("- ").append(p.getName())
                        .append(" ($").append(p.getPrice()).append(")\n");
            }

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);

            JsonArray messages = new JsonArray();

            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content",
                    "You are a helpful e-commerce shopping assistant. Help users find products, answer questions about shipping, returns, and payments. Be concise and friendly.\n\n"
                            + productContext.toString());
            messages.add(systemMessage);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);

            requestBody.add("messages", messages);
            requestBody.addProperty("max_tokens", 150);
            requestBody.addProperty("temperature", 0.7);

            request.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                Gson gson = new Gson();
                JsonObject responseJson = gson.fromJson(result.toString(), JsonObject.class);

                return responseJson
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();
            }
        }
    }

    public List<Product> getRecommendations() {
        return productRepository.findByFeaturedTrue();
    }
}
