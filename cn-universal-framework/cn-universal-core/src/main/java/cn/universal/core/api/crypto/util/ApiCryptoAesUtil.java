package cn.universal.core.api.crypto.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** NexIoT API AES/CBC/PKCS7(Base64) 工具。 */
public final class ApiCryptoAesUtil {

  private static final int BLOCK_SIZE = 16;
  private static final int AES_KEY_LENGTH = 32;
  private static final String AES_TRANSFORMATION = "AES/CBC/NoPadding";

  private ApiCryptoAesUtil() {}

  public static String encryptToBase64(byte[] data, String aesKey) {
    return Base64.getEncoder().encodeToString(aes(pad(data), aesKey, Cipher.ENCRYPT_MODE));
  }

  public static byte[] decryptFromBase64(byte[] encryptedBase64, String aesKey) {
    String encryptedText = new String(encryptedBase64, StandardCharsets.UTF_8).trim();
    byte[] encrypted = Base64.getDecoder().decode(encryptedText);
    return unpad(aes(encrypted, aesKey, Cipher.DECRYPT_MODE));
  }

  private static byte[] aes(byte[] input, String aesKey, int mode) {
    byte[] keyBytes = validateKey(aesKey);
    try {
      Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
      SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
      IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(keyBytes, 0, BLOCK_SIZE));
      cipher.init(mode, keySpec, iv);
      return cipher.doFinal(input);
    } catch (Exception e) {
      throw new IllegalStateException("API AES crypto failed", e);
    }
  }

  private static byte[] validateKey(String aesKey) {
    if (aesKey == null) {
      throw new IllegalArgumentException("AES key is not configured");
    }
    byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length != AES_KEY_LENGTH) {
      throw new IllegalArgumentException("Illegal AES key, UTF-8 byte length must be 32");
    }
    return keyBytes;
  }

  private static byte[] pad(byte[] src) {
    int amountToPad = BLOCK_SIZE - (src.length % BLOCK_SIZE);
    if (amountToPad == 0) {
      amountToPad = BLOCK_SIZE;
    }
    byte[] dest = Arrays.copyOf(src, src.length + amountToPad);
    Arrays.fill(dest, src.length, dest.length, (byte) amountToPad);
    return dest;
  }

  private static byte[] unpad(byte[] decrypted) {
    if (decrypted.length == 0) {
      return decrypted;
    }
    int pad = decrypted[decrypted.length - 1] & 0xFF;
    if (pad < 1 || pad > BLOCK_SIZE) {
      return decrypted;
    }
    return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
  }
}
