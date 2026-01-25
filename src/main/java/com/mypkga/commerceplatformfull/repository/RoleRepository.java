package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find role by name (case-insensitive)
     */
    Optional<Role> findByNameIgnoreCase(String name);
    
    /**
     * Find role by exact name
     */
    Optional<Role> findByName(String name);
    
    /**
     * Check if role exists by name
     */
    boolean existsByName(String name);
    
    /**
     * Find all roles ordered by name
     */
    List<Role> findAllByOrderByNameAsc();
    
    /**
     * Find roles that contain specific permission
     */
    @Query("SELECT r FROM Role r WHERE r.permissions LIKE %:permission% OR r.permissions = 'ALL'")
    List<Role> findByPermission(@Param("permission") String permission);
    
    /**
     * Count users for each role
     */
    @Query("SELECT r.name, COUNT(u) FROM Role r LEFT JOIN r.users u GROUP BY r.id, r.name")
    List<Object[]> countUsersByRole();
    
    /**
     * Find default customer role
     */
    @Query("SELECT r FROM Role r WHERE r.name = 'CUSTOMER'")
    Optional<Role> findDefaultCustomerRole();
}