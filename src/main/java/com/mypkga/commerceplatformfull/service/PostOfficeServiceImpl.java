package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.PostOffice;
import com.mypkga.commerceplatformfull.repository.PostOfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostOfficeServiceImpl implements PostOfficeService {
    
    private final PostOfficeRepository postOfficeRepository;
    
    @Override
    public List<PostOffice> getActivePostOffices() {
        return postOfficeRepository.findByActiveTrue();
    }
    
    @Override
    public List<PostOffice> searchPostOfficesByAddress(String address) {
        return postOfficeRepository.findByAddressContainingIgnoreCase(address);
    }
    
    @Override
    public List<PostOffice> searchPostOfficesByCity(String city) {
        return postOfficeRepository.findByCityContainingIgnoreCase(city);
    }
    
    @Override
    public List<PostOffice> searchPostOffices(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getActivePostOffices();
        }


        String trimmedSearchTerm = searchTerm.trim();
        
        // First try to search by address
        List<PostOffice> results = searchPostOfficesByAddress(trimmedSearchTerm);
        
        // If no results by address, try by city
        if (results.isEmpty()) {
            results = searchPostOfficesByCity(trimmedSearchTerm);
        }
        
        return results;
    }
    
    @Override
    public Optional<PostOffice> getPostOfficeById(Long id) {
        return postOfficeRepository.findById(id);
    }
}
