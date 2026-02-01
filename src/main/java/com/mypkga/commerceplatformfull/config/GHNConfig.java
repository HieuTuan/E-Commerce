package com.mypkga.commerceplatformfull.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ghn")
@Data
public class GHNConfig {
    
    private String apiUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api";
    private String token;
    private Integer shopId;
    
    // Warehouse information (kho hàng của shop)
    private Integer warehouseDistrictId;
    private String warehouseWardCode;
    private String warehouseAddress;
    private String warehouseName;
    private String warehousePhone;
    
    // Default service settings
    private Integer defaultServiceTypeId = 2; // Standard service
    private String defaultRequiredNote = "KHONGCHOXEMHANG"; // Don't allow inspection
    private Integer defaultPaymentTypeId = 2; // Sender pays
    
    // Retry settings
    private Integer maxRetries = 3;
    private Long retryDelayMs = 1000L;
    
    // Webhook settings
    private String webhookSecret;
    private Boolean webhookEnabled = true;
}