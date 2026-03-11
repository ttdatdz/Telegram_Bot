//package com.example.milk_tea_bot.service;
//
//import com.example.milk_tea_bot.model.MenuItem;
//import com.google.genai.Client;
//import com.google.genai.types.GenerateContentResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class GeminiService {
//
//    @Value("${gemini.api.key}")
//    private String apiKey;
//
//    @Autowired
//    private MenuService menuService;
//
//    public String askGemini(String userMessage) {
//
//        String menuContext = buildMenuContext();
//
//        String prompt = """
//You are an AI that extracts drink orders from Vietnamese messages.
//
//Your job is to understand ANY way a customer might order drinks.
//
//You must map the drink name to the correct itemId from the menu.
//
//Return ONLY JSON.
//
//JSON format:
//
//{
// "orders":[
//   {
//     "item":"TS01",
//     "size":"M",
//     "quantity":1
//   }
// ]
//}
//
//MENU (itemId = drink):
//
//""" + menuContext + """
//
//Rules:
//
//1. Understand natural Vietnamese ordering language.
//2. Customers may order many drinks in one sentence.
//3. Orders may be separated by:
//   - comma (,)
//   - "và"
//   - "với"
//   - "&"
//4. If size is missing → default M
//5. Size must be either M or L.
//6. Understand quantity written as:
//   - numbers (1,2,3)
//   - words (một, hai, ba, bốn)
//7. Ignore words like:
//   - cho mình
//   - mình cần
//   - lấy
//   - order
//8. Map the drink name to the closest menu item.
//9. NEVER invent new items outside the menu.
//10. Return JSON ONLY.
//
//Examples:
//
//User: 1 trà sữa truyền thống
//Output:
//{"orders":[{"item":"TS03","size":"M","quantity":1}]}
//
//User: 2 trà xoài size L và 1 cà phê đen
//Output:
//{"orders":[
// {"item":"TTG05","size":"L","quantity":2},
// {"item":"CF01","size":"M","quantity":1}
//]}
//
//User: 1 trà sữa khoai môn, 1 cà phê sữa
//Output:
//{"orders":[
// {"item":"TS04","size":"M","quantity":1},
// {"item":"CF02","size":"M","quantity":1}
//]}
//
//User message:
//""" + userMessage;
//
//        Client client = new Client.Builder()
//                .apiKey(apiKey)
//                .build();
//
//        GenerateContentResponse response =
//                client.models.generateContent(
//                        "gemini-2.0-flash",
//                        prompt,
//                        null
//                );
//
//        return response.text();
//    }
//
//    private String buildMenuContext() {
//
//        List<MenuItem> menu = menuService.getMenu();
//
//        StringBuilder sb = new StringBuilder();
//
//        for (MenuItem item : menu) {
//
//            sb.append(item.getItemId())
//                    .append(" = ")
//                    .append(item.getName())
//                    .append("\n");
//        }
//
//        return sb.toString();
//    }
//}



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
//            Bạn là một trợ lý bán hàng lễ phép cho quán trà sữa của Mẹ.
//            Nhiệm vụ: Trích xuất đơn hàng từ tin nhắn của khách: "%s"
//
//            DANH SÁCH MENU (itemId = Tên món):
//            %s
//
//            QUY TẮC TRẢ VỀ:
//            1. Chỉ trả về JSON duy nhất theo định dạng:
//            {
//              "orders": [
//                {"item": "ID_MON", "size": "M/L", "quantity": 1}
//              ]
//            }
//            2. Nếu khách không nói size, mặc định là "M".
//            3. Nếu khách nói số lượng bằng chữ (hai, ba), hãy chuyển thành số (2, 3).
//            4. Map tên món khách gọi với ID chính xác nhất trong MENU.
//            5. Nếu không phải tin nhắn đặt hàng, trả về: {"orders": []}
//
//            KHÔNG GIẢI THÍCH, CHỈ TRẢ VỀ JSON.
//            """.formatted(userMessage, menuContext);
        String prompt = """
    Bạn là Hùng, người con lễ phép hỗ trợ Mẹ bán trà sữa. Hãy phản hồi tin nhắn: "%s"
    
    DANH SÁCH MENU CHI TIẾT:
    %s

    QUY TẮC ỨNG XỬ:
    1. LUÔN LỄ PHÉP: Dùng các từ "Dạ", "Ạ", "Anh/Chị" trong lời thoại.
    2. TƯ VẤN: Nếu khách hỏi "uống gì", "sở thích", hãy chọn ngẫu nhiên 2 món từ Menu để gợi ý kèm lý do (ví dụ: món này đang hot).
    3. XỬ LÝ MÓN KHÔNG CÓ: Nếu khách gọi món KHÔNG có trong Menu (ví dụ: Hồng trà sữa), phải nói: "Dạ món này nhà mẹ con hiện không có ạ", sau đó gợi ý 1 món có sẵn ĐÚNG trong Menu. KHÔNG tự ý tạo mã món mới.
    4. HIỂN THỊ MENU: Nếu khách hỏi "menu", hãy trình bày đẹp mắt, phân loại theo Category (Trà sữa, Trà trái cây...). Mỗi món phải hiện rõ: [Mã món] - [Tên món] | Size M: [Giá] - Size L: [Giá].
    5. ĐẶT HÀNG: Trích xuất đúng mã món (itemId), size, số lượng. Nếu thiếu size, mặc định là "M".

    ĐỊNH DẠNG TRẢ VỀ (BẮT BUỘC PHẢI CÓ 2 DẤU PHÂN CÁCH ---):
    ---
    [Lời phản hồi thân thiện, tư vấn hoặc danh sách menu tại đây]
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