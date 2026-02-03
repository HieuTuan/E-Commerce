package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GHNProvinceResponse {
    private Integer code;
    private String message;
    private List<ProvinceData> data;

    @Data
    public static class ProvinceData {
        @JsonProperty("ProvinceID")
        private Integer provinceID;

        @JsonProperty("ProvinceName")
        private String provinceName;

        @JsonProperty("Code")
        private String code;

        @JsonProperty("NameExtension")
        private List<String> nameExtension;
    }
}
