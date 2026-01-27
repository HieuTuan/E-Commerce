package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.OTPVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTPVerification, Long> {

    // Find the latest OTP for an email
    Optional<OTPVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    // Find all OTPs for an email
    List<OTPVerification> findByEmailOrderByCreatedAtDesc(String email);

    // Find active (non-expired, non-verified) OTP for an email - get the latest one only
    @Query("SELECT o FROM OTPVerification o WHERE o.email = :email " +
           "AND o.verified = false AND o.expiresAt > :currentTime " +
           "ORDER BY o.createdAt DESC")
    List<OTPVerification> findActiveOTPsByEmail(@Param("email") String email, 
                                               @Param("currentTime") LocalDateTime currentTime);
    
    // Helper method to get the latest active OTP
    default Optional<OTPVerification> findActiveOTPByEmail(String email, LocalDateTime currentTime) {
        List<OTPVerification> otps = findActiveOTPsByEmail(email, currentTime);
        return otps.isEmpty() ? Optional.empty() : Optional.of(otps.get(0));
    }

    // Count OTP requests for an email within a time period (for rate limiting)
    @Query("SELECT COUNT(o) FROM OTPVerification o WHERE o.email = :email " +
           "AND o.createdAt > :since")
    long countOTPRequestsSince(@Param("email") String email, 
                              @Param("since") LocalDateTime since);

    // Find verified OTP for an email
    Optional<OTPVerification> findByEmailAndVerifiedTrue(String email);

    // Delete expired OTPs (cleanup)
    @Modifying
    @Query("DELETE FROM OTPVerification o WHERE o.expiresAt < :currentTime")
    void deleteExpiredOTPs(@Param("currentTime") LocalDateTime currentTime);

    // Delete all OTPs for an email
    @Modifying
    @Query("DELETE FROM OTPVerification o WHERE o.email = :email")
    void deleteByEmail(@Param("email") String email);

    // Check if email has recent failed attempts (for blocking logic)
    @Query("SELECT COUNT(o) FROM OTPVerification o WHERE o.email = :email " +
           "AND o.attempts >= 5 AND o.createdAt > :since")
    long countRecentFailedAttempts(@Param("email") String email, 
                                  @Param("since") LocalDateTime since);
    
    // Check if email is blocked based on recent failed attempts
    default boolean isEmailBlocked(String email, LocalDateTime since) {
        return countRecentFailedAttempts(email, since) > 0;
    }
}