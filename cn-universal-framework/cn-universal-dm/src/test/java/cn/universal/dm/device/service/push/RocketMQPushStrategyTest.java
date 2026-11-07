package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.entity.bo.UPPushBO;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.junit.jupiter.api.Test;

class RocketMQPushStrategyTest {

  @Test
  void shouldApplyNamespaceAndTlsToProducer() {
    RocketMQPushStrategy strategy = new RocketMQPushStrategy();
    DefaultMQProducer producer = new DefaultMQProducer("test-group");
    UPPushBO.RocketMQPushConfig config =
        UPPushBO.RocketMQPushConfig.builder()
            .namesrvAddr("localhost:9876")
            .namespace("tenant-a")
            .useTLS(true)
            .build();

    strategy.configureProducer(producer, config);

    assertEquals("localhost:9876", producer.getNamesrvAddr());
    assertEquals("tenant-a", producer.getNamespace());
    assertTrue(producer.isUseTLS());
  }

  @Test
  void testConnectionShouldRejectIncompleteAclCredentialsBeforeOpeningNetworkConnection() {
    RocketMQPushStrategy strategy = new RocketMQPushStrategy();
    UPPushBO.RocketMQPushConfig config =
        UPPushBO.RocketMQPushConfig.builder()
            .namesrvAddr("localhost:9876")
            .topic("events")
            .group("test-group")
            .accessKey("access-key")
            .build();

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> strategy.testConnection(config));

    assertEquals("RocketMQ AccessKey 和 SecretKey 必须同时配置", error.getMessage());
  }
}
