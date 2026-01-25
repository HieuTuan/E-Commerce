package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User registerUser(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    List<User> getAllUsers();
    
    Page<User> getAllUsers(Pageable pageable);

    User updateUser(User user);
    
    User saveUser(User user);

    void deleteUser(Long id);

    boolean existsByUsername(String username);

    User createAdminUser(User user);
    
    // Role-specific methods (updated to work with Role entity)
    List<User> getUsersByRoleName(String roleName);
    
    Page<User> getUsersByRoleName(String roleName, Pageable pageable);
    
    List<User> getUsersByRoleId(Long roleId);
    
    Page<User> getUsersByRoleId(Long roleId, Pageable pageable);
    
    // Helper methods for role management
    User assignRole(Long userId, Long roleId);
    
    User assignRole(Long userId, String roleName);
}
