package com.mypkga.commerceplatformfull.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final RestTemplate restTemplate;
    
    private static final String PROVINCES_API_BASE = "https://provinces.open-api.vn/api";

    /**
     * Get all provinces/cities
     */
    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        try {
            String url = PROVINCES_API_BASE + "/p/";
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching provinces: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error fetching provinces");
        }
    }

    /**
     * Get districts by province code
     */
    @GetMapping("/districts/{provinceCode}")
    public ResponseEntity<?> getDistricts(@PathVariable String provinceCode) {
        try {
            String url = PROVINCES_API_BASE + "/p/" + provinceCode + "?depth=2";
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching districts for province {}: {}", provinceCode, e.getMessage());
            return ResponseEntity.internalServerError().body("Error fetching districts");
        }
    }

    /**
     * Get wards by district code
     */
    @GetMapping("/wards/{districtCode}")
    public ResponseEntity<?> getWards(@PathVariable String districtCode) {
        try {
            String url = PROVINCES_API_BASE + "/d/" + districtCode + "?depth=2";
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching wards for district {}: {}", districtCode, e.getMessage());
            return ResponseEntity.internalServerError().body("Error fetching wards");
        }
    }
}