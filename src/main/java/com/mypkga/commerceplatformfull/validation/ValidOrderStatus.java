package com.mypkga.commerceplatformfull.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for order status transitions.
 */
@Documented
@Constraint(validatedBy = OrderStatusValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrderStatus {
    String message() default "Invalid order status transition";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}