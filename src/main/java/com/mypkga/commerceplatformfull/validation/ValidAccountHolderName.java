package com.mypkga.commerceplatformfull.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for account holder name format.
 * Validates that the name contains only uppercase letters and spaces.
 */
@Documented
@Constraint(validatedBy = AccountHolderNameValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccountHolderName {
    String message() default "Account holder name must be uppercase letters and spaces only";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}