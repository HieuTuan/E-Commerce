package com.mypkga.commerceplatformfull.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * Validator for bank account number format.
 * Validates that the account number contains only digits and is between 10-20 characters.
 */
@Component
public class BankAccountValidator implements ConstraintValidator<ValidBankAccount, String> {

    @Override
    public void initialize(ValidBankAccount constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String accountNumber, ConstraintValidatorContext context) {
        if (accountNumber == null) {
            return true; // Let @NotBlank handle null validation
        }
        
        // Check if account number contains only digits and is between 10-20 characters
        return accountNumber.matches("\\d{10,20}");
    }
}