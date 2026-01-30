package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    private LocalDateTime emailVerificationDate;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "NVARCHAR(200)")
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Cart cart;

    // Helper method to get role name
    @Transient
    public String getRoleName() {
        return role != null ? role.getName() : null;
    }

    // Helper method to check if user has specific permission
    @Transient
    public boolean hasPermission(String permission) {
        return role != null && role.hasPermission(permission);
    }

    // Helper method to check if user has specific role
    @Transient
    public boolean hasRole(String roleName) {
        return role != null && roleName.equals(role.getName());
    }

    // Helper method to check if user is admin
    @Transient
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    // Helper method to check if user is staff
    @Transient
    public boolean isStaff() {
        return hasRole("STAFF");
    }

    // Helper method to check if user is customer
    @Transient
    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    // Helper method to check if email is verified
    @Transient
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }
}
