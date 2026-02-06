package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.PostOffice;

import java.util.List;
import java.util.Optional;

public interface PostOfficeService {
    
    List<PostOffice> getActivePostOffices();
    
    List<PostOffice> searchPostOfficesByAddress(String address);
    
    List<PostOffice> searchPostOfficesByCity(String city);
    
    List<PostOffice> searchPostOffices(String searchTerm);
    
    Optional<PostOffice> getPostOfficeById(Long id);
}
