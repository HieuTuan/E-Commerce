package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User registerUser(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    List<User> getAllUsers();
    
    Page<User> getAllUsers(Pageable pageable);

    User updateUser(User user);
    
    User saveUser(User user);

    void deleteUser(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User createAdminUser(User user);
    
    // Role-specific methods
    List<User> getUsersByRole(User.UserRole role);
    
    Page<User> getUsersByRole(User.UserRole role, Pageable pageable);
}
