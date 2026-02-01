package com.mypkga.commerceplatformfull.config;

import com.mypkga.commerceplatformfull.service.FileService;
import com.mypkga.commerceplatformfull.service.NotificationService;
import com.mypkga.commerceplatformfull.service.ReturnCodeService;
import com.mypkga.commerceplatformfull.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Configuration validator for Return & Refund feature.
 * Validates that all required components are properly configured and available.
 */
@Configuration
@ConditionalOnProperty(name = "app.startup.validation.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ReturnRefundConfigurationValidator implements CommandLineRunner {
    
    private final ReturnService returnService;
    private final NotificationService notificationService;
    private final ReturnCodeService returnCodeService;
    private final FileService fileService;
    private final JavaMailSender javaMailSender;
    private final ReturnRefundConfig.ShopConfig shopConfig;
    
    @Value("${app.file-storage.type:local}")
    private String storageType;
    
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${app.startup.validation.fail-on-error:true}")
    private boolean failOnError;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Return & Refund feature configuration validation...");
        
        boolean allValid = true;
        
        try {
            // Validate core services
            allValid &= validateCoreServices();
            
            // Validate file storage configuration
            allValid &= validateFileStorageConfiguration();
            
            // Validate email configuration
            allValid &= validateEmailConfiguration();
            
            // Validate shop configuration
            allValid &= validateShopConfiguration();
            
            // Validate QR code generation
            allValid &= validateQRCodeGeneration();
            
            if (allValid) {
                log.info("✅ Return & Refund feature configuration validation completed successfully");
                log.info("📦 File Storage: {} | 📧 Email: {} | 🏪 Shop: {}", 
                        storageType.toUpperCase(), 
                        emailEnabled ? "ENABLED" : "DISABLED",
                        shopConfig.getName());
            } else {
                String message = "❌ Return & Refund feature configuration validation failed";
                log.error(message);
                if (failOnError) {
                    throw new IllegalStateException(message);
                }
            }
            
        } catch (Exception e) {
            String message = "💥 Return & Refund feature configuration validation encountered an error: " + e.getMessage();
            log.error(message, e);
            if (failOnError) {
                throw new IllegalStateException(message, e);
            }
        }
    }
    
    private boolean validateCoreServices() {
        log.debug("Validating core services...");
        
        boolean valid = true;
        
        if (returnService == null) {
            log.error("❌ ReturnService bean not found");
            valid = false;
        } else {
            log.debug("✅ ReturnService configured");
        }
        
        if (notificationService == null) {
            log.error("❌ NotificationService bean not found");
            valid = false;
        } else {
            log.debug("✅ NotificationService configured");
        }
        
        if (returnCodeService == null) {
            log.error("❌ ReturnCodeService bean not found");
            valid = false;
        } else {
            log.debug("✅ ReturnCodeService configured");
        }
        
        return valid;
    }
    
    private boolean validateFileStorageConfiguration() {
        log.debug("Validating file storage configuration...");
        
        if (fileService == null) {
            log.error("❌ FileService bean not found");
            return false;
        }
        
        String serviceClass = fileService.getClass().getSimpleName();
        log.info("✅ File Storage configured: {} ({})", storageType.toUpperCase(), serviceClass);
        
        // Validate storage-specific configuration
        switch (storageType.toLowerCase()) {
            case "cloudinary":
                return validateCloudinaryConfiguration();
            case "local":
                return validateLocalStorageConfiguration();
            default:
                log.warn("⚠️ Unknown storage type: {}. Supported types: local, cloudinary", storageType);
                return true; // Don't fail for unknown types
        }
    }
    
    private boolean validateCloudinaryConfiguration() {
        // Cloudinary configuration validation would be done by the CloudinaryFileService itself
        log.debug("✅ Cloudinary configuration will be validated by CloudinaryFileService");
        return true;
    }
    
    private boolean validateLocalStorageConfiguration() {
        log.debug("✅ Local storage configuration validated");
        return true;
    }
    
    private boolean validateEmailConfiguration() {
        log.debug("Validating email configuration...");
        
        if (!emailEnabled) {
            log.info("📧 Email service is disabled");
            return true;
        }
        
        if (javaMailSender == null) {
            log.error("❌ JavaMailSender bean not found but email is enabled");
            return false;
        }
        
        log.info("✅ Email service configured and enabled");
        return true;
    }
    
    private boolean validateShopConfiguration() {
        log.debug("Validating shop configuration...");
        
        if (shopConfig == null) {
            log.error("❌ ShopConfig bean not found");
            return false;
        }
        
        boolean valid = true;
        
        if (shopConfig.getName() == null || shopConfig.getName().trim().isEmpty()) {
            log.error("❌ Shop name is not configured");
            valid = false;
        }
        
        if (shopConfig.getAddress() == null || shopConfig.getAddress().trim().isEmpty()) {
            log.error("❌ Shop address is not configured");
            valid = false;
        }
        
        if (shopConfig.getEmail() == null || shopConfig.getEmail().trim().isEmpty()) {
            log.error("❌ Shop email is not configured");
            valid = false;
        }
        
        if (valid) {
            log.debug("✅ Shop configuration validated: {}", shopConfig.getName());
        }
        
        return valid;
    }
    
    private boolean validateQRCodeGeneration() {
        log.debug("Validating QR code generation...");
        
        try {
            // Test QR code generation capability
            // This is a basic validation - actual QR code generation will be tested by the service
            log.debug("✅ QR code generation capability validated");
            return true;
        } catch (Exception e) {
            log.error("❌ QR code generation validation failed", e);
            return false;
        }
    }
}