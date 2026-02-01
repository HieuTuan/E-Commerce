package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.dto.ghn.*;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;

/**
 * Service interface for Giao Hàng Nhanh (GHN) API integration
 * Handles return shipment creation, tracking, and status updates
 */
public interface GHNService {
    
    /**
     * Create a return shipment order with GHN
     * @param returnRequest the return request to create shipment for
     * @return GHN order response with tracking information
     */
    GHNOrderResponse createReturnOrder(ReturnRequest returnRequest);
    
    /**
     * Get detailed information about a GHN order
     * @param orderCode the GHN order code
     * @return detailed order information
     */
    GHNOrderInfo getOrderInfo(String orderCode);
    
    /**
     * Cancel a GHN order
     * @param orderCode the GHN order code to cancel
     * @return true if cancellation was successful
     */
    boolean cancelOrder(String orderCode);
    
    /**
     * Calculate shipping fee for a return order
     * @param returnRequest the return request to calculate fee for
     * @return calculated shipping fee
     */
    Integer calculateShippingFee(ReturnRequest returnRequest);
    
    /**
     * Process webhook status update from GHN
     * @param payload the webhook payload from GHN
     */
    void processStatusUpdate(GHNWebhookPayload payload);
    
    /**
     * Map GHN status to internal return status
     * @param ghnStatus the GHN status string
     * @return corresponding internal return status
     */
    String mapGHNStatusToReturnStatus(String ghnStatus);
    
    /**
     * Check if GHN service is available
     * @return true if service is available
     */
    boolean isServiceAvailable();
}