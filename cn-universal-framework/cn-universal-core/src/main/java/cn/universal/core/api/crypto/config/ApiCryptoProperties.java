package cn.universal.core.api.crypto.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** API 报文加解密配置。 */
@Getter
@Setter
@ConfigurationProperties(ApiCryptoProperties.PREFIX)
public class ApiCryptoProperties {

  public static final String PREFIX = "nexiot.api.crypto";

  /** 是否启用 API 报文加解密。 */
  private Boolean enabled = Boolean.TRUE;

  /** 参数级解密时读取的密文参数名。 */
  private String paramName = "data";

  /** AES 密钥，要求 UTF-8 字节长度为 32。 */
  private String aesKey;

  /** 是否让 Jackson 支持 text/plain 密文请求体。 */
  private Boolean supportTextPlain = Boolean.TRUE;
}
