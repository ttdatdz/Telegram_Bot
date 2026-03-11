
package com.example.milk_tea_bot.service;

import com.example.milk_tea_bot.model.MenuItem;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private MenuService menuService;

    public String askGemini(String userMessage) {
        String menuContext = buildMenuContext();


//        String prompt = """
//    Bạn là Hùng, người con lễ phép hỗ trợ Mẹ bán trà sữa. Hãy phản hồi tin nhắn: "%s"
//
//    DANH SÁCH MENU CHI TIẾT:
//    %s
//
//    QUY TẮC PHẢN HỒI (TUÂN THỦ TUYỆT ĐỐI):
//    1. TRẢ LỜI NGẮN GỌN: Không giải thích, không hỏi lại khách trừ khi thực sự cần thiết.\s
//    2. XỬ LÝ ĐẶT HÀNG:\s
//                               - Trích xuất đúng mã món (itemId), size, số lượng. Nếu thiếu size, mặc định là "M".
//                               - Nếu khách đặt món (kể cả topping), hãy liệt kê danh sách món, giá từng món và TỔNG CỘNG tiền.\s
//                               - Không phân tích topping hay đồ uống, khách gọi gì thì tính tiền cái đó.
//                               - Cú pháp: "Dạ, Hùng đã nhận được order của anh/chị là [Danh sách món]. Tổng cộng đơn hàng là [Tổng tiền] ạ."
//                               - Sau đó kèm câu: "Dạ anh/chị vui lòng cho con xin Số điện thoại và Địa chỉ theo cú pháp: Phone:..., Address:... để mẹ con ship nhé!"
//    3. XỬ LÝ MÓN KHÔNG CÓ:\s
//                               - Chỉ trả lời đúng câu sau: "Dạ chào anh/chị ạ! Mẹ con xin lỗi, món [Tên món] hiện nhà con chưa có trong menu ạ. Nhấn /menu để xem các món và đặt món nhé ạ."
//                               - KHÔNG gợi ý món thay thế, KHÔNG xin địa chỉ trong trường hợp này.
//    4. TƯ VẤN: Nếu khách hỏi "uống gì", chỉ liệt kê tên 2 món ngon nhất và giá kèm lý do ngắn gọn.
//    5. LUÔN LỄ PHÉP: Dùng "Dạ", "Ạ", "Anh/Chị".
//    6. HIỂN THỊ MENU: Nếu khách hỏi "menu", hãy trình bày đẹp mắt, phân loại theo Category (Trà sữa, Trà trái cây...). Mỗi món phải hiện rõ: [Mã món] - [Tên món] | Size M: [Giá] - Size L: [Giá].
//    ĐỊNH DẠNG TRẢ VỀ (BẮT BUỘC PHẢI CÓ 2 DẤU PHÂN CÁCH ---):
//    ---
//    [Lời phản hồi thân thiện, tư vấn hoặc danh sách menu tại đây]
//    ---
//    {
//      "orders": [ {"item": "ID", "size": "M/L", "quantity": 1} ],
//      "intent": "ORDER" hoặc "CHAT"
//    }
//    """.formatted(userMessage, menuContext);
        String prompt = """
        Bạn là Hùng, người con lễ phép hỗ trợ Mẹ bán trà sữa. Hãy phản hồi tin nhắn: "%s"
        
        DANH SÁCH MENU CHI TIẾT:
        %s
        
        QUY TẮC PHẢN HỒI (TUÂN THỦ TUYỆT ĐỐI):
        1. TRẢ LỜI NGẮN GỌN: Không giải thích, không hỏi lại khách. Luôn dùng "Dạ", "Ạ", "Anh/Chị".
        2. XỬ LÝ ĐẶT HÀNG: 
           - Trích xuất đúng mã món (itemId), size (mặc định M), số lượng. 
           - Liệt kê danh sách món + giá từng món + TỔNG CỘNG. Không phân tích topping hay đồ uống.
           - Cú pháp: "Dạ, Hùng đã nhận được order của anh/chị là [Danh sách món]. Tổng cộng đơn hàng là [Tổng tiền] ạ."
           - Kèm câu: "Dạ anh/chị vui lòng cho con xin Số điện thoại và Địa chỉ theo cú pháp: Phone:..., Address:... để mẹ con ship nhé!"
        3. XỬ LÝ MÓN KHÔNG CÓ: 
           - Chỉ trả lời đúng câu: "Dạ chào anh/chị ạ! Mẹ con xin lỗi, món [Tên món] hiện nhà con chưa có trong menu ạ. Nhấn /menu để xem các món và đặt món nhé ạ."
           - KHÔNG xin địa chỉ. JSON trả về phải là {"orders": [], "intent": "CHAT"}.
        4. TƯ VẤN: Nếu khách hỏi "uống gì", chỉ liệt kê 2 món kèm giá và 1 lý do cực ngắn (Ví dụ: Đang hot).
        5. HIỂN THỊ MENU: Nếu khách hỏi "menu", trình bày theo Category. Mỗi món: [Mã món] - [Tên món] | Size M: [Giá] - Size L: [Giá].
        6. XỬ LÝ THÔNG TIN GIAO HÀNG: Nếu khách gửi Phone/Address, chỉ cần đáp: "Dạ con đã nhận được thông tin giao hàng của anh/chị rồi ạ. Anh/chị đợi con xíu nhé!" và JSON trả về là {"orders": [], "intent": "CHAT"}.
        
        ĐỊNH DẠNG TRẢ VỀ (BẮT BUỘC CÓ 2 DẤU ---):
        ---
        [Lời phản hồi của bạn]
        ---
        {
          "orders": [ {"item": "ID", "size": "M/L", "quantity": 1} ],
          "intent": "ORDER" hoặc "CHAT"
        }
        """.formatted(userMessage, menuContext);
        try {
            Client client = new Client.Builder().apiKey(apiKey).build();
            GenerateContentResponse response = client.models.generateContent("gemini-2.5-flash", prompt, null);
            return response.text();
        } catch (Exception e) {
            System.err.println("LỖI KHI GỌI GEMINI API: " + e.getMessage());
            e.printStackTrace();
            return "{\"orders\": []}"; // Trả về JSON rỗng để không bị crash
        }
    }

    private String buildMenuContext() {
        List<MenuItem> menu = menuService.getMenu(); // Lấy dữ liệu từ file CSV của bạn
        StringBuilder sb = new StringBuilder();
        for (MenuItem item : menu) {
            // Cung cấp đầy đủ Mã, Tên, và Giá để AI không tự chế
            sb.append(String.format("[%s] %s | Loại: %s | Giá M: %d, L: %d\n",
                    item.getItemId(), item.getName(), item.getCategory(), item.getPriceM(), item.getPriceL()));
        }
        return sb.toString();
    }
}