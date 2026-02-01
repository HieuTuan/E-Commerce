package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.PostOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PostOffice entity operations.
 * Provides data access methods for post office management.
 */
@Repository
public interface PostOfficeRepository extends JpaRepository<PostOffice, Long> {
    
    /**
     * Find all active post offices
     */
    List<PostOffice> findByActiveTrue();
    
    /**
     * Find post offices by name (case-insensitive)
     */
    @Query("SELECT po FROM PostOffice po WHERE LOWER(po.name) LIKE LOWER(CONCAT('%', :name, '%')) AND po.active = true")
    List<PostOffice> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /**
     * Find post offices by address (case-insensitive)
     */
    @Query("SELECT po FROM PostOffice po WHERE LOWER(po.address) LIKE LOWER(CONCAT('%', :address, '%')) AND po.active = true")
    List<PostOffice> findByAddressContainingIgnoreCase(@Param("address") String address);
    
    /**
     * Find nearest post offices by coordinates (simplified distance calculation)
     * Note: This is a basic implementation. For production, consider using spatial databases
     * or external geocoding services for more accurate distance calculations.
     */
    @Query("SELECT po FROM PostOffice po WHERE po.active = true AND po.latitude IS NOT NULL AND po.longitude IS NOT NULL ORDER BY po.name")
    List<PostOffice> findActivePostOfficesWithCoordinates();
    
    /**
     * Find post offices in a specific city/area
     */
    @Query("SELECT po FROM PostOffice po WHERE LOWER(po.address) LIKE LOWER(CONCAT('%', :city, '%')) AND po.active = true ORDER BY po.name")
    List<PostOffice> findByCityContainingIgnoreCase(@Param("city") String city);
}