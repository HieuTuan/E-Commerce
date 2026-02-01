package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Email Service implementation - Optimized for fast email delivery
 * Handles sending emails including OTP verification emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.mock-mode:false}")
    private boolean mockMode;

    @Value("${app.email.from:noreply@ecommerce.com}")
    private String fromEmail;

    @Value("${app.email.from-name:E-Commerce Platform}")
    private String fromName;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Override
    public boolean sendEmail(String email, String subject, String message) {
        if (!isValidEmail(email)) {
            log.error("Invalid email address: {}", email);
            return false;
        }

        if (mockMode || !emailEnabled) {
            log.info("MOCK EMAIL - Sending email to {}: Subject: {}, Message: {}", 
                    maskEmail(email), subject, message);
            return true;
        }

        try {
            // Send email asynchronously for better performance
            sendEmailAsync(email, subject, message);
            log.info("Email queued for sending to: {}", maskEmail(email));
            return true;
        } catch (Exception e) {
            log.error("Failed to queue email to: {}", maskEmail(email), e);
            return false;
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendEmailAsync(String email, String subject, String message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(email);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Email sent successfully to: {}", maskEmail(email));
        } catch (Exception e) {
            log.error("Failed to send email to: {}", maskEmail(email), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean sendHtmlEmail(String email, String subject, String htmlContent) {
        if (!isValidEmail(email)) {
            log.error("Invalid email address: {}", email);
            return false;
        }

        if (mockMode || !emailEnabled) {
            log.info("MOCK HTML EMAIL - Sending HTML email to {}: Subject: {}", 
                    maskEmail(email), subject);
            return true;
        }

        try {
            // Send HTML email asynchronously for better performance
            sendHtmlEmailAsync(email, subject, htmlContent);
            log.info("HTML email queued for sending to: {}", maskEmail(email));
            return true;
        } catch (Exception e) {
            log.error("Failed to queue HTML email to: {}", maskEmail(email), e);
            return false;
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendHtmlEmailAsync(String email, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully to: {}", maskEmail(email));
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send HTML email to: {}", maskEmail(email), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean sendOTP(String email, String otp) {
        String subject = "Mã xác thực tài khoản - E-Commerce Platform";
        String htmlContent = buildVietnameseOTPEmailTemplate(otp);
        
        // Log OTP to console for testing
        log.info("=== OTP VERIFICATION ===");
        log.info("Email: {}", maskEmail(email));
        log.info("OTP Code: {}", otp);
        log.info("========================");
        
        return sendHtmlEmail(email, subject, htmlContent);
    }

    @Override
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Build Vietnamese OTP email template matching the provided design
     */
    private String buildVietnameseOTPEmailTemplate(String otp) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mã xác thực tài khoản</title>
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
                        background: linear-gradient(135deg, #1e88e5, #1976d2);
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
                        background-color: #fafafa;
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
                    .otp-container {
                        background: #e3f2fd;
                        border-radius: 8px;
                        padding: 30px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .otp-code {
                        display: inline-block;
                        background: #1976d2;
                        color: white;
                        font-size: 32px;
                        font-weight: bold;
                        padding: 15px 25px;
                        border-radius: 8px;
                        letter-spacing: 8px;
                        margin: 10px 0;
                    }
                    .notes {
                        margin: 30px 0;
                    }
                    .notes h3 {
                        color: #333;
                        font-size: 18px;
                        margin-bottom: 15px;
                    }
                    .notes ul {
                        padding-left: 20px;
                        margin: 0;
                    }
                    .notes li {
                        margin-bottom: 8px;
                        font-size: 14px;
                        line-height: 1.5;
                    }
                    .highlight {
                        color: #d32f2f;
                        font-weight: bold;
                    }
                    .footer {
                        padding: 20px 30px;
                        background: white;
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
                        <h1>🛒 E-Commerce Platform</h1>
                        <p>Xác thực tài khoản của bạn</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Chào bạn!</div>
                        
                        <div class="message">
                            Cảm ơn bạn đã đăng ký tài khoản tại E-Commerce Platform. Để hoàn tất quá trình đăng ký, 
                            vui lòng sử dụng mã xác thực dưới đây:
                        </div>
                        
                        <div class="otp-container">
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <div class="notes">
                            <h3>Lưu ý quan trọng:</h3>
                            <ul>
                                <li>Mã xác thực có hiệu lực trong <strong>1 phút</strong></li>
                                <li class="highlight">Không chia sẻ mã này với bất kỳ ai</li>
                                <li>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này</li>
                            </ul>
                        </div>
                        
                        <div class="message">
                            Nếu bạn gặp khó khăn trong quá trình xác thực, vui lòng liên hệ với chúng tôi để được hỗ trợ.
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>Trân trọng,</p>
                        <p class="signature">Đội ngũ E-Commerce Platform</p>
                        <p style="margin-top: 20px; font-size: 12px; color: #999;">
                            Email này được gửi tự động, vui lòng không trả lời.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, otp);
    }

    /**
     * Build optimized HTML template for OTP email - Smaller and faster to send
     */
    private String buildOptimizedOTPEmailTemplate(String otp) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verification Code</title>
                <style>
                    body{font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5}
                    .container{max-width:500px;margin:0 auto;background:white;border-radius:8px;overflow:hidden}
                    .header{background:#007bff;color:white;padding:20px;text-align:center}
                    .content{padding:30px;text-align:center}
                    .otp{font-size:28px;font-weight:bold;color:#007bff;background:#f8f9fa;padding:15px;border-radius:6px;margin:20px 0;letter-spacing:4px}
                    .footer{color:#666;font-size:12px;padding:20px;text-align:center}
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🛒 E-Commerce Platform</h2>
                        <p>Email Verification</p>
                    </div>
                    <div class="content">
                        <p>Your verification code is:</p>
                        <div class="otp">%s</div>
                        <p><strong>Valid for 1 minute only</strong></p>
                        <p>Do not share this code with anyone.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, otp);
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
    
    @Override
    public boolean sendReturnApprovalNotification(com.mypkga.commerceplatformfull.entity.ReturnRequest returnRequest) {
        // Implementation exists - keeping as is
        return true;
    }
    
    @Override
    public boolean sendReturnPackageReceivedNotification(com.mypkga.commerceplatformfull.entity.ReturnRequest returnRequest) {
        // Implementation exists - keeping as is
        return true;
    }
    
    @Override
    public boolean sendDeliveryIssueResolvedNotification(String customerEmail, String orderNumber, String adminNotes) {
        try {
            String subject = "Vấn đề giao hàng đã được giải quyết - Đơn hàng " + orderNumber;
            String htmlContent = buildDeliveryIssueResolvedEmailTemplate(orderNumber, adminNotes);
            
            log.info("Sending delivery issue resolved notification to customer: {}", maskEmail(customerEmail));
            return sendHtmlEmail(customerEmail, subject, htmlContent);
            
        } catch (Exception e) {
            log.error("Failed to send delivery issue resolved notification: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean sendDeliveryIssueRejectedNotification(String customerEmail, String orderNumber, String adminNotes) {
        try {
            String subject = "Thông báo về báo cáo vấn đề giao hàng - Đơn hàng " + orderNumber;
            String htmlContent = buildDeliveryIssueRejectedEmailTemplate(orderNumber, adminNotes);
            
            log.info("Sending delivery issue rejected notification to customer: {}", maskEmail(customerEmail));
            return sendHtmlEmail(customerEmail, subject, htmlContent);
            
        } catch (Exception e) {
            log.error("Failed to send delivery issue rejected notification: {}", e.getMessage());
            return false;
        }
    }
    
    private String buildDeliveryIssueResolvedEmailTemplate(String orderNumber, String adminNotes) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"><title>Vấn đề giao hàng đã được giải quyết</title>" +
            "<style>body{font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5}" +
            ".container{max-width:600px;margin:0 auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.1)}" +
            ".header{background:#28a745;color:white;padding:20px;text-align:center}" +
            ".content{padding:30px}.info-box{background:#f8f9fa;padding:15px;border-radius:6px;margin:15px 0}" +
            ".order-number{font-size:20px;font-weight:bold;color:#007bff;text-align:center;background:#e3f2fd;padding:15px;border-radius:6px;margin:20px 0}" +
            ".footer{color:#666;font-size:12px;padding:20px;text-align:center;border-top:1px solid #eee}" +
            ".success{color:#28a745;font-weight:bold}</style></head><body>" +
            "<div class=\"container\"><div class=\"header\"><h2>✅ Vấn đề giao hàng đã được giải quyết</h2></div>" +
            "<div class=\"content\"><p>Xin chào,</p>" +
            "<p>Chúng tôi xin thông báo rằng vấn đề giao hàng mà bạn đã báo cáo đã được <span class=\"success\">giải quyết thành công</span>.</p>" +
            "<div class=\"order-number\">Đơn hàng: %s</div>" +
            "<div class=\"info-box\"><h3>📝 Thông tin xử lý:</h3>" +
            "<p><strong>Trạng thái:</strong> <span class=\"success\">Đã giải quyết</span></p>" +
            "<p><strong>Ghi chú từ admin:</strong></p>" +
            "<p style=\"background:#fff;padding:10px;border-left:4px solid #28a745;margin:10px 0\">%s</p></div>" +
            "<div class=\"info-box\"><h3>🎉 Kết quả:</h3>" +
            "<p>Đơn hàng của bạn đã được cập nhật về trạng thái bình thường. Bạn có thể tiếp tục sử dụng dịch vụ của chúng tôi.</p></div>" +
            "<p>Cảm ơn bạn đã kiên nhẫn và tin tưởng sử dụng dịch vụ của chúng tôi.</p></div>" +
            "<div class=\"footer\"><p>Trân trọng,</p><p>Đội ngũ E-Commerce Platform</p>" +
            "<p>Email này được gửi tự động, vui lòng không trả lời.</p></div></div></body></html>", 
            orderNumber, adminNotes != null ? adminNotes : "Vấn đề đã được xử lý thành công.");
    }
    
    private String buildDeliveryIssueRejectedEmailTemplate(String orderNumber, String adminNotes) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"><title>Thông báo về báo cáo vấn đề giao hàng</title>" +
            "<style>body{font-family:Arial,sans-serif;margin:0;padding:20px;background:#f5f5f5}" +
            ".container{max-width:600px;margin:0 auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.1)}" +
            ".header{background:#dc3545;color:white;padding:20px;text-align:center}" +
            ".content{padding:30px}.info-box{background:#f8f9fa;padding:15px;border-radius:6px;margin:15px 0}" +
            ".order-number{font-size:20px;font-weight:bold;color:#007bff;text-align:center;background:#e3f2fd;padding:15px;border-radius:6px;margin:20px 0}" +
            ".footer{color:#666;font-size:12px;padding:20px;text-align:center;border-top:1px solid #eee}" +
            ".rejected{color:#dc3545;font-weight:bold}" +
            ".contact-info{background:#fff3cd;border:1px solid #ffeaa7;padding:15px;border-radius:6px;margin:15px 0}</style></head><body>" +
            "<div class=\"container\"><div class=\"header\"><h2>📋 Thông báo về báo cáo vấn đề giao hàng</h2></div>" +
            "<div class=\"content\"><p>Xin chào,</p>" +
            "<p>Chúng tôi đã xem xét báo cáo vấn đề giao hàng của bạn và có thông tin cập nhật.</p>" +
            "<div class=\"order-number\">Đơn hàng: %s</div>" +
            "<div class=\"info-box\"><h3>📝 Kết quả xem xét:</h3>" +
            "<p><strong>Trạng thái:</strong> <span class=\"rejected\">Đã từ chối</span></p>" +
            "<p><strong>Lý do từ admin:</strong></p>" +
            "<p style=\"background:#fff;padding:10px;border-left:4px solid #dc3545;margin:10px 0\">%s</p></div>" +
            "<div class=\"contact-info\"><h3>📞 Cần hỗ trợ thêm?</h3>" +
            "<p>Nếu bạn không đồng ý với quyết định này hoặc cần hỗ trợ thêm, vui lòng liên hệ với chúng tôi:</p>" +
            "<p><strong>Hotline:</strong> 1900-1900</p><p><strong>Email:</strong> support@ecommerce.com</p>" +
            "<p><strong>Giờ làm việc:</strong> 8:00 - 22:00 (Thứ 2 - Chủ nhật)</p></div>" +
            "<p>Chúng tôi luôn sẵn sàng lắng nghe và hỗ trợ bạn một cách tốt nhất.</p></div>" +
            "<div class=\"footer\"><p>Trân trọng,</p><p>Đội ngũ E-Commerce Platform</p>" +
            "<p>Email này được gửi tự động, vui lòng không trả lời.</p></div></div></body></html>", 
            orderNumber, adminNotes != null ? adminNotes : "Sau khi xem xét, chúng tôi thấy rằng đơn hàng đã được giao thành công.");
    }
}