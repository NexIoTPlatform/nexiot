package cn.universal.core.api.crypto.core;

import cn.universal.core.api.crypto.config.ApiCryptoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** API 报文加解密密钥读取。 */
@Component
@RequiredArgsConstructor
public class ApiCryptoKeyProvider {

  private final ApiCryptoProperties properties;

  public boolean isEnabled() {
    Boolean enabled = properties.getEnabled();
    return !Boolean.FALSE.equals(enabled);
  }

  public String resolveAesKey(String annotationSecretKey) {
    String key = firstText(annotationSecretKey, properties.getAesKey());
    if (!StringUtils.hasText(key)) {
      throw new IllegalStateException("AES key is not configured, set nexiot.api.crypto.aes-key");
    }
    return key;
  }

  private String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
