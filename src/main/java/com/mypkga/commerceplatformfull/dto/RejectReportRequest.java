package com.mypkga.commerceplatformfull.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectReportRequest {
    
    @NotBlank(message = "Reason is required when rejecting a report")
    private String reason;
}