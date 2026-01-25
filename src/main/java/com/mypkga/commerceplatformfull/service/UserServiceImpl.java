package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.Role;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.CartRepository;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Override
    @Transactional
    public User registerUser(User user) {
        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Assign default customer role
        Role defaultRole = roleService.getDefaultCustomerRole();
        user.setRole(defaultRole);
        user.setEnabled(true);

        // Save user
        User savedUser = userRepository.save(user);
        log.info("Registered new user: {} with role: {}", savedUser.getUsername(), defaultRole.getName());

        // Create cart for user
        Cart cart = new Cart();
        cart.setUser(savedUser);
        cartRepository.save(cart);

        return savedUser;
    }

    @Override
    @Transactional
    public User createAdminUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Assign admin role
        Role adminRole = roleService.getRoleByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        user.setRole(adminRole);
        user.setEnabled(true);
        
        User savedUser = userRepository.save(user);
        log.info("Created admin user: {}", savedUser.getUsername());
        return savedUser;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        log.info("Deleted user with ID: {}", id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // Role-specific methods (updated to work with Role entity)
    @Override
    public List<User> getUsersByRoleName(String roleName) {
        return userRepository.findByRoleName(roleName);
    }
    
    @Override
    public Page<User> getUsersByRoleName(String roleName, Pageable pageable) {
        return userRepository.findByRoleName(roleName, pageable);
    }
    
    @Override
    public List<User> getUsersByRoleId(Long roleId) {
        return userRepository.findByRoleId(roleId);
    }
    
    @Override
    public Page<User> getUsersByRoleId(Long roleId, Pageable pageable) {
        return userRepository.findByRoleId(roleId, pageable);
    }
    
    @Override
    @Transactional
    public User assignRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        Role role = roleService.getRoleById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));
        
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        log.info("Assigned role '{}' to user '{}'", role.getName(), user.getUsername());
        return updatedUser;
    }
    
    @Override
    @Transactional
    public User assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        Role role = roleService.getRoleByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found with name: " + roleName));
        
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        log.info("Assigned role '{}' to user '{}'", roleName, user.getUsername());
        return updatedUser;
    }
    


    // Email verification methods
    @Override
    @Transactional
    public User markEmailAsVerified(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setEmailVerified(true);
        user.setEmailVerificationDate(java.time.LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        log.info("Marked email as verified for user: {}", user.getUsername());
        return updatedUser;
    }
    
    @Override
    @Transactional
    public User markEmailAsVerified(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        user.setEmailVerified(true);
        user.setEmailVerificationDate(java.time.LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        log.info("Marked email as verified for user: {}", user.getUsername());
        return updatedUser;
    }
    
    @Override
    public boolean isEmailVerified(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return user.getEmailVerified() != null && user.getEmailVerified();
    }
    
    @Override
    public boolean isEmailVerified(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return user.getEmailVerified() != null && user.getEmailVerified();
    }
}
