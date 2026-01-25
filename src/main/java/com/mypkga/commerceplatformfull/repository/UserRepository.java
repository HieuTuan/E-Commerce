package com.mypkga.commerceplatformfull.repository;

import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
    
    // Role-based queries (updated to work with Role entity)
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.role.id = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);
    
    @Query("SELECT u FROM User u WHERE u.role.id = :roleId")
    Page<User> findByRoleId(@Param("roleId") Long roleId, Pageable pageable);
    
    // Count users by role
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.id = :roleId")
    long countByRoleId(@Param("roleId") Long roleId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
}
