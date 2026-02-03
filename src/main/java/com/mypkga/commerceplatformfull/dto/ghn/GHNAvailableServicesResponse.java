package com.mypkga.commerceplatformfull.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GHNAvailableServicesResponse {
    private Integer code;
    private String message;
    private List<ServiceData> data;

    @Data
    public static class ServiceData {
        @JsonProperty("service_id")
        private Integer serviceId;

        @JsonProperty("short_name")
        private String shortName;

        @JsonProperty("service_type_id")
        private Integer serviceTypeId;
    }
}
