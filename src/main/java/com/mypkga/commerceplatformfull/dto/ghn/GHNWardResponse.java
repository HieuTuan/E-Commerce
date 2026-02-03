package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GHNWardResponse {
    private Integer code;
    private String message;
    private List<WardData> data;

    @Data
    public static class WardData {
        @JsonProperty("WardCode")
        private String wardCode;

        @JsonProperty("DistrictID")
        private Integer districtID;

        @JsonProperty("WardName")
        private String wardName;

        @JsonProperty("NameExtension")
        private List<String> nameExtension;

        @JsonProperty("CanUpdateCOD")
        private Boolean canUpdateCOD;

        @JsonProperty("SupportType")
        private Integer supportType;

        @JsonProperty("PickType")
        private Integer pickType;

        @JsonProperty("DeliverType")
        private Integer deliverType;
    }
}
