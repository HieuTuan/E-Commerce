package com.mypkga.commerceplatformfull.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mypkga.commerceplatformfull.entity.OrderItem;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.service.ReturnCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ReturnCodeService that generates unique return codes
 * with QR code functionality for return processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnCodeServiceImpl implements ReturnCodeService {
    
    // Default shop address - in a real application this would come from configuration
    @Value("${app.shop.address:123 Main Street, Ho Chi Minh City, Vietnam}")
    private String shopAddress;
    
    @Value("${app.shop.name:E-Commerce Platform}")
    private String shopName;
    
    @Value("${app.shop.phone:+84-123-456-789}")
    private String shopPhone;
    
    private static final String RETURN_CODE_PREFIX = "RET";
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int QR_CODE_SIZE = 300;
    
    @Override
    public String generateReturnCode(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            log.error("Cannot generate return code: returnRequest is null");
            throw new RuntimeException("Failed to generate return code");
        }
        
        log.debug("Generating return code for return request ID: {}", returnRequest.getId());
        
        try {
            // Generate unique return code with format: RET-YYYYMMDD-ORDERID-RANDOM
            String dateStr = LocalDateTime.now().format(CODE_DATE_FORMAT);
            String orderIdStr = String.format("%06d", returnRequest.getOrder().getId());
            String randomStr = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            String returnCode = String.format("%s-%s-%s-%s", 
                RETURN_CODE_PREFIX, dateStr, orderIdStr, randomStr);
            
            log.info("Generated return code: {} for order ID: {}", 
                returnCode, returnRequest.getOrder().getId());
            
            return returnCode;
            
        } catch (Exception e) {
            log.error("Error generating return code for return request ID: {}", 
                returnRequest != null ? returnRequest.getId() : "null", e);
            throw new RuntimeException("Failed to generate return code", e);
        }
    }
    
    @Override
    public String generateQRCode(String returnCode, ReturnRequest returnRequest) {
        if (returnCode == null || returnCode.trim().isEmpty()) {
            log.error("Cannot generate QR code: returnCode is null or empty");
            throw new RuntimeException("Failed to generate QR code");
        }
        
        if (returnRequest == null) {
            log.error("Cannot generate QR code: returnRequest is null");
            throw new RuntimeException("Failed to generate QR code");
        }
        
        log.debug("Generating QR code for return code: {}", returnCode);
        
        try {
            // Create comprehensive QR code content with all required information
            String qrContent = buildQRCodeContent(returnCode, returnRequest);
            
            // Configure QR code generation
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 
                QR_CODE_SIZE, QR_CODE_SIZE, hints);
            
            // Convert to image and encode as Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();
            
            String base64QRCode = Base64.getEncoder().encodeToString(qrCodeBytes);
            
            log.info("Successfully generated QR code for return code: {}", returnCode);
            return base64QRCode;
            
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code for return code: {}", returnCode, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    
    @Override
    public boolean isValidReturnCode(String returnCode) {
        if (returnCode == null || returnCode.trim().isEmpty()) {
            return false;
        }
        
        // Check format: RET-YYYYMMDD-ORDERID-RANDOM
        String[] parts = returnCode.split("-");
        if (parts.length != 4) {
            return false;
        }
        
        // Validate prefix
        if (!RETURN_CODE_PREFIX.equals(parts[0])) {
            return false;
        }
        
        // Validate date format (8 digits)
        if (parts[1].length() != 8 || !parts[1].matches("\\d{8}")) {
            return false;
        }
        
        // Validate order ID format (6 digits)
        if (parts[2].length() != 6 || !parts[2].matches("\\d{6}")) {
            return false;
        }
        
        // Validate random part (8 alphanumeric characters)
        if (parts[3].length() != 8 || !parts[3].matches("[A-Z0-9]{8}")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String decodeReturnCode(String returnCode) {
        log.debug("Decoding return code: {}", returnCode);
        
        if (!isValidReturnCode(returnCode)) {
            throw new IllegalArgumentException("Invalid return code format: " + returnCode);
        }
        
        String[] parts = returnCode.split("-");
        String dateStr = parts[1];
        String orderIdStr = parts[2];
        String randomStr = parts[3];
        
        // Parse date
        String formattedDate = String.format("%s-%s-%s", 
            dateStr.substring(0, 4), 
            dateStr.substring(4, 6), 
            dateStr.substring(6, 8));
        
        // Parse order ID
        long orderId = Long.parseLong(orderIdStr);
        
        return String.format("Return Code: %s | Date: %s | Order ID: %d | Reference: %s", 
            returnCode, formattedDate, orderId, randomStr);
    }
    
    /**
     * Build comprehensive QR code content with all required information
     */
    private String buildQRCodeContent(String returnCode, ReturnRequest returnRequest) {
        StringBuilder content = new StringBuilder();
        
        // Return code information
        content.append("RETURN_CODE: ").append(returnCode).append("\n");
        content.append("ORDER_ID: ").append(returnRequest.getOrder().getId()).append("\n");
        content.append("ORDER_NUMBER: ").append(returnRequest.getOrder().getOrderNumber()).append("\n");
        
        // Product information
        content.append("PRODUCTS: ");
        String productInfo = returnRequest.getOrder().getItems().stream()
            .map(this::formatOrderItem)
            .collect(Collectors.joining("; "));
        content.append(productInfo).append("\n");
        
        // Customer information (sender address)
        content.append("SENDER_NAME: ").append(returnRequest.getOrder().getCustomerName()).append("\n");
        content.append("SENDER_PHONE: ").append(returnRequest.getOrder().getCustomerPhone()).append("\n");
        content.append("SENDER_ADDRESS: ").append(returnRequest.getOrder().getShippingAddress()).append("\n");
        
        // Shop information (receiver address)
        content.append("RECEIVER_NAME: ").append(shopName).append("\n");
        content.append("RECEIVER_PHONE: ").append(shopPhone).append("\n");
        content.append("RECEIVER_ADDRESS: ").append(shopAddress).append("\n");
        
        // Shipping information
        content.append("SHIPPING_SERVICE: Giao Hàng Nhanh (GHN)\n");
        content.append("SHIPPING_TYPE: Tự động qua API GHN\n");
        if (returnRequest.getGhnOrderCode() != null) {
            content.append("GHN_ORDER_CODE: ").append(returnRequest.getGhnOrderCode()).append("\n");
        }
        
        // Return information
        content.append("RETURN_REASON: ").append(returnRequest.getReason().getDisplayName()).append("\n");
        content.append("RETURN_DESCRIPTION: ").append(returnRequest.getDetailedDescription()).append("\n");
        content.append("BANK_INFO: ").append(formatBankInfo(returnRequest)).append("\n");
        
        // Timestamps
        content.append("CREATED_AT: ").append(returnRequest.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        content.append("GENERATED_AT: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return content.toString();
    }
    
    /**
     * Format order item information for QR code
     */
    private String formatOrderItem(OrderItem item) {
        return String.format("%s (x%d)", 
            item.getProductName() != null ? item.getProductName() : item.getProduct().getName(),
            item.getQuantity());
    }
    
    /**
     * Format bank information for QR code (masked for security)
     */
    private String formatBankInfo(ReturnRequest returnRequest) {
        String accountNumber = returnRequest.getBankInfo().getAccountNumber();
        String maskedAccount = accountNumber.substring(0, 3) + "***" + 
            accountNumber.substring(accountNumber.length() - 3);
        
        return String.format("%s - %s - %s", 
            returnRequest.getBankInfo().getBankName(),
            maskedAccount,
            returnRequest.getBankInfo().getAccountHolderName());
    }
}