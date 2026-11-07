package cn.universal.http.access.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.http.access.config.HttpIntegrationConfig.AuthType;
import org.junit.jupiter.api.Test;

class HttpConfigValidatorTest {
  private final HttpAccessProperties properties = new HttpAccessProperties();
  private final HttpConfigValidator validator = new HttpConfigValidator(properties);

  @Test
  void acceptsNetworkOwnedInboundAndOutboundConfiguration() {
    assertDoesNotThrow(() -> validator.validate(validConfig()));
  }

  @Test
  void rejectsLegacySchemaAndUnsafeAccessCode() {
    HttpIntegrationConfig config = validConfig();
    config.setSchemaVersion(1);
    config.setAccessCode("../admin");
    config.setRevision(0);
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> validator.validate(config));
    assertTrue(error.getMessage().contains("schemaVersion"));
    assertTrue(error.getMessage().contains("accessCode"));
    assertTrue(error.getMessage().contains("revision"));
  }

  @Test
  void rejectsAuthenticationHeaderInjection() {
    HttpIntegrationConfig config = validConfig();
    config.getInbound().getAuth().setHeaderName("X-Key\r\nInjected");
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> validator.validate(config));
    assertTrue(error.getMessage().contains("API key authentication"));
  }

  @Test
  void hmacRequiresSecretAndBoundedReplayWindow() {
    HttpIntegrationConfig config = validConfig();
    config.getInbound().getAuth().setType(AuthType.HMAC_SHA256);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
    config.getInbound().getAuth().setSecret("test-hmac-secret");
    assertDoesNotThrow(() -> validator.validate(config));
    config.getInbound().getAuth().setClockSkewSeconds(3601);
    assertThrows(IllegalArgumentException.class, () -> validator.validate(config));
  }

  @Test
  void requestAndResponseLimitsCannotExceedModuleBoundary() {
    HttpIntegrationConfig config = validConfig();
    config.getInbound().setMaxBodyBytes(properties.getHardMaxInboundBytes() + 1);
    config.getOutbound().setMaxRequestBytes(properties.getHardMaxOutboundRequestBytes() + 1);
    config.getOutbound().setMaxResponseBytes(properties.getHardMaxOutboundResponseBytes() + 1);
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> validator.validate(config));
    assertTrue(error.getMessage().contains("inbound.maxBodyBytes"));
    assertTrue(error.getMessage().contains("outbound.maxRequestBytes"));
    assertTrue(error.getMessage().contains("outbound.maxResponseBytes"));
  }

  @Test
  void parserRejectsObsoleteVendorOperationsInNetworkConfiguration() {
    assertThrows(IllegalArgumentException.class,
        () -> new HttpConfigParser().parse("{\"accessCode\":\"factory-east\",\"operations\":[]}"));
  }

  private HttpIntegrationConfig validConfig() {
    HttpIntegrationConfig config = new HttpIntegrationConfig();
    config.setAccessCode("factory-east");
    config.getInbound().getAuth().setValue("test-api-key");
    return config;
  }
}
