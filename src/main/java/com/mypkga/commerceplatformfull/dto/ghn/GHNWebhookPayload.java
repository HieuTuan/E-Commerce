package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GHNWebhookPayload {
    
    @JsonProperty("OrderCode")
    private String orderCode;
    
    @JsonProperty("Status")
    private String status;
    
    @JsonProperty("StatusText")
    private String statusText;
    
    @JsonProperty("Time")
    private String time;
    
    @JsonProperty("Reason")
    private String reason;
    
    @JsonProperty("ReasonCode")
    private String reasonCode;
    
    @JsonProperty("ShopID")
    private Integer shopId;
    
    @JsonProperty("ClientOrderCode")
    private String clientOrderCode;
    
    @JsonProperty("CODAmount")
    private Integer codAmount;
    
    @JsonProperty("CODTransferDate")
    private String codTransferDate;
    
    @JsonProperty("Weight")
    private Integer weight;
    
    @JsonProperty("Fee")
    private Integer fee;
    
    @JsonProperty("Description")
    private String description;
}