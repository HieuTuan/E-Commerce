package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.entity.PasswordResetToken;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.PasswordResetTokenRepository;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import com.mypkga.commerceplatformfull.service.EmailService;
import com.mypkga.commerceplatformfull.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.expiry-minutes:30}")
    private int resetExpiryMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public boolean generateAndSendResetToken(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", maskEmail(normalizedEmail));
            // Return true to prevent email enumeration attacks
            return true;
        }

        // Clean up any existing tokens for this email
        tokenRepository.deleteByEmail(normalizedEmail);

        // Generate reset token
        String token = generateResetToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resetExpiryMinutes);

        // Save token
        PasswordResetToken resetToken = new PasswordResetToken(token, normalizedEmail, expiresAt);
        tokenRepository.save(resetToken);

        // Send reset email
        boolean sent = sendResetEmail(normalizedEmail, token);

        if (!sent) {
            log.error("Failed to send password reset email to: {}", maskEmail(normalizedEmail));
            return false;
        }

        log.info("Password reset token generated and sent to: {}", maskEmail(normalizedEmail));
        return true;
    }

    @Override
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            log.warn("Invalid password reset token: {}", token.substring(0, Math.min(token.length(), 8)) + "...");
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        
        if (resetToken.isUsed()) {
            log.warn("Password reset token already used: {}", token.substring(0, Math.min(token.length(), 8)) + "...");
            return false;
        }

        if (resetToken.isExpired()) {
            log.warn("Password reset token expired: {}", token.substring(0, Math.min(token.length(), 8)) + "...");
            return false;
        }

        return true;
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        
        if (resetToken.isUsed() || resetToken.isExpired()) {
            return false;
        }

        // Find user and update password
        Optional<User> userOpt = userRepository.findByEmail(resetToken.getEmail());
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        log.info("Password reset successfully for email: {}", maskEmail(resetToken.getEmail()));
        return true;
    }

    @Override
    public String getEmailByToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        return tokenOpt.map(PasswordResetToken::getEmail).orElse(null);
    }

    private String generateResetToken() {
        // Generate a secure random token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        
        StringBuilder token = new StringBuilder();
        for (byte b : tokenBytes) {
            token.append(String.format("%02x", b));
        }
        
        return token.toString();
    }

    private boolean sendResetEmail(String email, String token) {
        try {
            String resetUrl = "http://localhost:8080/reset-password?token=" + token;
            
            String subject = "Password Reset Request - E-Commerce Platform";
            String htmlContent = buildResetEmailContent(resetUrl);
            
            return emailService.sendHtmlEmail(email, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            return false;
        }
    }

    private String buildResetEmailContent(String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Reset</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .reset-button { display: inline-block; background: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .warning { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Chào bạn!</h2>
                        <p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn trên E-Commerce Platform.</p>
                        
                        <p>Để đặt lại mật khẩu, vui lòng nhấp vào nút bên dưới:</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="reset-button">Đặt Lại Mật Khẩu</a>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Lưu ý quan trọng:</strong>
                            <ul>
                                <li>Link này sẽ hết hạn sau 30 phút</li>
                                <li>Chỉ sử dụng được một lần</li>
                                <li>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này</li>
                            </ul>
                        </div>
                        
                        <p>Nếu nút không hoạt động, bạn có thể copy và paste link sau vào trình duyệt:</p>
                        <p style="word-break: break-all; background: #f1f1f1; padding: 10px; border-radius: 3px;">%s</p>
                    </div>
                    <div class="footer">
                        <p>Trân trọng,<br>Đội ngũ E-Commerce Platform</p>
                        <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetUrl, resetUrl);
    }

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