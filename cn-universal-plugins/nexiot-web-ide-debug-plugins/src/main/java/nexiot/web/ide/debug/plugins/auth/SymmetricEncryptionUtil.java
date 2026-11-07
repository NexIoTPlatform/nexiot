package nexiot.web.ide.debug.plugins.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** JDK 21 对称加密工具类，支持AES和DES算法 */
public class SymmetricEncryptionUtil {

  // 密钥长度常量
  public static final int AES_128_KEY_SIZE = 128;
  public static final int AES_192_KEY_SIZE = 192;
  public static final int AES_256_KEY_SIZE = 256;
  public static final int DES_KEY_SIZE = 64;

  // 加密算法
  private static final String AES_ALGORITHM = "AES";
  private static final String DES_ALGORITHM = "DES";
  private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String DES_TRANSFORMATION = "DES/ECB/PKCS5Padding";

  // GCM参数
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  // 内置密钥（实际应用中建议从配置文件或环境变量读取）
  private static final String BUILTIN_AES_KEY = "yourSecureAESKey1234567890123456"; // 32字节(256位)
  private static final String BUILTIN_DES_KEY = "ABX12JNS"; // 8字节(64位)

  // 内置密钥的Base64编码
  private static final String BUILTIN_AES_KEY_BASE64;
  private static final String BUILTIN_DES_KEY_BASE64;

  static {
    try {
      // 初始化内置密钥的Base64编码
      BUILTIN_AES_KEY_BASE64 =
          Base64.getEncoder().encodeToString(BUILTIN_AES_KEY.getBytes(StandardCharsets.UTF_8));
      BUILTIN_DES_KEY_BASE64 =
          Base64.getEncoder().encodeToString(BUILTIN_DES_KEY.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new RuntimeException("初始化内置密钥失败", e);
    }
  }

  // 其他方法保持不变...

  /**
   * 使用内置AES密钥加密
   *
   * @param plaintext 明文
   * @return 加密结果（格式：IV:密文:标签）
   */
  public static String aesGcmEncryptWithBuiltinKey(String plaintext) throws Exception {
    return aesGcmEncrypt(plaintext, BUILTIN_AES_KEY_BASE64);
  }

  /**
   * 使用内置AES密钥解密
   *
   * @param ciphertext 密文（格式：IV:密文:标签）
   * @return 明文
   */
  public static String aesGcmDecryptWithBuiltinKey(String ciphertext) throws Exception {
    return aesGcmDecrypt(ciphertext, BUILTIN_AES_KEY_BASE64);
  }

  /**
   * 使用内置DES密钥加密
   *
   * @param plaintext 明文
   * @return 加密结果的Base64编码
   */
  public static String desEncryptWithBuiltinKey(String plaintext) {
    return desEncrypt(plaintext, BUILTIN_DES_KEY_BASE64);
  }

  /**
   * 使用内置DES密钥解密
   *
   * @param ciphertext 密文的Base64编码
   * @return 明文
   */
  public static String desDecryptWithBuiltinKey(String ciphertext) {
    return desDecrypt(ciphertext, BUILTIN_DES_KEY_BASE64);
  }

  /**
   * 使用AES/GCM加密
   *
   * @param plaintext 明文
   * @param base64Key 密钥的Base64编码
   * @return 加密结果（格式：IV:密文:标签）
   */
  public static String aesGcmEncrypt(String plaintext, String base64Key) throws Exception {
    // 解码密钥
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

    // 生成随机IV
    SecureRandom secureRandom = new SecureRandom();
    byte[] iv = new byte[GCM_IV_LENGTH];
    secureRandom.nextBytes(iv);

    // 初始化加密器
    Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
    GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

    // 加密
    byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

    // 拼接IV、密文和标签（GCM会自动处理标签）
    return Base64.getEncoder().encodeToString(iv)
        + ":"
        + Base64.getEncoder().encodeToString(ciphertext);
  }

  /**
   * 使用AES/GCM解密
   *
   * @param ciphertext 密文（格式：IV:密文:标签）
   * @param base64Key 密钥的Base64编码
   * @return 明文
   */
  public static String aesGcmDecrypt(String ciphertext, String base64Key) throws Exception {
    // 分割IV和密文
    String[] parts = ciphertext.split(":");
    byte[] iv = Base64.getDecoder().decode(parts[0]);
    byte[] encrypted = Base64.getDecoder().decode(parts[1]);

    // 解码密钥
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

    // 初始化解密器
    Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
    GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

    // 解密
    byte[] decrypted = cipher.doFinal(encrypted);
    return new String(decrypted, StandardCharsets.UTF_8);
  }

  /**
   * 使用DES加密
   *
   * @param plaintext 明文
   * @param base64Key 密钥的Base64编码
   * @return 加密结果的Base64编码
   */
  public static String desEncrypt(String plaintext, String base64Key) {
    // 解码密钥
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, DES_ALGORITHM);

    // 初始化加密器
    Cipher cipher = null;
    try {
      cipher = Cipher.getInstance(DES_TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * 使用DES解密
   *
   * @param ciphertext 密文的Base64编码
   * @param base64Key 密钥的Base64编码
   * @return 明文
   */
  public static String desDecrypt(String ciphertext, String base64Key) {
    // 解码密钥
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, DES_ALGORITHM);
    // 初始化解密器
    Cipher cipher = null;
    try {
      cipher = Cipher.getInstance(DES_TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      // 解密
      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
    }
    return null;
  }

  // 其他方法保持不变...

  public static void main(String[] args) {
    try {
      // 内置密钥示例
      String plaintext = "Hello, 内置密钥加密!";

      System.out.println("\n=== 使用内置AES密钥 ===");
      String aesCiphertext = aesGcmEncryptWithBuiltinKey(plaintext);
      String aesDecrypted = aesGcmDecryptWithBuiltinKey(aesCiphertext);
      System.out.println("AES密文: " + aesCiphertext);
      System.out.println("AES解密结果: " + aesDecrypted);

      System.out.println("\n=== 使用内置DES密钥 ===");
      String desCiphertext = desEncryptWithBuiltinKey(plaintext);
      String desDecrypted = desDecryptWithBuiltinKey(desCiphertext);
      System.out.println("DES密文: " + desCiphertext);
      System.out.println("DES解密结果: " + desDecrypted);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
