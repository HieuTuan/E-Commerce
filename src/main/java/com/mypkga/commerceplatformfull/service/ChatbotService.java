package com.mypkga.commerceplatformfull.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotService {

    @Autowired
    private ProductRepository productRepository;

    @Value("${groq.api-key}")
    private String apiKey;

    @Value("${groq.api-url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    public String processMessage(String userMessage) {
        // FORCE rule-based response để tránh AI tạo dữ liệu giả
        // Tạm thời tắt AI để đảm bảo chỉ dùng dữ liệu thực
        return processMessageWithRules(userMessage);

        // Uncomment các dòng dưới để bật lại AI
        /*
         * // Check if API is configured
         * if (apiKey == null || apiKey.isEmpty()) {
         * return processMessageWithRules(userMessage);
         * }
         * 
         * try {
         * return processMessageWithAI(userMessage);
         * } catch (Exception e) {
         * log.error("AI chatbot failed, using rule-based response", e);
         * return processMessageWithRules(userMessage);
         * }
         */
    }

    private String processMessageWithRules(String userMessage) {
        String msg = userMessage.toLowerCase();
        
        // 1. TƯ VẤN MUA HÀNG - Phân tích nhu cầu
        if (msg.contains("tư vấn") || msg.contains("tu van") || msg.contains("nên mua") || 
            msg.contains("lựa chọn") || msg.contains("lua chon") || msg.contains("recommend") ||
            msg.contains("gợi ý") || msg.contains("goi y") || msg.contains("phù hợp") || msg.contains("phu hop")) {
            return provideBuyingAdvice(msg, userMessage);
        }
        
        // 2. SO SÁNH GIÁ CẢ
        if (msg.contains("so sánh") || msg.contains("so sanh") || msg.contains("compare") ||
            msg.contains("khác biệt") || msg.contains("khac biet") || msg.contains("vs") || 
            msg.contains("giá cả") || msg.contains("gia ca") || msg.contains("price compare")) {
            return compareProducts(msg, userMessage);
        }
        
        // 3. CHÍNH SÁCH GIAO HÀNG CHI TIẾT
        if (msg.contains("chính sách giao hàng") || msg.contains("chinh sach giao hang") ||
            msg.contains("delivery policy") || msg.contains("thời gian giao") || msg.contains("thoi gian giao") ||
            msg.contains("phí ship") || msg.contains("phi ship") || msg.contains("shipping cost")) {
            return provideDeliveryPolicy(msg);
        }

        // 4. Tìm kiếm sản phẩm - CỤ THỂ VÀ CHÍNH XÁC HỠN
        if ((msg.contains("tìm") && (msg.contains("laptop") || msg.contains("sản phẩm") || msg.contains("máy"))) ||
            (msg.contains("tim") && (msg.contains("laptop") || msg.contains("san pham") || msg.contains("may"))) ||
            (msg.contains("show") && (msg.contains("laptop") || msg.contains("product"))) ||
            (msg.contains("find") && (msg.contains("laptop") || msg.contains("computer"))) ||
            (msg.contains("search") && (msg.contains("laptop") || msg.contains("product"))) ||
            (msg.contains("looking for") && (msg.contains("laptop") || msg.contains("computer"))) ||
            (msg.contains("muốn") && (msg.contains("laptop") || msg.contains("máy tính"))) ||
            (msg.contains("cần") && (msg.contains("laptop") || msg.contains("máy tính"))) ||
            (msg.contains("laptop") && (msg.contains("gaming") || msg.contains("văn phòng") || msg.contains("sinh viên"))) ||
            (msg.contains("gaming") && msg.length() > 6) ||
            (msg.matches(".*\\d+\\s*triệu.*") && msg.contains("laptop"))) {

            List<Product> products = searchProductsFromMessage(msg);
            log.info("Search completed. Found {} products for message: '{}'", products.size(), userMessage);

            // Xác định loại sản phẩm và thương hiệu đang tìm
            String productType = "";
            String brandInfo = "";
            String lowerMsg = msg.toLowerCase();

            // Tìm thương hiệu
            String[] brands = { "msi", "asus", "dell", "hp", "lenovo", "acer", "apple", "macbook", "thinkpad", "gaming",
                    "rog", "predator", "alienware", "surface" };
            for (String brand : brands) {
                if (lowerMsg.contains(brand)) {
                    brandInfo = brand.toUpperCase();
                    break;
                }
            }

            // Xác định loại sản phẩm
            if (msg.contains("laptop") && msg.contains("gaming")) {
                productType = "laptop gaming";
            } else if (msg.contains("laptop")) {
                productType = "laptop";
            } else if (msg.contains("điện thoại") || msg.contains("phone")) {
                productType = "điện thoại";
            } else if (msg.contains("gaming")) {
                productType = "sản phẩm gaming";
            } else {
                productType = "sản phẩm";
            }

            // Tìm giá tiền trong yêu cầu
            String priceRange = extractPriceFromMessage(msg);

            if (!products.isEmpty()) {
                StringBuilder response = new StringBuilder();
                // Thay đổi từ câu hỏi sang câu khẳng định với thông tin thương hiệu
                response.append("🔍 Tôi tìm thấy một số ");
                if (!brandInfo.isEmpty()) {
                    response.append("**").append(brandInfo).append("** ");
                }
                response.append(productType);
                if (!priceRange.isEmpty()) {
                    response.append(" phù hợp với giá ").append(priceRange);
                }
                response.append(" cho bạn:\n\n");

                for (Product product : products.subList(0, Math.min(5, products.size()))) {
                    response.append("🛍️ **").append(product.getName()).append("**\n");
                    response.append("💰 **Giá:** ").append(formatPrice(product.getPrice())).append("\n");

                    // Hiển thị thông số kỹ thuật thực tế từ database
                    if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                        String[] specs = extractSpecs(product.getDescription());
                        int maxSpecs = Math.min(3, specs.length);
                        for (int i = 0; i < maxSpecs; i++) {
                            response.append("  ✓ ").append(specs[i]).append("\n");
                        }
                    }

                    // Hiển thị category nếu có
                    if (product.getCategory() != null) {
                        response.append("  📂 **Danh mục:** ").append(product.getCategory().getName()).append("\n");
                    }

                    // Các action buttons
                    response.append("\n**Hành động:**\n");
                    response.append("🔍 [Xem chi tiết](/products/").append(product.getId()).append(")\n");
                    response.append("🛒 [Thêm vào giỏ hàng](?action=add-to-cart&product=").append(product.getId())
                            .append(")\n");
                    response.append("───────────────────\n\n");
                }

                // Thêm các action tổng quát
                String searchParam = productType.replace(" ", "%20");
                response.append("🎯 **Thêm tùy chọn:**\n");
                response.append("📋 [Xem tất cả ").append(productType).append("](/products?category=")
                        .append(searchParam).append(")\n");
                if (!priceRange.isEmpty()) {
                    response.append("💲 [Lọc theo giá ").append(priceRange).append("](/products?price=")
                            .append(priceRange).append(")\n");
                }
                response.append("🔄 [So sánh sản phẩm](/compare?products=");
                for (int i = 0; i < Math.min(3, products.size()); i++) {
                    if (i > 0)
                        response.append(",");
                    response.append(products.get(i).getId());
                }
                response.append(")\n\n");
                response.append("💬 **Cần hỗ trợ thêm?** Hãy hỏi tôi bất cứ điều gì!");
                return response.toString();

            } else {
                // Không tìm thấy sản phẩm phù hợp
                log.warn("No products found for search: '{}', productType: '{}', priceRange: '{}'", userMessage,
                        productType, priceRange);

                StringBuilder response = new StringBuilder();
                response.append("😔 Rất tiếc, hiện tại chúng tôi không có ").append(productType);
                if (!priceRange.isEmpty()) {
                    response.append(" trong tầm giá ").append(priceRange);
                }
                response.append(" phù hợp với yêu cầu của bạn.\n\n");

                // Gợi ý sản phẩm có sẵn
                try {
                    List<Product> alternativeProducts = getAlternativeProducts();
                    log.info("Found {} alternative products", alternativeProducts.size());

                    if (!alternativeProducts.isEmpty()) {
                        response.append("💡 **Sản phẩm có sẵn trong cửa hàng:**\n\n");

                        for (Product product : alternativeProducts.subList(0,
                                Math.min(3, alternativeProducts.size()))) {
                            response.append("🛍️ **").append(product.getName()).append("**\n");
                            response.append("💰 ").append(formatPrice(product.getPrice())).append("\n");

                            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                                String shortDesc = product.getDescription().length() > 60
                                        ? product.getDescription().substring(0, 60) + "..."
                                        : product.getDescription();
                                response.append("📝 ").append(shortDesc).append("\n");
                            }

                            // Category
                            if (product.getCategory() != null) {
                                response.append("📂 ").append(product.getCategory().getName()).append("\n");
                            }

                            // Action buttons
                            response.append("🔍 [Chi tiết](/products/").append(product.getId()).append(") | ");
                            response.append("🛒 [Mua ngay](?action=add-to-cart&product=").append(product.getId())
                                    .append(")\n");
                            response.append("───────────────────\n\n");
                        }

                        response.append("🏪 [Xem tất cả sản phẩm](/products)\n\n");
                    } else {
                        response.append("🔄 **Thử các gợi ý khác:**\n");
                        response.append("• Sản phẩm nổi bật\n");
                        response.append("• Thông tin giao hàng\n");
                        response.append("• Chính sách đổi trả\n");
                    }
                } catch (Exception e) {
                    log.error("Error getting alternative products", e);
                    response.append("🔄 **Thử lại với từ khóa khác hoặc liên hệ hỗ trợ: 1900-1234**");
                }

                return response.toString();
            }
        }

        // FAQ responses - Tiếng Việt (Cải thiện)
        if (msg.contains("return") || msg.contains("refund") || msg.contains("trả hàng") ||
                msg.contains("tra hang") || msg.contains("hoàn tiền") || msg.contains("hoan tien")) {
            return "📋 **Chính sách đổi trả chi tiết:**\n\n" +
                    "⏰ **Thời gian:**\n" +
                    "• Laptop: 15 ngày (kể từ ngày nhận hàng)\n" +
                    "• Phụ kiện: 7 ngày\n" +
                    "• Sản phẩm lỗi: 12 tháng bảo hành\n\n" +
                    "✅ **Điều kiện đổi trả:**\n" +
                    "• Sản phẩm nguyên vẹn, chưa qua sử dụng\n" +
                    "• Còn đầy đủ hộp, phụ kiện, hóa đơn\n" +
                    "• Không có vết trầy xước, vỡ hỏng\n\n" +
                    "💰 **Chi phí:**\n" +
                    "• Lỗi nhà sản xuất: MIỄN PHÍ\n" +
                    "• Đổi ý khách hàng: Chịu phí ship 2 chiều\n\n" +
                    "📞 **Liên hệ:** 1900-1234 hoặc chat với tôi!\n\n" +
                    "💡 Cần tư vấn đổi trả sản phẩm cụ thể? Hãy nói tên sản phẩm!";
        }

        if (msg.contains("shipping") || msg.contains("delivery") || msg.contains("giao hàng") ||
                msg.contains("vận chuyển") || msg.contains("van chuyen")) {
            return "🚚 **Thông tin vận chuyển:**\n\n" +
                    "• Miễn phí ship: Đơn hàng từ 500.000 VNĐ trở lên\n" +
                    "• Giao hàng tiêu chuẩn: 3-5 ngày làm việc\n" +
                    "• Giao hàng nhanh: Có phụ thu (1-2 ngày)\n\n" +
                    "📍 Bạn muốn kiểm tra thời gian giao hàng đến địa chỉ cụ thể không?";
        }

        if (msg.contains("payment") || msg.contains("pay") || msg.contains("thanh toán") ||
                msg.contains("thanh toan") || msg.contains("tiền")) {
            return "💳 **Phương thức thanh toán:**\n\n" +
                    "• VNPay (Visa, Mastercard, ATM)\n" +
                    "• Thanh toán khi nhận hàng (COD)\n" +
                    "• An toàn: Tất cả giao dịch được mã hóa bảo mật\n\n" +
                    "🔒 Bạn có thắc mắc gì về bảo mật thanh toán không?";
        }

        if (msg.contains("recommend") || msg.contains("suggestion") || 
            (msg.contains("gợi ý") && msg.contains("sản phẩm")) ||
            (msg.contains("goi y") && msg.contains("san pham")) || 
            msg.contains("đề xuất") || msg.contains("de xuat") ||
            msg.contains("sản phẩm nổi bật") || msg.contains("san pham noi bat") ||
            msg.contains("sản phẩm hot") || msg.contains("san pham hot") ||
            msg.contains("bán chạy") || msg.contains("ban chay") ||
            msg.equals("nổi bật") || msg.equals("noi bat") || 
            msg.equals("hot") || msg.equals("featured")) {
            List<Product> featured = productRepository.findByFeaturedTrue();
            if (!featured.isEmpty()) {
                StringBuilder response = new StringBuilder("⭐ **Sản phẩm nổi bật hôm nay:**\n\n");
                for (int i = 0; i < Math.min(5, featured.size()); i++) {
                    Product product = featured.get(i);
                    response.append("🔥 ").append(product.getName())
                            .append(" - ").append(formatPrice(product.getPrice()))
                            .append("\n");
                }
                response.append("\n✨ Những sản phẩm này đang được khách hàng yêu thích nhất!");
                return response.toString();
            }
        }

        // Lời chào và hướng dẫn - PHẢI KIỂM TRA TRƯỚC
        if (msg.contains("xin chào") || msg.contains("hello") || msg.contains("hi") ||
                msg.contains("chào") || msg.equals("") || msg.length() <= 3 ||
                msg.contains("start") || msg.contains("bắt đầu")) {
            return "👋 **Xin chào! Tôi là trợ lý mua sắm thông minh**\n\n" +
                    "🤖 **Tôi có thể giúp bạn:**\n" +
                    "• 🔍 **Tìm sản phẩm:** \"Tìm laptop gaming 20 triệu\"\n" +
                    "• 📊 **So sánh giá:** \"So sánh MSI vs ASUS\"\n" +
                    "• 🎯 **Tư vấn mua hàng:** \"Laptop nào phù hợp sinh viên?\"\n" +
                    "• 🚚 **Chính sách giao hàng:** \"Thông tin vận chuyển\"\n" +
                    "• 💳 **Thanh toán:** \"Phương thức thanh toán\"\n\n" +
                    "💡 **Ví dụ cụ thể:**\n" +
                    "• \"Tư vấn laptop gaming dưới 25 triệu\"\n" +
                    "• \"So sánh laptop Dell vs HP\"\n" +
                    "• \"Chính sách giao hàng HCM\"\n\n" +
                    "🚀 **Hãy bắt đầu bằng cách hỏi tôi điều gì đó!**";
        }

        // Câu hỏi chung không cụ thể
        if (msg.contains("có gì") || msg.contains("co gi") || msg.contains("show all") || 
            msg.contains("tất cả") || msg.contains("tat ca") || msg.contains("all products") ||
            msg.contains("sản phẩm gì") || msg.contains("san pham gi") || msg.equals("?")) {
            return "🤔 **Tôi có rất nhiều sản phẩm để giới thiệu!**\n\n" +
                    "💡 **Để tôi tư vấn tốt nhất, hãy cho tôi biết:**\n" +
                    "• 🎯 **Mục đích:** Gaming, văn phòng, học tập, thiết kế?\n" +
                    "• 💰 **Ngân sách:** Dưới 15 triệu, 15-25 triệu, trên 25 triệu?\n" +
                    "• 🏷️ **Thương hiệu ưa thích:** MSI, ASUS, Dell, HP, Lenovo?\n\n" +
                    "🔥 **Ví dụ câu hỏi cụ thể:**\n" +
                    "• \"Tư vấn laptop gaming 20 triệu\"\n" +
                    "• \"Laptop văn phòng Dell dưới 15 triệu\"\n" +
                    "• \"So sánh MacBook vs ThinkPad\"\n\n" +
                    "🎊 **Hoặc xem sản phẩm nổi bật:** \"Sản phẩm bán chạy\"";
        }

        return "🤔 **Tôi chưa hiểu rõ yêu cầu của bạn.**\n\n" +
                "💡 **Để tôi hỗ trợ tốt nhất, hãy thử:**\n\n" +
                "🔍 **Tìm kiếm cụ thể:**\n" +
                "• \"Tìm laptop gaming 20 triệu\"\n" +
                "• \"Laptop ASUS dưới 15 triệu\"\n" +
                "• \"MSI Creator cho thiết kế\"\n\n" +
                "📊 **So sánh sản phẩm:**\n" +
                "• \"So sánh MSI vs ASUS\"\n" +
                "• \"MacBook Air vs ThinkPad\"\n\n" +
                "🎯 **Tư vấn mua hàng:**\n" +
                "• \"Laptop nào tốt cho sinh viên?\"\n" +
                "• \"Tư vấn laptop gaming dưới 30 triệu\"\n\n" +
                "ℹ️ **Thông tin chính sách:**\n" +
                "• \"Chính sách giao hàng\"\n" +
                "• \"Phương thức thanh toán\"\n" +
                "• \"Chính sách đổi trả\"\n\n" +
                "📞 **Cần hỗ trợ ngay:** 1900-1234";
    }

    private List<Product> searchProductsFromMessage(String message) {
        String[] words = message.split("\\s+");
        List<Product> results = new ArrayList<>();
        BigDecimal maxPrice = null;
        BigDecimal minPrice = null;
        String brandKeyword = null;

        String lowerMessage = message.toLowerCase();
        log.info("Searching for products with message: {}", message);

        // Tìm thương hiệu laptop phổ biến
        String[] brands = { "msi", "asus", "dell", "hp", "lenovo", "acer", "apple", "macbook", "thinkpad", "gaming",
                "rog", "predator", "alienware", "surface" };
        for (String brand : brands) {
            if (lowerMessage.contains(brand)) {
                brandKeyword = brand;
                log.info("Found brand keyword: {}", brand);
                break;
            }
        }

        // Cải thiện việc tìm giá tiền trong tin nhắn
        java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("(\\d+)\\s*triệu");
        java.util.regex.Matcher matcher = pricePattern.matcher(lowerMessage);
        if (matcher.find()) {
            double priceInMillions = Double.parseDouble(matcher.group(1));
            // Tạo khoảng giá linh hoạt (± 2 triệu)
            minPrice = BigDecimal.valueOf(Math.max(0, (priceInMillions - 2) * 1000000));
            maxPrice = BigDecimal.valueOf((priceInMillions + 2) * 1000000);
            log.info("Price range: {} - {} VND", minPrice, maxPrice);
        } else {
            // Tìm số VNĐ trực tiếp
            java.util.regex.Pattern vndPattern = java.util.regex.Pattern
                    .compile("(\\d{1,3}(?:[,.]\\d{3})*)(?:\\s*(?:vnd|đ|dong))?");
            java.util.regex.Matcher vndMatcher = vndPattern.matcher(lowerMessage);
            if (vndMatcher.find()) {
                String priceStr = vndMatcher.group(1).replaceAll("[,.]", "");
                double priceVND = Double.parseDouble(priceStr);
                if (priceVND > 1000000) { // Nếu trên 1 triệu
                    minPrice = BigDecimal.valueOf(priceVND * 0.8); // ± 20%
                    maxPrice = BigDecimal.valueOf(priceVND * 1.2);
                    log.info("VND Price range: {} - {} VND", minPrice, maxPrice);
                }
            }
        }

        // Tìm sản phẩm theo từ khóa và thương hiệu
        Set<Product> uniqueResults = new HashSet<>();

        // Nếu có thương hiệu cụ thể, ưu tiên tìm theo thương hiệu
        if (brandKeyword != null) {
            final String finalBrandKeyword = brandKeyword;
            try {
                List<Product> brandProducts = productRepository.findAll().stream()
                        .filter(p -> p.getName().toLowerCase().contains(finalBrandKeyword) ||
                                (p.getDescription() != null
                                        && p.getDescription().toLowerCase().contains(finalBrandKeyword)))
                        .collect(Collectors.toList());
                uniqueResults.addAll(brandProducts);
                log.info("Found {} products for brand: {}", brandProducts.size(), finalBrandKeyword);
            } catch (Exception e) {
                log.error("Error searching for brand products: {}", finalBrandKeyword, e);
            }
        }

        // Tìm theo các từ khóa khác
        for (String word : words) {
            if (word.length() > 2 && !word.matches("\\d+") && !word.equals("triệu") && !word.equals("laptop")) {
                try {
                    List<Product> found = productRepository.searchProducts(word);
                    uniqueResults.addAll(found);
                    log.info("Found {} products for keyword: {}", found.size(), word);
                } catch (Exception e) {
                    log.error("Error searching for products with keyword: {}", word, e);
                }
            }
        }

        // Nếu không tìm thấy gì, tìm tất cả laptop
        if (uniqueResults.isEmpty()) {
            try {
                List<Product> allLaptops = productRepository.findAll().stream()
                        .filter(p -> p.getName().toLowerCase().contains("laptop") ||
                                p.getName().toLowerCase().contains("macbook") ||
                                (p.getCategory() != null && p.getCategory().getName().toLowerCase().contains("laptop")))
                        .collect(Collectors.toList());
                uniqueResults.addAll(allLaptops);
                log.info("Found {} laptop products as fallback", allLaptops.size());
            } catch (Exception e) {
                log.error("Error searching for laptop products", e);
            }
        }

        results = new ArrayList<>(uniqueResults);

        // Lọc theo khoảng giá nếu có
        if (minPrice != null && maxPrice != null) {
            final BigDecimal finalMinPrice = minPrice;
            final BigDecimal finalMaxPrice = maxPrice;
            results = results.stream()
                    .filter(p -> p.getPrice() != null &&
                            p.getPrice().compareTo(finalMinPrice) >= 0 &&
                            p.getPrice().compareTo(finalMaxPrice) <= 0)
                    .collect(Collectors.toList());
            log.info("Filtered to {} products within price range", results.size());
        }

        // Sắp xếp theo độ phù hợp: thương hiệu trùng khớp trước, sau đó theo giá
        if (brandKeyword != null) {
            final String finalBrandKeyword = brandKeyword;
            results.sort((p1, p2) -> {
                boolean p1HasBrand = p1.getName().toLowerCase().contains(finalBrandKeyword);
                boolean p2HasBrand = p2.getName().toLowerCase().contains(finalBrandKeyword);
                if (p1HasBrand && !p2HasBrand)
                    return -1;
                if (!p1HasBrand && p2HasBrand)
                    return 1;
                // Nếu cùng có hoặc không có thương hiệu, sắp xếp theo giá
                if (p1.getPrice() != null && p2.getPrice() != null) {
                    return p1.getPrice().compareTo(p2.getPrice());
                }
                return 0;
            });
        } else {
            // Sắp xếp theo giá nếu không có thương hiệu cụ thể
            results.sort((p1, p2) -> {
                if (p1.getPrice() != null && p2.getPrice() != null) {
                    return p1.getPrice().compareTo(p2.getPrice());
                }
                return 0;
            });
        }

        return results.stream().limit(10).collect(Collectors.toList());
    }
    
    // PHƯƠNG THỨC HỖ TRỢ
    
    private List<Product> findSuitableProducts(String purpose, String msg) {
        try {
            List<Product> allProducts = productRepository.findAll();
            
            return allProducts.stream()
                .filter(product -> {
                    String productInfo = (product.getName() + " " + 
                        (product.getDescription() != null ? product.getDescription() : "")).toLowerCase();
                    
                    switch (purpose) {
                        case "gaming":
                            return productInfo.contains("gaming") || productInfo.contains("rtx") || 
                                   productInfo.contains("gtx") || productInfo.contains("rog") ||
                                   productInfo.contains("predator") || productInfo.contains("alienware");
                        case "office":
                            return productInfo.contains("thinkpad") || productInfo.contains("latitude") ||
                                   productInfo.contains("elitebook") || productInfo.contains("business");
                        case "student":
                            return productInfo.contains("aspire") || productInfo.contains("pavilion") ||
                                   productInfo.contains("ideapad") || (product.getPrice() != null && 
                                   product.getPrice().compareTo(BigDecimal.valueOf(20000000)) <= 0);
                        case "design":
                            return productInfo.contains("creator") || productInfo.contains("studio") ||
                                   productInfo.contains("quadro") || productInfo.contains("workstation") ||
                                   productInfo.contains("precision");
                        default:
                            return productInfo.contains("laptop");
                    }
                })
                .sorted((p1, p2) -> {
                    if (p1.getPrice() != null && p2.getPrice() != null) {
                        return p1.getPrice().compareTo(p2.getPrice());
                    }
                    return 0;
                })
                .limit(5)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding suitable products for purpose: {}", purpose, e);
            return new ArrayList<>();
        }
    }
    
    private List<Product> findSimilarProducts(Product referenceProduct) {
        try {
            List<Product> allProducts = productRepository.findAll();
            
            return allProducts.stream()
                .filter(product -> !product.getId().equals(referenceProduct.getId()))
                .filter(product -> {
                    // Tìm sản phẩm cùng danh mục hoặc có từ khóa tương tự
                    if (product.getCategory() != null && referenceProduct.getCategory() != null &&
                        product.getCategory().getId().equals(referenceProduct.getCategory().getId())) {
                        return true;
                    }
                    
                    // So sánh giá trong khoảng ±30%
                    if (product.getPrice() != null && referenceProduct.getPrice() != null) {
                        BigDecimal refPrice = referenceProduct.getPrice();
                        BigDecimal minPrice = refPrice.multiply(BigDecimal.valueOf(0.7));
                        BigDecimal maxPrice = refPrice.multiply(BigDecimal.valueOf(1.3));
                        return product.getPrice().compareTo(minPrice) >= 0 && 
                               product.getPrice().compareTo(maxPrice) <= 0;
                    }
                    
                    return false;
                })
                .limit(3)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding similar products", e);
            return new ArrayList<>();
        }
    }
    
    private List<Product> getPopularProducts() {
        try {
            return productRepository.findAll().stream()
                .sorted((p1, p2) -> {
                    // Sắp xếp theo tên (có thể thay bằng số lượt xem, đánh giá, etc.)
                    return p1.getName().compareToIgnoreCase(p2.getName());
                })
                .limit(5)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting popular products", e);
            return new ArrayList<>();
        }
    }
    
    private String extractKeySpecs(String description) {
        if (description == null || description.isEmpty()) {
            return "Thông tin không có";
        }
        
        // Trích xuất thông số quan trọng
        StringBuilder specs = new StringBuilder();
        String lowerDesc = description.toLowerCase();
        
        // Tìm CPU
        if (lowerDesc.contains("i3")) specs.append("Intel i3, ");
        else if (lowerDesc.contains("i5")) specs.append("Intel i5, ");
        else if (lowerDesc.contains("i7")) specs.append("Intel i7, ");
        else if (lowerDesc.contains("i9")) specs.append("Intel i9, ");
        else if (lowerDesc.contains("ryzen")) specs.append("AMD Ryzen, ");
        
        // Tìm RAM
        if (lowerDesc.contains("4gb")) specs.append("4GB RAM, ");
        else if (lowerDesc.contains("8gb")) specs.append("8GB RAM, ");
        else if (lowerDesc.contains("16gb")) specs.append("16GB RAM, ");
        else if (lowerDesc.contains("32gb")) specs.append("32GB RAM, ");
        
        // Tìm GPU
        if (lowerDesc.contains("rtx")) {
            if (lowerDesc.contains("3060")) specs.append("RTX 3060, ");
            else if (lowerDesc.contains("3070")) specs.append("RTX 3070, ");
            else if (lowerDesc.contains("4060")) specs.append("RTX 4060, ");
            else specs.append("RTX GPU, ");
        } else if (lowerDesc.contains("gtx")) {
            specs.append("GTX GPU, ");
        }
        
        String result = specs.toString();
        return result.isEmpty() ? "Cấu hình cơ bản" : result.substring(0, result.length() - 2);
    }

    private String processMessageWithAI(String userMessage) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiUrl);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", "Bearer " + apiKey);

            // Tìm sản phẩm liên quan đến tin nhắn của user
            List<Product> relevantProducts = searchProductsFromMessage(userMessage);

            StringBuilder productContext = new StringBuilder();
            if (!relevantProducts.isEmpty()) {
                productContext.append("Sản phẩm liên quan:\n");
                for (Product p : relevantProducts) {
                    productContext.append("- ").append(p.getName())
                            .append(" (").append(formatPrice(p.getPrice())).append(")\n");

                    // Thêm thông số kỹ thuật
                    if (p.getDescription() != null) {
                        String[] specs = extractSpecs(p.getDescription());
                        for (String spec : specs) {
                            if (specs.length <= 3) { // Chỉ hiện 3 specs đầu để không quá dài
                                productContext.append("  + ").append(spec).append("\n");
                            }
                        }
                    }
                }
            } else {
                // Nếu không tìm thấy sản phẩm liên quan, hiển thị sản phẩm thay thế
                List<Product> alternativeProducts = getAlternativeProducts();
                productContext.append("Sản phẩm có sẵn:\n");
                for (Product p : alternativeProducts.subList(0, Math.min(5, alternativeProducts.size()))) {
                    productContext.append("- ").append(p.getName())
                            .append(" (").append(formatPrice(p.getPrice())).append(")\n");
                }
            }

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);

            JsonArray messages = new JsonArray();

            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content",
                    "Bạn là trợ lý mua sắm thông minh của cửa hàng thương mại điện tử. " +
                            "BẮT BUỘC phải trả lời bằng tiếng Việt. " +
                            "QUAN TRỌNG: CHỈ sử dụng thông tin sản phẩm có trong danh sách bên dưới. " +
                            "TUYỆT ĐỐI KHÔNG tự tạo ra tên sản phẩm, giá cả, hoặc thông số kỹ thuật không có trong dữ liệu.\n"
                            +
                            "DANH SÁCH SẢN PHẨM CỬA HÀNG:\n" +
                            productContext.toString() + "\n" +
                            "Nếu không có sản phẩm phù hợp trong danh sách trên:\n" +
                            "- Nói rõ 'Rất tiếc, chúng tôi hiện không có sản phẩm phù hợp'\n" +
                            "- Chỉ gợi ý các sản phẩm có trong danh sách trên\n" +
                            "- KHÔNG được tạo ra sản phẩm mới\n\n" +
                            "Chính sách cửa hàng:\n" +
                            "- 🚚 Miễn phí vận chuyển đơn hàng trên 500.000đ\n" +
                            "- 🔄 Đổi trả trong 7 ngày, không cần lý do\n" +
                            "- 💳 Thanh toán: COD, chuyển khoản, thẻ tín dụng\n" +
                            "- 🛡️ Bảo hành chính hãng theo quy định nhà sản xuất\n" +
                            "- 📞 Hỗ trợ 24/7 qua hotline và chat");
            messages.add(systemMessage);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);

            requestBody.add("messages", messages);
            requestBody.addProperty("max_tokens", 800);
            requestBody.addProperty("temperature", 0.7);

            request.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                Gson gson = new Gson();
                JsonObject responseJson = gson.fromJson(result.toString(), JsonObject.class);

                return responseJson
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();
            }
        }
    }

    public List<Product> getRecommendations() {
        return productRepository.findByFeaturedTrue();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null)
            return "Liên hệ";

        // Giá đã là VND, không cần convert
        long vndPrice = price.longValue();

        if (vndPrice >= 1000000) {
            double millions = vndPrice / 1000000.0;
            return String.format("%.1f triệu đ", millions);
        } else if (vndPrice >= 1000) {
            double thousands = vndPrice / 1000.0;
            return String.format("%.0f nghìn đ", thousands);
        } else {
            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            return formatter.format(vndPrice) + " đ";
        }
    }

    private String extractPriceFromMessage(String message) {
        String lowerMessage = message.toLowerCase();

        // Tìm pattern số + triệu (linh hoạt hơn)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*triệu");
        java.util.regex.Matcher matcher = pattern.matcher(lowerMessage);
        if (matcher.find()) {
            String priceStr = matcher.group(1).replace(",", ".");
            return "khoảng " + priceStr + " triệu";
        }

        // Tìm các cụm từ về giá phổ biến
        if (lowerMessage.contains("dưới 20 triệu") || lowerMessage.contains("< 20 triệu"))
            return "dưới 20 triệu";
        if (lowerMessage.contains("trên 25 triệu") || lowerMessage.contains("> 25 triệu"))
            return "trên 25 triệu";
        if (lowerMessage.contains("từ 15 đến 20 triệu"))
            return "15-20 triệu";
        if (lowerMessage.contains("từ 20 đến 25 triệu"))
            return "20-25 triệu";
        if (lowerMessage.contains("từ 25 đến 30 triệu"))
            return "25-30 triệu";

        // Tìm số VNĐ trực tiếp
        java.util.regex.Pattern vndPattern = java.util.regex.Pattern
                .compile("(\\d{1,2})(?:[,.]\\d{3}){2,}\\s*(?:vnd|đ|dong)?");
        java.util.regex.Matcher vndMatcher = vndPattern.matcher(lowerMessage);
        if (vndMatcher.find()) {
            String fullPrice = vndMatcher.group(0);
            // Chuyển đổi sang triệu để dễ hiểu
            try {
                String numberPart = vndMatcher.group(0).replaceAll("[^\\d,.]", "").replace(",", "").replace(".", "");
                if (numberPart.length() >= 7) { // Từ 1 triệu trở lên
                    double millions = Double.parseDouble(numberPart) / 1000000.0;
                    return String.format("khoảng %.1f triệu", millions);
                }
            } catch (NumberFormatException e) {
                log.warn("Could not parse price: {}", fullPrice);
            }
        }

        return "";
    }
    
    // PHƯƠNG THỨC TƯ VẤN MUA HÀNG
    private String provideBuyingAdvice(String msg, String originalMessage) {
        StringBuilder advice = new StringBuilder();
        advice.append("🎯 **Tư vấn mua hàng chuyên nghiệp**\n\n");
        
        // Phân tích nhu cầu dựa trên từ khóa
        String purpose = "";
        
        // Xác định mục đích sử dụng
        if (msg.contains("gaming") || msg.contains("chơi game") || msg.contains("choi game")) {
            purpose = "gaming";
            advice.append("🎮 **Laptop Gaming - Gợi ý của chúng tôi:**\n");
            advice.append("• **Dưới 20 triệu:** ASUS TUF Gaming - GTX 1650, i5, 8GB RAM\n");
            advice.append("• **20-30 triệu:** MSI Gaming - RTX 3060, i7, 16GB RAM\n");
            advice.append("• **Trên 30 triệu:** ASUS ROG - RTX 4070, i9, 32GB RAM\n\n");
        } else if (msg.contains("văn phòng") || msg.contains("van phong") || msg.contains("office") || msg.contains("làm việc")) {
            purpose = "office";
            advice.append("💼 **Laptop Văn phòng - Lựa chọn tối ưu:**\n");
            advice.append("• **Tiết kiệm:** HP Pavilion - i3, 8GB, SSD 256GB\n");
            advice.append("• **Cân bằng:** Lenovo ThinkPad - i5, 16GB, SSD 512GB\n");
            advice.append("• **Cao cấp:** Dell Latitude - i7, 16GB, SSD 1TB\n\n");
        } else if (msg.contains("sinh viên") || msg.contains("sinh vien") || msg.contains("student") || msg.contains("học tập")) {
            purpose = "student";
            advice.append("🎓 **Laptop Sinh viên - Phù hợp túi tiền:**\n");
            advice.append("• **Cơ bản:** Acer Aspire - i3, 4GB, HDD 1TB (10-12 triệu)\n");
            advice.append("• **Nâng cao:** HP Pavilion - i5, 8GB, SSD 256GB (15-18 triệu)\n");
            advice.append("• **Premium:** MacBook Air M1 - 8GB, SSD 256GB (22-25 triệu)\n\n");
        } else if (msg.contains("thiết kế") || msg.contains("thiet ke") || msg.contains("design") || msg.contains("đồ họa")) {
            purpose = "design";
            advice.append("🎨 **Laptop Thiết kế - Hiệu năng cao:**\n");
            advice.append("• **Entry:** Dell Inspiron - i5, 16GB, GTX 1650\n");
            advice.append("• **Pro:** MSI Creator - i7, 32GB, RTX 3070\n");
            advice.append("• **Workstation:** ThinkPad P1 - i9, 64GB, Quadro RTX\n\n");
        }
        
        // Tìm sản phẩm phù hợp trong database
        List<Product> suitableProducts = findSuitableProducts(purpose, msg);
        if (!suitableProducts.isEmpty()) {
            advice.append("✅ **Sản phẩm phù hợp có sẵn:**\n\n");
            for (Product product : suitableProducts.subList(0, Math.min(3, suitableProducts.size()))) {
                advice.append("📱 **").append(product.getName()).append("**\n");
                advice.append("💰 **Giá:** ").append(formatPrice(product.getPrice())).append("\n");
                if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                    String shortDesc = product.getDescription().length() > 100 ? 
                        product.getDescription().substring(0, 100) + "..." : product.getDescription();
                    advice.append("📝 ").append(shortDesc).append("\n");
                }
                advice.append("🔍 [Xem chi tiết](/products/").append(product.getId()).append(")\n");
                advice.append("───────────────\n\n");
            }
        }
        
        advice.append("💡 **Lời khuyên mua sắm:**\n");
        advice.append("• So sánh cấu hình và giá cả trước khi quyết định\n");
        advice.append("• Kiểm tra chính sách bảo hành và hỗ trợ\n");
        advice.append("• Cân nhắc nhu cầu sử dụng trong 3-5 năm tới\n\n");
        advice.append("❓ **Cần tư vấn thêm?** Hãy cho tôi biết:\n");
        advice.append("• Ngân sách của bạn\n");
        advice.append("• Mục đích sử dụng chính\n");
        advice.append("• Thương hiệu ưa thích\n");
        
        return advice.toString();
    }
    
    // PHƯƠNG THỨC SO SÁNH SẢN PHẨM
    private String compareProducts(String msg, String originalMessage) {
        StringBuilder comparison = new StringBuilder();
        comparison.append("📊 **So sánh sản phẩm chuyên nghiệp**\n\n");
        
        // Tìm các sản phẩm để so sánh
        List<Product> products = searchProductsFromMessage(msg);
        
        if (products.size() >= 2) {
            comparison.append("🔍 **So sánh 2 sản phẩm hàng đầu:**\n\n");
            
            Product product1 = products.get(0);
            Product product2 = products.get(1);
            
            // So sánh chi tiết
            comparison.append("📱 **").append(product1.getName()).append("**\n");
            comparison.append("💰 Giá: **").append(formatPrice(product1.getPrice())).append("**\n");
            if (product1.getDescription() != null) {
                comparison.append("📝 ").append(extractKeySpecs(product1.getDescription())).append("\n");
            }
            comparison.append("\n🆚\n\n");
            
            comparison.append("📱 **").append(product2.getName()).append("**\n");
            comparison.append("💰 Giá: **").append(formatPrice(product2.getPrice())).append("**\n");
            if (product2.getDescription() != null) {
                comparison.append("📝 ").append(extractKeySpecs(product2.getDescription())).append("\n");
            }
            
            // Phân tích giá cả
            comparison.append("\n💡 **Phân tích:**\n");
            if (product1.getPrice() != null && product2.getPrice() != null) {
                BigDecimal priceDiff = product1.getPrice().subtract(product2.getPrice()).abs();
                String cheaperProduct = product1.getPrice().compareTo(product2.getPrice()) < 0 ? 
                    product1.getName() : product2.getName();
                comparison.append("• **Giá rẻ hơn:** ").append(cheaperProduct)
                    .append(" (tiết kiệm ").append(formatPrice(priceDiff)).append(")\n");
            }
            
            comparison.append("\n🎯 **Gợi ý lựa chọn:**\n");
            comparison.append("• **Nếu ưu tiên giá:** Chọn sản phẩm rẻ hơn\n");
            comparison.append("• **Nếu ưu tiên hiệu năng:** So sánh cấu hình chi tiết\n");
            comparison.append("• **Nếu cần tư vấn:** Liên hệ hotline 1900-1234\n\n");
            
            comparison.append("🔗 **Hành động:**\n");
            comparison.append("🔍 [So sánh chi tiết](/compare?p1=").append(product1.getId())
                .append("&p2=").append(product2.getId()).append(")\n");
            comparison.append("🛒 [Mua ").append(product1.getName()).append("](?action=add-to-cart&product=")
                .append(product1.getId()).append(")\n");
            comparison.append("🛒 [Mua ").append(product2.getName()).append("](?action=add-to-cart&product=")
                .append(product2.getId()).append(")\n");
            
        } else if (products.size() == 1) {
            comparison.append("📱 Tìm thấy 1 sản phẩm: **").append(products.get(0).getName()).append("**\n\n");
            comparison.append("🔍 **Để so sánh, hãy thử:**\n");
            comparison.append("• \"So sánh [tên sản phẩm] vs [sản phẩm khác]\"\n");
            comparison.append("• \"So sánh laptop MSI vs ASUS\"\n");
            comparison.append("• \"Compare [brand] laptops\"\n\n");
            
            // Gợi ý sản phẩm tương tự để so sánh
            List<Product> similarProducts = findSimilarProducts(products.get(0));
            if (!similarProducts.isEmpty()) {
                comparison.append("💡 **Sản phẩm tương tự để so sánh:**\n");
                for (Product similar : similarProducts.subList(0, Math.min(2, similarProducts.size()))) {
                    comparison.append("• ").append(similar.getName()).append(" - ")
                        .append(formatPrice(similar.getPrice())).append("\n");
                }
            }
        } else {
            comparison.append("❌ **Không tìm thấy sản phẩm phù hợp để so sánh**\n\n");
            comparison.append("💡 **Thử các cách sau:**\n");
            comparison.append("• \"So sánh laptop gaming MSI vs ASUS\"\n");
            comparison.append("• \"Compare MacBook vs ThinkPad\"\n");
            comparison.append("• \"So sánh laptop 20 triệu\"\n\n");
            
            comparison.append("🔥 **Sản phẩm phổ biến để so sánh:**\n");
            List<Product> popularProducts = getPopularProducts();
            for (Product product : popularProducts.subList(0, Math.min(3, popularProducts.size()))) {
                comparison.append("• ").append(product.getName()).append(" - ")
                    .append(formatPrice(product.getPrice())).append("\n");
            }
        }
        
        return comparison.toString();
    }
    
    // PHƯƠNG THỨC CHÍNH SÁCH GIAO HÀNG CHI TIẾT
    private String provideDeliveryPolicy(String msg) {
        StringBuilder policy = new StringBuilder();
        policy.append("🚚 **Chính sách giao hàng chi tiết**\n\n");
        
        // Phí vận chuyển
        policy.append("💰 **Phí vận chuyển:**\n");
        policy.append("• **MIỄN PHÍ:** Đơn hàng từ 500.000đ trở lên\n");
        policy.append("• **Nội thành HCM/HN:** 30.000đ (dưới 500k)\n");
        policy.append("• **Tỉnh thành khác:** 50.000đ (dưới 500k)\n");
        policy.append("• **Vùng xa:** 80.000đ (dưới 500k)\n\n");
        
        // Thời gian giao hàng
        policy.append("⏰ **Thời gian giao hàng:**\n");
        policy.append("• **Nội thành HCM/HN:** 1-2 ngày làm việc\n");
        policy.append("• **Các tỉnh thành:** 2-3 ngày làm việc\n");
        policy.append("• **Vùng xa/đảo:** 3-5 ngày làm việc\n");
        policy.append("• **Giao hàng nhanh:** +50k phí (trong ngày)\n\n");
        
        // Đơn vị vận chuyển
        policy.append("📦 **Đối tác vận chuyển:**\n");
        policy.append("• **Giao hàng nhanh (GHN)** - Toàn quốc\n");
        policy.append("• **Viettel Post** - Vùng xa, đảo\n");
        policy.append("• **Grab/Be** - Giao hàng trong ngày\n\n");
        
        // Chính sách đặc biệt
        policy.append("⭐ **Ưu đãi đặc biệt:**\n");
        policy.append("• **Laptop > 15 triệu:** MIỄN PHÍ + Bảo hiểm\n");
        policy.append("• **Đơn > 1 triệu:** Giao 2 lần nếu vắng nhà\n");
        policy.append("• **Sản phẩm dễ vỡ:** Đóng gói đặc biệt\n\n");
        
        // Theo dõi đơn hàng
        policy.append("📱 **Theo dõi đơn hàng:**\n");
        policy.append("• **SMS/Email:** Thông báo tự động\n");
        policy.append("• **Website:** Tra cứu bằng mã đơn\n");
        policy.append("• **Hotline:** 1900-1234 (8h-22h)\n\n");
        
        // Chính sách hoàn tiền ship
        policy.append("💸 **Chính sách đặc biệt:**\n");
        policy.append("• **Giao trễ > 1 ngày:** Hoàn phí ship\n");
        policy.append("• **Sản phẩm lỗi:** Đổi trả miễn phí\n");
        policy.append("• **Hủy đơn do shop:** Hoàn 100% + phí ship\n\n");
        
        policy.append("📞 **Cần hỗ trợ thêm?**\n");
        policy.append("• Chat với tôi: \"Kiểm tra thời gian giao đến [địa chỉ]\"\n");
        policy.append("• Gọi hotline: 1900-1234\n");
        policy.append("• Email: support@shop.com\n");
        
        return policy.toString();
    }

    private String[] extractSpecs(String description) {
        // Chỉ lấy thông số từ mô tả thực tế trong database
        List<String> specs = new ArrayList<>();

        if (description == null || description.isEmpty()) {
            return specs.toArray(new String[0]);
        }

        // Lấy thông số từ mô tả thực tế
        if (description.contains("-") || description.contains("•") || description.contains("*")) {
            String[] lines = description.split("[\\n\\r]+");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                    String spec = line.substring(1).trim();
                    if (!spec.isEmpty()) {
                        specs.add(spec);
                    }
                }
            }
        } else {
            // Nếu không có format đặc biệt, cắt ngắn mô tả
            if (description.length() > 100) {
                specs.add(description.substring(0, 100) + "...");
            } else {
                specs.add(description);
            }
        }

        return specs.toArray(new String[0]);
    }

    private List<Product> getAlternativeProducts() {
        try {
            // Ưu tiên sản phẩm nổi bật
            List<Product> featured = productRepository.findByFeaturedTrue();
            if (!featured.isEmpty()) {
                log.info("Returning {} featured products as alternatives", featured.size());
                return featured;
            }

            // Nếu không có sản phẩm nổi bật, lấy sản phẩm mới nhất
            List<Product> recent = productRepository.findTop5ByOrderByCreatedDateDesc();
            if (!recent.isEmpty()) {
                log.info("Returning {} recent products as alternatives", recent.size());
                return recent;
            }

            // Cuối cùng, lấy bất kỳ sản phẩm nào có sẵn
            List<Product> allProducts = productRepository.findAll();
            if (!allProducts.isEmpty()) {
                log.info("Returning {} random products as alternatives", Math.min(5, allProducts.size()));
                return allProducts.subList(0, Math.min(5, allProducts.size()));
            }

            log.warn("No products found in database for alternatives");
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error getting alternative products from database", e);
            return new ArrayList<>();
        }
    }
}