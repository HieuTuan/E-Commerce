package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class representing bank information for refund processing.
 * This contains the customer's banking details needed for refund transfers.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundBankInfo {
    
    @NotBlank(message = "Bank name is required")
    @Column(name = "bank_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String bankName;
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{10,20}", message = "Account number must be 10-20 digits")
    @Column(name = "account_number", nullable = false, columnDefinition = "NVARCHAR(50)")
    private String accountNumber;
    
    @NotBlank(message = "Account holder name is required")
    @Pattern(regexp = "[A-Z\\s]+", message = "Account holder name must be uppercase letters and spaces only")
    @Column(name = "account_holder_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String accountHolderName;
}