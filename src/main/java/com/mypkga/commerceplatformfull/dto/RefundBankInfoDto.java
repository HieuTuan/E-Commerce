package com.mypkga.commerceplatformfull.dto;

import com.mypkga.commerceplatformfull.entity.RefundBankInfo;
import com.mypkga.commerceplatformfull.validation.ValidBankAccount;
import com.mypkga.commerceplatformfull.validation.ValidAccountHolderName;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for refund bank information with validation rules.
 * This contains the customer's banking details needed for refund transfers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundBankInfoDto {
    
    @NotBlank(message = "Bank name is required")
    private String bankName;
    
    @NotBlank(message = "Account number is required")
    @ValidBankAccount(message = "Account number must be 10-20 digits")
    private String accountNumber;
    
    @NotBlank(message = "Account holder name is required")
    @ValidAccountHolderName(message = "Account holder name must be uppercase letters and spaces only")
    private String accountHolderName;
    
    /**
     * Create DTO from entity
     */
    public static RefundBankInfoDto from(RefundBankInfo bankInfo) {
        if (bankInfo == null) {
            return null;
        }
        
        return new RefundBankInfoDto(
                bankInfo.getBankName(),
                bankInfo.getAccountNumber(),
                bankInfo.getAccountHolderName()
        );
    }
}