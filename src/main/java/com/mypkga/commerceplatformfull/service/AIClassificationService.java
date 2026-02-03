package com.mypkga.commerceplatformfull.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AIClassificationService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.api-url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    private static final Map<String, String[]> CATEGORY_KEYWORDS = new HashMap<>() {
        {
            put("Electronics", new String[] { "laptop", "phone", "computer", "tablet", "camera", "headphone", "speaker",
                    "tv", "monitor", "keyboard", "mouse" });
            put("Fashion", new String[] { "shirt", "dress", "pants", "shoes", "jacket", "coat", "hat", "bag", "watch",
                    "sunglasses", "clothing" });
            put("Home & Garden", new String[] { "furniture", "sofa", "chair", "table", "lamp", "curtain", "plant",
                    "garden", "decor", "kitchen" });
            put("Sports", new String[] { "ball", "gym", "fitness", "running", "yoga", "sports", "exercise", "bike",
                    "tennis", "golf" });
            put("Books", new String[] { "book", "novel", "magazine", "journal", "textbook", "literature", "reading" });
            put("Toys", new String[] { "toy", "game", "puzzle", "doll", "action figure", "board game", "lego" });
            put("Beauty", new String[] { "makeup", "cosmetic", "skincare", "perfume", "fragrance", "lotion", "cream" });
            put("Food & Beverage",
                    new String[] { "food", "drink", "coffee", "tea", "snack", "chocolate", "wine", "beverage" });
        }
    };

    public String classifyProduct(String productName, String description) {
        // Skip AI classification for better performance - use keyword-based only
        log.debug("Using keyword-based classification for product: {}", productName);
        return classifyByKeywords(productName, description);
        
        /* AI Classification disabled for performance
        // Try keyword-based classification first (fallback if API fails)
        String keywordCategory = classifyByKeywords(productName, description);

        // If API key is not configured, return keyword-based result
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENAI_API_KEY")) {
            log.info("OpenAI API key not configured, using keyword-based classification");
            return keywordCategory;
        }

        try {
            return classifyWithAI(productName, description);
        } catch (Exception e) {
            log.error("AI classification failed, falling back to keyword classification", e);
            return keywordCategory;
        }
        */
    }

    private String classifyByKeywords(String productName, String description) {
        String text = (productName + " " + description).toLowerCase();

        int maxMatches = 0;
        String bestCategory = "General";

        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            int matches = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase())) {
                    matches++;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }

    private String classifyWithAI(String productName, String description) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", "Bearer " + apiKey);

            // Build request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);

            JsonArray messages = new JsonArray();

            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content",
                    "You are a product classification assistant. Classify products into one of these categories: Electronics, Fashion, Home & Garden, Sports, Books, Toys, Beauty, Food & Beverage, General. Respond with only the category name.");
            messages.add(systemMessage);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content",
                    "Classify this product:\nName: " + productName + "\nDescription: " + description);
            messages.add(userMessage);

            requestBody.add("messages", messages);
            requestBody.addProperty("max_tokens", 50);
            requestBody.addProperty("temperature", 0.3);

            request.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                log.debug("OpenAI API response: {}", result.toString());

                Gson gson = new Gson();
                JsonObject responseJson = gson.fromJson(result.toString(), JsonObject.class);

                // Check if response has error
                if (responseJson.has("error")) {
                    JsonObject error = responseJson.getAsJsonObject("error");
                    log.error("OpenAI API error: {}", error.get("message").getAsString());
                    throw new RuntimeException("OpenAI API error: " + error.get("message").getAsString());
                }

                // Check if choices array exists and is not empty
                JsonArray choices = responseJson.getAsJsonArray("choices");
                if (choices == null || choices.size() == 0) {
                    log.error("OpenAI API returned no choices. Response: {}", result.toString());
                    throw new RuntimeException("OpenAI API returned no choices");
                }

                String category = choices
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();

                log.info("AI classified product '{}' as: {}", productName, category);
                return category;
            }
        }
    }
}
