package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.api-url}")
    private String apiUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    public String createPaymentUrl(Order order) {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_OrderInfo = "Thanh toan don hang " + order.getOrderNumber();
            String orderType = "other";
            String vnp_TxnRef = order.getOrderNumber();
            // Lấy địa chỉ IP (Hardcode 127.0.0.1 để chạy local, lên server cần lấy request.getRemoteAddr())
            String vnp_IpAddr = "127.0.0.1";
            String vnp_TmnCode = tmnCode;

            // Số tiền nhân 100 và chuyển sang int/long
            long amount = order.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue();

            Map<String, String> vnp_Params = new HashMap<>();

            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", orderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", returnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());

            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);


            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    // Quan trọng: Dùng UTF-8 thay vì US_ASCII
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    // Build query url
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String paymentUrl = apiUrl + "?" + queryUrl;
            log.info("Payment URL: " + paymentUrl); // In ra log để debug nếu cần
            return paymentUrl;

        } catch (Exception e) {
            log.error("Error creating VNPay payment URL", e);
            throw new RuntimeException("Error creating payment URL");
        }

    }

    public boolean verifyPaymentResponse(Map<String, String> params) {
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            if (params.containsKey("vnp_SecureHashType")) params.remove("vnp_SecureHashType");
            if (params.containsKey("vnp_SecureHash")) params.remove("vnp_SecureHash");

            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            String signValue = hmacSHA512(hashSecret, hashData.toString());
            return signValue.equals(vnp_SecureHash);

        } catch (Exception e) {
            log.error("Error verifying VNPay response", e);
            return false;
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : result) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC SHA512", e);
            return "";
        }
    }


}