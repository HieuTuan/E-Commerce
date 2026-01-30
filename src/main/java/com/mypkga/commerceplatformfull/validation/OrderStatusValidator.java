package com.mypkga.commerceplatformfull.validation;

import com.mypkga.commerceplatformfull.dto.StatusUpdateRequest;
import com.mypkga.commerceplatformfull.entity.OrderStatus;
import com.mypkga.commerceplatformfull.service.OrderTimelineService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validator for order status transitions.
 */
@Component
@RequiredArgsConstructor
public class OrderStatusValidator implements ConstraintValidator<ValidOrderStatus, StatusUpdateRequest> {

    private final OrderTimelineService orderTimelineService;

    @Override
    public void initialize(ValidOrderStatus constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(StatusUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getStatus() == null) {
            return false;
        }

        // Basic validation - ensure status is a valid enum value
        try {
            OrderStatus.valueOf(request.getStatus().name());
            return true;
        } catch (IllegalArgumentException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid order status: " + request.getStatus())
                    .addConstraintViolation();
            return false;
        }
    }
}