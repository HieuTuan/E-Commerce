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
public class GHNCreateReturnOrderRequest {
    private Integer paymentTypeId;
    private String note;
    private String requiredNote;
    private String fromName;
    private String fromPhone;
    private String fromAddress;
    private String fromWardName;
    private String fromDistrictName;
    private String fromProvinceName;
    private String returnPhone;
    private String returnAddress;
    private Integer returnDistrictId;
    private String returnWardCode;
    private String clientOrderCode;
    private String toName;
    private String toPhone;
    private String toAddress;
    private String toWardCode;
    private Integer toDistrictId;
    private Integer codAmount;
    private String content;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer pickStationId;
    private Integer deliverStationId;
    private Integer insuranceValue;
    private Integer serviceId;
    private Integer serviceTypeId;
    private String coupon;
    private List<Integer> pickShift;
    private List<GHNReturnItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNReturnItem {
        private String name;
        private String code;
        private Integer quantity;
        private Integer price;
        private Integer length;
        private Integer width;
        private Integer height;
        private Integer weight;
        private GHNCategory category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNCategory {
        private String level1;
    }
}