package nhom8.bai04;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client - Kết nối tới Server, gửi tin nhắn/file, và nhận tin từ Client khác
 *
 * Luồng hoạt động:
 * 1. Kết nối tới Server qua IP ZeroTier
 * 2. Khởi tạo 1 luồng ngầm (LangNghe) chuyên nhận tin nhắn từ Server/Client khác
 * 3. Luồng chính hiển thị menu để người dùng gõ tin nhắn hoặc gửi file
 */
public class Client {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== NHOM 8 - CLIENT ===");

        // --- Nhập IP Server ---
        System.out.print("Nhap IP Server (Enter = 127.0.0.1): ");
        String ip = sc.nextLine().trim();
        if (ip.isEmpty()) {
            ip = "127.0.0.1";
        }

        // --- Nhập Port ---
        System.out.print("Nhap Port (Enter = 5000): ");
        String portStr = sc.nextLine().trim();
        int port = 5000;
        if (!portStr.isEmpty()) {
            port = Integer.parseInt(portStr);
        }

        System.out.println("Dang ket noi toi " + ip + ":" + port + " ...");

        try {
            // Kết nối tới Server
            Socket socket = new Socket(ip, port);
            System.out.println("Ket noi thanh cong!\n");

            // Tạo luồng gửi dữ liệu tới Server
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            // Tạo luồng nhận dữ liệu từ Server
            DataInputStream input = new DataInputStream(socket.getInputStream());

            // ========== PHẦN MỚI: Luồng lắng nghe tin nhắn từ Server ==========
            // Luồng này chạy ngầm, liên tục đọc tin nhắn broadcast từ Server
            // (tin nhắn của Client khác hoặc thông báo hệ thống)
            Thread luongNghe = new Thread(new LangNghe(input));
            luongNghe.setDaemon(true); // Daemon = tự động tắt khi chương trình chính thoát
            luongNghe.start();

            boolean chay = true;

            while (chay) {
                System.out.println("\n--- MENU ---");
                System.out.println("1. Gui tin nhan (Broadcast - Ma hoa AES)");
                System.out.println("2. Gui file");
                System.out.println("3. Thoat");
                System.out.print("Chon (1/2/3): ");
                String chon = sc.nextLine().trim();

                switch (chon) {

                    case "1":
                        // ====== GỬI TIN NHẮN ======
                        System.out.print("Nhap tin nhan: ");
                        String tinNhan = sc.nextLine();

                        // Mã hóa trước khi gửi
                        String tinMaHoa = CryptoUtil.encrypt(tinNhan);
                        System.out.println("Tin nhan sau ma hoa: " + tinMaHoa);

                        // Gửi lệnh MSG + tin đã mã hóa lên Server
                        output.writeUTF("MSG");
                        output.writeUTF(tinMaHoa);
                        output.flush();

                        // Không cần đọc phản hồi ở đây nữa
                        // Luồng LangNghe sẽ tự động nhận và in ra màn hình
                        break;

                    case "2":
                        // ====== GỬI FILE ======
                        System.out.print("Nhap duong dan file: ");
                        String duongDan = sc.nextLine().trim();

                        File file = new File(duongDan);
                        if (!file.exists()) {
                            System.out.println("File khong ton tai!");
                            break;
                        }

                        // Gửi lệnh FILE + tên + kích thước
                        output.writeUTF("FILE");
                        output.writeUTF(file.getName());
                        output.writeLong(file.length());

                        // Đọc file và gửi từng khối 4KB
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int soByteDoc;

                        while ((soByteDoc = fis.read(buffer)) != -1) {
                            output.write(buffer, 0, soByteDoc);
                        }
                        output.flush();
                        fis.close();

                        System.out.println("Da gui xong file: " + file.getName());
                        // Luồng LangNghe sẽ tự nhận thông báo xác nhận từ Server
                        break;

                    case "3":
                        // ====== THOÁT ======
                        output.writeUTF("EXIT");
                        output.flush();
                        chay = false;
                        System.out.println("Da ngat ket noi.");
                        break;

                    default:
                        System.out.println("Lua chon khong hop le!");
                        break;
                }
            }

            socket.close();

        } catch (ConnectException e) {
            System.out.println("Khong the ket noi! Kiem tra Server da chay chua va IP/Port co dung khong.");
        } catch (IOException e) {
            System.out.println("Loi mang: " + e.getMessage());
        }
    }
}

/**
 * Luồng lắng nghe - chạy ngầm, liên tục nhận tin nhắn từ Server
 *
 * Server sẽ gửi 2 loại tin nhắn:
 * - "CHAT"     : Tin nhắn chat từ Client khác (broadcast)
 * - "THONGBAO" : Thông báo hệ thống (xác nhận gửi file, v.v.)
 */
class LangNghe implements Runnable {

    private DataInputStream input;

    LangNghe(DataInputStream input) {
        this.input = input;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Đọc loại tin nhắn
                String loai = input.readUTF();
                // Đọc nội dung
                String noiDung = input.readUTF();

                if (loai.equals("CHAT")) {
                    // Tin nhắn từ Client khác -> in ra màn hình
                    System.out.println("\n" + noiDung);
                } else if (loai.equals("THONGBAO")) {
                    // Thông báo hệ thống
                    System.out.println("\n[Thong bao]: " + noiDung);
                }
            }
        } catch (IOException e) {
            System.out.println("\nMat ket noi voi Server.");
        }
    }
}
