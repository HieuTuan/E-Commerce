package com.mypkga.commerceplatformfull.config;

import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class for Return & Refund feature components.
 * Wires together all necessary beans for the return/refund workflow including
 * QR code generation, cloud storage, and email services.
 */
@Configuration
@Slf4j
public class ReturnRefundConfig {
    
    @Value("${app.shop.name:E-Commerce Platform}")
    private String shopName;
    
    @Value("${app.shop.address:123 Main Street, District 1, Ho Chi Minh City, Vietnam}")
    private String shopAddress;
    
    @Value("${app.shop.phone:+84-123-456-789}")
    private String shopPhone;
    
    @Value("${app.shop.email:returns@ecommerce-platform.com}")
    private String shopEmail;
    
    /**
     * QR Code Writer bean for generating return codes.
     * Used by ReturnCodeService to create scannable QR codes.
     */
    @Bean
    public QRCodeWriter qrCodeWriter() {
        log.info("Configuring QR Code Writer for return code generation");
        return new QRCodeWriter();
    }
    
    /**
     * Shop configuration properties bean.
     * Contains shop information used in return processing and notifications.
     */
    @Bean
    public ShopConfig shopConfig() {
        log.info("Configuring shop information for return processing");
        return ShopConfig.builder()
                .name(shopName)
                .address(shopAddress)
                .phone(shopPhone)
                .email(shopEmail)
                .build();
    }
    
    /**
     * Configuration for development/testing environments.
     * Provides mock or local implementations when cloud services are not available.
     */
    @Configuration
    @Profile({"dev", "test"})
    static class DevelopmentConfig {
        
        @Bean
        public String developmentNotice() {
            log.info("Return & Refund feature configured for development/testing environment");
            log.info("Using local file storage and mock email services where applicable");
            return "development-mode";
        }
    }
    
    /**
     * Configuration for production environment.
     * Ensures all cloud services and production features are properly configured.
     */
    @Configuration
    @Profile("prod")
    static class ProductionConfig {
        
        @Bean
        public String productionNotice() {
            log.info("Return & Refund feature configured for production environment");
            log.info("Using cloud storage (Cloudinary) and real email services");
            return "production-mode";
        }
    }
    
    /**
     * Shop configuration data class.
     * Holds shop information used throughout the return/refund process.
     */
    public static class ShopConfig {
        private final String name;
        private final String address;
        private final String phone;
        private final String email;
        
        private ShopConfig(String name, String address, String phone, String email) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.email = email;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        
        public static class Builder {
            private String name;
            private String address;
            private String phone;
            private String email;
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder address(String address) {
                this.address = address;
                return this;
            }
            
            public Builder phone(String phone) {
                this.phone = phone;
                return this;
            }
            
            public Builder email(String email) {
                this.email = email;
                return this;
            }
            
            public ShopConfig build() {
                return new ShopConfig(name, address, phone, email);
            }
        }
    }
}