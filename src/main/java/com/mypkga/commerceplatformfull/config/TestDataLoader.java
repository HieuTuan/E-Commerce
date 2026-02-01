package com.mypkga.commerceplatformfull.config;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Test Data Loader - Tự động tạo dữ liệu test khi ứng dụng khởi động
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PostOfficeRepository postOfficeRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Bắt đầu tạo dữ liệu test...");

        // Tạo roles nếu chưa có
        createRoles();

        // Tạo users
        createUsers();

        // Tạo categories và products
        createProductsAndCategories();

        // Tạo post offices
//        createPostOffices();

        // Tạo orders và return requests


        log.info("Hoàn thành tạo dữ liệu test!");
    }

    private void createRoles() {
        if (roleRepository.count() == 0) {
            log.info("Tạo roles...");

            Role adminRole = new Role("ADMIN", "Administrator with full access", "ALL");
            Role staffRole = new Role("STAFF", "Staff member with return management access", "RETURN_MANAGEMENT,ORDER_VIEW,CUSTOMER_SUPPORT");
            Role customerRole = new Role("CUSTOMER", "Regular customer", "ORDER_CREATE,PROFILE_EDIT");
//            Role postOfficeRole = new Role("POST_OFFICE", "Post office staff for package receiving", "PACKAGE_RECEIVE,RETURN_CONFIRM");

            roleRepository.saveAll(List.of(adminRole, staffRole, customerRole));
            log.info("Đã tạo 4 roles: ADMIN, STAFF, CUSTOMER, POST_OFFICE");
        }
    }

    private void createUsers() {
        if (userRepository.count() == 0) {
            log.info("Tạo users...");

            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            Role staffRole = roleRepository.findByName("STAFF").orElseThrow();
            Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();


            // Admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin123@"));
            admin.setFullName("Quản trị viên hệ thống");
            admin.setEmail("admin@ecommerce.com");
            admin.setEmailVerified(true);
            admin.setPhone("0901234567");
            admin.setAddress("123 Đường Nguyễn Huệ, Quận 1, TP.HCM");
            admin.setRole(adminRole);
            admin.setEnabled(true);

            // Staff user
            User staff = new User();
            staff.setUsername("staff");
            staff.setPassword(passwordEncoder.encode("Admin123@"));
            staff.setFullName("Nguyễn Văn Tuấn");
            staff.setEmail("staff@ecommerce.com");
            staff.setEmailVerified(true);
            staff.setPhone("0902345678");
            staff.setAddress("456 Đường Lê Lợi, Quận 2, TP.HCM");
            staff.setRole(staffRole);
            staff.setEnabled(true);

            // Customer user
            User customer = new User();
            customer.setUsername("customer");
            customer.setPassword(passwordEncoder.encode("Admin123@"));
            customer.setFullName("Trần Thị Hương");
            customer.setEmail("customer@gmail.com");
            customer.setEmailVerified(true);
            customer.setPhone("0903456789");
            customer.setAddress("789 Đường Võ Văn Tần, Quận 3, TP.HCM");
            customer.setRole(customerRole);
            customer.setEnabled(true);


            // Assign to the first post office


            userRepository.saveAll(List.of(admin, staff, customer));
            log.info("Đã tạo 4 users: admin, staff, customer, postoffice");
        }
    }

    private void createProductsAndCategories() {
        if (categoryRepository.count() == 0) {
            log.info("Tạo categories và products...");

            // Tạo category
            Category electronics = new Category();
            electronics.setName("Dell");
            electronics.setDescription("Các sản phẩm điện tử");
            electronics = categoryRepository.save(electronics);

            // Tạo products
            Product laptop = new Product();
            laptop.setName("Laptop Dell Inspiron 15");
            laptop.setDescription("Laptop Dell Inspiron 15 inch, RAM 8GB, SSD 256GB");
            laptop.setPrice(BigDecimal.valueOf(15000000));
            laptop.setStockQuantity(50);
            laptop.setCategory(electronics);
            laptop.setFeatured(true);

            Product phone = new Product();
            phone.setName("iPhone 15 Pro");
            phone.setDescription("iPhone 15 Pro 128GB, màu Titan tự nhiên");
            phone.setPrice(BigDecimal.valueOf(28000000));
            phone.setStockQuantity(30);
            phone.setCategory(electronics);
            phone.setFeatured(true);

            productRepository.saveAll(List.of(laptop, phone));
            log.info("Đã tạo 2 products: Laptop Dell, iPhone 15 Pro");
        }
    }

    private void createPostOffices() {
        if (postOfficeRepository.count() == 0) {
            log.info("Tạo post offices...");

            PostOffice postOffice1 = new PostOffice();
            postOffice1.setName("J&T Express");
            postOffice1.setAddress("100 Nguyễn Huệ, Quận 1, TP.HCM");
            postOffice1.setPhone("028-3829-1234");
            postOffice1.setActive(true);



            postOfficeRepository.saveAll(List.of(postOffice1));
            log.info("Đã tạo 2 post offices");
        }
    }

    private void createOrdersAndReturnRequests() {
        if (orderRepository.count() == 0) {
            log.info("Tạo orders và return requests...");

            User customer = userRepository.findByUsername("customer").orElseThrow();
            Product laptop = productRepository.findByName("Laptop Dell Inspiron 15").orElseThrow();
            Product phone = productRepository.findByName("iPhone 15 Pro").orElseThrow();
            PostOffice postOffice = postOfficeRepository.findAll().get(0);

            // Tạo order 1 - DELIVERED (có thể tạo return request)
            Order order1 = new Order();
            order1.setUser(customer);
            order1.setStatus(OrderStatus.DELIVERED);
            order1.setCurrentStatus(OrderStatus.DELIVERED);
            order1.setPaymentStatus(Order.PaymentStatus.PAID);
            order1.setTotalAmount(BigDecimal.valueOf(15000000));
            order1.setShippingAddress("789 Đường Võ Văn Tần, Quận 3, TP.HCM");
            order1.setCustomerName("Trần Thị Hương");
            order1.setCustomerPhone("0903456789");
            order1.setOrderNumber("ORD-" + System.currentTimeMillis());
            order1.setPaymentMethod("VNPAY");
            order1.setUpdatedDate(LocalDateTime.now().minusDays(1));
            order1 = orderRepository.save(order1);

            // Tạo order item cho order 1
            OrderItem orderItem1 = new OrderItem();
            orderItem1.setOrder(order1);
            orderItem1.setProduct(laptop);
            orderItem1.setQuantity(1);
            orderItem1.setPrice(laptop.getPrice());
            orderItem1.setProductName(laptop.getName());
            orderItemRepository.save(orderItem1);

            // Tạo return request cho order 1 - Status RETURN_APPROVED để test
            ReturnRequest returnRequest = new ReturnRequest();
            returnRequest.setOrder(order1);
            returnRequest.setReason(ReturnReason.DEFECTIVE_ITEM);
            returnRequest.setDetailedDescription("Laptop bị lỗi màn hình, có vệt đen ở góc trái");
            returnRequest.setEvidenceVideoUrl("https://example.com/evidence-video-1.mp4");
            returnRequest.setReturnCode("RET-" + System.currentTimeMillis());
            returnRequest.setStatus(ReturnStatus.RETURN_APPROVED); // Đã duyệt, chờ khách gửi


            // Thông tin ngân hàng
            RefundBankInfo bankInfo = new RefundBankInfo();
            bankInfo.setBankName("Vietcombank");
            bankInfo.setAccountNumber("1234567890");
            bankInfo.setAccountHolderName("TRAN THI HUONG");
            returnRequest.setBankInfo(bankInfo);

            returnRequest.setCreatedAt(LocalDateTime.now());
            returnRequest.setUpdatedAt(LocalDateTime.now());

            returnRequestRepository.save(returnRequest);

            log.info("Đã tạo 1 order và 1 return request");
        }
    }
}