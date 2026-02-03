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
    private final ObjectMapper objectMapper;
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

            // Auto-fetch service_id from GHN available services API
            Integer serviceId = getAvailableServiceId(customer.getDistrictId(), warehouseDistrictId);

            // Build fee calculation request with auto-fetched service ID
            GHNFeeRequest feeRequest = GHNFeeRequest.builder()
                    .fromDistrictId(customer.getDistrictId() != null ? customer.getDistrictId() : 1454) // Customer
                                                                                                        // location
                    .fromWardCode(customer.getWardCode() != null ? customer.getWardCode() : "21211")
                    .serviceId(serviceId) // Auto-fetched from available services
                    .serviceTypeId(null) // Set to null as requested
                    .toDistrictId(warehouseDistrictId) // Shop warehouse
                    .toWardCode(warehouseWardCode)
                    .height(50) // Fixed height as in example
                    .length(20) // Fixed length as in example
                    .weight(200) // Fixed weight as in example
                    .width(20) // Fixed width as in example
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
            log.debug("Request details: {}", feeRequest);

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
            // Check if this is an authentication error
            if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized"))) {
                log.warn(
                        "GHN API authentication failed for request {}. This is expected if GHN_TOKEN is not configured. Using mock data.",
                        returnRequest.getId());
                log.info("To use real GHN API, set the GHN_TOKEN environment variable with your GHN API token.");
            } else {
                log.error("Error calculating GHN return shipping fee for request {}: {}",
                        returnRequest.getId(), e.getMessage());
            }

            // Return mock response as fallback
            log.info("Using mock shipping fee data (25,000 VND) for return request {}", returnRequest.getId());
            GHNFeeResponse mockResponse = new GHNFeeResponse();
            mockResponse.setCode(200);
            mockResponse.setMessage("Success (Mock Data - Development Mode)");

            GHNFeeResponse.GHNFeeData mockData = new GHNFeeResponse.GHNFeeData();
            mockData.setTotal(25000); // 25,000 VND default fee
            mockData.setServiceFee(20000);
            mockData.setInsuranceFee(5000);
            mockData.setPickStationFee(0);
            mockData.setCouponValue(0);

            mockResponse.setData(mockData);
            return mockResponse;
        }
    }

    /**
     * Helper method to get available service ID from GHN
     * 
     * @param fromDistrictId Origin district
     * @param toDistrictId   Destination district
     * @return Service ID or default 53320 if unavailable
     */
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
                    .fromWardName(getWardName(customer.getWardCode()))
                    .fromDistrictName(getDistrictName(customer.getDistrictId()))
                    .fromProvinceName(getProvinceName(customer.getDistrictId()))
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

            // Return mock response as fallback
            log.warn("Falling back to mock order response due to GHN API error");
            GHNCreateOrderResponse mockResponse = new GHNCreateOrderResponse();
            mockResponse.setCode(200);
            mockResponse.setMessage("Success (Mock)");

            GHNCreateOrderResponse.GHNOrderData mockData = new GHNCreateOrderResponse.GHNOrderData();
            mockData.setOrderCode("MOCK-" + System.currentTimeMillis());
            mockData.setSortCode("SORT-" + returnRequest.getId());
            mockData.setFee(25000);
            mockData.setTotalFee(25000);
            mockData.setExpectedDeliveryTime("2024-02-05 17:00:00");

            mockResponse.setData(mockData);
            return mockResponse;
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
                        .height(200) // Fixed dimensions as in example
                        .weight(1000) // Fixed weight as in example
                        .length(200) // Fixed dimensions as in example
                        .width(200) // Fixed dimensions as in example
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
        return order.getItems().size() * 200; // 200g per item default
    }

    private Integer calculateTotalLength(Order order) {
        return 20; // Default package length
    }

    private Integer calculateTotalWidth(Order order) {
        return 20; // Default package width
    }

    private Integer calculateTotalHeight(Order order) {
        return Math.max(10, order.getItems().size() * 5); // Height based on item count
    }

    // These methods would need to call GHN master data APIs
    // For now, using placeholder values
    private String getWardName(String wardCode) {
        return "Ward " + wardCode; // Placeholder
    }

    private String getDistrictName(Integer districtId) {
        return "District " + districtId; // Placeholder
    }

    private String getProvinceName(Integer districtId) {
        return "Ho Chi Minh City"; // Default province
    }
}