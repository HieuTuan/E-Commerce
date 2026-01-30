package com.mypkga.commerceplatformfull.service.orderstatus;

import com.mypkga.commerceplatformfull.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UIIntegrationServiceImpl implements UIIntegrationService {
    
    private final StateTransitionValidator stateTransitionValidator;
    private final OrderStatusManager orderStatusManager;
  
    @Override
    public List<StatusOption> getAvailableStatusOptions(Long orderId) {
        try {
            // Validate input parameter
            if (orderId == null) {
                log.warn("getAvailableStatusOptions called with null orderId");
                return List.of();
            }
            
            if (orderId <= 0) {
                log.warn("getAvailableStatusOptions called with invalid orderId: {}", orderId);
                return List.of();
            }
            
            log.debug("Getting available status options for order: {}", orderId);
            
            // Get current status of the order
            OrderStatus currentStatus;
            try {
                currentStatus = orderStatusManager.getCurrentStatus(orderId);
            } catch (IllegalArgumentException e) {
                log.warn("Order not found when getting status options for orderId: {}", orderId);
                return List.of();
            }
            
            // Check if order is in final state
            if (currentStatus.isFinalState()) {
                log.debug("Order {} is in final state {}, no status options available", orderId, currentStatus);
                return List.of();
            }
            
            // Get valid next states from validator
            List<OrderStatus> validNextStates = stateTransitionValidator.getValidNextStates(currentStatus);
            
            // Convert to StatusOption objects for UI display
            List<StatusOption> statusOptions = validNextStates.stream()
                .map(StatusOption::fromOrderStatus)
                .collect(Collectors.toList());
            
            log.debug("Found {} available status options for order {}: {}", 
                statusOptions.size(), orderId, 
                statusOptions.stream().map(option -> option.getStatus().name()).collect(Collectors.toList()));
            
            return statusOptions;
            
        } catch (Exception e) {
            log.error("Error getting available status options for order {}: {}", orderId, e.getMessage(), e);
            return List.of();
        }
    }
    
    @Override
    public boolean canChangeStatus(Long orderId) {
        try {
            // Validate input parameter
            if (orderId == null) {
                log.warn("canChangeStatus called with null orderId");
                return false;
            }
            
            if (orderId <= 0) {
                log.warn("canChangeStatus called with invalid orderId: {}", orderId);
                return false;
            }
            
            log.debug("Checking if status can be changed for order: {}", orderId);
            
            // Get current status of the order
            OrderStatus currentStatus;
            try {
                currentStatus = orderStatusManager.getCurrentStatus(orderId);
            } catch (IllegalArgumentException e) {
                log.warn("Order not found when checking if status can be changed for orderId: {}", orderId);
                return false;
            }
            
            // Check if order is in final state
            boolean canChange = !currentStatus.isFinalState();
            
            log.debug("Order {} with status {} can change status: {}", orderId, currentStatus, canChange);
            
            return canChange;
            
        } catch (Exception e) {
            log.error("Error checking if status can be changed for order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public StatusOption getCustomerDeliveryConfirmationOption(Long orderId) {
        try {
            // Validate input parameter
            if (orderId == null) {
                log.warn("getCustomerDeliveryConfirmationOption called with null orderId");
                return null;
            }
            
            if (orderId <= 0) {
                log.warn("getCustomerDeliveryConfirmationOption called with invalid orderId: {}", orderId);
                return null;
            }
            
            log.debug("Getting customer delivery confirmation option for order: {}", orderId);
            
            // Get current status of the order
            OrderStatus currentStatus;
            try {
                currentStatus = orderStatusManager.getCurrentStatus(orderId);
            } catch (IllegalArgumentException e) {
                log.warn("Order not found when getting customer delivery confirmation option for orderId: {}", orderId);
                return null;
            }
            
            // Only provide confirmation option if order is DELIVERED
            if (currentStatus != OrderStatus.DELIVERED) {
                log.debug("Order {} is not in DELIVERED status (current: {}), no customer confirmation option", 
                    orderId, currentStatus);
                return null;
            }
            
            // Check if CONFIRMED_BY_CUSTOMER is a valid next state
            if (!stateTransitionValidator.isValidTransition(currentStatus, OrderStatus.CONFIRMED_BY_CUSTOMER)) {
                log.debug("Transition from {} to CONFIRMED_BY_CUSTOMER is not valid for order {}", 
                    currentStatus, orderId);
                return null;
            }
            
            StatusOption confirmationOption = StatusOption.createCustomerDeliveryConfirmation();
            
            log.debug("Created customer delivery confirmation option for order {}", orderId);
            
            return confirmationOption;
            
        } catch (Exception e) {
            log.error("Error getting customer delivery confirmation option for order {}: {}", 
                orderId, e.getMessage(), e);
            return null;
        }
    }
  
    @Override
    public boolean requiresCustomerDeliveryConfirmation(Long orderId) {
        try {
            // Validate input parameter
            if (orderId == null) {
                log.warn("requiresCustomerDeliveryConfirmation called with null orderId");
                return false;
            }
            
            if (orderId <= 0) {
                log.warn("requiresCustomerDeliveryConfirmation called with invalid orderId: {}", orderId);
                return false;
            }
            
            log.debug("Checking if customer delivery confirmation is required for order: {}", orderId);
            
            // Get current status of the order
            OrderStatus currentStatus;
            try {
                currentStatus = orderStatusManager.getCurrentStatus(orderId);
            } catch (IllegalArgumentException e) {
                log.warn("Order not found when checking customer delivery confirmation requirement for orderId: {}", orderId);
                return false;
            }
            
            // Customer confirmation is required if order is DELIVERED and can transition to CONFIRMED_BY_CUSTOMER
            boolean requiresConfirmation = currentStatus == OrderStatus.DELIVERED && 
                stateTransitionValidator.isValidTransition(currentStatus, OrderStatus.CONFIRMED_BY_CUSTOMER);
            
            log.debug("Order {} with status {} requires customer delivery confirmation: {}", 
                orderId, currentStatus, requiresConfirmation);
            
            return requiresConfirmation;
            
        } catch (Exception e) {
            log.error("Error checking if customer delivery confirmation is required for order {}: {}", 
                orderId, e.getMessage(), e);
            return false;
        }
    }
}