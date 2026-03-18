package com.mypkga.commerceplatformfull;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class SpotBugsTester {

    // ==========================================
    // 1. TÍNH NĂNG BUG DETECTION (ĐÃ FIX LỖI LOGIC)
    // ==========================================

    // Fix: Truyền biến text từ ngoài vào để check null có ý nghĩa hơn, không gán chết bằng null nữa
    public void triggerNullPointerFixed(String text) {
        if (text != null) {
            System.out.println("Độ dài chuỗi: " + text.length());
        } else {
            System.out.println("Chuỗi này đang rỗng, an toàn rồi nha bồ!");
        }
    }

    // Fix: Có điều kiện dừng đệ quy
    public void testInfiniteRecursionFixed(int count) {
        if (count > 5) {
            return;
        }
        testInfiniteRecursionFixed(count + 1);
    }

    // Fix: Xoá biến thừa, logic rành mạch
    public void testDeadCodeAndUnusedFixed(boolean isValid) {
        if (isValid) {
            System.out.println("Code dọn sạch sẽ, không còn rác bộ nhớ!");
        }
    }

    // ==========================================
    // 2. TÍNH NĂNG SECURITY ANALYSIS (ĐÃ FIX BẢO MẬT)
    // ==========================================

    // Fix: Truyền pass từ tham số, và thêm try-with-resources để tự động đóng Connection (hết Bad Practice)
    public void testHardcodedPassFixed(String dbPassword) {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String user = "admin";

        try (Connection conn = java.sql.DriverManager.getConnection(url, user, dbPassword)) {
            System.out.println("Kết nối DB an toàn và đã tự động đóng!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fix: Dùng PreparedStatement + try-with-resources tự động đóng (Hết Bad Practice)
    public void testSqlInjectionFixed(Connection conn, String userInput) {
        String query = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userInput);
            pstmt.execute();
            System.out.println("Truy vấn an toàn, không sợ hacker!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fix: Chuyển sang đọc JSON và khai báo chuẩn UTF-8 để hết bị báo Dodgy Code
    public void testDeserializationFixed(byte[] safeJsonBytes) {
        String jsonData = new String(safeJsonBytes, StandardCharsets.UTF_8);
        System.out.println("Nói KHÔNG với ObjectInputStream nguy hiểm, đã chuyển sang xài JSON: " + jsonData);
    }
}