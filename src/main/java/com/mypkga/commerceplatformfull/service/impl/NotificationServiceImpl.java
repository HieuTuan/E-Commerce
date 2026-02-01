package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.service.EmailService;
import com.mypkga.commerceplatformfull.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Implementation of NotificationService for handling return request email notifications.
 * This service creates and sends HTML email templates for approval, rejection, and completion
 * notifications with proper recipient validation using customer email addresses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private final EmailService emailService;
    
    @Value("${app.support.hotline:1900-123-456}")
    private String supportHotline;
    
    @Value("${app.support.email:support@ecommerce.com}")
    private String supportEmail;
    
    @Value("${app.company.name:E-Commerce Platform}")
    private String companyName;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    @Override
    @Transactional
    public boolean sendApprovalNotification(ReturnRequest returnRequest) {
        log.info("Sending approval notification for return request {}", returnRequest.getId());
        
        if (!validateCustomerEmail(returnRequest)) {
            log.error("Cannot send approval notification - invalid customer email for return request {}", 
                    returnRequest.getId());
            return false;
        }
        
        String customerEmail = getCustomerEmail(returnRequest);
        String subject = "Yêu cầu hoàn trả đã được chấp nhận - " + returnRequest.getOrder().getOrderNumber();
        String htmlContent = buildApprovalEmailTemplate(returnRequest);
        
        boolean sent = emailService.sendHtmlEmail(customerEmail, subject, htmlContent);
        
        if (sent) {
            log.info("Approval notification sent successfully to {} for return request {}", 
                    maskEmail(customerEmail), returnRequest.getId());
        } else {
            log.error("Failed to send approval notification to {} for return request {}", 
                    maskEmail(customerEmail), returnRequest.getId());
        }
        
        return sent;
    }
    
    @Override
    @Transactional
    public boolean sendRejectionNotification(ReturnRequest returnRequest) {
        log.info("Sending rejection notification for return request {}", returnRequest.getId());
        
        if (!validateCustomerEmail(returnRequest)) {
            log.error("Cannot send rejection notification - invalid customer email for return request {}", 
                    returnRequest.getId());
            return false;
        }
        
        String customerEmail = getCustomerEmail(returnRequest);
        String subject = "Yêu cầu hoàn trả đã bị từ chối - " + returnRequest.getOrder().getOrderNumber();
        String htmlContent = buildRejectionEmailTemplate(returnRequest);
        
        boolean sent = emailService.sendHtmlEmail(customerEmail, subject, htmlContent);
        
        if (sent) {
            log.info("Rejection notification sent successfully to {} for return request {}", 
                    maskEmail(customerEmail), returnRequest.getId());
        } else {
            log.error("Failed to send rejection notification to {} for return request {}", 
                    maskEmail(customerEmail), returnRequest.getId());
        }
        
        return sent;
    }
    
    @Override
    @Transactional
    public boolean sendCompletionNotification(ReturnRequest returnRequest) {
        log.info("Sending completion notification for return request {}", returnRequest.getId());
        
        if (!validateCustomerEmail(returnRequest)) {
            log.error("Cannot send completion notification - invalid customer email for return request {}", 
                    returnRequest.getId());
            return false;
        }
        
        String customerEmail = getCustomerEmail(returnRequest);
        String subject = "Hoàn tiền thành công - " + returnRequest.getOrder().getOrderNumber();
        String htmlContent = buildCompletionEmailTemplate(returnRequest);
        
        boolean sent = emailService.sendHtmlEmail(customerEmail, subject, htmlContent);
        
        if (sent) {
            log.info("Completion notification sent successfully to {} for return request {}", 
                    maskEmail(customerEmail), returnRequest.getId());
        } else {
            log.error("Failed to send completion notification to {} for return request {}", 
                    maskEmail(customerEmail), returnRequest.getId());
        }
        
        return sent;
    }
    
    @Override
    public boolean validateCustomerEmail(ReturnRequest returnRequest) {
        String customerEmail = getCustomerEmail(returnRequest);
        return customerEmail != null && emailService.isValidEmail(customerEmail);
    }
    
    @Override
    public String getCustomerEmail(ReturnRequest returnRequest) {
        if (returnRequest == null || returnRequest.getOrder() == null) {
            return null;
        }
        
        User customer = returnRequest.getOrder().getUser();
        return customer != null ? customer.getEmail() : null;
    }
    
    /**
     * Build HTML email template for return request approval notification.
     * Contains post office name, 48-hour deadline, and tracking code.
     */
    private String buildApprovalEmailTemplate(ReturnRequest returnRequest) {
        String customerName = returnRequest.getOrder().getUser().getFullName();
        String orderNumber = returnRequest.getOrder().getOrderNumber();
        String shippingInfo = "Giao Hàng Nhanh (GHN)";
        String shippingDetails = "Vận chuyển tự động qua GHN";
        String returnCode = returnRequest.getReturnCode() != null ? returnRequest.getReturnCode() : "Đang tạo mã";
        String approvalDate = returnRequest.getProcessedAt() != null ? 
            returnRequest.getProcessedAt().format(DATE_FORMATTER) : "Vừa xong";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Yêu cầu hoàn trả đã được chấp nhận</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background-color: #f5f5f5;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #4caf50, #45a049);
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0 0 10px 0;
                        font-size: 28px;
                        font-weight: bold;
                    }
                    .header p {
                        margin: 0;
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 24px;
                        font-weight: bold;
                        margin-bottom: 20px;
                        color: #333;
                    }
                    .message {
                        font-size: 16px;
                        line-height: 1.6;
                        margin-bottom: 30px;
                        color: #555;
                    }
                    .info-box {
                        background: #e8f5e8;
                        border-left: 4px solid #4caf50;
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .info-item {
                        margin-bottom: 15px;
                        display: flex;
                        align-items: flex-start;
                    }
                    .info-item:last-child {
                        margin-bottom: 0;
                    }
                    .info-label {
                        font-weight: bold;
                        min-width: 120px;
                        color: #333;
                    }
                    .info-value {
                        color: #555;
                        flex: 1;
                    }
                    .return-code {
                        background: #4caf50;
                        color: white;
                        font-size: 18px;
                        font-weight: bold;
                        padding: 10px 15px;
                        border-radius: 6px;
                        display: inline-block;
                        letter-spacing: 2px;
                    }
                    .deadline-warning {
                        background: #fff3cd;
                        border: 1px solid #ffeaa7;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 20px 0;
                        text-align: center;
                    }
                    .deadline-warning h3 {
                        color: #856404;
                        margin: 0 0 10px 0;
                        font-size: 18px;
                    }
                    .deadline-warning p {
                        color: #856404;
                        margin: 0;
                        font-weight: bold;
                    }
                    .instructions {
                        background: #f8f9fa;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 20px 0;
                    }
                    .instructions h3 {
                        color: #333;
                        margin: 0 0 15px 0;
                        font-size: 18px;
                    }
                    .instructions ol {
                        margin: 0;
                        padding-left: 20px;
                    }
                    .instructions li {
                        margin-bottom: 10px;
                        line-height: 1.5;
                    }
                    .footer {
                        padding: 20px 30px;
                        background: #f8f9fa;
                        border-top: 1px solid #eee;
                    }
                    .footer p {
                        margin: 5px 0;
                        font-size: 14px;
                        color: #666;
                    }
                    .signature {
                        font-weight: bold;
                        color: #333;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ %s</h1>
                        <p>Yêu cầu hoàn trả đã được chấp nhận</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Chào %s!</div>
                        
                        <div class="message">
                            Chúng tôi vui mừng thông báo rằng yêu cầu hoàn trả của bạn đã được chấp nhận. 
                            Dưới đây là thông tin chi tiết để bạn có thể gửi hàng hoàn trả.
                        </div>
                        
                        <div class="info-box">
                            <div class="info-item">
                                <span class="info-label">Đơn hàng:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Ngày chấp nhận:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Mã hoàn trả:</span>
                                <span class="info-value">
                                    <span class="return-code">%s</span>
                                </span>
                            </div>
                        </div>
                        
                        <div class="deadline-warning">
                            <h3>⏰ Lưu ý quan trọng</h3>
                            <p>Bạn có 48 giờ kể từ thời điểm này để gửi hàng hoàn trả</p>
                        </div>
                        
                        <div class="instructions">
                            <h3>📦 Hướng dẫn gửi hàng hoàn trả</h3>
                            <ol>
                                <li><strong>Đóng gói sản phẩm</strong> cẩn thận trong hộp gốc (nếu có)</li>
                                <li><strong>In mã QR hoàn trả</strong> và dán lên bao bì</li>
                                <li><strong>Mang đến bưu điện</strong> được chỉ định dưới đây</li>
                                <li><strong>Xuất trình mã hoàn trả</strong> cho nhân viên bưu điện</li>
                                <li><strong>Giữ biên lai</strong> để theo dõi quá trình vận chuyển</li>
                            </ol>
                        </div>
                        
                        <div class="info-box">
                            <h3 style="margin: 0 0 15px 0; color: #333;">📍 Thông tin bưu điện</h3>
                            <div class="info-item">
                                <span class="info-label">Tên:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Địa chỉ:</span>
                                <span class="info-value">%s</span>
                            </div>
                        </div>
                        
                        <div class="message">
                            Sau khi chúng tôi nhận được hàng hoàn trả và kiểm tra, chúng tôi sẽ tiến hành hoàn tiền 
                            vào tài khoản ngân hàng mà bạn đã cung cấp. Quá trình này thường mất 3-5 ngày làm việc.
                        </div>
                        
                        <div class="message">
                            Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi qua hotline: 
                            <strong>%s</strong> hoặc email: <strong>%s</strong>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>Trân trọng,</p>
                        <p class="signature">Đội ngũ %s</p>
                        <p style="margin-top: 20px; font-size: 12px; color: #999;">
                            Email này được gửi tự động, vui lòng không trả lời.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            companyName, customerName, orderNumber, approvalDate, returnCode, 
            shippingInfo, shippingDetails, supportHotline, supportEmail, companyName);
    }
    
    /**
     * Build HTML email template for return request rejection notification.
     * Contains rejection reason and hotline contact information.
     */
    private String buildRejectionEmailTemplate(ReturnRequest returnRequest) {
        String customerName = returnRequest.getOrder().getUser().getFullName();
        String orderNumber = returnRequest.getOrder().getOrderNumber();
        String rejectionReason = returnRequest.getRejectionReason() != null ? 
            returnRequest.getRejectionReason() : "Không đáp ứng điều kiện hoàn trả";
        String rejectionDate = returnRequest.getProcessedAt() != null ? 
            returnRequest.getProcessedAt().format(DATE_FORMATTER) : "Vừa xong";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Yêu cầu hoàn trả đã bị từ chối</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background-color: #f5f5f5;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #f44336, #d32f2f);
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0 0 10px 0;
                        font-size: 28px;
                        font-weight: bold;
                    }
                    .header p {
                        margin: 0;
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 24px;
                        font-weight: bold;
                        margin-bottom: 20px;
                        color: #333;
                    }
                    .message {
                        font-size: 16px;
                        line-height: 1.6;
                        margin-bottom: 30px;
                        color: #555;
                    }
                    .info-box {
                        background: #ffebee;
                        border-left: 4px solid #f44336;
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .info-item {
                        margin-bottom: 15px;
                        display: flex;
                        align-items: flex-start;
                    }
                    .info-item:last-child {
                        margin-bottom: 0;
                    }
                    .info-label {
                        font-weight: bold;
                        min-width: 120px;
                        color: #333;
                    }
                    .info-value {
                        color: #555;
                        flex: 1;
                    }
                    .reason-box {
                        background: #fff3e0;
                        border: 1px solid #ffcc02;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 20px 0;
                    }
                    .reason-box h3 {
                        color: #e65100;
                        margin: 0 0 10px 0;
                        font-size: 18px;
                    }
                    .reason-box p {
                        color: #bf360c;
                        margin: 0;
                        font-style: italic;
                        line-height: 1.5;
                    }
                    .contact-box {
                        background: #e3f2fd;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 20px 0;
                        text-align: center;
                    }
                    .contact-box h3 {
                        color: #1976d2;
                        margin: 0 0 15px 0;
                        font-size: 18px;
                    }
                    .contact-info {
                        display: flex;
                        justify-content: space-around;
                        flex-wrap: wrap;
                        gap: 20px;
                    }
                    .contact-item {
                        flex: 1;
                        min-width: 200px;
                    }
                    .contact-item h4 {
                        color: #1976d2;
                        margin: 0 0 5px 0;
                        font-size: 16px;
                    }
                    .contact-item p {
                        color: #333;
                        margin: 0;
                        font-weight: bold;
                        font-size: 18px;
                    }
                    .footer {
                        padding: 20px 30px;
                        background: #f8f9fa;
                        border-top: 1px solid #eee;
                    }
                    .footer p {
                        margin: 5px 0;
                        font-size: 14px;
                        color: #666;
                    }
                    .signature {
                        font-weight: bold;
                        color: #333;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>❌ %s</h1>
                        <p>Yêu cầu hoàn trả đã bị từ chối</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Chào %s!</div>
                        
                        <div class="message">
                            Chúng tôi rất tiếc phải thông báo rằng yêu cầu hoàn trả của bạn đã bị từ chối 
                            sau khi xem xét kỹ lưỡng.
                        </div>
                        
                        <div class="info-box">
                            <div class="info-item">
                                <span class="info-label">Đơn hàng:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Ngày từ chối:</span>
                                <span class="info-value">%s</span>
                            </div>
                        </div>
                        
                        <div class="reason-box">
                            <h3>📋 Lý do từ chối</h3>
                            <p>%s</p>
                        </div>
                        
                        <div class="message">
                            Chúng tôi hiểu rằng bạn có thể không hài lòng với quyết định này. 
                            Nếu bạn có bất kỳ thắc mắc nào hoặc muốn khiếu nại về quyết định này, 
                            vui lòng liên hệ với chúng tôi ngay lập tức.
                        </div>
                        
                        <div class="contact-box">
                            <h3>📞 Liên hệ hỗ trợ khách hàng</h3>
                            <div class="contact-info">
                                <div class="contact-item">
                                    <h4>Hotline</h4>
                                    <p>%s</p>
                                </div>
                                <div class="contact-item">
                                    <h4>Email</h4>
                                    <p>%s</p>
                                </div>
                            </div>
                            <p style="margin-top: 15px; color: #666; font-size: 14px;">
                                Thời gian hỗ trợ: 8:00 - 22:00 (Thứ 2 - Chủ nhật)
                            </p>
                        </div>
                        
                        <div class="message">
                            Đội ngũ chăm sóc khách hàng của chúng tôi sẽ hỗ trợ bạn giải quyết vấn đề 
                            một cách nhanh chóng và thỏa đáng nhất.
                        </div>
                        
                        <div class="message">
                            Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi.
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>Trân trọng,</p>
                        <p class="signature">Đội ngũ %s</p>
                        <p style="margin-top: 20px; font-size: 12px; color: #999;">
                            Email này được gửi tự động, vui lòng không trả lời.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            companyName, customerName, orderNumber, rejectionDate, rejectionReason, 
            supportHotline, supportEmail, companyName);
    }
    
    /**
     * Build HTML email template for refund completion notification.
     * Contains account information and thank you message.
     */
    private String buildCompletionEmailTemplate(ReturnRequest returnRequest) {
        String customerName = returnRequest.getOrder().getUser().getFullName();
        String orderNumber = returnRequest.getOrder().getOrderNumber();
        String completionDate = returnRequest.getProcessedAt() != null ? 
            returnRequest.getProcessedAt().format(DATE_FORMATTER) : "Vừa xong";
        
        // Bank information
        String bankName = returnRequest.getBankInfo().getBankName();
        String accountNumber = maskAccountNumber(returnRequest.getBankInfo().getAccountNumber());
        String accountHolderName = returnRequest.getBankInfo().getAccountHolderName();
        
        // Order amount (assuming this would be available from order)
        String refundAmount = returnRequest.getOrder().getTotalAmount().toString() + " VNĐ";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Hoàn tiền thành công</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background-color: #f5f5f5;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #2196f3, #1976d2);
                        color: white;
                        padding: 30px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0 0 10px 0;
                        font-size: 28px;
                        font-weight: bold;
                    }
                    .header p {
                        margin: 0;
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 24px;
                        font-weight: bold;
                        margin-bottom: 20px;
                        color: #333;
                    }
                    .message {
                        font-size: 16px;
                        line-height: 1.6;
                        margin-bottom: 30px;
                        color: #555;
                    }
                    .success-box {
                        background: #e8f5e8;
                        border-left: 4px solid #4caf50;
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 4px;
                        text-align: center;
                    }
                    .success-box h3 {
                        color: #2e7d32;
                        margin: 0 0 10px 0;
                        font-size: 20px;
                    }
                    .success-box .amount {
                        font-size: 32px;
                        font-weight: bold;
                        color: #2e7d32;
                        margin: 10px 0;
                    }
                    .info-box {
                        background: #f8f9fa;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 20px 0;
                    }
                    .info-item {
                        margin-bottom: 15px;
                        display: flex;
                        align-items: flex-start;
                    }
                    .info-item:last-child {
                        margin-bottom: 0;
                    }
                    .info-label {
                        font-weight: bold;
                        min-width: 140px;
                        color: #333;
                    }
                    .info-value {
                        color: #555;
                        flex: 1;
                    }
                    .bank-info {
                        background: #e3f2fd;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 20px 0;
                    }
                    .bank-info h3 {
                        color: #1976d2;
                        margin: 0 0 15px 0;
                        font-size: 18px;
                    }
                    .timeline-box {
                        background: #fff3e0;
                        border-radius: 6px;
                        padding: 20px;
                        margin: 20px 0;
                    }
                    .timeline-box h3 {
                        color: #f57c00;
                        margin: 0 0 15px 0;
                        font-size: 18px;
                    }
                    .timeline-box p {
                        color: #e65100;
                        margin: 0;
                        font-weight: bold;
                    }
                    .thank-you {
                        background: linear-gradient(135deg, #ff9800, #f57c00);
                        color: white;
                        border-radius: 6px;
                        padding: 30px;
                        margin: 30px 0;
                        text-align: center;
                    }
                    .thank-you h3 {
                        margin: 0 0 15px 0;
                        font-size: 24px;
                    }
                    .thank-you p {
                        margin: 0;
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .footer {
                        padding: 20px 30px;
                        background: #f8f9fa;
                        border-top: 1px solid #eee;
                    }
                    .footer p {
                        margin: 5px 0;
                        font-size: 14px;
                        color: #666;
                    }
                    .signature {
                        font-weight: bold;
                        color: #333;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>💰 %s</h1>
                        <p>Hoàn tiền thành công</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Chào %s!</div>
                        
                        <div class="message">
                            Chúng tôi vui mừng thông báo rằng quá trình hoàn tiền cho đơn hàng của bạn 
                            đã được hoàn tất thành công.
                        </div>
                        
                        <div class="success-box">
                            <h3>✅ Hoàn tiền thành công</h3>
                            <div class="amount">%s</div>
                            <p>đã được chuyển vào tài khoản của bạn</p>
                        </div>
                        
                        <div class="info-box">
                            <div class="info-item">
                                <span class="info-label">Đơn hàng:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Ngày hoàn tiền:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Số tiền hoàn:</span>
                                <span class="info-value"><strong>%s</strong></span>
                            </div>
                        </div>
                        
                        <div class="bank-info">
                            <h3>🏦 Thông tin tài khoản nhận tiền</h3>
                            <div class="info-item">
                                <span class="info-label">Ngân hàng:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Số tài khoản:</span>
                                <span class="info-value">%s</span>
                            </div>
                            <div class="info-item">
                                <span class="info-label">Chủ tài khoản:</span>
                                <span class="info-value">%s</span>
                            </div>
                        </div>
                        
                        <div class="timeline-box">
                            <h3>⏰ Thời gian xử lý</h3>
                            <p>Tiền sẽ có trong tài khoản của bạn trong vòng 1-3 ngày làm việc</p>
                        </div>
                        
                        <div class="message">
                            Nếu sau 3 ngày làm việc bạn chưa nhận được tiền, vui lòng liên hệ với chúng tôi 
                            qua hotline: <strong>%s</strong> hoặc email: <strong>%s</strong>
                        </div>
                        
                        <div class="thank-you">
                            <h3>🙏 Cảm ơn bạn!</h3>
                            <p>
                                Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi. 
                                Chúng tôi hy vọng sẽ được phục vụ bạn tốt hơn trong tương lai.
                            </p>
                        </div>
                        
                        <div class="message">
                            Chúng tôi luôn nỗ lực cải thiện chất lượng sản phẩm và dịch vụ. 
                            Ý kiến đóng góp của bạn rất quan trọng với chúng tôi.
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>Trân trọng,</p>
                        <p class="signature">Đội ngũ %s</p>
                        <p style="margin-top: 20px; font-size: 12px; color: #999;">
                            Email này được gửi tự động, vui lòng không trả lời.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            companyName, customerName, refundAmount, orderNumber, completionDate, refundAmount,
            bankName, accountNumber, accountHolderName, supportHotline, supportEmail, companyName);
    }
    
    /**
     * Mask account number for security (show only last 4 digits)
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }
    
    /**
     * Mask email for logging (show only first 2 chars and domain)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domain;
        } else {
            return localPart.substring(0, 2) + "***" + domain;
        }
    }
}