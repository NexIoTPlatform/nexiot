package cn.universal.common.security;

import cn.hutool.core.util.StrUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-GCM encryption for certificate material persisted in the database. */
public final class CertificateFieldCrypto {

  private static final String PREFIX = "enc:gcm:v1:";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH = 128;
  private static final SecureRandom RANDOM = new SecureRandom();
  private static volatile SecretKeySpec key;

  private CertificateFieldCrypto() {}

  public static void configure(String keySeed) {
    if (StrUtil.isBlank(keySeed)) {
      key = null;
      return;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      key = new SecretKeySpec(digest.digest(keySeed.getBytes(StandardCharsets.UTF_8)), "AES");
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialise certificate encryption", ex);
    }
  }

  public static boolean isEncrypted(String value) {
    return value != null && value.startsWith(PREFIX);
  }

  public static String encrypt(String plainText) {
    if (StrUtil.isBlank(plainText) || isEncrypted(plainText)) {
      return plainText;
    }
    SecretKeySpec activeKey = requireKey();
    try {
      byte[] iv = new byte[IV_LENGTH];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, activeKey, new GCMParameterSpec(TAG_LENGTH, iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return PREFIX
          + Base64.getEncoder()
              .encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encrypt certificate material", ex);
    }
  }

  public static String decrypt(String value) {
    if (StrUtil.isBlank(value) || !isEncrypted(value)) {
      // Legacy plaintext remains readable so existing MQTT/TCP certificates keep working.
      return value;
    }
    SecretKeySpec activeKey = requireKey();
    try {
      byte[] encoded = Base64.getDecoder().decode(value.substring(PREFIX.length()));
      if (encoded.length <= IV_LENGTH) {
        throw new IllegalArgumentException("encrypted certificate material is truncated");
      }
      ByteBuffer buffer = ByteBuffer.wrap(encoded);
      byte[] iv = new byte[IV_LENGTH];
      buffer.get(iv);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, activeKey, new GCMParameterSpec(TAG_LENGTH, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decrypt certificate material", ex);
    }
  }

  private static SecretKeySpec requireKey() {
    SecretKeySpec activeKey = key;
    if (activeKey == null) {
      throw new IllegalStateException(
          "Certificate encryption key is not configured; set NEXIOT_CERTIFICATE_MASTER_KEY");
    }
    return activeKey;
  }
}
