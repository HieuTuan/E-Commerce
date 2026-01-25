package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions; // JSON string or comma-separated values

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    // Constructor for easy role creation
    public Role(String name, String description, String permissions) {
        this.name = name;
        this.description = description;
        this.permissions = permissions;
    }

    // Helper method to check if role has specific permission
    public boolean hasPermission(String permission) {
        if (permissions == null) return false;
        if ("ALL".equals(permissions)) return true;
        return permissions.contains(permission);
    }

    // Helper method to get permissions as list
    public List<String> getPermissionsList() {
        if (permissions == null || permissions.trim().isEmpty()) {
            return new ArrayList<>();
        }
        if ("ALL".equals(permissions)) {
            return List.of("ALL");
        }
        return List.of(permissions.split(","));
    }
}