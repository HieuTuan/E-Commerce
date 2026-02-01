package com.mypkga.commerceplatformfull.dto;

import com.mypkga.commerceplatformfull.entity.ReturnReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for creating a return request with all required validation.
 * This contains all information needed to submit a return request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequestDto {
    
    @NotNull(message = "Return reason is required")
    private ReturnReason reason;
    
    @NotBlank(message = "Detailed description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String detailedDescription;
    
    @NotNull(message = "Evidence video is required")
    private MultipartFile evidenceVideo;
    
    @Valid
    @NotNull(message = "Bank information is required")
    private RefundBankInfoDto bankInfo;
}