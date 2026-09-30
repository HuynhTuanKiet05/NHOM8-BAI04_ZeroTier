# BÀI TẬP LỚN MÔN LẬP TRÌNH MẠNG MÁY TÍNH - NHÓM 8

## 1. Giới thiệu đề tài
Ứng dụng truyền thông điệp (Chat) và truyền tập tin (File Transfer) theo mô hình **Client - Server** đa luồng (Multi-threaded), hỗ trợ:
- Giao tiếp giữa nhiều máy tính trên môi trường Internet thông qua mạng LAN ảo **ZeroTier VPN**.
- Cơ chế **Broadcast** chuyển tiếp tin nhắn đến toàn bộ các máy khách khác trong phòng.
- Bảo mật thông điệp bằng thuật toán mã hóa đối xứng **AES 128-bit**.
- Truyền tập tin trực tiếp theo luồng byte qua Socket TCP và lưu trữ tại Server.

---

## 2. Cấu trúc mã nguồn

```text
NHOM8-BAI04/
├── nbproject/                  # Cấu hình dự án NetBeans
├── src/
│   └── nhom8/
│       └── bai04/
│           ├── Server.java     # Máy chủ TCP, quản lý đa client & broadcast
│           ├── Client.java     # Máy khách (kèm luồng ngầm nhận tin nhắn)
│           └── CryptoUtil.java # Tiện ích mã hóa & giải mã AES 128-bit
├── server_files/               # Thư mục tự động lưu các file Server nhận được
├── build.xml                   # File cấu hình Ant build chuẩn
└── README.md                   # Tài liệu hướng dẫn
```

---

## 3. Yêu cầu môi trường
- **Hệ điều hành:** Windows / macOS / Linux.
- **Java Development Kit (JDK):** JDK 8 trở lên (khuyến nghị JDK 8, 11, 17 hoặc 21).
- **Phần mềm (tùy chọn):** NetBeans IDE, IntelliJ IDEA, VS Code hoặc Terminal/Command Prompt.
- **Mạng (nếu chạy giữa 2 máy khác mạng):** Đã kết nối cùng Network ZeroTier VPN.

---

## 4. Hướng dẫn chạy chương trình

### Cách 1: Chạy trực tiếp trên NetBeans IDE
1. Mở NetBeans -> **File** -> **Open Project** -> Chọn thư mục `NHOM8-BAI04`.
2. Mở file `src/nhom8/bai04/Server.java` -> Chuột phải chọn **Run File** (Server sẽ lắng nghe ở cổng mặc định `5000`).
3. Mở file `src/nhom8/bai04/Client.java` -> Chuột phải chọn **Run File** để khởi động máy khách.
   - Khi chạy trên cùng 1 máy: Nhấn `Enter` để lấy IP mặc định `127.0.0.1` và Port `5000`.
   - Khi chạy qua ZeroTier giữa 2 máy khác nhau: Nhập địa chỉ IP ZeroTier của máy chạy Server (ví dụ: `10.100.90.x`).
4. Có thể mở thêm nhiều cửa sổ `Client.java` để thử nghiệm chat nhóm nhiều người cùng lúc.

---

### Cách 2: Chạy bằng Dòng lệnh (Terminal / CMD)

**Bước 1: Biên dịch mã nguồn**
```bash
javac -encoding UTF-8 -d build/classes src/nhom8/bai04/*.java
```

**Bước 2: Khởi động Server**
```bash
java -cp build/classes nhom8.bai04.Server
```

**Bước 3: Mở một cửa sổ Terminal mới và khởi động Client**
```bash
java -cp build/classes nhom8.bai04.Client
```

---

## 5. Các chức năng trên Client
1. **1. Gửi tin nhắn:** Nhập nội dung văn bản. Tin nhắn sẽ tự động được mã hóa AES thành chuỗi Base64 trước khi truyền qua mạng và được Server giải mã rồi broadcast cho các Client khác.
2. **2. Gửi file:** Nhập đường dẫn file từ ổ cứng (ví dụ: `baocao.pdf`). File sẽ được truyền qua socket và lưu vào thư mục `server_files/` trên máy chủ.
3. **3. Thoát:** Gửi tín hiệu ngắt kết nối an toàn tới Server và đóng chương trình.
