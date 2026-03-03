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
import java.util.stream.Stream;

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

        // Xử lý yêu cầu so sánh sản phẩm
        if (msg.contains("so sánh") || msg.contains("so sanh") || msg.contains("compare") ||
                msg.contains("comparison") || msg.contains("khác biệt") || msg.contains("khac biet") ||
                (msg.contains("và") && (msg.contains("asus") || msg.contains("acer") || msg.contains("msi") ||
                        msg.contains("dell") || msg.contains("hp") || msg.contains("lenovo")))) {
            return handleProductComparison(userMessage);
        }

        // Tìm kiếm sản phẩm - hỗ trợ tiếng Việt
        if (msg.contains("tìm") || msg.contains("tim") || msg.contains("show") || msg.contains("find") ||
                msg.contains("search") || msg.contains("looking for") || msg.contains("muốn") ||
                msg.contains("cần") || msg.contains("laptop") || msg.contains("gaming") ||
                msg.contains("sản phẩm") || msg.contains("san pham")) {

            List<Product> products = searchProductsFromMessage(msg);
            log.info("Search completed. Found {} products for message: '{}'", products.size(), userMessage);

            // Detect category và price range để kiểm tra exact match
            String detectedCategory = detectAiCategoryFromMessage(msg);
            PriceFilter pf = parsePriceFilter(msg);
            // Tạo nhãn hiển thị cho giá
            String priceRange = "";
            if (pf.minPrice != null || pf.maxPrice != null) {
                java.util.regex.Matcher pfMatcher = java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*triệu")
                        .matcher(msg);
                if (pfMatcher.find()) {
                    String numStr = pfMatcher.group(1);
                    if ("under".equals(pf.type)) {
                        priceRange = "dưới " + numStr + " triệu";
                    } else if ("above".equals(pf.type)) {
                        priceRange = "trên " + numStr + " triệu";
                    } else {
                        priceRange = "khoảng " + numStr + " triệu";
                    }
                }
            }

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

            if (!products.isEmpty()) {
                StringBuilder response = new StringBuilder();

                // Kiểm tra xem có phải là kết quả chính xác hay đề xuất thay thế
                boolean isExactMatch = true;
                boolean hasPriceFilter = !priceRange.isEmpty();
                boolean hasBrandFilter = !brandInfo.isEmpty();

                // Tạo biến final để sử dụng trong lambda expressions
                final String finalBrandInfoForLambda = brandInfo;
                final String finalDetectedCategoryForLambda = detectedCategory;

                // Logic kiểm tra exact match - dùng lại parsePriceFilter để nhất quán
                if (hasBrandFilter && hasPriceFilter) {
                    PriceFilter checkPf = parsePriceFilter(userMessage.toLowerCase());
                    final BigDecimal checkMin = checkPf.minPrice;
                    final BigDecimal checkMax = checkPf.maxPrice;

                    boolean hasExactBrandAndPrice = products.stream()
                            .anyMatch(p -> {
                                boolean matchesBrand = p.getName().toLowerCase()
                                        .contains(finalBrandInfoForLambda.toLowerCase()) ||
                                        (p.getDescription() != null && p.getDescription().toLowerCase()
                                                .contains(finalBrandInfoForLambda.toLowerCase()));
                                boolean matchesPrice = p.getPrice() != null &&
                                        (checkMin == null || p.getPrice().compareTo(checkMin) >= 0) &&
                                        (checkMax == null || p.getPrice().compareTo(checkMax) <= 0);
                                return matchesBrand && matchesPrice;
                            });

                    if (!hasExactBrandAndPrice) {
                        isExactMatch = false;
                    }
                }
                // Kiểm tra khi chỉ có brand mà không có price
                else if (hasBrandFilter && !hasPriceFilter) {
                    boolean hasBrandMatch = products.stream()
                            .anyMatch(p -> p.getName().toLowerCase().contains(finalBrandInfoForLambda.toLowerCase()) ||
                                    (p.getDescription() != null && p.getDescription().toLowerCase()
                                            .contains(finalBrandInfoForLambda.toLowerCase())));
                    if (!hasBrandMatch) {
                        isExactMatch = false;
                    }
                }
                // Kiểm tra khi chỉ có price mà không có brand
                else if (!hasBrandFilter && hasPriceFilter && detectedCategory != null) {
                    // Kiểm tra xem có sản phẩm nào vừa đúng category vừa đúng giá không
                    boolean hasExactCategoryAndPrice = products.stream()
                            .anyMatch(p -> p.getAiCategory() != null
                                    && finalDetectedCategoryForLambda.equals(p.getAiCategory()));

                    if (!hasExactCategoryAndPrice) {
                        isExactMatch = false;
                    }
                }

                // Tạo header response dựa trên loại kết quả
                if (isExactMatch) {
                    response.append("🔍 Tôi tìm thấy một số ");
                    response.append(productType);
                    if (!brandInfo.isEmpty()) {
                        response.append(" **").append(brandInfo).append("**");
                    }
                    if (!priceRange.isEmpty()) {
                        response.append(" phù hợp với giá ").append(priceRange);
                    }
                    response.append(" cho bạn:\n\n");
                } else {
                    response.append("🤔 Rất tiếc, tôi không tìm thấy ").append(productType);
                    if (!brandInfo.isEmpty()) {
                        response.append(" ").append(brandInfo);
                    }
                    if (!priceRange.isEmpty()) {
                        response.append(" với mức giá ").append(priceRange);
                    }
                    response.append(" phù hợp.\n\n");
                    response.append(
                            "💡 **Tôi sẽ đề xuất một vài sản phẩm phù hợp với một trong số những yêu cầu của bạn:**\n\n");
                }

                // Tạo các biến final để sử dụng trong loop
                final boolean finalIsExactMatch = isExactMatch;
                final String finalBrandInfo = brandInfo;
                final String finalDetectedCategory = detectedCategory;
                final boolean finalHasPriceFilter = hasPriceFilter;

                for (Product product : products.subList(0, Math.min(5, products.size()))) {
                    response.append("🛍️ **").append(product.getName()).append("**\n");
                    response.append("💰 **Giá:** ").append(formatPrice(product.getPrice()));

                    // Thêm thông tin về mức độ phù hợp
                    if (!finalIsExactMatch) {
                        boolean matchesBrand = !finalBrandInfo.isEmpty() &&
                                (product.getName().toLowerCase().contains(finalBrandInfo.toLowerCase()) ||
                                        (product.getDescription() != null && product.getDescription().toLowerCase()
                                                .contains(finalBrandInfo.toLowerCase())));
                        boolean matchesCategory = finalDetectedCategory != null
                                && finalDetectedCategory.equals(product.getAiCategory());

                        if (matchesBrand) {
                            response.append(" 🏷️ *Đúng thương hiệu*");
                        } else if (matchesCategory) {
                            response.append(" ✅ *Đúng loại*");
                        } else if (finalHasPriceFilter) {
                            response.append(" 💲 *Giá phù hợp*");
                        }
                    }
                    response.append("\n");

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
                        response.append("  📂 **Danh mục:** ").append(product.getCategory().getName());
                        if (product.getAiCategory() != null) {
                            response.append(" (").append(product.getAiCategory()).append(")");
                        }
                        response.append("\n");
                    }

                    // Các action buttons
                    response.append("\n**Hành động:**\n");
                    response.append("🔍 [Xem chi tiết](/products/").append(product.getId()).append(")\n");
                    response.append(
                            "<button class='btn btn-sm btn-primary chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                            .append(product.getId()).append("'>🛒 Thêm vào giỏ hàng</button>\n");
                    response.append("───────────────────\n\n");
                }

                // Thêm gợi ý tìm kiếm khác nếu là kết quả thay thế
                if (!isExactMatch) {
                    response.append("🎯 **Gợi ý tìm kiếm khác:**\n");
                    if (detectedCategory != null) {
                        response.append("• \"").append(productType).append(" giá rẻ\" - Tìm tùy chọn rẻ hơn\n");
                        response.append("• \"").append(productType).append(" cao cấp\" - Tìm tùy chọn tốt hơn\n");
                    }
                    if (hasPriceFilter) {
                        response.append("• \"laptop ").append(priceRange).append("\" - Tìm laptop khác cùng giá\n");
                        response.append("• \"laptop ").append(priceRange).append(" bất kỳ\" - Mở rộng tìm kiếm\n");
                    }
                    response.append("\n");
                }

                // Thêm tùy chọn đơn giản
                response.append("🎯 **Thêm tùy chọn:**\n");
                response.append("📋 [Xem tất cả sản phẩm](/products)\n");
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
                            response.append(
                                    "<button class='btn btn-sm btn-success chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                                    .append(product.getId())
                                    .append("'>🛒 Mua ngay</button>\n");
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

        // FAQ responses - Tiếng Việt
        if (msg.contains("return") || msg.contains("refund") || msg.contains("trả hàng") ||
                msg.contains("tra hang") || msg.contains("hoàn tiền") || msg.contains("hoan tien")) {
            return "📋 **Chính sách đổi trả:**\n\n" +
                    "• Thời gian: Trong vòng 30 ngày kể từ ngày mua\n" +
                    "• Điều kiện: Sản phẩm chưa sử dụng, còn nguyên bao bì\n" +
                    "• Liên hệ: Vui lòng liên hệ team hỗ trợ để được trợ giúp\n\n" +
                    "💬 Bạn có cần hỗ trợ thêm về chính sách đổi trả không?";
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

        if (msg.contains("recommend") || msg.contains("suggestion") || msg.contains("gợi ý") ||
                msg.contains("goi y") || msg.contains("đề xuất") || msg.contains("de xuat") ||
                msg.contains("nổi bật") || msg.contains("hot") || msg.contains("bán chạy")) {
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

        // Lời chào và hướng dẫn
        if (msg.contains("xin chào") || msg.contains("hello") || msg.contains("hi") ||
                msg.contains("chào") || msg.length() < 10) {
            return "👋 **Xin chào! Tôi là trợ lý mua sắm thông minh**\n\n" +
                    "🤖 Tôi có thể giúp bạn:\n" +
                    "• 🔍 Tìm sản phẩm phù hợp\n" +
                    "• 💰 So sánh giá cả\n" +
                    "• 📦 Thông tin vận chuyển\n" +
                    "• 💳 Hướng dẫn thanh toán\n" +
                    "• 🔄 Chính sách đổi trả\n\n" +
                    "💡 **Thử hỏi:** \"Tìm laptop gaming 20 triệu\" hoặc \"Sản phẩm nổi bật\"";
        }

        return "🤔 Tôi chưa hiểu rõ yêu cầu của bạn. Bạn có thể nói rõ hơn không?\n\n" +
                "💡 **Gợi ý:** Hãy thử hỏi về:\n" +
                "• Tìm sản phẩm: \"Tìm laptop gaming\"\n" +
                "• Sản phẩm nổi bật: \"Gợi ý sản phẩm\"\n" +
                "• Chính sách: \"Thông tin giao hàng\"";
    }

    // Nội bộ: kết quả parse giá
    private static class PriceFilter {
        BigDecimal minPrice = null; // null = không giới hạn dưới
        BigDecimal maxPrice = null; // null = không giới hạn trên
        String type = ""; // "under", "above", "around", ""
    }

    private PriceFilter parsePriceFilter(String lowerMessage) {
        PriceFilter pf = new PriceFilter();
        // Pattern: số + triệu (VD: "20 triệu", "20triệu")
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*triệu");
        java.util.regex.Matcher m = p.matcher(lowerMessage);
        if (!m.find()) {
            // Thử số nguyên đơn lẻ lớn hơn 1000 (giá VNĐ thẳng)
            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\b(\\d{5,})\\b");
            java.util.regex.Matcher m2 = p2.matcher(lowerMessage);
            if (m2.find()) {
                double val = Double.parseDouble(m2.group(1));
                BigDecimal price = BigDecimal.valueOf(val);
                pf.type = "around";
                pf.minPrice = price.multiply(BigDecimal.valueOf(0.8));
                pf.maxPrice = price.multiply(BigDecimal.valueOf(1.2));
            }
            return pf;
        }
        double millions = Double.parseDouble(m.group(1).replace(",", "."));
        BigDecimal price = BigDecimal.valueOf(millions * 1_000_000);

        boolean hasDuoi = lowerMessage.contains("dưới") || lowerMessage.contains("duoi")
                || lowerMessage.contains("dưới đây") || lowerMessage.contains("không quá")
                || lowerMessage.contains("tầm") || lowerMessage.contains("tam");
        boolean hasTren = lowerMessage.contains("trên") || lowerMessage.contains("tren")
                || lowerMessage.contains("từ") || lowerMessage.contains("hơn") || lowerMessage.contains("hon");

        if (hasDuoi && !hasTren) {
            pf.type = "under";
            pf.minPrice = BigDecimal.ZERO;
            pf.maxPrice = price;
        } else if (hasTren && !hasDuoi) {
            pf.type = "above";
            pf.minPrice = price;
            pf.maxPrice = null; // không giới hạn trên
        } else {
            // "khoảng X triệu" hoặc không rõ -> ±2 triệu
            pf.type = "around";
            BigDecimal margin = BigDecimal.valueOf(2_000_000);
            pf.minPrice = price.subtract(margin).max(BigDecimal.ZERO);
            pf.maxPrice = price.add(margin);
        }
        return pf;
    }

    private List<Product> searchProductsFromMessage(String message) {
        String[] words = message.split("\\s+");
        List<Product> results = new ArrayList<>();
        String lowerMessage = message.toLowerCase();
        String brandKeyword = null;

        // Detect brand from message
        String[] brands = { "asus", "acer", "lenovo", "hp", "dell", "msi", "apple", "macbook", "surface", "microsoft",
                "gigabyte", "alienware", "razer", "origin pc" };
        for (String brand : brands) {
            if (lowerMessage.contains(brand)) {
                brandKeyword = brand;
                break;
            }
        }

        // Parse khoảng giá từ tin nhắn
        PriceFilter pf = parsePriceFilter(lowerMessage);
        final BigDecimal minPrice = pf.minPrice;
        final BigDecimal maxPrice = pf.maxPrice;

        // Tìm sản phẩm theo từ khóa và thương hiệu
        Set<Product> uniqueResults = new HashSet<>();
        boolean foundByBrand = false;

        // Nếu có thương hiệu cụ thể, ưu tiên tìm theo thương hiệu TRƯỚC
        if (brandKeyword != null) {
            final String finalBrandKeyword = brandKeyword;
            try {
                List<Product> brandProducts = productRepository.findAll().stream()
                        .filter(p -> p.getName().toLowerCase().contains(finalBrandKeyword) ||
                                (p.getDescription() != null
                                        && p.getDescription().toLowerCase().contains(finalBrandKeyword)))
                        .collect(Collectors.toList());

                if (!brandProducts.isEmpty()) {
                    uniqueResults.addAll(brandProducts);
                    foundByBrand = true;
                    log.info("Found {} products for brand: {}", brandProducts.size(), finalBrandKeyword);
                }
            } catch (Exception e) {
                log.error("Error searching for brand products: {}", finalBrandKeyword, e);
            }
        }

        // CHỈ tìm theo AI_CATEGORY nếu không tìm thấy sản phẩm theo brand
        if (!foundByBrand) {
            String aiCategory = detectAiCategoryFromMessage(lowerMessage);
            if (aiCategory != null) {
                try {
                    List<Product> categoryProducts = productRepository.findByAiCategory(aiCategory);
                    uniqueResults.addAll(categoryProducts);
                    log.info("Found {} products for ai_category: {}", categoryProducts.size(), aiCategory);
                } catch (Exception e) {
                    log.error("Error searching products by ai_category: {}", aiCategory, e);
                }
            }

            // ĐỒNG THỜI tìm thêm theo keywords trong tên và mô tả để có nhiều kết quả hơn
            for (String word : words) {
                if (word.length() > 2 && !word.matches("\\d+") && !word.equals("triệu") && !word.equals("dưới")
                        && !word.equals("trên")) {
                    try {
                        // Tìm trong tên sản phẩm
                        List<Product> nameMatches = productRepository.findAll().stream()
                                .filter(p -> p.getName().toLowerCase().contains(word.toLowerCase()))
                                .collect(Collectors.toList());
                        uniqueResults.addAll(nameMatches);

                        // Tìm trong description - đặc biệt hữu ích cho "office", "văn phòng"
                        List<Product> descriptionMatches = productRepository.findAll().stream()
                                .filter(p -> p.getDescription() != null &&
                                        p.getDescription().toLowerCase().contains(word.toLowerCase()))
                                .collect(Collectors.toList());
                        uniqueResults.addAll(descriptionMatches);

                        if (!nameMatches.isEmpty() || !descriptionMatches.isEmpty()) {
                            log.info("Found {} products for keyword '{}' (name: {}, description: {})",
                                    nameMatches.size() + descriptionMatches.size(), word, nameMatches.size(),
                                    descriptionMatches.size());
                        }
                    } catch (Exception e) {
                        log.error("Error searching for keyword: {}", word, e);
                    }
                }
            }
        }

        // Chỉ tìm theo repository search nếu vẫn không có kết quả
        if (uniqueResults.isEmpty()) {
            for (String word : words) {
                if (word.length() > 2 && !word.matches("\\d+") && !word.equals("triệu") && !word.equals("dưới")
                        && !word.equals("trên")) {
                    try {
                        List<Product> found = productRepository.searchProducts(word);
                        uniqueResults.addAll(found);
                        log.info("Found {} products for keyword: {}", found.size(), word);
                    } catch (Exception e) {
                        log.error("Error searching for products with keyword: {}", word, e);
                    }
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

        // Lưu lại danh sách trước khi lọc giá để dùng cho fallback
        List<Product> resultsBeforePriceFilter = new ArrayList<>(results);
        String detectedCategory = detectAiCategoryFromMessage(lowerMessage);

        // ---- Lọc theo khoảng giá (VNĐ) ----
        boolean hasPriceConstraint = (minPrice != null || maxPrice != null);
        if (hasPriceConstraint) {
            results = results.stream()
                    .filter(p -> {
                        if (p.getPrice() == null)
                            return false;
                        boolean okMin = (minPrice == null) || p.getPrice().compareTo(minPrice) >= 0;
                        boolean okMax = (maxPrice == null) || p.getPrice().compareTo(maxPrice) <= 0;
                        return okMin && okMax;
                    })
                    .collect(Collectors.toList());
            log.info("Filtered to {} products within price range [min={}, max={}]", results.size(), minPrice, maxPrice);

            // FALLBACK: Không tìm thấy trong khoảng giá
            if (results.isEmpty() && !resultsBeforePriceFilter.isEmpty()) {
                log.info("No products in price range, providing fallback suggestions");

                // Fallback 1: Cùng thương hiệu, sắp theo giá
                if (brandKeyword != null) {
                    final String fBrand = brandKeyword;
                    try {
                        List<Product> sameBrandProducts = productRepository.findAll().stream()
                                .filter(p -> p.getName().toLowerCase().contains(fBrand) ||
                                        (p.getDescription() != null
                                                && p.getDescription().toLowerCase().contains(fBrand)))
                                .sorted((p1, p2) -> {
                                    if (p1.getPrice() != null && p2.getPrice() != null)
                                        return p1.getPrice().compareTo(p2.getPrice());
                                    return 0;
                                })
                                .limit(5)
                                .collect(Collectors.toList());
                        if (!sameBrandProducts.isEmpty()) {
                            results = sameBrandProducts;
                            log.info("Fallback: Found {} products for same brand: {}", results.size(), fBrand);
                        }
                    } catch (Exception e) {
                        log.error("Error in brand fallback", e);
                    }
                }

                // Fallback 2: Cùng category
                if (results.isEmpty() && detectedCategory != null) {
                    try {
                        List<Product> sameCategoryProducts = productRepository.findByAiCategory(detectedCategory);
                        if (!sameCategoryProducts.isEmpty()) {
                            results = sameCategoryProducts.stream()
                                    .sorted((p1, p2) -> {
                                        if (p1.getPrice() != null && p2.getPrice() != null)
                                            return p1.getPrice().compareTo(p2.getPrice());
                                        return 0;
                                    })
                                    .limit(5)
                                    .collect(Collectors.toList());
                            log.info("Fallback: Found {} products in same category", results.size());
                        }
                    } catch (Exception e) {
                        log.error("Error in category fallback", e);
                    }
                }

                // Fallback 3: Gần giá nhất
                if (results.isEmpty()) {
                    try {
                        // Điểm tham chiếu: dùng maxPrice hoặc minPrice
                        BigDecimal targetPrice = maxPrice != null ? maxPrice
                                : (minPrice != null ? minPrice : BigDecimal.ZERO);
                        results = productRepository.findAll().stream()
                                .filter(p -> p.getPrice() != null)
                                .sorted((p1, p2) -> {
                                    BigDecimal diff1 = p1.getPrice().subtract(targetPrice).abs();
                                    BigDecimal diff2 = p2.getPrice().subtract(targetPrice).abs();
                                    return diff1.compareTo(diff2);
                                })
                                .limit(3)
                                .collect(Collectors.toList());
                        log.info("Fallback: Found {} products closest to target price", results.size());
                    } catch (Exception e) {
                        log.error("Error in closest price fallback", e);
                    }
                }
            }
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

    private List<Product> findCheapestProducts(String purpose, String msg) {
        try {
            List<Product> allProducts = productRepository.findAll();

            // Lọc sản phẩm theo mục đích sử dụng
            Stream<Product> filteredStream = allProducts.stream()
                    .filter(product -> {
                        if (product.getPrice() == null)
                            return false;

                        String productInfo = (product.getName() + " " +
                                (product.getDescription() != null ? product.getDescription() : "") + " " +
                                (product.getAiCategory() != null ? product.getAiCategory() : "")).toLowerCase();

                        switch (purpose) {
                            case "gaming":
                                return productInfo.contains("gaming") || productInfo.contains("rtx") ||
                                        productInfo.contains("gtx") || productInfo.contains("rog") ||
                                        productInfo.contains("predator") || productInfo.contains("alienware") ||
                                        (product.getAiCategory() != null
                                                && product.getAiCategory().equals("laptop_gaming"));
                            case "office":
                                return productInfo.contains("thinkpad") || productInfo.contains("latitude") ||
                                        productInfo.contains("elitebook") || productInfo.contains("business") ||
                                        (product.getAiCategory() != null
                                                && (product.getAiCategory().equals("laptop_office") ||
                                                        product.getAiCategory().equals("laptop_business")));
                            case "student":
                                return productInfo.contains("aspire") || productInfo.contains("pavilion") ||
                                        productInfo.contains("ideapad") ||
                                        (product.getAiCategory() != null
                                                && product.getAiCategory().equals("laptop_student"))
                                        ||
                                        product.getPrice().compareTo(BigDecimal.valueOf(20000000)) <= 0; // Dưới 20
                                                                                                         // triệu
                            case "design":
                                return productInfo.contains("creator") || productInfo.contains("studio") ||
                                        productInfo.contains("quadro") || productInfo.contains("workstation") ||
                                        productInfo.contains("precision") ||
                                        (product.getAiCategory() != null
                                                && product.getAiCategory().equals("laptop_creator"));
                            default:
                                // Nếu không có mục đích cụ thể, lấy tất cả laptop
                                return productInfo.contains("laptop") || productInfo.contains("macbook");
                        }
                    });

            // Sắp xếp theo giá từ thấp đến cao và lấy 10 sản phẩm đầu
            return filteredStream
                    .sorted((p1, p2) -> {
                        if (p1.getPrice() != null && p2.getPrice() != null) {
                            return p1.getPrice().compareTo(p2.getPrice());
                        }
                        return 0;
                    })
                    .limit(10)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding cheapest products for purpose: {}", purpose, e);

            // Fallback: lấy sản phẩm giá rẻ nhất tổng thể
            try {
                return productRepository.findAll().stream()
                        .filter(p -> p.getPrice() != null)
                        .sorted((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()))
                        .limit(5)
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                log.error("Error in fallback cheapest products", ex);
                return new ArrayList<>();
            }
        }
    }

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
        if (lowerDesc.contains("i3"))
            specs.append("Intel i3, ");
        else if (lowerDesc.contains("i5"))
            specs.append("Intel i5, ");
        else if (lowerDesc.contains("i7"))
            specs.append("Intel i7, ");
        else if (lowerDesc.contains("i9"))
            specs.append("Intel i9, ");
        else if (lowerDesc.contains("ryzen"))
            specs.append("AMD Ryzen, ");

        // Tìm RAM
        if (lowerDesc.contains("4gb"))
            specs.append("4GB RAM, ");
        else if (lowerDesc.contains("8gb"))
            specs.append("8GB RAM, ");
        else if (lowerDesc.contains("16gb"))
            specs.append("16GB RAM, ");
        else if (lowerDesc.contains("32gb"))
            specs.append("32GB RAM, ");

        // Tìm GPU
        if (lowerDesc.contains("rtx")) {
            if (lowerDesc.contains("3060"))
                specs.append("RTX 3060, ");
            else if (lowerDesc.contains("3070"))
                specs.append("RTX 3070, ");
            else if (lowerDesc.contains("4060"))
                specs.append("RTX 4060, ");
            else
                specs.append("RTX GPU, ");
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
        // Tìm số tiền trong tin nhắn
        if (message.contains("20 triệu") || message.contains("20triệu"))
            return "khoảng 20 triệu";
        if (message.contains("15 triệu") || message.contains("15triệu"))
            return "khoảng 15 triệu";
        if (message.contains("25 triệu") || message.contains("25triệu"))
            return "khoảng 25 triệu";
        if (message.contains("30 triệu") || message.contains("30triệu"))
            return "khoảng 30 triệu";

        // Tìm pattern số + triệu
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*triệu");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return "khoảng " + matcher.group(1) + " triệu";
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
            advice.append("🎮 **Laptop Gaming - Sản phẩm giá rẻ nhất:**\n\n");
        } else if (msg.contains("văn phòng") || msg.contains("van phong") || msg.contains("office")
                || msg.contains("làm việc")) {
            purpose = "office";
            advice.append("💼 **Laptop Văn phòng - Sản phẩm giá rẻ nhất:**\n\n");
        } else if (msg.contains("sinh viên") || msg.contains("sinh vien") || msg.contains("student")
                || msg.contains("học tập")) {
            purpose = "student";
            advice.append("🎓 **Laptop Sinh viên - Sản phẩm giá rẻ nhất:**\n\n");
        } else if (msg.contains("thiết kế") || msg.contains("thiet ke") || msg.contains("design")
                || msg.contains("đồ họa")) {
            purpose = "design";
            advice.append("🎨 **Laptop Thiết kế - Sản phẩm giá rẻ nhất:**\n\n");
        } else {
            advice.append("💰 **Sản phẩm giá rẻ nhất trong cửa hàng:**\n\n");
        }

        // Tìm sản phẩm giá rẻ nhất theo mục đích sử dụng
        List<Product> cheapestProducts = findCheapestProducts(purpose, msg);
        if (!cheapestProducts.isEmpty()) {
            for (Product product : cheapestProducts.subList(0, Math.min(5, cheapestProducts.size()))) {
                advice.append("🛍️ **").append(product.getName()).append("**\n");
                advice.append("💰 **Giá:** ").append(formatPrice(product.getPrice()));

                // Hiển thị category và ai_category nếu có
                if (product.getCategory() != null) {
                    advice.append(" - ").append(product.getCategory().getName());
                }
                if (product.getAiCategory() != null) {
                    advice.append(" (").append(product.getAiCategory().replace("laptop_", "")).append(")");
                }
                advice.append("\n");

                // Hiển thị mô tả ngắn gọn
                if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                    String shortDesc = product.getDescription().length() > 80
                            ? product.getDescription().substring(0, 80) + "..."
                            : product.getDescription();
                    advice.append("📝 ").append(shortDesc).append("\n");
                }

                // Action buttons
                advice.append("🔍 [Xem chi tiết](/products/").append(product.getId()).append(") | ");
                advice.append(
                        "<button class='btn btn-sm btn-primary chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                        .append(product.getId()).append("'>🛒 Thêm vào giỏ</button>\n");
                advice.append("───────────────────\n\n");
            }
        } else {
            // Nếu không tìm thấy sản phẩm theo mục đích cụ thể, lấy sản phẩm giá rẻ nhất
            // tổng thể
            try {
                List<Product> allCheapProducts = productRepository.findAll().stream()
                        .filter(p -> p.getPrice() != null)
                        .sorted((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()))
                        .limit(5)
                        .collect(Collectors.toList());

                if (!allCheapProducts.isEmpty()) {
                    advice.append("✨ **Sản phẩm giá rẻ nhất hiện có:**\n\n");
                    for (Product product : allCheapProducts) {
                        advice.append("🛍️ **").append(product.getName()).append("**\n");
                        advice.append("💰 **Giá:** ").append(formatPrice(product.getPrice())).append("\n");
                        advice.append("🔍 [Xem chi tiết](/products/").append(product.getId()).append(") | ");
                        advice.append(
                                "<button class='btn btn-sm btn-primary chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                                .append(product.getId()).append("'>🛒 Thêm vào giỏ</button>\n");
                        advice.append("───────────────────\n\n");
                    }
                } else {
                    advice.append("❌ **Hiện tại không có sản phẩm nào có sẵn.**\n\n");
                }
            } catch (Exception e) {
                log.error("Error getting all cheap products", e);
                advice.append("❌ **Có lỗi xảy ra khi tìm sản phẩm. Vui lòng thử lại sau.**\n\n");
            }
        }

        advice.append("💡 **Lời khuyên mua sắm:**\n");
        advice.append("• Sản phẩm giá rẻ nhất không phải lúc nào cũng tốt nhất\n");
        advice.append("• So sánh cấu hình và đánh giá trước khi quyết định\n");
        advice.append("• Kiểm tra chính sách bảo hành và hỗ trợ kỹ thuật\n");
        advice.append("• Cân nhắc nhu cầu sử dụng thực tế của bạn\n\n");

        advice.append("❓ **Cần tư vấn cụ thể hơn?** Hãy cho tôi biết:\n");
        advice.append("• 💰 Ngân sách cụ thể của bạn\n");
        advice.append("• 🎯 Mục đích sử dụng chính (gaming, văn phòng, học tập...)\n");
        advice.append("• 🏷️ Thương hiệu ưa thích (MSI, ASUS, Dell, HP...)\n");

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
                String cheaperProduct = product1.getPrice().compareTo(product2.getPrice()) < 0 ? product1.getName()
                        : product2.getName();
                comparison.append("• **Giá rẻ hơn:** ").append(cheaperProduct)
                        .append(" (tiết kiệm ").append(formatPrice(priceDiff)).append(")\n");
            }

            comparison.append("\n🎯 **Gợi ý lựa chọn:**\n");
            comparison.append("• **Nếu ưu tiên giá:** Chọn sản phẩm rẻ hơn\n");
            comparison.append("• **Nếu ưu tiên hiệu năng:** So sánh cấu hình chi tiết\n");
            comparison.append("• **Nếu cần tư vấn:** Liên hệ hotline 1900-1234\n\n");

            comparison.append("🔗 **Hành động:**\n");
            comparison.append(
                    "<button class='btn btn-sm btn-primary chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                    .append(product1.getId()).append("'>🛒 Mua ").append(product1.getName()).append("</button>\n");
            comparison.append(
                    "<button class='btn btn-sm btn-primary chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                    .append(product2.getId()).append("'>🛒 Mua ").append(product2.getName()).append("</button>\n");

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

    /**
     * Phát hiện ai_category từ message người dùng
     */
    private String detectAiCategoryFromMessage(String lowerMessage) {
        // Gaming laptops - ưu tiên cao nhất vì thường tìm nhiều nhất
        if (lowerMessage.contains("gaming") || lowerMessage.contains("game") ||
                lowerMessage.contains("chơi game") || lowerMessage.contains("choi game")) {
            return "laptop_gaming";
        }

        // Student laptops
        if (lowerMessage.contains("sinh viên") || lowerMessage.contains("sinh vien") ||
                lowerMessage.contains("student") || lowerMessage.contains("học tập") ||
                lowerMessage.contains("hoc tap") || lowerMessage.contains("học sinh") ||
                lowerMessage.contains("hoc sinh")) {
            return "laptop_student";
        }

        // Business laptops - cải thiện từ khóa
        if (lowerMessage.contains("doanh nhân") || lowerMessage.contains("doanh nhan") ||
                lowerMessage.contains("business") || lowerMessage.contains("công việc") ||
                lowerMessage.contains("cong viec") || lowerMessage.contains("kinh doanh") ||
                lowerMessage.contains("professional") || lowerMessage.contains("pro") ||
                lowerMessage.contains("enterprise") || lowerMessage.contains("corporate")) {
            return "laptop_business";
        }

        // Office laptops - cải thiện để nhận diện tốt hơn
        if (lowerMessage.contains("văn phòng") || lowerMessage.contains("van phong") ||
                lowerMessage.contains("office") || lowerMessage.contains("ofice") || // typo phổ biến
                lowerMessage.contains("làm việc") || lowerMessage.contains("lam viec") ||
                lowerMessage.contains("work") || lowerMessage.contains("công sở") ||
                lowerMessage.contains("cong so") || lowerMessage.contains("nhân viên") ||
                lowerMessage.contains("nhan vien") || lowerMessage.contains("employee")) {
            return "laptop_office";
        }

        // Creator laptops
        if (lowerMessage.contains("creator") || lowerMessage.contains("sáng tạo") ||
                lowerMessage.contains("sang tao") || lowerMessage.contains("thiết kế") ||
                lowerMessage.contains("thiet ke") || lowerMessage.contains("design")) {
            return "laptop_creator";
        }

        // Programming laptops
        if (lowerMessage.contains("lập trình") || lowerMessage.contains("lap trinh") ||
                lowerMessage.contains("programming") || lowerMessage.contains("code") ||
                lowerMessage.contains("developer") || lowerMessage.contains("dev")) {
            return "laptop_programming";
        }

        // Premium laptops
        if (lowerMessage.contains("cao cấp") || lowerMessage.contains("cao cap") ||
                lowerMessage.contains("premium") || lowerMessage.contains("đắt") ||
                lowerMessage.contains("dat") || lowerMessage.contains("sang trọng") ||
                lowerMessage.contains("sang trong")) {
            return "laptop_premium";
        }

        // Ultrabook laptops
        if (lowerMessage.contains("ultrabook") || lowerMessage.contains("mỏng nhẹ") ||
                lowerMessage.contains("mong nhe") || lowerMessage.contains("thin") ||
                lowerMessage.contains("light") || lowerMessage.contains("mỏng") ||
                lowerMessage.contains("mong")) {
            return "laptop_ultrabook";
        }

        // 2in1 laptops
        if (lowerMessage.contains("2in1") || lowerMessage.contains("tablet") ||
                lowerMessage.contains("cảm ứng") || lowerMessage.contains("cam ung") ||
                lowerMessage.contains("convertible") || lowerMessage.contains("lai")) {
            return "laptop_2in1";
        }

        // Mainstream laptops
        if (lowerMessage.contains("mainstream") || lowerMessage.contains("phổ thông") ||
                lowerMessage.contains("pho thong") || lowerMessage.contains("trung bình") ||
                lowerMessage.contains("trung binh") || lowerMessage.contains("bình dân") ||
                lowerMessage.contains("binh dan")) {
            return "laptop_mainstream";
        }

        return null; // Không tìm thấy category phù hợp
    }

    private String handleProductComparison(String userMessage) {
        try {
            List<String> productNames = extractProductNames(userMessage);

            if (productNames.size() < 2) {
                return "🔍 Để so sánh sản phẩm, vui lòng cung cấp tên cụ thể của 2 sản phẩm.\n\n" +
                        "💡 **Ví dụ:** \"So sánh ASUS ROG Strix G15 và MSI Katana GF66\"\n" +
                        "📝 Hãy thử lại với tên sản phẩm đầy đủ hơn!";
            }

            List<Product> foundProducts = new ArrayList<>();
            List<String> notFoundProducts = new ArrayList<>();

            for (String productName : productNames) {
                Product product = findProductByName(productName);
                if (product != null) {
                    foundProducts.add(product);
                } else {
                    notFoundProducts.add(productName);
                }
            }

            if (foundProducts.size() < 2) {
                StringBuilder response = new StringBuilder("❌ **Không tìm thấy đủ sản phẩm để so sánh:**\n\n");

                if (!notFoundProducts.isEmpty()) {
                    response.append("🚫 **Không tìm thấy:** ").append(String.join(", ", notFoundProducts))
                            .append("\n\n");
                }

                if (!foundProducts.isEmpty()) {
                    response.append("✅ **Đã tìm thấy:** ").append(foundProducts.get(0).getName()).append("\n\n");
                }

                response.append("💡 **Gợi ý:** Hãy thử tìm kiếm với từ khóa chung như:\n")
                        .append("• \"Tìm laptop gaming\" để xem danh sách\n")
                        .append("• \"Laptop ASUS\" để xem sản phẩm ASUS\n")
                        .append("• \"Laptop dưới 20 triệu\" để tìm theo giá");

                return response.toString();
            }

            return generateProductComparison(foundProducts.get(0), foundProducts.get(1));

        } catch (Exception e) {
            log.error("Error in product comparison", e);
            return "❌ Có lỗi xảy ra khi so sánh sản phẩm. Vui lòng thử lại sau!";
        }
    }

    private List<String> extractProductNames(String message) {
        List<String> productNames = new ArrayList<>();
        String lowerMessage = message.toLowerCase();

        // Common product patterns
        String[] patterns = {
                "asus [^\\s]+ [^\\s]+",
                "acer [^\\s]+ [^\\s]+",
                "msi [^\\s]+ [^\\s]+",
                "dell [^\\s]+ [^\\s]+",
                "hp [^\\s]+ [^\\s]+",
                "lenovo [^\\s]+ [^\\s]+",
                "macbook [^\\s]+",
                "thinkpad [^\\s]+",
                "surface [^\\s]+"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern,
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(message);
            while (m.find()) {
                String found = m.group().trim();
                if (!productNames.contains(found)) {
                    productNames.add(found);
                }
            }
        }

        return productNames;
    }

    private Product findProductByName(String productName) {
        try {
            String[] keywords = productName.toLowerCase().split("\\s+");

            List<Product> allProducts = productRepository.findAll();
            Product bestMatch = null;
            int maxMatches = 0;

            for (Product product : allProducts) {
                String productFullName = product.getName().toLowerCase();
                int matches = 0;

                for (String keyword : keywords) {
                    if (productFullName.contains(keyword)) {
                        matches++;
                    }
                }

                if (matches > maxMatches && matches >= 2) { // At least 2 keywords match
                    maxMatches = matches;
                    bestMatch = product;
                }
            }

            return bestMatch;
        } catch (Exception e) {
            log.error("Error finding product by name: {}", productName, e);
            return null;
        }
    }

    private String generateProductComparison(Product product1, Product product2) {
        StringBuilder comparison = new StringBuilder();

        comparison.append("⚖️ **So sánh sản phẩm:**\n\n")
                .append("🆚 **").append(product1.getName())
                .append("** vs **").append(product2.getName()).append("**\n\n");

        // Price comparison
        comparison.append("💰 **Giá cả:**\n")
                .append("• ").append(product1.getName()).append(": ")
                .append(formatPrice(product1.getPrice())).append("\n")
                .append("• ").append(product2.getName()).append(": ")
                .append(formatPrice(product2.getPrice())).append("\n");

        // Price difference
        if (product1.getPrice() != null && product2.getPrice() != null) {
            BigDecimal priceDiff = product1.getPrice().subtract(product2.getPrice()).abs();
            String cheaper = product1.getPrice().compareTo(product2.getPrice()) < 0 ? product1.getName()
                    : product2.getName();
            comparison.append("📊 ").append(cheaper).append(" rẻ hơn ")
                    .append(formatPrice(priceDiff)).append("\n\n");
        }

        // Brand and category comparison
        String brand1 = extractBrand(product1.getName());
        String brand2 = extractBrand(product2.getName());

        if (!brand1.equals(brand2)) {
            comparison.append("🏷️ **Thương hiệu:**\n")
                    .append("• ").append(product1.getName()).append(": ").append(brand1).append("\n")
                    .append("• ").append(product2.getName()).append(": ").append(brand2).append("\n\n");
        }

        // Category comparison
        if (product1.getAiCategory() != null && product2.getAiCategory() != null) {
            comparison.append("📂 **Phân loại:**\n")
                    .append("• ").append(product1.getName()).append(": ")
                    .append(getCategoryDisplayName(product1.getAiCategory())).append("\n")
                    .append("• ").append(product2.getName()).append(": ")
                    .append(getCategoryDisplayName(product2.getAiCategory())).append("\n\n");
        }

        // Descriptions comparison
        if (product1.getDescription() != null && product2.getDescription() != null) {
            comparison.append("📝 **Mô tả:**\n")
                    .append("• **").append(product1.getName()).append(":** ")
                    .append(truncateDescription(product1.getDescription())).append("\n")
                    .append("• **").append(product2.getName()).append(":** ")
                    .append(truncateDescription(product2.getDescription())).append("\n\n");
        }

        // Purchase recommendation
        comparison.append("🛒 **Khuyến nghị:**\n");
        if (product1.getPrice().compareTo(product2.getPrice()) < 0) {
            comparison.append("• Chọn **").append(product1.getName()).append("** nếu bạn muốn tiết kiệm\n");
            comparison.append("• Chọn **").append(product2.getName()).append("** nếu ngân sách thoải mái hơn\n\n");
        } else {
            comparison.append("• Chọn **").append(product2.getName()).append("** nếu bạn muốn tiết kiệm\n");
            comparison.append("• Chọn **").append(product1.getName()).append("** nếu ngân sách thoải mái hơn\n\n");
        }

        // Action buttons
        comparison.append("<div class='chatbot-action-buttons'>\n")
                .append("<button class='btn btn-primary chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                .append(product1.getId()).append("'>🛒 Thêm ").append(product1.getName()).append(" vào giỏ</button>\n")
                .append("<button class='btn btn-primary chatbot-action-btn' data-action='add-to-cart' data-product-id='")
                .append(product2.getId()).append("'>🛒 Thêm ").append(product2.getName()).append(" vào giỏ</button>\n")
                .append("</div>\n\n")
                .append("💬 Cần hỗ trợ thêm? Hãy hỏi tôi!");

        return comparison.toString();
    }

    private String extractBrand(String productName) {
        String lowerName = productName.toLowerCase();
        String[] brands = { "asus", "acer", "msi", "dell", "hp", "lenovo", "apple", "surface" };

        for (String brand : brands) {
            if (lowerName.contains(brand)) {
                return brand.toUpperCase();
            }
        }
        return "Unknown";
    }

    private String getCategoryDisplayName(String aiCategory) {
        switch (aiCategory) {
            case "laptop_gaming":
                return "Laptop Gaming";
            case "laptop_student":
                return "Laptop Sinh viên";
            case "laptop_business":
                return "Laptop Doanh nhân";
            case "laptop_office":
                return "Laptop Văn phòng";
            case "laptop_creator":
                return "Laptop Content Creator";
            case "laptop_programming":
                return "Laptop Lập trình";
            case "laptop_premium":
                return "Laptop Cao cấp";
            case "laptop_ultrabook":
                return "Ultrabook";
            case "laptop_2in1":
                return "Laptop 2-in-1";
            case "laptop_mainstream":
                return "Laptop Phổ thông";
            default:
                return "Laptop";
        }
    }

    private String truncateDescription(String description) {
        if (description == null || description.length() <= 100) {
            return description;
        }
        return description.substring(0, 100) + "...";
    }
}