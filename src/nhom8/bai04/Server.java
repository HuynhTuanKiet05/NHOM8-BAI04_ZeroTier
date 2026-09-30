package nhom8.bai04;

import java.io.*;
import java.net.*;

/**
 * Server - Máy chủ lắng nghe kết nối từ Client
 * 
 * Luồng hoạt động:
 * 1. Server mở cổng 5000 và chờ Client kết nối
 * 2. Khi có Client kết nối -> tạo 1 Thread riêng để phục vụ Client đó
 * 3. Server tiếp tục chờ Client tiếp theo (hỗ trợ nhiều Client cùng lúc)
 */
public class Server {

    // Cổng mà Server lắng nghe (Client phải kết nối đúng cổng này)
    static final int PORT = 5000;

    public static void main(String[] args) {

        // Tạo thư mục để lưu file nhận được từ Client
        File folder = new File("server_files");
        if (!folder.exists()) {
            folder.mkdir();
        }

        System.out.println("=== NHOM 8 - SERVER ===");
        System.out.println("Server dang lang nghe o port: " + PORT);
        System.out.println("Thu muc luu file: " + folder.getAbsolutePath());

        try {
            // Tạo ServerSocket để lắng nghe kết nối trên cổng PORT
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Dang cho Client ket noi...\n");

            int soClient = 0;

            // Vòng lặp vô hạn: luôn chờ Client mới kết nối
            while (true) {
                // accept() sẽ DỪNG ở đây cho đến khi có 1 Client kết nối vào
                Socket socket = serverSocket.accept();
                soClient++;

                String clientIP = socket.getInetAddress().getHostAddress();
                System.out.println("[+] Client #" + soClient + " da ket noi tu IP: " + clientIP);

                // Tạo Thread riêng để xử lý Client này
                // Nhờ đó Server có thể phục vụ nhiều Client cùng lúc (Multi-threading)
                Thread thread = new Thread(new XuLyClient(socket, soClient));
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Loi Server: " + e.getMessage());
        }
    }
}

/**
 * Lớp xử lý cho từng Client trên một Thread riêng biệt
 * Implements Runnable để có thể chạy trên Thread
 */
class XuLyClient implements Runnable {

    private Socket socket;  // Kết nối với Client
    private int clientId;   // Số thứ tự Client

    public XuLyClient(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try {
            // Tạo luồng đọc dữ liệu từ Client
            DataInputStream input = new DataInputStream(socket.getInputStream());

            // Tạo luồng ghi dữ liệu gửi về Client
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            // Vòng lặp liên tục đọc lệnh từ Client
            while (true) {

                // Đọc lệnh từ Client (readUTF đọc chuỗi String)
                String lenh;
                try {
                    lenh = input.readUTF();
                } catch (EOFException e) {
                    // Client đã đóng kết nối đột ngột
                    break;
                }

                // ====== XỬ LÝ LỆNH GỬI TIN NHẮN ======
                if (lenh.equals("MSG")) {
                    // Đọc tin nhắn đã mã hóa từ Client
                    String tinMaHoa = input.readUTF();

                    // Giải mã tin nhắn bằng CryptoUtil
                    String tinGoc = CryptoUtil.decrypt(tinMaHoa);

                    System.out.println("[Client #" + clientId + " - TIN NHAN]");
                    System.out.println("  Du lieu ma hoa: " + tinMaHoa);
                    System.out.println("  Noi dung goc:   " + tinGoc);

                    // Gửi phản hồi về Client
                    output.writeUTF("Server da nhan tin nhan: " + tinGoc);
                    output.flush(); // flush = đẩy dữ liệu đi ngay, không đợi buffer đầy
                }

                // ====== XỬ LÝ LỆNH GỬI FILE ======
                else if (lenh.equals("FILE")) {
                    // Đọc tên file và kích thước file từ Client
                    String tenFile = input.readUTF();
                    long kichThuoc = input.readLong();

                    System.out.println("[Client #" + clientId + " - FILE] Dang nhan: " + tenFile
                            + " (" + kichThuoc + " bytes)");

                    // Tạo file mới để ghi dữ liệu nhận được
                    File fileLuu = new File("server_files", tenFile);
                    FileOutputStream fos = new FileOutputStream(fileLuu);

                    // Đọc dữ liệu từ Client theo từng khối 4KB và ghi vào file
                    byte[] buffer = new byte[4096];
                    long conLai = kichThuoc;

                    while (conLai > 0) {
                        // Đọc tối đa buffer.length bytes hoặc số byte còn lại
                        int soByteDoc = input.read(buffer, 0, (int) Math.min(buffer.length, conLai));
                        if (soByteDoc == -1) break; // Hết dữ liệu

                        fos.write(buffer, 0, soByteDoc); // Ghi vào file
                        conLai = conLai - soByteDoc;
                    }

                    fos.close();
                    System.out.println("[OK] Da luu file: " + fileLuu.getAbsolutePath());

                    // Gửi phản hồi về Client
                    output.writeUTF("Server da nhan file: " + tenFile + " thanh cong!");
                    output.flush();
                }

                // ====== XỬ LÝ LỆNH THOÁT ======
                else if (lenh.equals("EXIT")) {
                    System.out.println("[-] Client #" + clientId + " da thoat.");
                    break;
                }
            }

            // Đóng kết nối
            socket.close();
            System.out.println("[-] Da dong ket noi voi Client #" + clientId);

        } catch (IOException e) {
            System.out.println("[-] Client #" + clientId + " mat ket noi: " + e.getMessage());
        }
    }
}
