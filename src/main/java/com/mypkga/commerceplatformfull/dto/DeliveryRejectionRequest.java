package com.mypkga.commerceplatformfull.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for delivery rejection requests with validation.
 */
@Data
public class DeliveryRejectionRequest {
    
    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, max = 500, message = "Rejection reason must be between 10 and 500 characters")
    private String reason;
}