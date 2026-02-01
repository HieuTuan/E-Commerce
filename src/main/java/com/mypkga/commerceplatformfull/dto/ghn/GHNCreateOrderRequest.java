package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class GHNCreateOrderRequest {
    
    @JsonProperty("payment_type_id")
    private Integer paymentTypeId = 2; // Người gửi trả phí
    
    private String note;
    
    @JsonProperty("required_note")
    private String requiredNote = "KHONGCHOXEMHANG";
    
    @JsonProperty("return_phone")
    private String returnPhone;
    
    @JsonProperty("return_address")
    private String returnAddress;
    
    @JsonProperty("return_district_id")
    private Integer returnDistrictId;
    
    @JsonProperty("return_ward_code")
    private String returnWardCode;
    
    @JsonProperty("client_order_code")
    private String clientOrderCode;
    
    @JsonProperty("to_district_id")
    private Integer toDistrictId;
    
    @JsonProperty("to_ward_code")
    private String toWardCode;
    
    @JsonProperty("to_name")
    private String toName;
    
    @JsonProperty("to_phone")
    private String toPhone;
    
    @JsonProperty("to_address")
    private String toAddress;
    
    @JsonProperty("cod_amount")
    private Integer codAmount = 0;
    
    private String content;
    
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    
    @JsonProperty("pick_station_id")
    private Integer pickStationId;
    
    @JsonProperty("deliver_station_id")
    private Integer deliverStationId;
    
    @JsonProperty("insurance_value")
    private Integer insuranceValue;
    
    @JsonProperty("service_id")
    private Integer serviceId;
    
    @JsonProperty("service_type_id")
    private Integer serviceTypeId = 2;
    
    private String coupon;
    
    private List<GHNItem> items;
}