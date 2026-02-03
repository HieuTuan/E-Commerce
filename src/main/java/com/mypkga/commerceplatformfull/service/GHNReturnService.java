package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.dto.ghn.*;
import com.mypkga.commerceplatformfull.entity.ReturnRequest;

/**
 * Service for integrating with GHN (Giao Hang Nhanh) API for return shipping
 */
public interface GHNReturnService {
    
    /**
     * Calculate shipping fee for return order from customer to shop
     * 
     * @param returnRequest the return request containing customer and order info
     * @return GHN fee response with calculated shipping cost
     */
    GHNFeeResponse calculateReturnShippingFee(ReturnRequest returnRequest);
    
    /**
     * Create return shipping order with GHN
     * 
     * @param returnRequest the return request to create shipping for
     * @return GHN order response with tracking information
     */
    GHNCreateOrderResponse createReturnShippingOrder(ReturnRequest returnRequest);
    
    /**
     * Get order status from GHN
     * 
     * @param orderCode GHN order code
     * @return order status information
     */
    String getOrderStatus(String orderCode);
    
    /**
     * Cancel GHN order
     * 
     * @param orderCode GHN order code to cancel
     * @return true if cancelled successfully
     */
    boolean cancelOrder(String orderCode);
}