package com.mypkga.commerceplatformfull.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a post office location where customers can drop off return items.
 * Post offices are predefined locations managed by the system.
 */
@Entity
@Table(name = "post_offices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostOffice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String name;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(200)")
    private String address;
    
    @Column(columnDefinition = "NVARCHAR(20)")
    private String phone;
    
    @Column(columnDefinition = "NVARCHAR(100)")
    private String email;
    
    @Column(name = "operating_hours", columnDefinition = "NVARCHAR(100)")
    private String operatingHours;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Geographic coordinates for distance calculations
    @Column(precision = 10)
    private Double latitude;
    
    @Column(precision = 11)
    private Double longitude;
}