package com.mypkga.commerceplatformfull.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mypkga.commerceplatformfull.entity.PostOffice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for post office information in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostOfficeDto {
    
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String operatingHours;
    private Boolean active;
    private Double latitude;
    private Double longitude;
    
    /**
     * Create DTO from entity
     */
    public static PostOfficeDto from(PostOffice postOffice) {
        if (postOffice == null) {
            return null;
        }
        
        return PostOfficeDto.builder()
                .id(postOffice.getId())
                .name(postOffice.getName())
                .address(postOffice.getAddress())
                .phone(postOffice.getPhone())
                .email(postOffice.getEmail())
                .operatingHours(postOffice.getOperatingHours())
                .active(postOffice.getActive())
                .latitude(postOffice.getLatitude())
                .longitude(postOffice.getLongitude())
                .build();
    }
}