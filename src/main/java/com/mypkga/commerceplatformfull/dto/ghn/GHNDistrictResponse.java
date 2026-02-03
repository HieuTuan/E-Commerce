package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GHNDistrictResponse {
    private Integer code;
    private String message;
    private List<DistrictData> data;

    @Data
    public static class DistrictData {
        @JsonProperty("DistrictID")
        private Integer districtID;

        @JsonProperty("ProvinceID")
        private Integer provinceID;

        @JsonProperty("DistrictName")
        private String districtName;

        @JsonProperty("Code")
        private String code;

        @JsonProperty("Type")
        private Integer type;

        @JsonProperty("SupportType")
        private Integer supportType;

        @JsonProperty("NameExtension")
        private List<String> nameExtension;
    }
}
