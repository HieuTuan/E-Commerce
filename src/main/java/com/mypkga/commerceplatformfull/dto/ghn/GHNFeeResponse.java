package com.mypkga.commerceplatformfull.dto.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNFeeResponse {
    private Integer code;
    private String message;
    private GHNFeeData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNFeeData {
        private Integer total;
        private Integer serviceFee;
        private Integer insuranceFee;
        private Integer pickStationFee;
        private Integer couponValue;
        private Integer r2sFee;
        private Integer returnAgain;
        private Integer documentReturn;
        private Integer doubleCheck;
        private Integer codFee;
        private Integer pickRemoteAreasFee;
        private Integer deliverRemoteAreasFee;
        private Integer codFailedFee;
    }
}