package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Role;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.repository.RoleRepository;
import com.mypkga.commerceplatformfull.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Feature: role-table-separation, Property 1: Role entity management integrity
 * 
 * Property-based tests for role management operations to ensure proper database 
 * relationships and referential integrity are maintained.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleServicePropertyTest {

    @Autowired
    private RoleService roleService;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();
        roleRepository.deleteAll();
        
        // Initialize default roles for testing
        roleService.initializeDefaultRoles();
    }

    /**
     * Integration test to verify role system is working correctly
     */
    @Test
    void roleSystemIntegrationTest() {
        // Test 1: Verify default roles exist
        Optional<Role> customerRole = roleService.getRoleByName("CUSTOMER");
        Optional<Role> adminRole = roleService.getRoleByName("ADMIN");
        
        assertThat(customerRole).isPresent();
        assertThat(adminRole).isPresent();
        
        // Test 2: Create a new role
        Role testRole = roleService.createRole("TEST_ROLE", "Test role for integration", "TEST_PERMISSION");
        assertThat(testRole).isNotNull();
        assertThat(testRole.getName()).isEqualTo("TEST_ROLE");
        
        // Test 3: Create user with role
        User testUser = new User();
        testUser.setUsername("testuser_integration");
        testUser.setPassword("password123");
        testUser.setFullName("Test User");
        testUser.setRole(testRole);
        testUser.setEnabled(true);
        
        User savedUser = userRepository.save(testUser);
        assertThat(savedUser.getRole()).isNotNull();
        assertThat(savedUser.getRole().getName()).isEqualTo("TEST_ROLE");
        
        // Test 4: Verify role-based access control methods
        assertThat(savedUser.hasRole("TEST_ROLE")).isTrue();
        assertThat(savedUser.hasRole("ADMIN")).isFalse();
        assertThat(savedUser.hasPermission("TEST_PERMISSION")).isTrue();
        assertThat(savedUser.hasPermission("NON_EXISTENT")).isFalse();
        
        // Test 5: Update role
        Role updatedRole = roleService.updateRole(testRole.getId(), "TEST_ROLE", 
                "Updated description", "UPDATED_PERMISSION");
        assertThat(updatedRole.getDescription()).isEqualTo("Updated description");
        
        // Test 6: Delete role (should reassign users)
        roleService.deleteRole(testRole.getId());
        User reassignedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(reassignedUser.getRole().getName()).isEqualTo("CUSTOMER"); // Default role
    }

    /**
     * Property 1: Role entity management integrity
     * For any role management operation (create, update, assign, delete), 
     * the system should maintain proper database relationships and referential integrity
     * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.5
     */
    @Property(tries = 100)
    void roleManagementMaintainsIntegrity(
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String roleName,
            @ForAll @StringLength(min = 5, max = 100) String description,
            @ForAll @StringLength(min = 3, max = 50) String permissions) {
        
        // Ensure unique role name for this test
        String uniqueRoleName = "TEST_" + roleName + "_" + System.nanoTime();
        
        // Test 1: Role creation maintains integrity
        Role createdRole = roleService.createRole(uniqueRoleName, description, permissions);
        
        assertThat(createdRole).isNotNull();
        assertThat(createdRole.getId()).isNotNull();
        assertThat(createdRole.getName()).isEqualTo(uniqueRoleName);
        assertThat(createdRole.getDescription()).isEqualTo(description);
        assertThat(createdRole.getPermissions()).isEqualTo(permissions);
        
        // Verify role exists in database
        Optional<Role> foundRole = roleRepository.findById(createdRole.getId());
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo(uniqueRoleName);
        
        // Test 2: Role assignment maintains 1-n relationship
        // Create a test user and assign the role
        User testUser = new User();
        testUser.setUsername("testuser_" + System.nanoTime());
        testUser.setPassword("password123");
        testUser.setFullName("Test User");
        testUser.setRole(createdRole);
        testUser.setEnabled(true);
        
        User savedUser = userRepository.save(testUser);
        
        // Verify relationship integrity
        assertThat(savedUser.getRole()).isNotNull();
        assertThat(savedUser.getRole().getId()).isEqualTo(createdRole.getId());
        
        // Verify multiple users can have same role (1-n relationship)
        User secondUser = new User();
        secondUser.setUsername("testuser2_" + System.nanoTime());
        secondUser.setPassword("password123");
        secondUser.setFullName("Test User 2");
        secondUser.setRole(createdRole);
        secondUser.setEnabled(true);
        
        User savedSecondUser = userRepository.save(secondUser);
        assertThat(savedSecondUser.getRole().getId()).isEqualTo(createdRole.getId());
        
        // Test 3: Role update maintains integrity
        String updatedDescription = "Updated " + description;
        String updatedPermissions = "UPDATED_" + permissions;
        
        Role updatedRole = roleService.updateRole(createdRole.getId(), uniqueRoleName, 
                updatedDescription, updatedPermissions);
        
        assertThat(updatedRole.getDescription()).isEqualTo(updatedDescription);
        assertThat(updatedRole.getPermissions()).isEqualTo(updatedPermissions);
        
        // Verify users still have correct role reference after update
        User refreshedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(refreshedUser.getRole().getId()).isEqualTo(updatedRole.getId());
        assertThat(refreshedUser.getRole().getDescription()).isEqualTo(updatedDescription);
        
        // Test 4: Permission checking works correctly
        boolean hasPermission = roleService.hasPermission(updatedRole.getId(), updatedPermissions);
        assertThat(hasPermission).isTrue();
        
        boolean hasNonExistentPermission = roleService.hasPermission(updatedRole.getId(), "NON_EXISTENT");
        assertThat(hasNonExistentPermission).isFalse();
    }

    @Property(tries = 50)
    void roleDeletionHandlesUserReassignmentCorrectly(
            @ForAll @AlphaChars @StringLength(min = 3, max = 15) String roleName) {
        
        String uniqueRoleName = "DELETE_TEST_" + roleName + "_" + System.nanoTime();
        
        // Create a non-default role for deletion testing
        Role roleToDelete = roleService.createRole(uniqueRoleName, "Test role for deletion", "TEST_PERMISSION");
        
        // Create users with this role
        User user1 = new User();
        user1.setUsername("deluser1_" + System.nanoTime());
        user1.setPassword("password123");
        user1.setFullName("Delete Test User 1");
        user1.setRole(roleToDelete);
        user1.setEnabled(true);
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setUsername("deluser2_" + System.nanoTime());
        user2.setPassword("password123");
        user2.setFullName("Delete Test User 2");
        user2.setRole(roleToDelete);
        user2.setEnabled(true);
        userRepository.save(user2);
        
        // Get default customer role for reassignment verification
        Role defaultRole = roleService.getDefaultCustomerRole();
        
        // Delete the role
        roleService.deleteRole(roleToDelete.getId());
        
        // Verify role is deleted
        Optional<Role> deletedRole = roleRepository.findById(roleToDelete.getId());
        assertThat(deletedRole).isEmpty();
        
        // Verify users are reassigned to default role
        User reassignedUser1 = userRepository.findById(user1.getId()).orElseThrow();
        User reassignedUser2 = userRepository.findById(user2.getId()).orElseThrow();
        
        assertThat(reassignedUser1.getRole().getId()).isEqualTo(defaultRole.getId());
        assertThat(reassignedUser2.getRole().getId()).isEqualTo(defaultRole.getId());
    }

    @Property(tries = 30)
    void defaultRolesDeletionIsPreventedCorrectly() {
        // Test that default roles cannot be deleted
        Role customerRole = roleService.getRoleByName("CUSTOMER").orElseThrow();
        Role adminRole = roleService.getRoleByName("ADMIN").orElseThrow();
        
        // Attempting to delete default roles should throw exception
        assertThatThrownBy(() -> roleService.deleteRole(customerRole.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete default role");
                
        assertThatThrownBy(() -> roleService.deleteRole(adminRole.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete default role");
    }

    @Property(tries = 50)
    void duplicateRoleNamesArePreventedCorrectly(
            @ForAll @AlphaChars @StringLength(min = 3, max = 15) String roleName) {
        
        String uniqueRoleName = "DUP_TEST_" + roleName + "_" + System.nanoTime();
        
        // Create first role
        roleService.createRole(uniqueRoleName, "First role", "PERMISSION1");
        
        // Attempting to create role with same name should throw exception
        assertThatThrownBy(() -> roleService.createRole(uniqueRoleName, "Second role", "PERMISSION2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Property(tries = 30)
    void rolePermissionCheckingWorksCorrectly(
            @ForAll @AlphaChars @StringLength(min = 3, max = 15) String permission1,
            @ForAll @AlphaChars @StringLength(min = 3, max = 15) String permission2) {
        
        String uniqueRoleName = "PERM_TEST_" + System.nanoTime();
        String permissions = permission1 + "," + permission2;
        
        Role role = roleService.createRole(uniqueRoleName, "Permission test role", permissions);
        
        // Test individual permissions
        assertThat(roleService.hasPermission(role.getId(), permission1)).isTrue();
        assertThat(roleService.hasPermission(role.getId(), permission2)).isTrue();
        
        // Test non-existent permission
        assertThat(roleService.hasPermission(role.getId(), "NON_EXISTENT_PERMISSION")).isFalse();
        
        // Test ALL permission role
        Role allPermRole = roleService.createRole("ALL_PERM_" + System.nanoTime(), "All permissions", "ALL");
        assertThat(roleService.hasPermission(allPermRole.getId(), permission1)).isTrue();
        assertThat(roleService.hasPermission(allPermRole.getId(), permission2)).isTrue();
        assertThat(roleService.hasPermission(allPermRole.getId(), "ANY_PERMISSION")).isTrue();
    }
}