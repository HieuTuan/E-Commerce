package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNFeeRequest {
    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("from_ward_code")
    private String fromWardCode;

    @JsonProperty("service_id")
    private Integer serviceId;

    @JsonProperty("service_type_id")
    private Integer serviceTypeId;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("weight")
    private Integer weight;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("insurance_value")
    private Integer insuranceValue;

    @JsonProperty("cod_failed_amount")
    private Integer codFailedAmount;

    @JsonProperty("coupon")
    private String coupon;

    @JsonProperty("items")
    private List<GHNItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNItem {
        @JsonProperty("name")
        private String name;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("height")
        private Integer height;

        @JsonProperty("weight")
        private Integer weight;

        @JsonProperty("length")
        private Integer length;

        @JsonProperty("width")
        private Integer width;
    }
}