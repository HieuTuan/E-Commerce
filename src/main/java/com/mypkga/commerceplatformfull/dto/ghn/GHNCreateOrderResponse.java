package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GHNCreateOrderResponse {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private GHNOrderData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GHNOrderData {
        @JsonProperty("order_code")
        private String orderCode;

        @JsonProperty("sort_code")
        private String sortCode;

        @JsonProperty("trans_type")
        private String transType;

        @JsonProperty("ward_encode")
        private String wardEncode;

        @JsonProperty("dist_encode")
        private String distEncode;

        // Fee is returned as a complex object, we ignore it for now
        // If needed, create a nested FeeDetail class
        // For now, just use totalFee which is usually an integer
        @JsonProperty("total_fee")
        private Integer totalFee;

        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;
    }
}