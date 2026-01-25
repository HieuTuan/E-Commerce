package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Role;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.RoleRepository;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Role createRole(String name, String description, String permissions) {
        if (roleRepository.existsByName(name)) {
            throw new RuntimeException("Role with name '" + name + "' already exists");
        }
        
        Role role = new Role(name, description, permissions);
        Role savedRole = roleRepository.save(role);
        log.info("Created new role: {}", name);
        return savedRole;
    }

    @Override
    @Transactional
    public Role updateRole(Long id, String name, String description, String permissions) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        
        // Check if name is being changed and if new name already exists
        if (!role.getName().equals(name) && roleRepository.existsByName(name)) {
            throw new RuntimeException("Role with name '" + name + "' already exists");
        }
        
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions);
        
        Role updatedRole = roleRepository.save(role);
        log.info("Updated role: {} (ID: {})", name, id);
        return updatedRole;
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
        
        // Don't allow deletion of default roles
        if ("CUSTOMER".equals(role.getName()) || "ADMIN".equals(role.getName())) {
            throw new RuntimeException("Cannot delete default role: " + role.getName());
        }
        
        // Reassign users to default customer role
        Role defaultRole = getDefaultCustomerRole();
        List<User> usersWithRole = role.getUsers();
        
        if (!usersWithRole.isEmpty()) {
            for (User user : usersWithRole) {
                user.setRole(defaultRole);
                userRepository.save(user);
            }
            log.info("Reassigned {} users from role '{}' to default role '{}'", 
                    usersWithRole.size(), role.getName(), defaultRole.getName());
        }
        
        roleRepository.delete(role);
        log.info("Deleted role: {} (ID: {})", role.getName(), id);
    }

    @Override
    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    @Override
    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Role getDefaultCustomerRole() {
        return roleRepository.findDefaultCustomerRole()
                .orElseThrow(() -> new RuntimeException("Default CUSTOMER role not found. Please initialize default roles."));
    }

    @Override
    public boolean hasPermission(Long roleId, String permission) {
        Optional<Role> role = roleRepository.findById(roleId);
        return role.map(r -> r.hasPermission(permission)).orElse(false);
    }

    @Override
    public List<Role> getRolesWithPermission(String permission) {
        return roleRepository.findByPermission(permission);
    }

    @Override
    @Transactional
    public void initializeDefaultRoles() {
        log.info("Initializing default roles...");
        
        // Create ADMIN role
        if (!roleRepository.existsByName("ADMIN")) {
            createRole("ADMIN", "Administrator with full access", "ALL");
        }
        
        // Create STAFF role
        if (!roleRepository.existsByName("STAFF")) {
            createRole("STAFF", "Staff member with limited admin access", "MANAGE_ORDERS,MANAGE_CUSTOMERS");
        }
        
        // Create CUSTOMER role
        if (!roleRepository.existsByName("CUSTOMER")) {
            createRole("CUSTOMER", "Regular customer", "SHOP,VIEW_ORDERS");
        }
        
        // Create GUEST role
        if (!roleRepository.existsByName("GUEST")) {
            createRole("GUEST", "Guest user with limited access", "VIEW_PRODUCTS");
        }
        
        log.info("Default roles initialization completed");
    }
}