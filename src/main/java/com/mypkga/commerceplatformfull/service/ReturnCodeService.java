package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.ReturnRequest;

/**
 * Service interface for generating return codes with QR code functionality.
 * This service handles the creation of unique return codes that contain all
 * required information for return processing including product details,
 * addresses, and post office information.
 */
public interface ReturnCodeService {
    
    /**
     * Generate a unique return code for a return request.
     * The return code contains all necessary information for return processing.
     * 
     * @param returnRequest The return request to generate a code for
     * @return The generated unique return code string
     */
    String generateReturnCode(ReturnRequest returnRequest);
    
    /**
     * Generate a QR code image for a return code.
     * The QR code contains all required information in a scannable format.
     * 
     * @param returnCode The return code to encode in QR format
     * @param returnRequest The return request containing additional information
     * @return Base64 encoded QR code image
     */
    String generateQRCode(String returnCode, ReturnRequest returnRequest);
    
    /**
     * Validate if a return code is valid and properly formatted.
     * 
     * @param returnCode The return code to validate
     * @return true if the return code is valid, false otherwise
     */
    boolean isValidReturnCode(String returnCode);
    
    /**
     * Extract return request information from a return code.
     * This is used when scanning QR codes to retrieve the original request data.
     * 
     * @param returnCode The return code to decode
     * @return Decoded return information as a formatted string
     */
    String decodeReturnCode(String returnCode);
}