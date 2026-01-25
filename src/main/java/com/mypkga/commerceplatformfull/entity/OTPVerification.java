package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OTPVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String otpCode;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean blocked = false;

    // Constructor for creating new OTP verification
    public OTPVerification(String email, String otpCode, LocalDateTime expiresAt) {
        this.email = email;
        this.otpCode = otpCode;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.verified = false;
        this.blocked = false;
    }

    // Helper method to check if OTP is expired
    @Transient
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Helper method to increment attempts
    public void incrementAttempts() {
        this.attempts++;
    }

    // Helper method to check if max attempts reached
    @Transient
    public boolean isMaxAttemptsReached() {
        return attempts >= 3; // Maximum 3 attempts
    }

    // Helper method to mark as verified
    public void markAsVerified() {
        this.verified = true;
    }

    // Helper method to block further attempts
    public void block() {
        this.blocked = true;
    }
}