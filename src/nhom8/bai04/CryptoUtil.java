package nhom8.bai04;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Lớp tiện ích hỗ trợ mã hóa và giải mã tin nhắn bằng AES (Điểm cộng bảo mật)
 * 
 * AES = Advanced Encryption Standard, là thuật toán mã hóa phổ biến nhất hiện nay.
 * Khóa 16 ký tự = 128-bit. Cả Client và Server dùng chung khóa này để mã hóa/giải mã.
 */
public class CryptoUtil {

    // Khóa bí mật dùng chung, phải đúng 16 ký tự (= 128-bit)
    private static final String SECRET_KEY = "Nhom8MangMayTinh";

    /**
     * Mã hóa một chuỗi văn bản thành chuỗi đã mã hóa (không đọc được)
     * Ví dụ: "Xin chào" -> "aBcDeFgHiJkLmN=="
     */
    public static String encrypt(String text) {
        try {
            // Tạo khóa AES từ chuỗi SECRET_KEY
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            // Tạo bộ mã hóa AES
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // Mã hóa chuỗi text thành mảng byte
            byte[] encryptedBytes = cipher.doFinal(text.getBytes("UTF-8"));

            // Chuyển mảng byte thành chuỗi Base64 để dễ truyền qua mạng
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            System.out.println("Lỗi mã hóa: " + e.getMessage());
            return text; // Nếu lỗi thì trả về text gốc
        }
    }

    /**
     * Giải mã chuỗi đã mã hóa thành văn bản gốc ban đầu
     * Ví dụ: "aBcDeFgHiJkLmN==" -> "Xin chào"
     */
    public static String decrypt(String encryptedText) {
        try {
            // Tạo khóa AES (giống bên encrypt)
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            // Tạo bộ giải mã AES
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            // Chuyển chuỗi Base64 thành mảng byte, rồi giải mã
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));

            // Chuyển mảng byte thành chuỗi văn bản
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            System.out.println("Lỗi giải mã: " + e.getMessage());
            return encryptedText; // Nếu lỗi thì trả về chuỗi gốc
        }
    }
}
