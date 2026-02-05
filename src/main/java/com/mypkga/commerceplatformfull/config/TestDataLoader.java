package com.mypkga.commerceplatformfull.config;

import com.mypkga.commerceplatformfull.entity.*;
import com.mypkga.commerceplatformfull.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Tạo roles nếu chưa có
        createRoles();

        // Tạo users
        createUsers();

        // Tạo categories
        createCategories();
    }

    private void createRoles() {
        if (roleRepository.count() == 0) {
            log.info("Tạo roles...");

            Role adminRole = new Role("ADMIN", "Administrator with full access", "ALL");
            Role staffRole = new Role("STAFF", "Staff member with return management access", "RETURN_MANAGEMENT,ORDER_VIEW,CUSTOMER_SUPPORT");
            Role customerRole = new Role("CUSTOMER", "Regular customer", "ORDER_CREATE,PROFILE_EDIT");

            roleRepository.saveAll(List.of(adminRole, staffRole, customerRole));
            log.info("Đã tạo 3 roles: ADMIN, STAFF, CUSTOMER");
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
            admin.setEmail("tuan01062004kt@gmail.com");
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
            customer.setEmail("tuannhse182788@fpt.edu.vn");
            customer.setEmailVerified(true);
            customer.setPhone("0903456789");
            customer.setAddress("789 Đường Võ Văn Tần, Quận 3, TP.HCM");
            customer.setRole(customerRole);
            customer.setEnabled(true);

            userRepository.saveAll(List.of(admin, staff, customer));
            log.info("Đã tạo 3 users: admin, staff, customer");
        }
    }

    private void createCategories() {
        if (categoryRepository.count() == 0) {
            log.info("Tạo categories...");

            // Tạo các categories theo yêu cầu
            Category apple = new Category();
            apple.setName("Apple");
            apple.setDescription("Sản phẩm Apple");
            Category msi = new Category();
            msi.setName("MSI");
            msi.setDescription("Sản phẩm MSI");
            Category dell = new Category();
            dell.setName("Dell");
            dell.setDescription("Sản phẩm Dell");

            Category hp = new Category();
            hp.setName("HP");
            hp.setDescription("Sản phẩm HP");

            Category asus = new Category();
            asus.setName("Asus");
            asus.setDescription("Sản phẩm Asus");

            Category lenovo = new Category();
            lenovo.setName("Lenovo");
            lenovo.setDescription("Sản phẩm Lenovo");

            Category acer = new Category();
            acer.setName("Acer");
            acer.setDescription("Sản phẩm Acer");

            Category ms = new Category();
            ms.setName("MS");
            ms.setDescription("Sản phẩm Microsoft");

            Category huawei = new Category();
            huawei.setName("Huawei");
            huawei.setDescription("Sản phẩm Huawei");

            Category gigabyte = new Category();
            gigabyte.setName("Gigabyte");
            gigabyte.setDescription("Sản phẩm Gigabyte");

            categoryRepository.saveAll(List.of(apple, dell, hp, asus, lenovo, acer, ms, huawei, gigabyte,msi));
            log.info("Đã tạo 9 categories: Apple, Dell, HP, Asus, Lenovo, Acer, MS, Huawei, Gigabyte");
        }
    }
}