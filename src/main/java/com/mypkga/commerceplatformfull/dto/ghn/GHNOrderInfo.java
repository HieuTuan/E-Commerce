package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GHNOrderInfo {
    private Integer code;
    private String message;
    private GHNOrderInfoData data;
    
    @Data
    public static class GHNOrderInfoData {
        @JsonProperty("order_code")
        private String orderCode;
        
        @JsonProperty("sort_code")
        private String sortCode;
        
        private String status;
        
        @JsonProperty("status_text")
        private String statusText;
        
        @JsonProperty("created_date")
        private String createdDate;
        
        @JsonProperty("updated_date")
        private String updatedDate;
        
        @JsonProperty("pick_date")
        private String pickDate;
        
        @JsonProperty("deliver_date")
        private String deliverDate;
        
        @JsonProperty("customer_fullname")
        private String customerFullname;
        
        @JsonProperty("customer_tel")
        private String customerTel;
        
        private String address;
        
        private String content;
        
        private Integer weight;
        
        @JsonProperty("converted_weight")
        private Integer convertedWeight;
        
        @JsonProperty("service_type_id")
        private Integer serviceTypeId;
        
        @JsonProperty("payment_type_id")
        private Integer paymentTypeId;
        
        @JsonProperty("custom_service_fee")
        private Integer customServiceFee;
        
        @JsonProperty("cod_amount")
        private Integer codAmount;
        
        @JsonProperty("cod_collect_date")
        private String codCollectDate;
        
        @JsonProperty("cod_transfer_date")
        private String codTransferDate;
        
        @JsonProperty("is_cod_transferred")
        private Boolean isCodTransferred;
        
        @JsonProperty("is_cod_collected")
        private Boolean isCodCollected;
        
        @JsonProperty("insurance_value")
        private Integer insuranceValue;
        
        @JsonProperty("order_value")
        private Integer orderValue;
        
        @JsonProperty("pick_station_id")
        private Integer pickStationId;
        
        @JsonProperty("deliver_station_id")
        private Integer deliverStationId;
        
        @JsonProperty("money_collect_date")
        private String moneyCollectDate;
        
        @JsonProperty("money_transfer_date")
        private String moneyTransferDate;
        
        @JsonProperty("cancel_date")
        private String cancelDate;
        
        @JsonProperty("return_date")
        private String returnDate;
        
        @JsonProperty("warehousing_date")
        private String warehousingDate;
        
        @JsonProperty("leadtime")
        private String leadtime;
        
        @JsonProperty("order_date")
        private String orderDate;
        
        private List<GHNLogEntry> logs;
    }
    
    @Data
    public static class GHNLogEntry {
        private String status;
        
        @JsonProperty("updated_date")
        private String updatedDate;
        
        private String reason;
        
        @JsonProperty("reason_code")
        private String reasonCode;
    }
}