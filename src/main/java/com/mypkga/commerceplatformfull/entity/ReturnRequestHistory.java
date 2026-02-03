package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track history of return request status changes and actions
 */
@Entity
@Table(name = "return_request_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ReturnStatus previousStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ReturnStatus newStatus;
    
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // APPROVE, REJECT, SHIP, RECEIVE, REFUND, etc.
    
    @Column(name = "notes", columnDefinition = "NVARCHAR(1000)")
    private String notes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // GHN related fields
    @Column(name = "ghn_order_code", length = 50)
    private String ghnOrderCode;
    
    @Column(name = "ghn_fee")
    private Integer ghnFee;
    
    @Column(name = "ghn_tracking_url", length = 500)
    private String ghnTrackingUrl;
    
    // Static factory methods for common actions
    public static ReturnRequestHistory createApprovalHistory(ReturnRequest returnRequest, User staff, String ghnOrderCode, Integer ghnFee) {
        ReturnRequestHistory history = new ReturnRequestHistory();
        history.setReturnRequest(returnRequest);
        history.setPreviousStatus(ReturnStatus.REFUND_REQUESTED);
        history.setNewStatus(ReturnStatus.RETURN_APPROVED);
        history.setActionType("APPROVE");
        history.setNotes("Yêu cầu hoàn hàng đã được duyệt. Đơn vận chuyển GHN: " + ghnOrderCode);
        history.setPerformedBy(staff);
        history.setGhnOrderCode(ghnOrderCode);
        history.setGhnFee(ghnFee);
        return history;
    }
    
    public static ReturnRequestHistory createRejectionHistory(ReturnRequest returnRequest, User staff, String reason) {
        ReturnRequestHistory history = new ReturnRequestHistory();
        history.setReturnRequest(returnRequest);
        history.setPreviousStatus(ReturnStatus.REFUND_REQUESTED);
        history.setNewStatus(ReturnStatus.REFUND_REJECTED);
        history.setActionType("REJECT");
        history.setNotes("Yêu cầu hoàn hàng bị từ chối. Lý do: " + reason);
        history.setPerformedBy(staff);
        return history;
    }
    
    public static ReturnRequestHistory createShippingHistory(ReturnRequest returnRequest, String ghnOrderCode) {
        ReturnRequestHistory history = new ReturnRequestHistory();
        history.setReturnRequest(returnRequest);
        history.setPreviousStatus(ReturnStatus.RETURN_APPROVED);
        history.setNewStatus(ReturnStatus.RETURNING);
        history.setActionType("SHIP");
        history.setNotes("Khách hàng đã gửi hàng hoàn trả. Mã vận đơn: " + ghnOrderCode);
        history.setGhnOrderCode(ghnOrderCode);
        return history;
    }
    
    public static ReturnRequestHistory createReceiptHistory(ReturnRequest returnRequest, User staff) {
        ReturnRequestHistory history = new ReturnRequestHistory();
        history.setReturnRequest(returnRequest);
        history.setPreviousStatus(ReturnStatus.RETURNING);
        history.setNewStatus(ReturnStatus.RETURN_RECEIVED);
        history.setActionType("RECEIVE");
        history.setNotes("Đã nhận được hàng hoàn trả từ khách hàng");
        history.setPerformedBy(staff);
        return history;
    }
    
    public static ReturnRequestHistory createRefundHistory(ReturnRequest returnRequest, User staff) {
        ReturnRequestHistory history = new ReturnRequestHistory();
        history.setReturnRequest(returnRequest);
        history.setPreviousStatus(ReturnStatus.RETURN_RECEIVED);
        history.setNewStatus(ReturnStatus.REFUNDED);
        history.setActionType("REFUND");
        history.setNotes("Đã hoàn tiền thành công cho khách hàng");
        history.setPerformedBy(staff);
        return history;
    }
}