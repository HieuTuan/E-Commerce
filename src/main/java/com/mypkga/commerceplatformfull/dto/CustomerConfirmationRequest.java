package com.mypkga.commerceplatformfull.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer delivery confirmation requests.
 * 
 * This DTO is used when customers confirm receipt of their delivered orders.
 * It includes optional notes that customers can provide about the delivery.
 * 
 * Requirements: 1.5, 4.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerConfirmationRequest {
    
    /**
     * Optional notes from the customer about the delivery.
     * Limited to 500 characters to prevent excessive data.
     */
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    /**
     * Optional rating for the delivery experience (1-5).
     * Can be used for delivery service quality tracking.
     */
    private Integer deliveryRating;
}