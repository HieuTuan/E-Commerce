package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a delivery issue report from customer
 */
@Entity
@Table(name = "delivery_issue_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryIssueReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;
    
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;
    
    @Column(name = "issue_type", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String issueType;
    
    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private String resolvedBy;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    public enum ReportStatus {
        PENDING("Đang xử lý"),
        RESOLVED("Đã giải quyết"),
        REJECTED("Đã từ chối");
        
        private final String displayName;
        
        ReportStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}