package com.mypkga.commerceplatformfull.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for bank account number format.
 * Validates that the account number contains only digits and is between 10-20 characters.
 */
@Documented
@Constraint(validatedBy = BankAccountValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBankAccount {
    String message() default "Account number must be 10-20 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}