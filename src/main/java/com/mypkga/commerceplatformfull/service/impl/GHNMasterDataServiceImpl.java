package com.mypkga.commerceplatformfull.service.impl;

import com.mypkga.commerceplatformfull.dto.ghn.*;
import com.mypkga.commerceplatformfull.service.GHNMasterDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GHNMasterDataServiceImpl implements GHNMasterDataService {

    private final RestTemplate restTemplate;

    @Value("${ghn.api-url}")
    private String ghnApiUrl;

    @Value("${ghn.token}")
    private String ghnToken = "78be1310-ffe5-11f0-a3d6-dac90fb956b5";

    @Value("${ghn.shop-id}")
    private Integer ghnShopId;

    public GHNMasterDataServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Cacheable(value = "ghnProvinces", unless = "#result == null || #result.isEmpty()")
    public List<GHNProvinceResponse.ProvinceData> getProvinces() {
        try {
            String url = ghnApiUrl + "/master-data/province";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Fetching provinces from GHN API");
            ResponseEntity<GHNProvinceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GHNProvinceResponse.class);

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                log.info("Successfully fetched {} provinces from GHN", response.getBody().getData().size());
                return response.getBody().getData();
            } else {
                log.warn("GHN province API returned non-200 code: {}", response.getBody());
                return getMockProvinces();
            }

        } catch (Exception e) {
            log.error("Error fetching provinces from GHN: {}", e.getMessage());
            log.info("Using mock province data for development");
            return getMockProvinces();
        }
    }

    private List<GHNProvinceResponse.ProvinceData> getMockProvinces() {
        // Mock data for development when GHN token is not available
        List<GHNProvinceResponse.ProvinceData> mockProvinces = new java.util.ArrayList<>();

        GHNProvinceResponse.ProvinceData prov1 = new GHNProvinceResponse.ProvinceData();
        prov1.setProvinceID(202);
        prov1.setProvinceName("Hồ Chí Minh");
        prov1.setCode("HCM");
        mockProvinces.add(prov1);

        GHNProvinceResponse.ProvinceData prov2 = new GHNProvinceResponse.ProvinceData();
        prov2.setProvinceID(201);
        prov2.setProvinceName("Hà Nội");
        prov2.setCode("HN");
        mockProvinces.add(prov2);

        GHNProvinceResponse.ProvinceData prov3 = new GHNProvinceResponse.ProvinceData();
        prov3.setProvinceID(203);
        prov3.setProvinceName("Đà Nẵng");
        prov3.setCode("DN");
        mockProvinces.add(prov3);

        log.info("Returned {} mock provinces", mockProvinces.size());
        return mockProvinces;
    }

    @Override
    @Cacheable(value = "ghnDistricts", key = "#provinceId", unless = "#result == null || #result.isEmpty()")
    public List<GHNDistrictResponse.DistrictData> getDistricts(Integer provinceId) {
        try {
            String url = ghnApiUrl + "/master-data/district";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("province_id", provinceId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Fetching districts for province {} from GHN API", provinceId);
            ResponseEntity<GHNDistrictResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GHNDistrictResponse.class);

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                log.info("Successfully fetched {} districts for province {}",
                        response.getBody().getData().size(), provinceId);
                return response.getBody().getData();
            } else {
                log.warn("GHN district API returned non-200 code: {}", response.getBody());
                return getMockDistricts(provinceId);
            }

        } catch (Exception e) {
            log.error("Error fetching districts for province {}: {}", provinceId, e.getMessage());
            log.info("Using mock district data for development");
            return getMockDistricts(provinceId);
        }
    }

    private List<GHNDistrictResponse.DistrictData> getMockDistricts(Integer provinceId) {
        List<GHNDistrictResponse.DistrictData> mockDistricts = new java.util.ArrayList<>();

        if (provinceId == 202) { // HCM
            GHNDistrictResponse.DistrictData dist1 = new GHNDistrictResponse.DistrictData();
            dist1.setDistrictID(1454);
            dist1.setDistrictName("Quận 1");
            dist1.setProvinceID(202);
            mockDistricts.add(dist1);

            GHNDistrictResponse.DistrictData dist2 = new GHNDistrictResponse.DistrictData();
            dist2.setDistrictID(1455);
            dist2.setDistrictName("Quận 2");
            dist2.setProvinceID(202);
            mockDistricts.add(dist2);
        } else if (provinceId == 201) { // HN
            GHNDistrictResponse.DistrictData dist1 = new GHNDistrictResponse.DistrictData();
            dist1.setDistrictID(1482);
            dist1.setDistrictName("Quận Hoàn Kiếm");
            dist1.setProvinceID(201);
            mockDistricts.add(dist1);
        } else if (provinceId == 203) { // DN
            GHNDistrictResponse.DistrictData dist1 = new GHNDistrictResponse.DistrictData();
            dist1.setDistrictID(1550);
            dist1.setDistrictName("Quận Hải Châu");
            dist1.setProvinceID(203);
            mockDistricts.add(dist1);
        }

        log.info("Returned {} mock districts for province {}", mockDistricts.size(), provinceId);
        return mockDistricts;
    }

    @Override
    @Cacheable(value = "ghnWards", key = "#districtId", unless = "#result == null || #result.isEmpty()")
    public List<GHNWardResponse.WardData> getWards(Integer districtId) {
        try {
            String url = ghnApiUrl + "/master-data/ward?district_id";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("district_id", districtId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Fetching wards for district {} from GHN API", districtId);
            ResponseEntity<GHNWardResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GHNWardResponse.class);

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                log.info("Successfully fetched {} wards for district {}",
                        response.getBody().getData().size(), districtId);
                return response.getBody().getData();
            } else {
                log.warn("GHN ward API returned non-200 code: {}", response.getBody());
                return getMockWards(districtId);
            }

        } catch (Exception e) {
            log.error("Error fetching wards for district {}: {}", districtId, e.getMessage());
            log.info("Using mock ward data for development");
            return getMockWards(districtId);
        }
    }

    private List<GHNWardResponse.WardData> getMockWards(Integer districtId) {
        List<GHNWardResponse.WardData> mockWards = new java.util.ArrayList<>();

        GHNWardResponse.WardData ward1 = new GHNWardResponse.WardData();
        ward1.setWardCode("21211");
        ward1.setDistrictID(districtId);
        ward1.setWardName("Phường Bến Nghé");
        mockWards.add(ward1);

        GHNWardResponse.WardData ward2 = new GHNWardResponse.WardData();
        ward2.setWardCode("21212");
        ward2.setDistrictID(districtId);
        ward2.setWardName("Phường Bến Thành");
        mockWards.add(ward2);

        log.info("Returned {} mock wards for district {}", mockWards.size(), districtId);
        return mockWards;
    }

    @Override
    public List<GHNAvailableServicesResponse.ServiceData> getAvailableServices(Integer fromDistrictId,
            Integer toDistrictId) {
        try {
            String url = ghnApiUrl + "/v2/shipping-order/available-services";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", ghnToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("shop_id", ghnShopId);
            requestBody.put("from_district", fromDistrictId);
            requestBody.put("to_district", toDistrictId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Fetching available services from district {} to {} from GHN API",
                    fromDistrictId, toDistrictId);
            ResponseEntity<GHNAvailableServicesResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GHNAvailableServicesResponse.class);

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                log.info("Successfully fetched {} available services", response.getBody().getData().size());
                return response.getBody().getData();
            } else {
                log.warn("GHN available services API returned non-200 code: {}", response.getBody());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("Error fetching available services from {} to {}: {}",
                    fromDistrictId, toDistrictId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
