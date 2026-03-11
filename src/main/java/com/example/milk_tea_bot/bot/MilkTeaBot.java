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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

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
        handleCallback(chatId, update.getCallbackQuery().getData(), update);
        return;
    }

    if (!update.hasMessage() || !update.getMessage().hasText()) return;

    String text = update.getMessage().getText();
    Long chatId = update.getMessage().getChatId();
    MOM_CHAT_ID = chatId.toString();

    // 1. Xử lý lưu Số điện thoại & Địa chỉ theo cú pháp khách nhập
    if (text.toLowerCase().contains("phone:") || text.toLowerCase().contains("address:")) {
        handleContactInfo(chatId, text);
        return;
    }

    // 2. Các lệnh cơ bản
    if (text.equalsIgnoreCase("/start")) {
        send(chatId, "🧋 Chào anh/chị!\n\nQuán mẹ em đang mở cửa.\nXem menu bằng /menu\nHoặc nhắn để đặt món nhé 😊");
    } else if (text.equalsIgnoreCase("/menu")) {
        send(chatId, getMenuText());
    } else if (text.startsWith("/ĐM")) {
        handleReplaceItem(chatId, text);
    } else {
        // Xử lý đặt món qua AI
        handleAIOrder(chatId, text);
    }
}
    private void handleContactInfo(Long chatId, String text) {
        AIOrderResponse order = pendingOrders.get(chatId);
        if (order == null) {
            send(chatId, "📌 Anh/chị vui lòng chọn món trước khi nhập địa chỉ nhé!");
            return;
        }

        String[] lines = text.split("\n|,");
        for (String line : lines) {
            String content = line.trim();
            if (content.toLowerCase().startsWith("phone:")) {
                order.setPhone(content.substring(6).trim());
            }
            if (content.toLowerCase().startsWith("address:")) {
                order.setAddress(content.substring(8).trim());
            }
        }

        // Kiểm tra nếu khách đã cung cấp đủ cả 2 thông tin thì mới hiện bảng xác nhận đơn
        if (order.getPhone() != null && order.getAddress() != null) {
            send(chatId, "✅ Đã ghi nhận thông tin giao hàng!");
            showConfirmation(chatId, order); // <--- BÂY GIỜ MỚI HIỆN BẢNG XÁC NHẬN
        } else {
            send(chatId, "Dạ con đã nhận được một phần thông tin. Anh/chị bổ sung nốt "
                    + (order.getPhone() == null ? "Số điện thoại (Phone:)" : "Địa chỉ (Address:)") + " để con lên đơn nhé!");
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
                       // showConfirmation(chatId, res);
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
        // Hiển thị thông tin giao hàng nếu có
        if (res.getPhone() != null) sb.append("📱 SĐT: ").append(res.getPhone()).append("\n");
        if (res.getAddress() != null) sb.append("📍 ĐC: ").append(res.getAddress()).append("\n");
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

    private void sendOrderToMom(String text, AIOrderResponse order) {
        SendMessage msg = new SendMessage();
        msg.setChatId(MOM_CHAT_ID);
        msg.setText(text);
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 1. Nút Nhận đơn và Hủy đơn (Dòng đầu tiên)
        InlineKeyboardButton btnAccept = new InlineKeyboardButton();
        btnAccept.setText("✅ Nhận đơn");
        btnAccept.setCallbackData("MOM_ACCEPT_" + order.getOrders().get(0).getItem()); // Hoặc ID đơn hàng nếu có

        InlineKeyboardButton btnReject = new InlineKeyboardButton();
        btnReject.setText("🚫 Hủy đơn/Đóng cửa");
        btnReject.setCallbackData("MOM_REJECT");

        rows.add(Arrays.asList(btnAccept, btnReject));

        // 2. Danh sách nút "Hết món" cho từng sản phẩm
        for (AIOrderItem o : order.getOrders()) {
            MenuItem item = menuService.find(o.getItem());
            InlineKeyboardButton btnOut = new InlineKeyboardButton();
            btnOut.setText("❌ Hết: " + (item != null ? item.getName() : o.getItem()));
            btnOut.setCallbackData("OUT_OF_STOCK_" + o.getItem());
            rows.add(Collections.singletonList(btnOut));
        }

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);

        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleCallback(Long chatId, String data,Update update) {
        // Luồng cho khách hàng xác nhận đơn
        if ("CONFIRM_YES".equals(data)) {
            // Không remove đơn ngay, giữ lại để lát nữa lấy kèm SĐT/Địa chỉ
            AIOrderResponse order = pendingOrders.get(chatId);
            if (order != null) {

                StringBuilder momMsg = new StringBuilder();

                momMsg.append("🚨 ĐƠN MỚI\n");
                momMsg.append("━━━━━━━━━━━━━━\n");
                momMsg.append("Khách: ").append(chatId).append("\n\n");
                momMsg.append("📱 SĐT: ").append(order.getPhone()).append("\n");
                momMsg.append("📍 ĐC: ").append(order.getAddress()).append("\n\n");
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
                        "✅ Em đã báo đơn cho mẹ rồi ạ!");

                pendingOrders.remove(chatId);
            }
        }

        // Luồng cho Mẹ xử lý đơn
        else if (data.startsWith("MOM_ACCEPT_")) {
            // 1. Lấy thông tin tin nhắn cũ
            Long momChatId = chatId; // ID của Mẹ
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String oldText = "";
            if (update.getCallbackQuery().getMessage() instanceof org.telegram.telegrambots.meta.api.objects.Message) {
                oldText = ((org.telegram.telegrambots.meta.api.objects.Message) update.getCallbackQuery().getMessage()).getText();
            }
            // 2. Báo cho khách hàng (Giả định bạn có cách lấy chatId khách, ở đây dùng tạm chatId vì đang test 1 mình)
            send(chatId, "🥰 Dạ mẹ em đã nhận đơn và đang làm rồi ạ! Anh/chị đợi xíu nhé.");

            // 3. Cập nhật tin nhắn bên máy Mẹ: Đổi chữ và XÓA BUTTON
            editMessage(momChatId, messageId, oldText + "\n\n✅ **TRẠNG THÁI: ĐÃ NHẬN ĐƠN**");
        }

        else if ("MOM_REJECT".equals(data)) {
            // Mẹ nhấn Hủy đơn
            send(chatId, "😭 Dạ em xin lỗi, hiện tại quán mẹ em đang bận hoặc đã đóng cửa nên không nhận đơn được ạ. Mong anh/chị thông cảm!");
        }

        else if (data.startsWith("OUT_OF_STOCK_")) {
            String itemId = data.replace("OUT_OF_STOCK_", "");
            MenuItem item = menuService.find(itemId);
            String itemName = (item != null) ? item.getName() : itemId;

            AIOrderResponse order = pendingOrders.get(chatId);

            // Đánh dấu hoặc xóa món đã hết khỏi danh sách pending
            if (order != null && order.getOrders() != null) {
                order.getOrders().removeIf(o -> o.getItem().equals(itemId));
            }

            StringBuilder sb = new StringBuilder();
            sb.append("😭 Dạ anh chị ơi, món *").append(itemName).append("* mẹ em vừa báo hết mất rồi ạ!\n");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // Logic xử lý nút bấm thông minh
            if (order != null && !order.getOrders().isEmpty()) {
                // TRƯỜNG HỢP 1: CÒN MÓN KHÁC TRONG ĐƠN
                sb.append("Anh chị muốn đổi món mới hay vẫn đặt các món còn lại ạ?");

                InlineKeyboardButton btnChange = new InlineKeyboardButton("🔄 Đổi món khác");
                btnChange.setCallbackData("CUST_CHANGE_ITEM");

                InlineKeyboardButton btnKeep = new InlineKeyboardButton("🛒 Đặt món còn lại");
                btnKeep.setCallbackData("CUST_KEEP_REST");

                rows.add(Arrays.asList(btnChange, btnKeep));
            } else {
                // TRƯỜNG HỢP 2: HẾT SẠCH MÓN (Đơn ban đầu chỉ có 1 món)
                sb.append("Vì đơn này chỉ có 1 món nên anh chị vui lòng đổi món khác hoặc chọn lại giúp con nhé!");

                InlineKeyboardButton btnChange = new InlineKeyboardButton("🔄 Đổi món khác");
                btnChange.setCallbackData("CUST_CHANGE_ITEM");
                rows.add(Collections.singletonList(btnChange));
            }

            // Luôn có 2 nút này ở dưới
            InlineKeyboardButton btnRestart = new InlineKeyboardButton("🆕 Chọn lại từ đầu");
            btnRestart.setCallbackData("CUST_RESTART");

            InlineKeyboardButton btnAbort = new InlineKeyboardButton("❌ Hủy đơn");
            btnAbort.setCallbackData("CUST_ABORT");

            rows.add(Arrays.asList(btnRestart, btnAbort));

            markup.setKeyboard(rows);
            sendWithMarkup(chatId, sb.toString(), markup);
        }// Khách chọn đổi món
        else if ("CUST_CHANGE_ITEM".equals(data)) {
            send(chatId, "Dạ, để đổi món anh chị vui lòng nhắn tin theo cú pháp:\n` /ĐM MaMon Size SoLuong `\n(Ví dụ: `/ĐM TS01 L 1`) để con thay thế món đã hết ạ!");
        }

        // Khách chọn đặt các món còn lại
        else if ("CUST_KEEP_REST".equals(data)) {
            AIOrderResponse order = pendingOrders.get(chatId);
            if (order != null) {
                // Vì trong OUT_OF_STOCK_ bạn đã dùng removeIf để xóa món hết rồi,
                // nên giờ chỉ cần hiện lại bảng xác nhận với các món còn lại thôi.
                showConfirmation(chatId, order);
            } else {
                send(chatId, "Dạ đơn hàng cũ không còn hiệu lực, anh chị đặt lại món mới giúp con nhé!");
            }
        }

        // Khách hủy đơn
        else if ("CUST_ABORT".equals(data)) {
            pendingOrders.remove(chatId);
            send(chatId, "Dạ con đã hủy đơn. Cảm ơn Anh/chị. Hẹn gặp lại nhé! ❤️");
        }

        // Khách chọn lại
        else if ("CUST_RESTART".equals(data)) {
            pendingOrders.remove(chatId);
            send(chatId, "Dạ mời anh chị xem lại /menu để chọn món mới ạ.");
        }else{
            send(chatId, "Dạ mời anh chị xem lại /menu để chọn món mới ạ.");
        }

    }

    private void handleReplaceItem(Long chatId, String text) {
        try {
            String[] parts = text.split("\\s+");
            if (parts.length < 4) throw new Exception();

            String newId = parts[1];
            String size = parts[2].toUpperCase();
            int qty = Integer.parseInt(parts[3]);

            // Kiểm tra món có tồn tại trong menu không
            if (menuService.find(newId) == null) {
                send(chatId, "❌ Mã món `" + newId + "` không tồn tại ạ.");
                return;
            }

            AIOrderResponse currentOrder = pendingOrders.get(chatId);
            if (currentOrder == null) {
                // Nếu lỡ bị null thì tạo mới luôn
                currentOrder = new AIOrderResponse();
                currentOrder.setOrders(new ArrayList<>());
                pendingOrders.put(chatId, currentOrder);
            }

            // Tạo item mới và thêm vào đơn hàng
            AIOrderItem newItem = new AIOrderItem();
            newItem.setItem(newId);
            newItem.setSize(size);
            newItem.setQuantity(qty);

            currentOrder.getOrders().add(newItem);

            // Sau khi thêm xong, hiển thị lại bảng xác nhận cho khách chốt đơn mới
            send(chatId, "🔄 Đã thêm món mới vào đơn cho anh chị rồi ạ:");
            showConfirmation(chatId, currentOrder);

        } catch (Exception e) {
            send(chatId, "❌ Cú pháp chưa đúng ạ. Anh chị nhắn: `/ĐM TS01 L 1` nhé!");
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
    private void sendWithMarkup(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        msg.setParseMode("Markdown"); // Để in đậm, in nghiêng đẹp hơn
        msg.setReplyMarkup(markup);

        try {
            execute(msg);
        } catch (Exception e) {
            System.err.println("Lỗi gửi tin nhắn kèm Markup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void editMessage(Long chatId, Integer messageId, String newText) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId.toString());
        edit.setMessageId(messageId);
        edit.setText(newText);
        edit.setParseMode("Markdown");
        edit.setReplyMarkup(null); // Xóa bỏ toàn bộ các nút bấm (Nhận đơn, Hủy, Hết món)

        try {
            execute(edit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}