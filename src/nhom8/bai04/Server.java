package nhom8.bai04;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * Server - Máy chủ hỗ trợ nhiều Client chat với nhau (Broadcast)
 *
 * Luồng hoạt động:
 * 1. Server mở cổng 5000 và chờ Client kết nối
 * 2. Khi có Client mới -> lưu vào danh sách, tạo Thread riêng phục vụ
 * 3. Khi Client gửi tin nhắn -> Server chuyển tiếp (broadcast) cho TẤT CẢ Client khác
 * 4. Khi Client gửi file -> Server lưu file và thông báo cho mọi người
 */
public class Server {

    static final int PORT = 5000;

    // Danh sách lưu tất cả Client đang online (dùng để broadcast tin nhắn)
    static ArrayList<ThongTinClient> danhSachClient = new ArrayList<>();

    public static void main(String[] args) {

        // Tạo thư mục lưu file nhận được
        File folder = new File("server_files");
        if (!folder.exists()) {
            folder.mkdir();
        }

        System.out.println("=== NHOM 8 - SERVER (BROADCAST CHAT) ===");
        System.out.println("Server dang lang nghe o port: " + PORT);
        System.out.println("Thu muc luu file: " + folder.getAbsolutePath());
        System.out.println("Dang cho Client ket noi...\n");

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);

            int soClient = 0;

            while (true) {
                // Chờ Client kết nối
                Socket socket = serverSocket.accept();
                soClient++;

                String clientIP = socket.getInetAddress().getHostAddress();
                System.out.println("[+] Client #" + soClient + " da ket noi tu IP: " + clientIP);

                // Tạo luồng đọc/ghi cho Client này
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                // Lưu thông tin Client vào danh sách
                ThongTinClient thongTin = new ThongTinClient(soClient, output);
                themClient(thongTin);

                // Thông báo cho tất cả Client khác biết có người mới vào
                guiChoTatCa("CHAT",
                        "[Server]: Client #" + soClient + " (" + clientIP + ") da tham gia phong chat!",
                        soClient);

                // Tạo Thread riêng để xử lý Client này
                Thread thread = new Thread(new XuLyClient(socket, input, output, thongTin));
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Loi Server: " + e.getMessage());
        }
    }

    /**
     * Gửi tin nhắn cho TẤT CẢ Client khác (trừ người gửi)
     * synchronized = chỉ cho 1 Thread gọi hàm này tại 1 thời điểm (tránh xung đột)
     */
    static synchronized void guiChoTatCa(String loai, String noiDung, int idNguoiGui) {
        for (ThongTinClient client : danhSachClient) {
            // Không gửi lại cho chính người gửi
            if (client.id != idNguoiGui) {
                try {
                    client.output.writeUTF(loai);
                    client.output.writeUTF(noiDung);
                    client.output.flush();
                } catch (IOException e) {
                    // Client đã mất kết nối, bỏ qua
                }
            }
        }
    }

    /**
     * Gửi tin nhắn cho 1 Client cụ thể (dùng để gửi xác nhận riêng)
     */
    static synchronized void guiRieng(DataOutputStream output, String loai, String noiDung) {
        try {
            output.writeUTF(loai);
            output.writeUTF(noiDung);
            output.flush();
        } catch (IOException e) {
            // Client đã mất kết nối
        }
    }

    // Thêm Client vào danh sách
    static synchronized void themClient(ThongTinClient client) {
        danhSachClient.add(client);
    }

    // Xóa Client khỏi danh sách
    static synchronized void xoaClient(ThongTinClient client) {
        danhSachClient.remove(client);
    }
}

/**
 * Lưu thông tin của mỗi Client đang kết nối
 */
class ThongTinClient {
    int id;                    // Số thứ tự Client
    DataOutputStream output;   // Luồng ghi để gửi dữ liệu cho Client này

    ThongTinClient(int id, DataOutputStream output) {
        this.id = id;
        this.output = output;
    }
}

/**
 * Xử lý từng Client trên Thread riêng biệt
 */
class XuLyClient implements Runnable {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private ThongTinClient thongTin;

    public XuLyClient(Socket socket, DataInputStream input, DataOutputStream output, ThongTinClient thongTin) {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.thongTin = thongTin;
    }

    @Override
    public void run() {
        int clientId = thongTin.id;

        try {
            while (true) {
                // Đọc lệnh từ Client
                String lenh;
                try {
                    lenh = input.readUTF();
                } catch (EOFException e) {
                    break;
                }

                // ====== GỬI TIN NHẮN (BROADCAST CHO TẤT CẢ) ======
                if (lenh.equals("MSG")) {
                    String tinMaHoa = input.readUTF();
                    String tinGoc = CryptoUtil.decrypt(tinMaHoa);

                    System.out.println("[Client #" + clientId + " - TIN NHAN]");
                    System.out.println("  Du lieu ma hoa: " + tinMaHoa);
                    System.out.println("  Noi dung goc:   " + tinGoc);

                    // Chuyển tiếp tin nhắn cho TẤT CẢ Client khác (Broadcast)
                    Server.guiChoTatCa("CHAT",
                            "[Client #" + clientId + "]: " + tinGoc,
                            clientId);

                    // Gửi xác nhận riêng cho người gửi
                    Server.guiRieng(output, "THONGBAO",
                            "Tin nhan da duoc gui den tat ca (" + (Server.danhSachClient.size() - 1) + " nguoi)!");
                }

                // ====== GỬI FILE ======
                else if (lenh.equals("FILE")) {
                    String tenFile = input.readUTF();
                    long kichThuoc = input.readLong();

                    System.out.println("[Client #" + clientId + " - FILE] Dang nhan: "
                            + tenFile + " (" + kichThuoc + " bytes)");

                    // Lưu file vào thư mục server_files
                    File fileLuu = new File("server_files", tenFile);
                    FileOutputStream fos = new FileOutputStream(fileLuu);

                    byte[] buffer = new byte[4096];
                    long conLai = kichThuoc;

                    while (conLai > 0) {
                        int soByteDoc = input.read(buffer, 0, (int) Math.min(buffer.length, conLai));
                        if (soByteDoc == -1) break;
                        fos.write(buffer, 0, soByteDoc);
                        conLai = conLai - soByteDoc;
                    }
                    fos.close();

                    System.out.println("[OK] Da luu file: " + fileLuu.getAbsolutePath());

                    // Gửi xác nhận cho người gửi
                    Server.guiRieng(output, "THONGBAO",
                            "Server da nhan file: " + tenFile + " thanh cong!");

                    // Thông báo cho tất cả Client khác
                    Server.guiChoTatCa("CHAT",
                            "[Server]: Client #" + clientId + " da gui 1 file: " + tenFile,
                            clientId);
                }

                // ====== THOÁT ======
                else if (lenh.equals("EXIT")) {
                    System.out.println("[-] Client #" + clientId + " da thoat.");
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("[-] Client #" + clientId + " mat ket noi: " + e.getMessage());
        } finally {
            // Xóa Client khỏi danh sách và thông báo cho mọi người
            Server.xoaClient(thongTin);
            Server.guiChoTatCa("CHAT",
                    "[Server]: Client #" + clientId + " da roi phong chat.",
                    -1);

            try { socket.close(); } catch (IOException e) {}
            System.out.println("[-] Da dong ket noi voi Client #" + clientId);
        }
    }
}
