package cn.universal.security.wechat;

import cn.universal.common.exception.IoTException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 使用机器私有主密钥加密公众号凭据；租户与字段名作为认证数据防止密文串用。 */
@Component
public class WechatConfigCrypto {
  private static final String PREFIX = "gcm:v1:";
  private final String configuredKey;
  private final SecureRandom random = new SecureRandom();

  public WechatConfigCrypto(@Value("${NEXIOT_WECHAT_CRYPTO_KEY:}") String configuredKey) {
    this.configuredKey = configuredKey;
  }

  public String encrypt(String value, String context) {
    if (value == null || value.isBlank()) return null;
    try {
      byte[] nonce = new byte[12];
      random.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
      byte[] packed = Arrays.copyOf(nonce, nonce.length + encrypted.length);
      System.arraycopy(encrypted, 0, packed, nonce.length, encrypted.length);
      return PREFIX + Base64.getEncoder().encodeToString(packed);
    } catch (GeneralSecurityException e) {
      throw new IoTException("公众号凭据加密失败，请检查服务器主密钥配置");
    }
  }

  public String decrypt(String value, String context) {
    if (value == null || value.isBlank()) return null;
    SecretKeySpec key = key();
    try {
      if (!value.startsWith(PREFIX)) throw new GeneralSecurityException();
      byte[] packed = Base64.getDecoder().decode(value.substring(PREFIX.length()));
      if (packed.length < 29) throw new GeneralSecurityException();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, packed, 0, 12));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      return new String(cipher.doFinal(packed, 12, packed.length - 12), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IoTException("公众号凭据解密失败，请检查各节点 NEXIOT_WECHAT_CRYPTO_KEY 是否一致");
    }
  }

  private SecretKeySpec key() {
    if (configuredKey == null || configuredKey.isBlank())
      throw new IoTException("请先在服务器私有环境配置 NEXIOT_WECHAT_CRYPTO_KEY（Base64 编码的 32 字节随机主密钥）");
    try {
      byte[] bytes = Base64.getDecoder().decode(configuredKey.trim());
      if (bytes.length != 32) throw new IllegalArgumentException();
      return new SecretKeySpec(bytes, "AES");
    } catch (IllegalArgumentException e) {
      throw new IoTException("NEXIOT_WECHAT_CRYPTO_KEY 必须是 Base64 编码的 32 字节随机主密钥");
    }
  }
}
