package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    
    /**
     * Create a new role
     */
    Role createRole(String name, String description, String permissions);
    
    /**
     * Update an existing role
     */
    Role updateRole(Long id, String name, String description, String permissions);
    
    /**
     * Delete a role (with user reassignment to default role)
     */
    void deleteRole(Long id);
    
    /**
     * Get role by ID
     */
    Optional<Role> getRoleById(Long id);
    
    /**
     * Get role by name
     */
    Optional<Role> getRoleByName(String name);
    
    /**
     * Get all roles
     */
    List<Role> getAllRoles();
    
    /**
     * Get default customer role
     */
    Role getDefaultCustomerRole();
    
    /**
     * Check if role has specific permission
     */
    boolean hasPermission(Long roleId, String permission);
    
    /**
     * Get roles with specific permission
     */
    List<Role> getRolesWithPermission(String permission);
    
    /**
     * Initialize default roles if they don't exist
     */
    void initializeDefaultRoles();
}