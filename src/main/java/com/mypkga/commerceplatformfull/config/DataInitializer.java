package com.mypkga.commerceplatformfull.config;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

        private final CategoryRepository categoryRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final ReviewRepository reviewRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
                log.info("Starting data initialization...");

                // Initialize Users
                initializeUsers();

                // Initialize Categories
//                initializeCategories();

                // Initialize Products
//                initializeProducts();

                // Initialize Reviews
//                initializeReviews();

                log.info("Data initialization completed successfully!");
        }

        private void initializeUsers() {
                if (userRepository.count() == 0) {
                        log.info("Initializing users...");

                        // Admin user
                        User admin = new User();
                        admin.setUsername("admin");
                        admin.setEmail("admin@commerceplatform.com");
                        admin.setPassword(passwordEncoder.encode("admin123"));
                        admin.setFullName("Administrator");
                        admin.setPhone("0123456789");
                        admin.setAddress("123 Admin Street, Hanoi, Vietnam");
                        admin.setRole(User.UserRole.ADMIN);
                        admin.setEnabled(true);

                        // Customer users
                        User customer1 = new User();
                        customer1.setUsername("john_doe");
                        customer1.setEmail("john.doe@example.com");
                        customer1.setPassword(passwordEncoder.encode("password123"));
                        customer1.setFullName("John Doe");
                        customer1.setPhone("0987654321");
                        customer1.setAddress("456 Customer Avenue, Ho Chi Minh, Vietnam");
                        customer1.setRole(User.UserRole.CUSTOMER);
                        customer1.setEnabled(true);

                        User customer2 = new User();
                        customer2.setUsername("jane_smith");
                        customer2.setEmail("jane.smith@example.com");
                        customer2.setPassword(passwordEncoder.encode("password123"));
                        customer2.setFullName("Jane Smith");
                        customer2.setPhone("0912345678");
                        customer2.setAddress("789 Buyer Street, Da Nang, Vietnam");
                        customer2.setRole(User.UserRole.CUSTOMER);
                        customer2.setEnabled(true);

                        User customer3 = new User();
                        customer3.setUsername("mike_wilson");
                        customer3.setEmail("mike.wilson@example.com");
                        customer3.setPassword(passwordEncoder.encode("password123"));
                        customer3.setFullName("Mike Wilson");
                        customer3.setPhone("0901234567");
                        customer3.setAddress("321 Shopping Road, Hai Phong, Vietnam");
                        customer3.setRole(User.UserRole.CUSTOMER);
                        customer3.setEnabled(true);

                        // Staff user
                        User staff = new User();
                        staff.setUsername("staff");
                        staff.setEmail("staff@commerceplatform.com");
                        staff.setPassword(passwordEncoder.encode("staff123"));
                        staff.setFullName("Staff Member");
                        staff.setPhone("0123456788");
                        staff.setAddress("456 Staff Street, Hanoi, Vietnam");
                        staff.setRole(User.UserRole.STAFF);
                        staff.setEnabled(true);

                        userRepository.saveAll(Arrays.asList(admin, customer1, customer2, customer3, staff));
                        log.info("Successfully initialized {} users", 5);
                } else {
                        log.info("Users already exist, skipping initialization");
                }
        }

        private void initializeCategories() {
                if (categoryRepository.count() == 0) {
                        log.info("Initializing categories...");

                        List<Category> categories = Arrays.asList(
                                        createCategory("Electronics", "Electronic devices and gadgets",
                                                        "/images/categories/electronics.jpg"),
                                        createCategory("Clothing", "Fashion and apparel",
                                                        "/images/categories/clothing.jpg"),
                                        createCategory("Books", "Books and educational materials",
                                                        "/images/categories/books.jpg"),
                                        createCategory("Home & Garden", "Home improvement and garden supplies",
                                                        "/images/categories/home-garden.jpg"),
                                        createCategory("Sports & Outdoors", "Sports equipment and outdoor gear",
                                                        "/images/categories/sports.jpg"),
                                        createCategory("Toys & Games", "Toys, games, and entertainment",
                                                        "/images/categories/toys.jpg"),
                                        createCategory("Health & Beauty", "Health and beauty products",
                                                        "/images/categories/health-beauty.jpg"),
                                        createCategory("Food & Beverages", "Food and drink items",
                                                        "/images/categories/food.jpg"));

                        categoryRepository.saveAll(categories);
                        log.info("Successfully initialized {} categories", categories.size());
                } else {
                        log.info("Categories already exist, skipping initialization");
                }
        }

        private void initializeProducts() {
                if (productRepository.count() == 0) {
                        log.info("Initializing products...");

                        List<Category> categories = categoryRepository.findAll();

                        // Electronics Products
                        Category electronics = findCategoryByName(categories, "Electronics");
                        Product laptop = createProduct("Laptop Gaming Asus ROG",
                                        "High-performance gaming laptop with RTX 4060, 16GB RAM, 512GB SSD",
                                        new BigDecimal("29999000"), 15, electronics, "/images/products/laptop.jpg",
                                        true);

                        Product smartphone = createProduct("Samsung Galaxy S24 Ultra",
                                        "Latest flagship smartphone with 200MP camera, 12GB RAM",
                                        new BigDecimal("28999000"), 25, electronics, "/images/products/smartphone.jpg",
                                        true);

                        Product headphone = createProduct("Sony WH-1000XM5 Headphones",
                                        "Premium noise-cancelling wireless headphones",
                                        new BigDecimal("8990000"), 30, electronics, "/images/products/headphones.jpg",
                                        false);

                        Product smartwatch = createProduct("Apple Watch Series 9",
                                        "Advanced health and fitness tracking smartwatch",
                                        new BigDecimal("10990000"), 20, electronics, "/images/products/smartwatch.jpg",
                                        true);

                        // Clothing Products
                        Category clothing = findCategoryByName(categories, "Clothing");
                        Product tshirt = createProduct("Premium Cotton T-Shirt",
                                        "Comfortable 100% cotton t-shirt, available in multiple colors",
                                        new BigDecimal("299000"), 100, clothing, "/images/products/tshirt.jpg", false);

                        Product jeans = createProduct("Slim Fit Denim Jeans",
                                        "Classic blue denim jeans with modern slim fit",
                                        new BigDecimal("599000"), 75, clothing, "/images/products/jeans.jpg", false);

                        Product jacket = createProduct("Winter Jacket",
                                        "Warm and stylish winter jacket with hood",
                                        new BigDecimal("1299000"), 40, clothing, "/images/products/jacket.jpg", true);

                        // Books Products
                        Category books = findCategoryByName(categories, "Books");
                        Product novel = createProduct("The Great Novel Collection",
                                        "Classic literature collection featuring timeless stories",
                                        new BigDecimal("450000"), 50, books, "/images/products/books.jpg", false);

                        Product cookbook = createProduct("Master Chef Cookbook",
                                        "Professional cooking techniques and recipes",
                                        new BigDecimal("350000"), 35, books, "/images/products/cookbook.jpg", false);

                        // Home & Garden Products
                        Category homeGarden = findCategoryByName(categories, "Home & Garden");
                        Product coffeemaker = createProduct("Automatic Coffee Maker",
                                        "Programmable coffee maker with thermal carafe",
                                        new BigDecimal("2490000"), 22, homeGarden, "/images/products/coffeemaker.jpg",
                                        false);

                        Product vacuum = createProduct("Robot Vacuum Cleaner",
                                        "Smart robot vacuum with automatic charging",
                                        new BigDecimal("6990000"), 18, homeGarden, "/images/products/vacuum.jpg", true);

                        // Sports & Outdoors Products
                        Category sports = findCategoryByName(categories, "Sports & Outdoors");
                        Product yoga = createProduct("Premium Yoga Mat",
                                        "Non-slip exercise mat with carrying strap",
                                        new BigDecimal("450000"), 60, sports, "/images/products/yoga-mat.jpg", false);

                        Product bicycle = createProduct("Mountain Bike 27.5\"",
                                        "Durable mountain bike with 21-speed gear system",
                                        new BigDecimal("8500000"), 12, sports, "/images/products/bicycle.jpg", true);

                        // Toys & Games Products
                        Category toys = findCategoryByName(categories, "Toys & Games");
                        Product boardgame = createProduct("Strategy Board Game",
                                        "Family-friendly strategy game for 2-6 players",
                                        new BigDecimal("890000"), 45, toys, "/images/products/boardgame.jpg", false);

                        Product lego = createProduct("Building Blocks Set 1000pcs",
                                        "Creative building blocks for kids and adults",
                                        new BigDecimal("1990000"), 30, toys, "/images/products/lego.jpg", true);

                        // Health & Beauty Products
                        Category health = findCategoryByName(categories, "Health & Beauty");
                        Product skincare = createProduct("Premium Skincare Set",
                                        "Complete skincare routine with natural ingredients",
                                        new BigDecimal("1290000"), 55, health, "/images/products/skincare.jpg", false);

                        Product perfume = createProduct("Luxury Perfume 100ml",
                                        "Long-lasting fragrance with elegant scent",
                                        new BigDecimal("2490000"), 28, health, "/images/products/perfume.jpg", true);

                        // Food & Beverages Products
                        Category food = findCategoryByName(categories, "Food & Beverages");
                        Product coffee = createProduct("Premium Arabica Coffee 500g",
                                        "100% Arabica beans from Vietnam highlands",
                                        new BigDecimal("350000"), 80, food, "/images/products/coffee.jpg", false);

                        Product tea = createProduct("Organic Green Tea Collection",
                                        "Assorted premium green tea varieties",
                                        new BigDecimal("290000"), 65, food, "/images/products/tea.jpg", false);

                        productRepository.saveAll(Arrays.asList(
                                        laptop, smartphone, headphone, smartwatch,
                                        tshirt, jeans, jacket,
                                        novel, cookbook,
                                        coffeemaker, vacuum,
                                        yoga, bicycle,
                                        boardgame, lego,
                                        skincare, perfume,
                                        coffee, tea));

                        log.info("Successfully initialized {} products", 19);
                } else {
                        log.info("Products already exist, skipping initialization");
                }
        }

        private void initializeReviews() {
                if (reviewRepository.count() == 0) {
                        log.info("Initializing reviews...");

                        List<User> users = userRepository.findAll();
                        List<Product> products = productRepository.findAll();

                        if (!users.isEmpty() && !products.isEmpty()) {
                                // Get sample users (excluding admin)
                                User john = findUserByUsername(users, "john_doe");
                                User jane = findUserByUsername(users, "jane_smith");
                                User mike = findUserByUsername(users, "mike_wilson");

                                // Add reviews for some products
                                Review review1 = createReview(products.get(0), john, 5,
                                                "Excellent laptop! Performance is outstanding, perfect for gaming and work.",
                                                true);

                                Review review2 = createReview(products.get(0), jane, 4,
                                                "Great product but a bit pricey. Worth it for the quality though.",
                                                true);

                                Review review3 = createReview(products.get(1), mike, 5,
                                                "Best smartphone I've ever owned. Camera quality is amazing!", true);

                                Review review4 = createReview(products.get(2), john, 5,
                                                "Noise cancellation is incredible. Great for long flights.", true);

                                Review review5 = createReview(products.get(3), jane, 4,
                                                "Very useful smartwatch with great health tracking features.", true);

                                Review review6 = createReview(products.get(4), mike, 5,
                                                "Comfortable t-shirt, fits perfectly. Will buy more colors!", true);

                                Review review7 = createReview(products.get(6), john, 5,
                                                "Perfect winter jacket, keeps me warm in cold weather.", true);

                                Review review8 = createReview(products.get(10), jane, 4,
                                                "Robot vacuum works well, saves a lot of time on cleaning.", true);

                                reviewRepository.saveAll(Arrays.asList(
                                                review1, review2, review3, review4,
                                                review5, review6, review7, review8));

                                log.info("Successfully initialized {} reviews", 8);
                        }
                } else {
                        log.info("Reviews already exist, skipping initialization");
                }
        }

        // Helper methods
        private Category createCategory(String name, String description, String imageUrl) {
                Category category = new Category();
                category.setName(name);
                category.setDescription(description);
                category.setImageUrl(imageUrl);
                return category;
        }

        private Product createProduct(String name, String description, BigDecimal price,
                        int stock, Category category, String imageUrl, boolean featured) {
                Product product = new Product();
                product.setName(name);
                product.setDescription(description);
                product.setPrice(price);
                product.setStockQuantity(stock);
                product.setCategory(category);
                product.setImageOriginal(imageUrl);
                product.setFeatured(featured);
                return product;
        }

        private Review createReview(Product product, User user, int rating, String comment, boolean approved) {
                Review review = new Review();
                review.setProduct(product);
                review.setUser(user);
                review.setRating(rating);
                review.setComment(comment);
                review.setApproved(approved);
                return review;
        }

        private Category findCategoryByName(List<Category> categories, String name) {
                return categories.stream()
                                .filter(c -> c.getName().equals(name))
                                .findFirst()
                                .orElse(null);
        }

        private User findUserByUsername(List<User> users, String username) {
                return users.stream()
                                .filter(u -> u.getUsername().equals(username))
                                .findFirst()
                                .orElse(null);
        }
}
