package com.mypkga.commerceplatformfull.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;
import com.mypkga.commerceplatformfull.entity.ReturnReason;
import com.mypkga.commerceplatformfull.entity.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for return request responses.
 * Contains all information about a return request for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReturnRequestDto {
    
    private Long id;
    private Long orderId;
    private ReturnReason reason;
    private String detailedDescription;
    private String evidenceVideoUrl;
    private String returnCode;
    private ReturnStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // GHN shipping information
    private String ghnOrderCode;
    private String ghnTrackingNumber;
    private String ghnStatus;
    
    // Bank information
    private RefundBankInfoDto bankInfo;
    
    /**
     * Create DTO from entity
     */
    public static ReturnRequestDto from(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            return null;
        }
        
        return ReturnRequestDto.builder()
                .id(returnRequest.getId())
                .orderId(returnRequest.getOrder() != null ? returnRequest.getOrder().getId() : null)
                .reason(returnRequest.getReason())
                .detailedDescription(returnRequest.getDetailedDescription())
                .evidenceVideoUrl(returnRequest.getEvidenceVideoUrl())
                .returnCode(returnRequest.getReturnCode())
                .status(returnRequest.getStatus())
                .rejectionReason(returnRequest.getRejectionReason())
                .createdAt(returnRequest.getCreatedAt())
                .updatedAt(returnRequest.getUpdatedAt())
                .ghnOrderCode(returnRequest.getGhnOrderCode())
                .ghnTrackingNumber(returnRequest.getGhnTrackingNumber())
                .ghnStatus(returnRequest.getGhnStatus())
                .bankInfo(RefundBankInfoDto.from(returnRequest.getBankInfo()))
                .build();
    }
}