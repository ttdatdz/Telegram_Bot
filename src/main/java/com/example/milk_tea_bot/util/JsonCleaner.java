package com.example.milk_tea_bot.util;

public class JsonCleaner {

    public static String clean(String text) {
        if (text == null) return "{\"orders\":[]}";
        String cleaned = text.trim();
        if (cleaned.contains("{")) {
            cleaned = cleaned.substring(cleaned.indexOf("{"), cleaned.lastIndexOf("}") + 1);
        }
        return cleaned;
    }
}