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
        // Check if API is configured
        if (apiKey == null || apiKey.isEmpty()) {
            return processMessageWithRules(userMessage);
        }

        try {
            return processMessageWithAI(userMessage);
        } catch (Exception e) {
            log.error("AI chatbot failed, using rule-based response", e);
            return processMessageWithRules(userMessage);
        }
        */
    }

    private String processMessageWithRules(String userMessage) {
        String msg = userMessage.toLowerCase();

        // Tìm kiếm sản phẩm - hỗ trợ tiếng Việt
        if (msg.contains("tìm") || msg.contains("tim") || msg.contains("show") || msg.contains("find") || 
            msg.contains("search") || msg.contains("looking for") || msg.contains("muốn") || 
            msg.contains("cần") || msg.contains("laptop") || msg.contains("gaming") ||
            msg.contains("sản phẩm") || msg.contains("san pham")) {
            
            List<Product> products = searchProductsFromMessage(msg);
            log.info("Search completed. Found {} products for message: '{}'", products.size(), userMessage);
            
            // Xác định loại sản phẩm đang tìm
            String productType = "";
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
                // Thay đổi từ câu hỏi sang câu khẳng định
                response.append("🔍 Tôi tìm thấy một số ").append(productType);
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
                    response.append("🛒 [Thêm vào giỏ hàng](?action=add-to-cart&product=").append(product.getId()).append(")\n");
                    response.append("───────────────────\n\n");
                }
                
                // Thêm các action tổng quát
                String searchParam = productType.replace(" ", "%20");
                response.append("🎯 **Thêm tùy chọn:**\n");
                response.append("📋 [Xem tất cả ").append(productType).append("](/products?category=").append(searchParam).append(")\n");
                if (!priceRange.isEmpty()) {
                    response.append("💲 [Lọc theo giá ").append(priceRange).append("](/products?price=").append(priceRange).append(")\n");
                }
                response.append("🔄 [So sánh sản phẩm](/compare?products=");
                for (int i = 0; i < Math.min(3, products.size()); i++) {
                    if (i > 0) response.append(",");
                    response.append(products.get(i).getId());
                }
                response.append(")\n\n");
                response.append("💬 **Cần hỗ trợ thêm?** Hãy hỏi tôi bất cứ điều gì!");
                return response.toString();
                
            } else {
                // Không tìm thấy sản phẩm phù hợp
                log.warn("No products found for search: '{}', productType: '{}', priceRange: '{}'", userMessage, productType, priceRange);
                
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
                        
                        for (Product product : alternativeProducts.subList(0, Math.min(3, alternativeProducts.size()))) {
                            response.append("🛍️ **").append(product.getName()).append("**\n");
                            response.append("💰 ").append(formatPrice(product.getPrice())).append("\n");
                            
                            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                                String shortDesc = product.getDescription().length() > 60 ? 
                                    product.getDescription().substring(0, 60) + "..." : product.getDescription();
                                response.append("📝 ").append(shortDesc).append("\n");
                            }
                            
                            // Category
                            if (product.getCategory() != null) {
                                response.append("📂 ").append(product.getCategory().getName()).append("\n");
                            }
                            
                            // Action buttons
                            response.append("🔍 [Chi tiết](/products/").append(product.getId()).append(") | ");
                            response.append("🛒 [Mua ngay](?action=add-to-cart&product=").append(product.getId()).append(")\n");
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

    private List<Product> searchProductsFromMessage(String message) {
        String[] words = message.split("\\s+");
        List<Product> results = new ArrayList<>();
        BigDecimal maxPrice = null;

        // Tìm giá tiền trong tin nhắn (ví dụ: "20 triệu", "500000")
        for (String word : words) {
            if (word.matches("\\d+")) {
                double number = Double.parseDouble(word);
                if (number > 1000) { // Nếu là số lớn, có thể là giá VNĐ
                    maxPrice = BigDecimal.valueOf(number / 24000); // Convert VNĐ to USD
                } else if (number > 0 && message.contains("triệu")) {
                    maxPrice = BigDecimal.valueOf((number * 1000000) / 24000); // triệu VNĐ to USD
                }
            }
        }

        // Tìm sản phẩm theo từ khóa
        for (String word : words) {
            if (word.length() > 2) { // Bỏ qua từ quá ngắn
                try {
                    List<Product> found = productRepository.searchProducts(word);
                    results.addAll(found);
                    log.info("Found {} products for keyword: {}", found.size(), word);
                    
                    // Tìm theo category name - tìm rộng hơn
                    if (word.equals("laptop") || word.equals("máy") || word.equals("may") || word.equals("gaming")) {
                        List<Product> additional = productRepository.findAll().stream()
                                .filter(p -> p.getName().toLowerCase().contains("laptop") || 
                                            p.getName().toLowerCase().contains("gaming") ||
                                            (p.getCategory() != null && p.getCategory().getName().toLowerCase().contains("laptop")))
                                .collect(Collectors.toList());
                        results.addAll(additional);
                        log.info("Found {} additional laptop/gaming products", additional.size());
                    }
                } catch (Exception e) {
                    log.error("Error searching for products with keyword: {}", word, e);
                }
            }
        }

        // Lọc theo giá nếu có
        if (maxPrice != null) {
            final BigDecimal priceLimit = maxPrice;
            results = results.stream()
                    .filter(p -> p.getPrice() != null && p.getPrice().compareTo(priceLimit) <= 0)
                    .collect(Collectors.toList());
        }

        return results.stream().distinct().limit(10).collect(Collectors.toList());
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
                    "TUYỆT ĐỐI KHÔNG tự tạo ra tên sản phẩm, giá cả, hoặc thông số kỹ thuật không có trong dữ liệu.\n" +
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
        if (price == null) return "Liên hệ";
        
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
        if (message.contains("20 triệu") || message.contains("20triệu")) return "khoảng 20 triệu";
        if (message.contains("15 triệu") || message.contains("15triệu")) return "khoảng 15 triệu";
        if (message.contains("25 triệu") || message.contains("25triệu")) return "khoảng 25 triệu";
        if (message.contains("30 triệu") || message.contains("30triệu")) return "khoảng 30 triệu";
        
        // Tìm pattern số + triệu
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*triệu");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return "khoảng " + matcher.group(1) + " triệu";
        }
        
        return "";
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