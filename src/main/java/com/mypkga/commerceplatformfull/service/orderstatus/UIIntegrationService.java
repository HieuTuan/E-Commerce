package com.mypkga.commerceplatformfull.service.orderstatus;

import java.util.List;
public interface UIIntegrationService {
  
    List<StatusOption> getAvailableStatusOptions(Long orderId);
    
    boolean canChangeStatus(Long orderId);
    
    StatusOption getCustomerDeliveryConfirmationOption(Long orderId);
  
    boolean requiresCustomerDeliveryConfirmation(Long orderId);
}