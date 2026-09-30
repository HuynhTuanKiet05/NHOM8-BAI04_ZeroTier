package nhom8.bai04;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client - Kết nối tới Server và gửi tin nhắn hoặc file
 * 
 * Luồng hoạt động:
 * 1. Nhập IP và Port của Server (dùng IP ZeroTier khi test khác mạng)
 * 2. Kết nối tới Server
 * 3. Hiện menu: Gửi tin nhắn / Gửi file / Thoát
 */
public class Client {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== NHOM 8 - CLIENT ===");

        // --- Nhập IP Server ---
        // Nếu test trên cùng 1 máy: dùng 127.0.0.1
        // Nếu test qua ZeroTier: nhập IP ZeroTier của máy chạy Server
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
            // Tạo kết nối TCP Socket tới Server
            Socket socket = new Socket(ip, port);
            System.out.println("Ket noi thanh cong!\n");

            // Tạo luồng gửi dữ liệu tới Server
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            // Tạo luồng nhận dữ liệu từ Server
            DataInputStream input = new DataInputStream(socket.getInputStream());

            boolean chay = true;

            while (chay) {
                // Hiển thị menu
                System.out.println("--- MENU ---");
                System.out.println("1. Gui tin nhan (Ma hoa AES)");
                System.out.println("2. Gui file");
                System.out.println("3. Thoat");
                System.out.print("Chon (1/2/3): ");
                String chon = sc.nextLine().trim();

                switch (chon) {

                    case "1":
                        // ====== GỬI TIN NHẮN ======
                        System.out.print("Nhap tin nhan: ");
                        String tinNhan = sc.nextLine();

                        // Mã hóa tin nhắn trước khi gửi (bảo mật AES)
                        String tinMaHoa = CryptoUtil.encrypt(tinNhan);
                        System.out.println("Tin nhan sau ma hoa: " + tinMaHoa);

                        // Gửi lệnh "MSG" để Server biết đây là tin nhắn
                        output.writeUTF("MSG");
                        // Gửi nội dung tin nhắn đã mã hóa
                        output.writeUTF(tinMaHoa);
                        output.flush();

                        // Đọc phản hồi từ Server
                        String phanHoi = input.readUTF();
                        System.out.println("[Server]: " + phanHoi + "\n");
                        break;

                    case "2":
                        // ====== GỬI FILE ======
                        System.out.print("Nhap duong dan file: ");
                        String duongDan = sc.nextLine().trim();

                        // Kiểm tra file có tồn tại không
                        File file = new File(duongDan);
                        if (!file.exists()) {
                            System.out.println("File khong ton tai!\n");
                            break;
                        }

                        // Gửi lệnh "FILE" để Server biết đây là file
                        output.writeUTF("FILE");
                        // Gửi tên file
                        output.writeUTF(file.getName());
                        // Gửi kích thước file (số byte)
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

                        // Đọc phản hồi từ Server
                        String phanHoiFile = input.readUTF();
                        System.out.println("[Server]: " + phanHoiFile + "\n");
                        break;

                    case "3":
                        // ====== THOÁT ======
                        output.writeUTF("EXIT");
                        output.flush();
                        chay = false;
                        System.out.println("Da ngat ket noi.");
                        break;

                    default:
                        System.out.println("Lua chon khong hop le!\n");
                        break;
                }
            }

            // Đóng kết nối
            socket.close();

        } catch (ConnectException e) {
            System.out.println("Khong the ket noi! Kiem tra Server da chay chua va IP/Port co dung khong.");
        } catch (IOException e) {
            System.out.println("Loi mang: " + e.getMessage());
        }
    }
}
