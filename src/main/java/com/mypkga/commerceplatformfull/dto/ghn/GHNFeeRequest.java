package com.mypkga.commerceplatformfull.dto.ghn;

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
    private Integer fromDistrictId;
    private String fromWardCode;
    private Integer serviceId;
    private Integer serviceTypeId;
    private Integer toDistrictId;
    private String toWardCode;
    private Integer height;
    private Integer length;
    private Integer weight;
    private Integer width;
    private Integer insuranceValue;
    private Integer codFailedAmount;
    private String coupon;
    private List<GHNItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNItem {
        private String name;
        private Integer quantity;
        private Integer height;
        private Integer weight;
        private Integer length;
        private Integer width;
    }
}