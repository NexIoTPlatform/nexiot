package cn.universal.core.api.crypto.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class ApiCryptoAesUtilTest {

  private static final String AES_KEY = "O2BEeIv399qHQNhD6aGW8R8DEj4bqHXm";

  @Test
  public void shouldDecryptEncryptedBase64Body() {
    byte[] json = "{\"id\":1,\"sql\":\"select 1\"}".getBytes(StandardCharsets.UTF_8);

    String encrypted = ApiCryptoAesUtil.encryptToBase64(json, AES_KEY);
    byte[] decrypted =
        ApiCryptoAesUtil.decryptFromBase64(encrypted.getBytes(StandardCharsets.UTF_8), AES_KEY);

    assertArrayEquals(json, decrypted);
  }

  @Test
  public void shouldMatchFrontendAesCiphertext() {
    byte[] json = "{\"id\":1,\"sql\":\"select 1\"}".getBytes(StandardCharsets.UTF_8);

    String encrypted = ApiCryptoAesUtil.encryptToBase64(json, AES_KEY);

    assertEquals("sgZzaIqgsSMA8rpO1/YUnTPTMnyMPHdNoH37MwF1THk=", encrypted);
  }
}
