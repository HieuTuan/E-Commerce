package com.mypkga.commerceplatformfull.controller;

import com.mypkga.commerceplatformfull.dto.ghn.*;
import com.mypkga.commerceplatformfull.service.GHNMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for GHN Master Data
 * Provides endpoints for fetching provinces, districts, wards, and available
 * services
 */
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@Slf4j
public class GHNMasterDataController {

    private final GHNMasterDataService ghnMasterDataService;

    /**
     * Get all provinces
     */
    @GetMapping("/provinces")
    public ResponseEntity<List<GHNProvinceResponse.ProvinceData>> getProvinces() {
        try {
            List<GHNProvinceResponse.ProvinceData> provinces = ghnMasterDataService.getProvinces();
            return ResponseEntity.ok(provinces);
        } catch (Exception e) {
            log.error("Error fetching provinces: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all districts in a province
     * 
     * @param provinceId GHN Province ID
     */
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(
            @RequestParam(value = "provinceId", required = false) String provinceIdStr) {
        try {
            // Handle null or empty parameter
            if (provinceIdStr == null || provinceIdStr.trim().isEmpty() || "undefined".equals(provinceIdStr)) {
                log.warn("Province ID is null, empty, or undefined - returning empty list");
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }

            // Attempt to parse to Integer
            Integer provinceId;
            try {
                provinceId = Integer.parseInt(provinceIdStr);
            } catch (NumberFormatException e) {
                log.error("Invalid provinceId format: '{}' - cannot convert to Integer", provinceIdStr);
                return ResponseEntity.badRequest().body("Invalid province ID format: " + provinceIdStr);
            }

            List<GHNDistrictResponse.DistrictData> districts = ghnMasterDataService.getDistricts(provinceId);
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            log.error("Error fetching districts for province {}: {}", provinceIdStr, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all wards in a district
     * 
     * @param districtId GHN District ID
     */
    @GetMapping("/wards")
    public ResponseEntity<?> getWards(
            @RequestParam(value = "districtId", required = false) String districtIdStr) {
        try {
            // Handle null or empty parameter
            if (districtIdStr == null || districtIdStr.trim().isEmpty() || "undefined".equals(districtIdStr)) {
                log.warn("District ID is null, empty, or undefined - returning empty list");
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }

            // Attempt to parse to Integer
            Integer districtId;
            try {
                districtId = Integer.parseInt(districtIdStr);
            } catch (NumberFormatException e) {
                log.error("Invalid districtId format: '{}' - cannot convert to Integer", districtIdStr);
                return ResponseEntity.badRequest().body("Invalid district ID format: " + districtIdStr);
            }

            List<GHNWardResponse.WardData> wards = ghnMasterDataService.getWards(districtId);
            return ResponseEntity.ok(wards);
        } catch (Exception e) {
            log.error("Error fetching wards for district {}: {}", districtIdStr, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get available shipping services between two districts
     * 
     * @param fromDistrict Origin district ID
     * @param toDistrict   Destination district ID
     */
    @GetMapping("/services")
    public ResponseEntity<List<GHNAvailableServicesResponse.ServiceData>> getAvailableServices(
            @RequestParam Integer fromDistrict,
            @RequestParam Integer toDistrict) {
        try {
            List<GHNAvailableServicesResponse.ServiceData> services = ghnMasterDataService
                    .getAvailableServices(fromDistrict, toDistrict);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            log.error("Error fetching available services from {} to {}: {}",
                    fromDistrict, toDistrict, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
