package com.example.milk_tea_bot.bot;

import com.example.milk_tea_bot.dto.AIOrderResponse;
import com.example.milk_tea_bot.model.AIOrderItem;
import com.example.milk_tea_bot.model.MenuItem;
import com.example.milk_tea_bot.service.GeminiService;
import com.example.milk_tea_bot.service.MenuService;
import com.example.milk_tea_bot.service.OrderService;
import com.example.milk_tea_bot.util.JsonCleaner;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MilkTeaBot extends TelegramLongPollingBot {

    private final String token;
    private final String username;

    // test mode: gửi đơn lại chính user
    private String MOM_CHAT_ID;

    private final Map<Long, AIOrderResponse> pendingOrders = new ConcurrentHashMap<>();

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private OrderService orderService;

    public MilkTeaBot(
            @Value("${bot.token}") String token,
            @Value("${bot.username}") String username) {

        this.token = token;
        this.username = username;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasCallbackQuery()) {

            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            handleCallback(chatId, update.getCallbackQuery().getData());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        // TEST MODE
        MOM_CHAT_ID = chatId.toString();

        if (text.equalsIgnoreCase("/start")) {

            send(chatId,
                    "🧋 Chào anh/chị!\n\n"
                            + "Quán mẹ em đang mở cửa.\n"
                            + "Xem menu bằng /menu\n"
                            + "Hoặc nhắn để đặt món nhé 😊");

        } else if (text.equalsIgnoreCase("/menu")) {

            send(chatId, getMenuText());

        } else {

            handleAIOrder(chatId, text);

        }
    }

    private void handleAIOrder(Long chatId, String text) {

        try {

            sendTypingAction(chatId);

            String aiResponse = geminiService.askGemini(text);

            System.out.println("AI RESPONSE:\n" + aiResponse);

            String chatReply = "";
            String jsonPart = "";

            int jsonStart = aiResponse.indexOf("{");

            if (jsonStart != -1) {

                chatReply = aiResponse.substring(0, jsonStart).trim();
                jsonPart = aiResponse.substring(jsonStart).trim();

            } else {

                chatReply = aiResponse.trim();
            }

            if (!chatReply.isEmpty()) {
                send(chatId, chatReply);
            }

            if (!jsonPart.isEmpty()) {

                String cleanedJson = JsonCleaner.clean(jsonPart);

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);

                try {

                    AIOrderResponse res =
                            mapper.readValue(cleanedJson, AIOrderResponse.class);

                    if (res.getOrders() != null && !res.getOrders().isEmpty()) {

                        pendingOrders.put(chatId, res);
                        showConfirmation(chatId, res);
                    }

                } catch (Exception ex) {

                    ex.printStackTrace();

                    send(chatId,
                            "😅 Em chưa hiểu đơn ạ.\n"
                                    + "Anh/chị ghi rõ giúp em nhé!");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            send(chatId,
                    "😭 Bot bị lỗi một chút.\n"
                            + "Anh/chị nhắn lại giúp em nhé!");
        }
    }

    private void sendTypingAction(Long chatId) {

        SendChatAction action = new SendChatAction();

        action.setChatId(chatId.toString());
        action.setAction(ActionType.TYPING);

        try {
            execute(action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showConfirmation(Long chatId, AIOrderResponse res) {

        StringBuilder sb = new StringBuilder();

        sb.append("🧋 *ĐƠN HÀNG CỦA ANH/CHỊ*\n");
        sb.append("━━━━━━━━━━━━━━\n\n");

        int grandTotal = 0;

        for (AIOrderItem o : res.getOrders()) {

            MenuItem item = menuService.find(o.getItem());

            if (item == null) {

                send(chatId,
                        "❌ Món này quán chưa có.\n"
                                + "Anh/chị xem menu giúp em nhé!");

                return;
            }

            int total =
                    orderService.calculate(
                            item,
                            o.getSize(),
                            o.getQuantity());

            grandTotal += total;

            sb.append("🍹 *").append(item.getName()).append("*\n");
            sb.append("Size: ").append(o.getSize()).append("\n");
            sb.append("SL: ").append(o.getQuantity()).append("\n");
            sb.append("💰 ").append(total).append("đ\n\n");
        }

        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("💵 *TỔNG: ").append(grandTotal).append("đ*\n\n");
        sb.append("Anh/chị xác nhận giúp em nhé 👇");

        SendMessage msg = new SendMessage();

        msg.setChatId(chatId.toString());
        msg.setText(sb.toString());
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton confirm = new InlineKeyboardButton();
        confirm.setText("✅ Đặt luôn");
        confirm.setCallbackData("CONFIRM_YES");

        InlineKeyboardButton cancel = new InlineKeyboardButton();
        cancel.setText("❌ Chọn lại");
        cancel.setCallbackData("CONFIRM_NO");

        rows.add(Arrays.asList(confirm, cancel));

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);

        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(Long chatId, String data) {

        if ("CONFIRM_YES".equals(data)) {

            AIOrderResponse order = pendingOrders.get(chatId);

            if (order != null) {

                StringBuilder momMsg = new StringBuilder();

                momMsg.append("🚨 ĐƠN MỚI\n");
                momMsg.append("━━━━━━━━━━━━━━\n");
                momMsg.append("Khách: ").append(chatId).append("\n\n");

                int total = 0;

                for (AIOrderItem o : order.getOrders()) {

                    MenuItem item = menuService.find(o.getItem());

                    int price =
                            orderService.calculate(
                                    item,
                                    o.getSize(),
                                    o.getQuantity());

                    total += price;

                    momMsg.append(item.getName()).append("\n");
                    momMsg.append("Size ")
                            .append(o.getSize())
                            .append(" x")
                            .append(o.getQuantity())
                            .append(" = ")
                            .append(price)
                            .append("đ\n\n");
                }

                momMsg.append("━━━━━━━━━━━━━━\n");
                momMsg.append("TỔNG: ").append(total).append("đ");

                sendOrderToMom(momMsg.toString(), order);

                send(chatId,
                        "✅ Em đã báo mẹ làm rồi ạ!");

                pendingOrders.remove(chatId);
            }

        } else if ("CONFIRM_NO".equals(data)) {

            send(chatId,
                    "👍 Anh/chị chọn lại món giúp em nhé!");

        }
    }

    private void sendOrderToMom(String text, AIOrderResponse order) {

        SendMessage msg = new SendMessage();

        msg.setChatId(MOM_CHAT_ID);
        msg.setText(text);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (AIOrderItem o : order.getOrders()) {

            InlineKeyboardButton btn = new InlineKeyboardButton();

            btn.setText("❌ Hết món: " + o.getItem());
            btn.setCallbackData("OUT_OF_STOCK_" + o.getItem());

            rows.add(Collections.singletonList(btn));
        }

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);

        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(Long chatId, String text) {

        SendMessage msg = new SendMessage();

        msg.setChatId(chatId.toString());
        msg.setText(text);

        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMenuText() {

        StringBuilder sb = new StringBuilder();

        sb.append("MENU QUÁN MẸ\n");
        sb.append("━━━━━━━━━━━━━━\n\n");

        Map<String, List<MenuItem>> grouped = new LinkedHashMap<>();

        for (MenuItem item : menuService.getMenu()) {

            grouped.computeIfAbsent(
                    item.getCategory(),
                    k -> new ArrayList<>()).add(item);
        }

        for (String category : grouped.keySet()) {

            sb.append(category.toUpperCase()).append("\n");

            for (MenuItem item : grouped.get(category)) {

                sb.append(item.getItemId())
                        .append(" - ")
                        .append(item.getName())
                        .append("\n");

                sb.append("M: ")
                        .append(item.getPriceM())
                        .append("đ | L: ")
                        .append(item.getPriceL())
                        .append("đ\n\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}