package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.entity.bo.UPPushBO;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.apache.kafka.clients.producer.ProducerConfig;

class KafkaPushStrategyTest {

  private final KafkaPushStrategy strategy = new KafkaPushStrategy();

  @Test
  void shouldUsePlaintextWhenAuthenticationIsNotConfigured() {
    Properties properties =
        strategy.buildProperties(
            UPPushBO.KafkaPushConfig.builder().bootstrapServers("localhost:9092").build());

    assertEquals("PLAINTEXT", properties.getProperty("security.protocol"));
    assertFalse(properties.containsKey("sasl.jaas.config"));
  }

  @Test
  void shouldBuildScramSslAuthenticationPropertiesAndEscapeCredentials() {
    Properties properties =
        strategy.buildProperties(
            UPPushBO.KafkaPushConfig.builder()
                .bootstrapServers("localhost:9093")
                .securityProtocol("sasl_ssl")
                .saslMechanism("scram-sha-512")
                .username("app\"user")
                .password("sec\\ret")
                .build());

    assertEquals("SASL_SSL", properties.getProperty("security.protocol"));
    assertEquals("SCRAM-SHA-512", properties.getProperty("sasl.mechanism"));
    String jaas = properties.getProperty("sasl.jaas.config");
    assertTrue(jaas.contains("ScramLoginModule"));
    assertTrue(jaas.contains("username=\"app\\\"user\""));
    assertTrue(jaas.contains("password=\"sec\\\\ret\""));
  }

  @Test
  void shouldUseIntegerKafkaTimeoutProperties() {
    Properties properties =
        strategy.buildProperties(
            UPPushBO.KafkaPushConfig.builder().bootstrapServers("localhost:9092").build());

    assertTrue(properties.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG) instanceof Integer);
    assertTrue(properties.get(ProducerConfig.MAX_BLOCK_MS_CONFIG) instanceof Integer);
  }

  @Test
  void testConnectionShouldRejectMissingSaslCredentialsBeforeOpeningNetworkConnection() {
    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                strategy.testConnection(
                    UPPushBO.KafkaPushConfig.builder()
                        .bootstrapServers("localhost:9092")
                        .topic("events")
                        .securityProtocol("SASL_SSL")
                        .username("app")
                        .build()));

    assertEquals("SASL 鉴权必须同时配置用户名和密码", error.getMessage());
  }
}
