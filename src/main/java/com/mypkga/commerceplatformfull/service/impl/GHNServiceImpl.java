package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.config.GHNConfig;
import com.mypkga.commerceplatformfull.dto.ghn.*;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.ReturnStatus;
import com.mypkga.commerceplatformfull.repository.ReturnRequestRepository;
import com.mypkga.commerceplatformfull.service.GHNService;
import com.mypkga.commerceplatformfull.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNServiceImpl implements GHNService {

    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate;
    private final ReturnRequestRepository returnRequestRepository;
    private final EmailService emailService;

    private static final Map<String, ReturnStatus> GHN_STATUS_MAPPING;

    static {
        GHN_STATUS_MAPPING = new HashMap<>();
        GHN_STATUS_MAPPING.put("ready_to_pick", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("picking", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("picked", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("storing", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("transporting", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("sorting", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("delivering", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("delivered", ReturnStatus.RETURN_RECEIVED);
        GHN_STATUS_MAPPING.put("delivery_fail", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("waiting_to_return", ReturnStatus.RETURNING);
        GHN_STATUS_MAPPING.put("return", ReturnStatus.RETURN_FAILED);
        GHN_STATUS_MAPPING.put("returned", ReturnStatus.RETURN_FAILED);
        GHN_STATUS_MAPPING.put("exception", ReturnStatus.RETURN_FAILED);
        GHN_STATUS_MAPPING.put("damage", ReturnStatus.RETURN_FAILED);
        GHN_STATUS_MAPPING.put("lost", ReturnStatus.RETURN_FAILED);
    }

    @Override
    public GHNOrderResponse createReturnOrder(ReturnRequest returnRequest) {
        log.info("Creating GHN return order for return request: {}", returnRequest.getId());

        try {
            // Build GHN order request
            GHNCreateOrderRequest request = buildGHNOrderRequest(returnRequest);

            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnConfig.getToken());
            headers.set("ShopId", ghnConfig.getShopId().toString());

            HttpEntity<GHNCreateOrderRequest> entity = new HttpEntity<>(request, headers);

            // Call GHN API
            String url = ghnConfig.getApiUrl() + "/v2/shipping-order/create";
            ResponseEntity<GHNOrderResponse> response = restTemplate.postForEntity(url, entity, GHNOrderResponse.class);

            GHNOrderResponse ghnResponse = response.getBody();

            if (ghnResponse != null && ghnResponse.getCode() == 200) {
                log.info("Successfully created GHN order: {} for return: {}",
                        ghnResponse.getData().getOrderCode(), returnRequest.getId());
                return ghnResponse;
            } else {
                log.error("Failed to create GHN order. Response: {}", ghnResponse);
                throw new RuntimeException("GHN API returned error: " +
                        (ghnResponse != null ? ghnResponse.getMessage() : "Unknown error"));
            }

        } catch (RestClientException e) {
            log.error("Error calling GHN API for return request: {}", returnRequest.getId(), e);
            throw new RuntimeException("Failed to create GHN order", e);
        }
    }

    @Override
    public GHNOrderInfo getOrderInfo(String orderCode) {
        log.info("Getting GHN order info for order code: {}", orderCode);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("token", ghnConfig.getToken());
            headers.set("ShopId", ghnConfig.getShopId().toString());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = ghnConfig.getApiUrl() + "/v2/shipping-order/detail?order_code=" + orderCode;
            ResponseEntity<GHNOrderInfo> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    GHNOrderInfo.class);

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error getting GHN order info for order code: {}", orderCode, e);
            throw new RuntimeException("Failed to get GHN order info", e);
        }
    }

    @Override
    public boolean cancelOrder(String orderCode) {
        log.info("Cancelling GHN order: {}", orderCode);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnConfig.getToken());
            headers.set("ShopId", ghnConfig.getShopId().toString());

            Map<String, String> requestBody = Map.of("order_codes", "[\"" + orderCode + "\"]");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            String url = ghnConfig.getApiUrl() + "/v2/shipping-order/cancel";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            log.info("GHN cancel response: {}", response.getBody());
            return response.getStatusCode() == HttpStatus.OK;

        } catch (RestClientException e) {
            log.error("Error cancelling GHN order: {}", orderCode, e);
            return false;
        }
    }

    @Override
    public Integer calculateShippingFee(ReturnRequest returnRequest) {
        // Implementation for fee calculation
        // This would call GHN's fee calculation API
        log.info("Calculating shipping fee for return request: {}", returnRequest.getId());

        // For now, return a default fee - implement actual API call later
        return 30000; // 30,000 VND default
    }

    @Override
    @Transactional
    public void processStatusUpdate(GHNWebhookPayload payload) {
        log.info("Processing GHN status update for order: {} with status: {}",
                payload.getOrderCode(), payload.getStatus());

        try {
            // Find return request by GHN order code
            ReturnRequest returnRequest = returnRequestRepository.findByGhnOrderCode(payload.getOrderCode())
                    .orElse(null);

            if (returnRequest == null) {
                log.warn("No return request found for GHN order code: {}", payload.getOrderCode());
                return;
            }

            // Update GHN status
            returnRequest.setGhnStatus(payload.getStatus());

            // Map GHN status to internal status
            ReturnStatus newStatus = GHN_STATUS_MAPPING.get(payload.getStatus().toLowerCase());
            if (newStatus != null && newStatus != returnRequest.getStatus()) {
                ReturnStatus oldStatus = returnRequest.getStatus();
                returnRequest.setStatus(newStatus);

                // Update timestamps based on status
                if (newStatus == ReturnStatus.RETURNING && payload.getStatus().equals("picked")) {
                    returnRequest.setPickupTime(LocalDateTime.now());
                } else if (newStatus == ReturnStatus.RETURN_RECEIVED) {
                    returnRequest.setDeliveryTime(LocalDateTime.now());
                }

                returnRequestRepository.save(returnRequest);

                log.info("Updated return request {} status from {} to {} based on GHN status: {}",
                        returnRequest.getId(), oldStatus, newStatus, payload.getStatus());

                // Send notification email to customer
                sendStatusUpdateEmail(returnRequest, payload);
            }

        } catch (Exception e) {
            log.error("Error processing GHN status update for order: {}", payload.getOrderCode(), e);
        }
    }

    @Override
    public String mapGHNStatusToReturnStatus(String ghnStatus) {
        ReturnStatus status = GHN_STATUS_MAPPING.get(ghnStatus.toLowerCase());
        return status != null ? status.name() : ReturnStatus.RETURNING.name();
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            // Simple health check - could ping GHN API
            return ghnConfig.getToken() != null && ghnConfig.getShopId() != null;
        } catch (Exception e) {
            log.error("GHN service availability check failed", e);
            return false;
        }
    }

    private GHNCreateOrderRequest buildGHNOrderRequest(ReturnRequest returnRequest) {
        // Build items list
        List<GHNItem> items = List.of(
                GHNItem.builder()
                        .name("Hàng hoàn trả - " + returnRequest.getReturnCode())
                        .code(returnRequest.getReturnCode())
                        .quantity(1)
                        .price(0) // Return items have no value for shipping
                        .weight(500) // Default 500g
                        .length(20)
                        .width(15)
                        .height(10)
                        .category("Hàng hoàn trả")
                        .build());

        return GHNCreateOrderRequest.builder()
                .paymentTypeId(ghnConfig.getDefaultPaymentTypeId())
                .note("Đơn hoàn trả - " + returnRequest.getReason().getDisplayName())
                .requiredNote(ghnConfig.getDefaultRequiredNote())
                .returnPhone(ghnConfig.getWarehousePhone())
                .returnAddress(ghnConfig.getWarehouseAddress())
                .returnDistrictId(ghnConfig.getWarehouseDistrictId())
                .returnWardCode(ghnConfig.getWarehouseWardCode())
                .clientOrderCode(returnRequest.getReturnCode())
                .toDistrictId(1) // Default district - should be configured
                .toWardCode("21211") // Default ward - should be configured
                .toName(returnRequest.getOrder().getUser().getFullName())
                .toPhone(returnRequest.getOrder().getUser().getPhone())
                .toAddress(returnRequest.getOrder().getShippingAddress())
                .codAmount(0)
                .content("Hàng hoàn trả")
                .weight(500)
                .length(20)
                .width(15)
                .height(10)
                .serviceTypeId(ghnConfig.getDefaultServiceTypeId())
                .items(items)
                .build();
    }

    private void sendStatusUpdateEmail(ReturnRequest returnRequest, GHNWebhookPayload payload) {
        try {
            String subject = "Cập nhật trạng thái đơn hoàn trả #" + returnRequest.getReturnCode();
            String statusText = getVietnameseStatusText(payload.getStatus());

            String content = String.format(
                    "Xin chào %s,\n\n" +
                            "Đơn hoàn trả #%s của bạn đã được cập nhật trạng thái:\n\n" +
                            "Trạng thái: %s\n" +
                            "Thời gian: %s\n" +
                            "Mã vận đơn: %s\n\n" +
                            "Bạn có thể theo dõi chi tiết tại: %s\n\n" +
                            "Trân trọng,\nĐội ngũ hỗ trợ khách hàng",
                    returnRequest.getOrder().getUser().getFullName(),
                    returnRequest.getReturnCode(),
                    statusText,
                    payload.getTime(),
                    payload.getOrderCode(),
                    "https://yoursite.com/returns/" + returnRequest.getId());

            emailService.sendEmail(
                    returnRequest.getOrder().getUser().getEmail(),
                    subject,
                    content);

        } catch (Exception e) {
            log.error("Failed to send status update email for return: {}", returnRequest.getId(), e);
        }
    }

    private String getVietnameseStatusText(String ghnStatus) {
        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick" -> "Chờ lấy hàng";
            case "picking" -> "Đang lấy hàng";
            case "picked" -> "Đã lấy hàng";
            case "storing" -> "Hàng đang ở kho";
            case "transporting" -> "Đang vận chuyển";
            case "sorting" -> "Đang phân loại";
            case "delivering" -> "Đang giao hàng";
            case "delivered" -> "Đã giao hàng";
            case "delivery_fail" -> "Giao hàng thất bại";
            case "waiting_to_return" -> "Chờ hoàn hàng";
            case "return" -> "Đang hoàn hàng";
            case "returned" -> "Đã hoàn hàng";
            case "exception" -> "Có sự cố";
            case "damage" -> "Hàng bị hỏng";
            case "lost" -> "Hàng bị thất lạc";
            default -> ghnStatus;
        };
    }
}