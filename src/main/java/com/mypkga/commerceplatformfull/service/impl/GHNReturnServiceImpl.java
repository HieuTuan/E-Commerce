package com.mypkga.commerceplatformfull.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypkga.commerceplatformfull.dto.ghn.*;
import com.mypkga.commerceplatformfull.entity.Order;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.GHNReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNReturnServiceImpl implements GHNReturnService {

    private final RestTemplate restTemplate;
    private final com.mypkga.commerceplatformfull.service.GHNMasterDataService ghnMasterDataService;

    @Value("${ghn.api-url}")
    private String ghnApiUrl;

    @Value("${ghn.token}")
    private String ghnToken;

    @Value("${ghn.shop-id}")
    private String ghnShopId;

    @Value("${ghn.warehouse-district-id}")
    private Integer warehouseDistrictId;

    @Value("${ghn.warehouse-ward-code}")
    private String warehouseWardCode;

    @Value("${ghn.warehouse-address}")
    private String warehouseAddress;

    @Value("${ghn.warehouse-name}")
    private String warehouseName;

    @Value("${ghn.warehouse-phone}")
    private String warehousePhone;

    @Value("${ghn.default-service-type-id}")
    private Integer defaultServiceTypeId;

    @Override
    public GHNFeeResponse calculateReturnShippingFee(ReturnRequest returnRequest) {
        try {
            Order order = returnRequest.getOrder();
            User customer = order.getUser();

            // Debug: Log customer location data
            log.info("Customer location - DistrictId: {}, WardCode: {}",
                    customer.getDistrictId(), customer.getWardCode());

            // Use customer location with fallback to default if null
            Integer fromDistrictId = customer.getDistrictId() != null ? customer.getDistrictId() : 1454;
            String fromWardCode = customer.getWardCode() != null && !customer.getWardCode().isEmpty()
                    ? customer.getWardCode()
                    : "21208";

            log.info("Using fromDistrictId: {}, fromWardCode: {} for fee calculation",
                    fromDistrictId, fromWardCode);

            // Auto-fetch service_id from GHN available services API
            Integer serviceId = getAvailableServiceId(fromDistrictId, warehouseDistrictId);

            // Build fee calculation request with auto-fetched service ID
            GHNFeeRequest feeRequest = GHNFeeRequest.builder()
                    .fromDistrictId(fromDistrictId) // Customer location (with fallback)
                    .fromWardCode(fromWardCode)
                    .serviceId(serviceId) // Auto-fetched from available services
                    .serviceTypeId(null) // Set to null as requested
                    .toDistrictId(1454) // Shop warehouse
                    .toWardCode("21208")
                    .height(calculateTotalHeight(order)) // Calculate based on order
                    .length(calculateTotalLength(order)) // Calculate based on order
                    .weight(calculateTotalWeight(order)) // Calculate based on order
                    .width(calculateTotalWidth(order)) // Calculate based on order
                    .insuranceValue(order.getTotalAmount().intValue())
                    .codFailedAmount(2000) // Set COD failed amount as in example
                    .coupon(null)
                    .items(buildGHNItems(order))
                    .build();

            // Set headers exactly as GHN documentation
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);
            headers.set("ShopId", ghnShopId);

            HttpEntity<GHNFeeRequest> entity = new HttpEntity<>(feeRequest, headers);

            // Debug logging (show only partial token for security)
            String tokenPreview = ghnToken != null && ghnToken.length() > 10
                    ? ghnToken.substring(0, 8) + "..." + ghnToken.substring(ghnToken.length() - 4)
                    : "NULL_OR_EMPTY";
            log.info("Calling GHN fee API. Token preview: {}, ShopId: {}", tokenPreview, ghnShopId);
            log.info("Request body - fromDistrictId: {}, fromWardCode: '{}', toDistrictId: {}, toWardCode: '{}'",
                    feeRequest.getFromDistrictId(), feeRequest.getFromWardCode(),
                    feeRequest.getToDistrictId(), feeRequest.getToWardCode());

            // Call GHN API
            String url = ghnApiUrl + "/v2/shipping-order/fee";
            ResponseEntity<GHNFeeResponse> response = restTemplate.postForEntity(url, entity, GHNFeeResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                GHNFeeResponse feeResponse = response.getBody();
                log.info("GHN fee calculated for return request {}: {} VND",
                        returnRequest.getId(), feeResponse.getData().getTotal());
                return feeResponse;
            } else {
                throw new RuntimeException("Failed to calculate GHN shipping fee");
            }

        } catch (Exception e) {
            log.error("Error calculating GHN return shipping fee for request {}: {}",
                    returnRequest.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to calculate GHN shipping fee: " + e.getMessage(), e);
        }
    }

    private Integer getAvailableServiceId(Integer fromDistrictId, Integer toDistrictId) {
        try {
            List<GHNAvailableServicesResponse.ServiceData> services = ghnMasterDataService
                    .getAvailableServices(fromDistrictId, toDistrictId);

            if (services != null && !services.isEmpty()) {
                // Return the first available service ID
                Integer serviceId = services.get(0).getServiceId();
                log.info("Auto-selected service_id {} for route {} to {}",
                        serviceId, fromDistrictId, toDistrictId);
                return serviceId;
            } else {
                log.warn("No available services found for route {} to {}, using default 53320",
                        fromDistrictId, toDistrictId);
                return 53320; // Default fallback
            }
        } catch (Exception e) {
            log.error("Error fetching available services, using default service_id 53320: {}", e.getMessage());
            return 53320; // Default fallback
        }
    }

    @Override
    public GHNCreateOrderResponse createReturnShippingOrder(ReturnRequest returnRequest) {
        try {
            Order order = returnRequest.getOrder();
            User customer = order.getUser();

            // Build create order request
            GHNCreateReturnOrderRequest createRequest = GHNCreateReturnOrderRequest.builder()
                    .paymentTypeId(2) // Shop pays shipping fee
                    .note("Đơn hoàn hàng - Return Order #" + returnRequest.getId())
                    .requiredNote("KHONGCHOXEMHANG")
                    .fromName(customer.getFullName())
                    .fromPhone(customer.getPhone())
                    .fromAddress(customer.getAddress())
                    // Note: GHN only needs district/ward CODES, not names
                    // Removed fromWardName, fromDistrictName, fromProvinceName to avoid validation
                    // errors
                    .returnPhone(warehousePhone)
                    .returnAddress(warehouseAddress)
                    .returnDistrictId(warehouseDistrictId)
                    .returnWardCode(warehouseWardCode)
                    .clientOrderCode("RET-" + returnRequest.getId())
                    .toName(warehouseName)
                    .toPhone(warehousePhone)
                    .toAddress(warehouseAddress)
                    .toWardCode(warehouseWardCode)
                    .toDistrictId(warehouseDistrictId)
                    .codAmount(0) // No COD for returns
                    .content("Hoàn hàng đơn " + order.getOrderNumber())
                    .weight(calculateTotalWeight(order))
                    .length(calculateTotalLength(order))
                    .width(calculateTotalWidth(order))
                    .height(calculateTotalHeight(order))
                    .pickStationId(null)
                    .deliverStationId(null)
                    .insuranceValue(order.getTotalAmount().intValue())
                    .serviceId(0)
                    .serviceTypeId(defaultServiceTypeId)
                    .coupon(null)
                    .pickShift(List.of(2)) // Afternoon pickup
                    .items(buildGHNReturnItems(order))
                    .build();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);
            headers.set("ShopId", ghnShopId);

            HttpEntity<GHNCreateReturnOrderRequest> entity = new HttpEntity<>(createRequest, headers);

            // Call GHN API
            String url = ghnApiUrl + "/v2/shipping-order/create";
            ResponseEntity<GHNCreateOrderResponse> response = restTemplate.postForEntity(url, entity,
                    GHNCreateOrderResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                GHNCreateOrderResponse createResponse = response.getBody();
                log.info("GHN return order created for request {}: order code {}",
                        returnRequest.getId(), createResponse.getData().getOrderCode());
                return createResponse;
            } else {
                throw new RuntimeException("Failed to create GHN return shipping order");
            }

        } catch (Exception e) {
            log.error("Error creating GHN return shipping order for request {}: {}",
                    returnRequest.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create GHN return shipping order: " + e.getMessage(), e);
        }
    }

    @Override
    public String getOrderStatus(String orderCode) {
        try {
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);
            headers.set("ShopId", ghnShopId);

            HttpEntity<String> entity = new HttpEntity<>("{\"order_code\":\"" + orderCode + "\"}", headers);

            // Call GHN API
            String url = ghnApiUrl + "/v2/shipping-order/soc";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("GHN order status retrieved for order code: {}", orderCode);
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get GHN order status");
            }

        } catch (Exception e) {
            log.error("Error getting GHN order status for code {}: {}", orderCode, e.getMessage(), e);
            throw new RuntimeException("Failed to get order status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelOrder(String orderCode) {
        try {
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);
            headers.set("ShopId", ghnShopId);

            HttpEntity<String> entity = new HttpEntity<>("{\"order_codes\":[\"" + orderCode + "\"]}", headers);

            // Call GHN API
            String url = ghnApiUrl + "/v2/switch-status/cancel";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("GHN order cancelled: {}", orderCode);
                return true;
            } else {
                log.warn("Failed to cancel GHN order: {}", orderCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Error cancelling GHN order {}: {}", orderCode, e.getMessage(), e);
            return false;
        }
    }

    // Helper methods
    private List<GHNFeeRequest.GHNItem> buildGHNItems(Order order) {
        return order.getItems().stream()
                .map(item -> GHNFeeRequest.GHNItem.builder()
                        .name(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .height(15) // Realistic height per item in cm
                        .weight(300) // Realistic weight per item in grams (0.3kg)
                        .length(20) // Realistic length per item in cm
                        .width(15) // Realistic width per item in cm
                        .build())
                .collect(Collectors.toList());
    }

    private List<GHNCreateReturnOrderRequest.GHNReturnItem> buildGHNReturnItems(Order order) {
        return order.getItems().stream()
                .map(item -> GHNCreateReturnOrderRequest.GHNReturnItem.builder()
                        .name(item.getProduct().getName())
                        .code(item.getProduct().getId().toString())
                        .quantity(item.getQuantity())
                        .price(item.getPrice().intValue())
                        .length(20)
                        .width(20)
                        .height(20)
                        .weight(200)
                        .category(GHNCreateReturnOrderRequest.GHNCategory.builder()
                                .level1(item.getProduct().getCategory().getName())
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    private Integer calculateTotalWeight(Order order) {
        // Calculate total weight: 300g per item
        int totalWeight = order.getItems().stream()
                .mapToInt(item -> item.getQuantity() * 300)
                .sum();
        return Math.max(100, totalWeight); // Minimum 100g
    }

    private Integer calculateTotalLength(Order order) {
        // Base length + extra for more items
        int itemCount = order.getItems().stream()
                .mapToInt(item -> item.getQuantity())
                .sum();
        return Math.min(50, 20 + (itemCount / 3) * 5); // Max 50cm
    }

    private Integer calculateTotalWidth(Order order) {
        // Base width + extra for more items
        int itemCount = order.getItems().stream()
                .mapToInt(item -> item.getQuantity())
                .sum();
        return Math.min(40, 15 + (itemCount / 3) * 5); // Max 40cm
    }

    private Integer calculateTotalHeight(Order order) {
        // Height based on item count: stacking items
        int itemCount = order.getItems().stream()
                .mapToInt(item -> item.getQuantity())
                .sum();
        return Math.min(50, Math.max(15, itemCount * 5)); // Min 15cm, Max 50cm
    }
}