package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.service.SMSService;
import com.mypkga.commerceplatformfull.util.PhoneNumberValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS Service implementation
 * Currently uses mock implementation for development
 * Can be extended to integrate with real SMS providers like Twilio, etc.
 */
@Service
@Slf4j
public class SMSServiceImpl implements SMSService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.sms.provider:mock}")
    private String smsProvider;

    @Override
    public boolean sendMessage(String phoneNumber, String message) {
        if (!isValidPhoneNumber(phoneNumber)) {
            log.error("Invalid phone number: {}", PhoneNumberValidator.maskPhoneNumber(phoneNumber));
            return false;
        }

        String formattedNumber = formatPhoneNumber(phoneNumber);
        
        if (mockMode || !smsEnabled) {
            log.info("MOCK SMS - Sending message to {}: {}", 
                    PhoneNumberValidator.maskPhoneNumber(formattedNumber), message);
            return true;
        }

        // Real SMS implementation would go here
        return sendRealSMS(formattedNumber, message);
    }

    @Override
    public boolean sendOTP(String phoneNumber, String otp) {
        String message = String.format(
            "Mã xác thực của bạn là: %s. Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với ai.", 
            otp
        );
        return sendMessage(phoneNumber, message);
    }

    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        return PhoneNumberValidator.isValidPhoneNumber(phoneNumber);
    }

    @Override
    public String formatPhoneNumber(String phoneNumber) {
        return PhoneNumberValidator.formatToInternational(phoneNumber);
    }

    /**
     * Send real SMS using configured provider
     * This method should be implemented based on the chosen SMS provider
     */
    private boolean sendRealSMS(String phoneNumber, String message) {
        switch (smsProvider.toLowerCase()) {
            case "twilio":
                return sendViaTwilio(phoneNumber, message);
            case "esms":
                return sendViaESMS(phoneNumber, message);
            default:
                log.warn("Unknown SMS provider: {}. Using mock mode.", smsProvider);
                return true;
        }
    }

    /**
     * Send SMS via Twilio
     * Implementation placeholder - requires Twilio SDK
     */
    private boolean sendViaTwilio(String phoneNumber, String message) {
        log.info("Sending SMS via Twilio to {}", PhoneNumberValidator.maskPhoneNumber(phoneNumber));
        // TODO: Implement Twilio integration
        // Requires: com.twilio.sdk:twilio dependency
        return true;
    }

    /**
     * Send SMS via eSMS (Vietnamese SMS provider)
     * Implementation placeholder
     */
    private boolean sendViaESMS(String phoneNumber, String message) {
        log.info("Sending SMS via eSMS to {}", PhoneNumberValidator.maskPhoneNumber(phoneNumber));
        // TODO: Implement eSMS integration
        return true;
    }
}