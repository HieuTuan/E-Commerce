package com.mypkga.commerceplatformfull.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mypkga.commerceplatformfull.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class MomoService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.api-url}")
    private String apiUrl;

    @Value("${momo.return-url}")
    private String returnUrl;

    @Value("${momo.notify-url}")
    private String notifyUrl;

    private final Gson gson = new Gson();

    /**
     * Create Momo payment request
     */
    public String createPaymentUrl(Order order) {
        try {
            String requestId = String.valueOf(System.currentTimeMillis());
            String orderId = order.getOrderNumber();
            String orderInfo = "Payment for order " + orderId;
            String amount = String.valueOf(order.getTotalAmount().longValue());
            String requestType = "captureWallet";
            String extraData = ""; // Optional

            // Create raw signature
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    accessKey, amount, extraData, notifyUrl, orderId, orderInfo, partnerCode, returnUrl, requestId,
                    requestType);

            // Generate signature using HMAC SHA256
            String signature = generateHmacSHA256(rawSignature, secretKey);

            // Create request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("partnerCode", partnerCode);
            requestBody.addProperty("partnerName", "E-Commerce Platform");
            requestBody.addProperty("storeId", "ECommerceStore");
            requestBody.addProperty("requestId", requestId);
            requestBody.addProperty("amount", amount);
            requestBody.addProperty("orderId", orderId);
            requestBody.addProperty("orderInfo", orderInfo);
            requestBody.addProperty("redirectUrl", returnUrl);
            requestBody.addProperty("ipnUrl", notifyUrl);
            requestBody.addProperty("lang", "vi");
            requestBody.addProperty("extraData", extraData);
            requestBody.addProperty("requestType", requestType);
            requestBody.addProperty("signature", signature);

            // Send request to Momo
            String response = sendPostRequest(apiUrl, requestBody.toString());

            // Parse response
            JsonObject responseJson = gson.fromJson(response, JsonObject.class);

            if (responseJson.has("payUrl")) {
                return responseJson.get("payUrl").getAsString();
            } else {
                log.error("Momo payment URL creation failed: {}", response);
                throw new RuntimeException("Failed to create Momo payment URL");
            }

        } catch (Exception e) {
            log.error("Error creating Momo payment URL", e);
            throw new RuntimeException("Error creating Momo payment URL: " + e.getMessage());
        }
    }

    /**
     * Verify Momo payment callback
     */
    public boolean verifyPaymentCallback(String partnerCode, String orderId, String requestId,
            String amount, String orderInfo, String orderType,
            String transId, String resultCode, String message,
            String payType, String responseTime, String extraData,
            String signature) {
        try {
            // Create raw signature for verification
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                    accessKey, amount, extraData, message, orderId, orderInfo, orderType, partnerCode, payType,
                    requestId, responseTime, resultCode, transId);

            // Generate signature
            String expectedSignature = generateHmacSHA256(rawSignature, secretKey);

            // Verify signature
            boolean isValid = expectedSignature.equals(signature);

            if (!isValid) {
                log.error("Momo signature verification failed. Expected: {}, Got: {}", expectedSignature, signature);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying Momo payment", e);
            return false;
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String generateHmacSHA256(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Send POST request
     */
    private String sendPostRequest(String url, String jsonBody) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
}
