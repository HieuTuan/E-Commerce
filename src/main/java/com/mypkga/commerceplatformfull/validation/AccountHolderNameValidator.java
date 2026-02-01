package com.mypkga.commerceplatformfull.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * Validator for account holder name format.
 * Validates that the name contains only uppercase letters and spaces, without accents.
 */
@Component
public class AccountHolderNameValidator implements ConstraintValidator<ValidAccountHolderName, String> {

    @Override
    public void initialize(ValidAccountHolderName constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String accountHolderName, ConstraintValidatorContext context) {
        if (accountHolderName == null) {
            return true; // Let @NotBlank handle null validation
        }
        
        // Check if name contains only uppercase letters and spaces
        // This pattern allows uppercase A-Z and spaces, no accents or special characters
        return accountHolderName.matches("[A-Z\\s]+") && 
               !accountHolderName.trim().isEmpty() && // Not just spaces
               !accountHolderName.startsWith(" ") && // No leading spaces
               !accountHolderName.endsWith(" ") && // No trailing spaces
               !accountHolderName.contains("  "); // No double spaces
    }
}