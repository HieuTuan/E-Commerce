package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.dto.ghn.*;

import java.util.List;

/**
 * Service for fetching GHN Master Data (provinces, districts, wards, services)
 */
public interface GHNMasterDataService {

    /**
     * Get all provinces from GHN
     */
    List<GHNProvinceResponse.ProvinceData> getProvinces();

    /**
     * Get all districts in a province
     * 
     * @param provinceId GHN Province ID
     */
    List<GHNDistrictResponse.DistrictData> getDistricts(Integer provinceId);

    /**
     * Get all wards in a district
     * 
     * @param districtId GHN District ID
     */
    List<GHNWardResponse.WardData> getWards(Integer districtId);

    /**
     * Get available shipping services between two districts
     * 
     * @param fromDistrictId Origin district ID
     * @param toDistrictId   Destination district ID
     */
    List<GHNAvailableServicesResponse.ServiceData> getAvailableServices(Integer fromDistrictId, Integer toDistrictId);
}
