package com.example.milk_tea_bot.util;

public class JsonCleaner {

//    public static String clean(String text) {
//        if (text == null || text.isEmpty()) return "{\"orders\":[]}";
//
//        // 1. Xóa các block Markdown nếu có
//        String cleaned = text.replaceAll("(?s)```json|```", "").trim();
//
//        // 2. Tìm vị trí ngoặc nhọn đầu tiên và cuối cùng để trích xuất JSON thuần
//        int start = cleaned.indexOf("{");
//        int end = cleaned.lastIndexOf("}");
//
//        if (start != -1 && end != -1 && end > start) {
//            cleaned = cleaned.substring(start, end + 1);
//        } else {
//            // Nếu không tìm thấy cấu trúc JSON, trả về object rỗng để tránh crash Jackson
//            return "{\"orders\":[]}";
//        }
//
//        return cleaned;
//    }

    public static String clean(String text) {
        if (text == null) return "{\"orders\":[]}";
        String cleaned = text.trim();
        if (cleaned.contains("{")) {
            cleaned = cleaned.substring(cleaned.indexOf("{"), cleaned.lastIndexOf("}") + 1);
        }
        return cleaned;
    }
}