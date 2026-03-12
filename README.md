#  Milk Tea AI Bot - Telegram Order Automation

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini%202.0%20Flash-blue)](https://deepmind.google/technologies/gemini/)
[![Telegram](https://img.shields.io/badge/Bot-Telegram%20API-blue)](https://core.telegram.org/bots/api)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue)](https://www.docker.com/)

Một dự án Chatbot thông minh hỗ trợ mẹ quản lý và tự động hóa quy trình đặt trà sữa. Hệ thống tích hợp AI để hiểu ngôn ngữ tự nhiên, giúp người lớn tuổi (người bán) vận hành cửa hàng dễ dàng qua Telegram.

---

## 🚀 Tính năng nổi bật

-  AI Natural Language Processing: Tích hợp **Gemini 2.0 Flash** để hiểu yêu cầu đặt món tự nhiên, tư vấn món dựa trên sở thích và trích xuất thông tin đơn hàng sang định dạng JSON chuẩn.
-  Smart Ordering Flow: Luồng chốt đơn 3 bước chặt chẽ: trích xuất món -> thu thập thông tin giao hàng (SĐT/Địa chỉ) -> xác nhận cuối cùng.
-  Exception Handling (Điểm nhấn): Xử lý tình huống "Hết món" linh hoạt. Hỗ trợ khách hàng đổi món nhanh bằng lệnh `/ĐM`, giữ nguyên thông tin giao hàng cũ để tối ưu UX.
-  Merchant Interface: Giao diện nút bấm (Inline Keyboard) dành riêng cho người bán (Mẹ) để "Nhận đơn" hoặc "Báo hết món" chỉ với một chạm.
-  State Management: Quản lý trạng thái đơn hàng thời gian thực, tự động cập nhật tin nhắn để tránh thao tác nhầm lẫn.

---

## 🛠 Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.5
- **AI Engine**: Google Gemini API
- **Library**: TelegramBots Spring Boot Starter
- **Storage**: CSV Database (đảm bảo tính gọn nhẹ và dễ cập nhật Menu)
- **Deployment**: Docker, Render Cloud

---
## ⚙️ Cài đặt & Chạy thử (Installation & Deployment)

Dự án đã được cấu hình tối ưu để triển khai nhanh chóng trên môi trường Cloud (Render) và máy cục bộ (Local).

##  1. Deploy trên Render (Khuyên dùng)

Dự án đã được deploy và chạy ổn định trên **Render Cloud**.

Ban giám khảo có thể trải nghiệm trực tiếp mà **không cần cài đặt thêm**.

### Các bước deploy nếu muốn chạy lại:

1. Clone project từ GitHub
2. Tạo Web Service trên Render
3. Thiết lập **Environment Variables**

### 2. Chạy thử tại máy cục bộ (Local)
Nếu quý Ban giám khảo muốn kiểm tra mã nguồn và chạy thử tại máy cá nhân, vui lòng thực hiện các bước sau:

**Yêu cầu hệ thống:**
- JDK 17+
- Maven 3.6+
- Docker (Tùy chọn)

**Các bước khởi chạy:**

1. **Cấu hình biến môi trường:**
   Vì lý do bảo mật, các API Key đã được xóa khỏi mã nguồn. Quý vị vui lòng thiết lập biến môi trường trên máy hoặc điền trực tiếp vào file `src/main/resources/application.properties`:
   ```properties
   gemini.api.key=YOUR_GEMINI_API_KEY
   bot.token=YOUR_TELEGRAM_BOT_TOKEN
   bot.username=YOUR_BOT_USERNAME
2. Build dự án bằng Maven bằng lệnh
```text
"mvn clean package -DskipTests -Dmaven.test.skip=true"
```
4. Chạy ứng dụng bằng lệnh
```text
"java -jar target/milk-tea-bot-0.0.1-SNAPSHOT.jar"
```
---
## 📂 Cấu trúc dự án (Project Structure)
```text
src/main/java/com/example/milk_tea_bot/
├── bot/            # Logic xử lý Telegram Bot & Callback
├── dto/            # Data Transfer Objects (AI Response mapping)
├── model/          # Định nghĩa MenuItem, OrderItem
├── service/        # Gemini AI Service, Menu & Order Logic
└── util/           # Tiện ích làm sạch dữ liệu JSON từ AI

