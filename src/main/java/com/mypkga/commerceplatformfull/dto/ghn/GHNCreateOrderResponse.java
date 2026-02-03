package com.mypkga.commerceplatformfull.dto.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNCreateOrderResponse {
    private Integer code;
    private String message;
    private GHNOrderData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNOrderData {
        private String orderCode;
        private String sortCode;
        private String transType;
        private String wardEncode;
        private String distEncode;
        private Integer fee;
        private Integer totalFee;
        private String expectedDeliveryTime;
    }
}