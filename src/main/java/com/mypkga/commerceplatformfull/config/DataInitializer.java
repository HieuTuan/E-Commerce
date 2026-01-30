//package com.mypkga.commerceplatformfull.config;
//
//import com.mypkga.commerceplatformfull.entity.*;
//import com.mypkga.commerceplatformfull.repository.*;
//import com.mypkga.commerceplatformfull.service.RoleService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class DataInitializer implements CommandLineRunner {
//
//       private final UserRepository userRepository;
//       private final PasswordEncoder passwordEncoder;
//       private final CategoryRepository categoryRepository;
//       private final ProductRepository productRepository;
//       private final ProductImageRepository productImageRepository;
//       private final ReviewRepository reviewRepository;
//       private final RoleService roleService;
//
//       @Override
//       public void run(String... args) throws Exception {
//
//               log.info("Starting data initialization...");
//
//               // Initialize roles first
//               roleService.initializeDefaultRoles();
//           Role adminRole = roleService.getRoleByName("ADMIN").orElseThrow();
//           Role customerRole = roleService.getRoleByName("CUSTOMER").orElseThrow();
//           Role staffRole = roleService.getRoleByName("STAFF").orElseThrow();
//           User existingAdmin2 = userRepository.findByUsername("administrator").orElse(null);
//           if (existingAdmin2 != null) {
//               log.info("Updating existing administrator user with new credentials...");
//               existingAdmin2.setPassword(passwordEncoder.encode("Admin123@"));
//               existingAdmin2.setFullName("System Administrator");
//               existingAdmin2.setEmail("administrator@gmail.com");
//               existingAdmin2.setEmailVerified(true);
//               existingAdmin2.setEmailVerificationDate(LocalDateTime.now());
//               existingAdmin2.setPhone("0123456788");
//               existingAdmin2.setAddress("456 Admin Street, Hanoi, Vietnam");
//               existingAdmin2.setRole(adminRole);
//               existingAdmin2.setEnabled(true);
//               userRepository.save(existingAdmin2);
//               log.info("Administrator user updated successfully");
//           } else {
//               log.info("Creating new administrator user...");
//               User admin2 = new User();
//               admin2.setUsername("administrator");
//               admin2.setPassword(passwordEncoder.encode("Admin123@"));
//               admin2.setFullName("System Administrator");
//               admin2.setEmail("administrator@gmail.com");
//               admin2.setEmailVerified(true);
//               admin2.setEmailVerificationDate(LocalDateTime.now());
//               admin2.setPhone("0123456788");
//               admin2.setAddress("456 Admin Street, Hanoi, Vietnam");
//               admin2.setRole(adminRole);
//               admin2.setEnabled(true);
//               userRepository.save(admin2);
//               log.info("Administrator user created successfully");
//           }
//
//           log.info("Data initialization completed successfully!");
//       }
//
//
//}