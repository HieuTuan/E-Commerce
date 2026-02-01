package com.mypkga.commerceplatformfull.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mypkga.commerceplatformfull.service.ReturnEligibilityService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for return eligibility check responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EligibilityDto {
    
    private boolean eligible;
    private String reason;
    private Long remainingHours;
    
    /**
     * Create DTO from eligibility result
     */
    public static EligibilityDto from(ReturnEligibilityService.EligibilityResult result, Long remainingHours) {
        return EligibilityDto.builder()
                .eligible(result.isEligible())
                .reason(result.getReason())
                .remainingHours(remainingHours)
                .build();
    }
}