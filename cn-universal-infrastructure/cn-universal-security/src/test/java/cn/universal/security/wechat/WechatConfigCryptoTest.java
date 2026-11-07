package cn.universal.security.wechat;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.IoTException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class WechatConfigCryptoTest {
  private final WechatConfigCrypto crypto = new WechatConfigCrypto(key((byte) 7));

  static String key(byte value) {
    byte[] bytes = new byte[32];
    java.util.Arrays.fill(bytes, value);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Test
  void encryptedSecretsRoundTripWithRandomizedCiphertexts() {
    String first = crypto.encrypt("secret-公众号", "tenant-a:appSecret");
    String second = crypto.encrypt("secret-公众号", "tenant-a:appSecret");
    assertTrue(first.startsWith("gcm:v1:"));
    assertFalse(first.contains("secret"));
    assertNotEquals(first, second);
    assertEquals("secret-公众号", crypto.decrypt(first, "tenant-a:appSecret"));
  }

  @Test
  void tenantFieldWrongKeyAndTamperedCiphertextAreRejected() {
    String encrypted = crypto.encrypt("credential", "tenant-a:appSecret");
    assertThrows(IoTException.class, () -> crypto.decrypt(encrypted, "tenant-b:appSecret"));
    assertThrows(IoTException.class, () -> crypto.decrypt(encrypted, "tenant-a:callbackToken"));
    assertThrows(IoTException.class, () -> new WechatConfigCrypto(key((byte) 8)).decrypt(encrypted, "tenant-a:appSecret"));
    byte[] packed = Base64.getDecoder().decode(encrypted.substring("gcm:v1:".length()));
    packed[packed.length - 1] ^= 1;
    assertThrows(IoTException.class, () -> crypto.decrypt("gcm:v1:" + Base64.getEncoder().encodeToString(packed), "tenant-a:appSecret"));
    assertThrows(IoTException.class, () -> crypto.decrypt("plaintext credential", "tenant-a:appSecret"));
  }

  @Test
  void emptyValuesNeedNoKeyButSecretsFailWithoutAValidPrivateKey() {
    WechatConfigCrypto missing = new WechatConfigCrypto("");
    assertNull(missing.encrypt(null, "context"));
    assertNull(missing.decrypt("", "context"));
    assertTrue(assertThrows(IoTException.class, () -> missing.encrypt("secret", "context"))
        .getMessage().contains("NEXIOT_WECHAT_CRYPTO_KEY"));
    assertThrows(IoTException.class, () -> new WechatConfigCrypto("invalid").encrypt("secret", "context"));
    assertThrows(IoTException.class, () -> new WechatConfigCrypto(Base64.getEncoder().encodeToString(new byte[16])).encrypt("secret", "context"));
  }
}
