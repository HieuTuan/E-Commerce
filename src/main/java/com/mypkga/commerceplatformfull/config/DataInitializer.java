package com.mypkga.commerceplatformfull.config;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.*;
import com.mypkga.commerceplatformfull.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final CategoryRepository categoryRepository;
        private final ProductRepository productRepository;
        private final ProductImageRepository productImageRepository;
        private final ReviewRepository reviewRepository;
        private final RoleService roleService;

        @Override
        public void run(String... args) throws Exception {
                log.info("Starting data initialization...");
                
                // Initialize roles first
                roleService.initializeDefaultRoles();
                
                initializeUsers();
                initializeCategories();
                initializeProducts();
                initializeReviews();

                log.info("Data initialization completed successfully!");
        }

        private void initializeUsers() {
                if (userRepository.count() == 0) {
                        log.info("Initializing users...");

                        // Get roles
                        Role adminRole = roleService.getRoleByName("ADMIN").orElseThrow();
                        Role customerRole = roleService.getRoleByName("CUSTOMER").orElseThrow();
                        Role staffRole = roleService.getRoleByName("STAFF").orElseThrow();

                        // Admin user
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setFullName("Administrator");
                    admin.setEmail("admin@commerce.com");
                    admin.setEmailVerified(true);
                    admin.setEmailVerificationDate(LocalDateTime.now());
                    admin.setPhone("0123456789");
                    admin.setAddress("123 Admin Street, Hanoi, Vietnam");
                    admin.setRole(adminRole);
                    admin.setEnabled(true);


                    User customer1 = new User();
                    customer1.setUsername("john_doe");
                    customer1.setPassword(passwordEncoder.encode("password123"));
                    customer1.setFullName("John Doe");
                    customer1.setEmail("john.doe@gmail.com");
                    customer1.setEmailVerified(true);
                    customer1.setEmailVerificationDate(LocalDateTime.now());
                    customer1.setPhone("0987654321");
                    customer1.setAddress("456 Customer Avenue, Ho Chi Minh, Vietnam");
                    customer1.setRole(customerRole);
                    customer1.setEnabled(true);


                    User staff = new User();
                    staff.setUsername("staff");
                    staff.setPassword(passwordEncoder.encode("staff123"));
                    staff.setFullName("Staff Member");
                    staff.setEmail("staff@commerce.com");
                    staff.setEmailVerified(true);
                    staff.setEmailVerificationDate(LocalDateTime.now());
                    staff.setPhone("0123456788");
                    staff.setAddress("456 Staff Street, Hanoi, Vietnam");
                    staff.setRole(staffRole);
                    staff.setEnabled(true);


                    userRepository.saveAll(Arrays.asList(admin, customer1, staff));
                        log.info("Successfully initialized {} users with roles", 5);
                } else {
                        log.info("Users already exist, skipping initialization");
                }
        }

        private void initializeCategories() {
                if (categoryRepository.count() == 0) {
                        log.info("Initializing categories...");

                        Category electronics = new Category();
                        electronics.setName("Electronics");
                        electronics.setDescription("Electronic devices and gadgets");

                        Category clothing = new Category();
                        clothing.setName("Clothing");
                        clothing.setDescription("Fashion and apparel");

                        Category books = new Category();
                        books.setName("Books");
                        books.setDescription("Books and literature");

                        Category home = new Category();
                        home.setName("Home & Garden");
                        home.setDescription("Home improvement and garden supplies");

                        Category sports = new Category();
                        sports.setName("Sports");
                        sports.setDescription("Sports equipment and accessories");

                        categoryRepository.saveAll(Arrays.asList(electronics, clothing, books, home, sports));
                        log.info("Successfully initialized {} categories", 5);
                } else {
                        log.info("Categories already exist, skipping initialization");
                }
        }

        private void initializeProducts() {
                if (productRepository.count() == 0) {
                        log.info("Initializing products...");

                        List<Category> categories = categoryRepository.findAll();
                        if (categories.isEmpty()) {
                                log.warn("No categories found, skipping product initialization");
                                return;
                        }

                        // Sample products with real uploaded images
                        String[][] imageData = {
                                {"07ef3ebe-dc0a-488e-a3ef-e83e0bb91424", "original.png", "thumbnail.jpg", "medium.jpg", "large.jpg"},
                                {"1a7474eb-3b52-48f9-9820-c19e7773bfa7", "original.png", "thumbnail.jpg", "medium.jpg", "large.jpg"},
                                {"2a9a0754-41e4-479a-ab43-4e153f925ad1", "original.png", "thumbnail.jpg", "medium.jpg", "large.jpg"},
                                {"2d85dca9-869d-4b07-b76f-68a18f841932", "original.png", "thumbnail.jpg", "medium.jpg", "large.jpg"},
                                {"394b7de4-9dba-4748-8c62-f9b2ba53bcd5", "original.png", "thumbnail.jpg", "medium.jpg", "large.jpg"}
                        };

                        String[] productNames = {
                                "Premium Wireless Headphones",
                                "Smart Fitness Tracker",
                                "Organic Cotton T-Shirt",
                                "Professional Camera Lens",
                                "Ergonomic Office Chair"
                        };

                        String[] descriptions = {
                                "High-quality wireless headphones with noise cancellation and premium sound quality. Perfect for music lovers and professionals.",
                                "Advanced fitness tracker with heart rate monitoring, GPS, and waterproof design. Track your health and fitness goals.",
                                "Comfortable organic cotton t-shirt made from sustainable materials. Available in multiple colors and sizes.",
                                "Professional-grade camera lens with superior optics and build quality. Ideal for photography enthusiasts.",
                                "Ergonomic office chair designed for comfort and productivity. Adjustable height and lumbar support included."
                        };

                        BigDecimal[] prices = {
                                new BigDecimal("2500000"), // 2,500,000 VND
                                new BigDecimal("1800000"), // 1,800,000 VND
                                new BigDecimal("450000"),  // 450,000 VND
                                new BigDecimal("12000000"), // 12,000,000 VND
                                new BigDecimal("3200000")  // 3,200,000 VND
                        };

                        for (int i = 0; i < productNames.length && i < imageData.length; i++) {
                                Product product = new Product();
                                product.setName(productNames[i]);
                                product.setDescription(descriptions[i]);
                                product.setPrice(prices[i]);
                                product.setStockQuantity(50 + (i * 10));
                                product.setCategory(categories.get(i % categories.size()));
                                product.setFeatured(i < 3); // First 3 products are featured

                                Product savedProduct = productRepository.save(product);

                                // Create ProductImage with real uploaded images
                                ProductImage productImage = new ProductImage();
                                productImage.setProduct(savedProduct);
                                productImage.setImageOriginal(imageData[i][0] + "_" + imageData[i][1]);
                                productImage.setImageThumbnail(imageData[i][0] + "_" + imageData[i][2]);
                                productImage.setImageMedium(imageData[i][0] + "_" + imageData[i][3]);
                                productImage.setImageLarge(imageData[i][0] + "_" + imageData[i][4]);
                                productImage.setDisplayOrder(0);
                                productImage.setIsPrimary(true);

                                productImageRepository.save(productImage);
                        }

                        log.info("Successfully initialized {} products with images", productNames.length);
                } else {
                        log.info("Products already exist, skipping initialization");
                }
        }

        private void initializeReviews() {
                if (reviewRepository.count() == 0) {
                        log.info("Initializing reviews...");

                        List<Product> products = productRepository.findAll();
                        List<User> customers = userRepository.findByRoleName("CUSTOMER");

                        if (products.isEmpty() || customers.isEmpty()) {
                                log.warn("No products or customers found, skipping review initialization");
                                return;
                        }

                        String[] reviewComments = {
                                "Excellent product! Highly recommended.",
                                "Good quality for the price. Fast delivery.",
                                "Amazing features and great build quality.",
                                "Perfect for my needs. Will buy again.",
                                "Outstanding customer service and product quality."
                        };

                        int[] ratings = {5, 4, 5, 4, 5};

                        for (int i = 0; i < Math.min(products.size(), reviewComments.length); i++) {
                                Review review = new Review();
                                review.setProduct(products.get(i));
                                review.setUser(customers.get(i % customers.size()));
                                review.setRating(ratings[i]);
                                review.setComment(reviewComments[i]);

                                reviewRepository.save(review);
                        }

                        log.info("Successfully initialized {} reviews", Math.min(products.size(), reviewComments.length));
                } else {
                        log.info("Reviews already exist, skipping initialization");
                }
        }
}