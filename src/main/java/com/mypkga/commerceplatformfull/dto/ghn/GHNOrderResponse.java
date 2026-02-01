package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GHNOrderResponse {
    private Integer code;
    private String message;
    private GHNOrderData data;
    
    @Data
    public static class GHNOrderData {
        @JsonProperty("order_code")
        private String orderCode;
        
        @JsonProperty("sort_code")
        private String sortCode;
        
        @JsonProperty("trans_type")
        private String transType;
        
        @JsonProperty("ward_encode")
        private String wardEncode;
        
        @JsonProperty("district_encode")
        private String districtEncode;
        
        private GHNFee fee;
        
        @JsonProperty("total_fee")
        private Integer totalFee;
        
        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;
    }
    
    @Data
    public static class GHNFee {
        private Integer main;
        private Integer insurance;
        
        @JsonProperty("cod_fee")
        private Integer codFee;
        
        @JsonProperty("station_do")
        private Integer stationDo;
        
        @JsonProperty("station_pu")
        private Integer stationPu;
        
        @JsonProperty("return")
        private Integer returnFee;
        
        private Integer r2s;
        
        @JsonProperty("return_again")
        private Integer returnAgain;
        
        private Integer coupon;
    }
}