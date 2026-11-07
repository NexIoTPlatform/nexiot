package cn.universal.dm.device.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.bo.UPPushBO;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UPPushConfigServiceTest {

  @Test
  void existingChannelShouldDefaultSupportButRequireExplicitEnable() throws Exception {
    UPPushConfigService service = new UPPushConfigService();

    UPPushBO enabled = parse(service, "{\"http\":{\"url\":\"http://localhost\",\"enable\":true}}");
    UPPushBO disabled = parse(service, "{\"http\":{\"url\":\"http://localhost\"}}");

    assertTrue(enabled.getHttp().isSupport());
    assertTrue(enabled.getHttp().isEnable());
    assertTrue(disabled.getHttp().isSupport());
    assertFalse(disabled.getHttp().isEnable());
  }

  @Test
  void shouldParseExternalMqttAndTcpDefaults() throws Exception {
    UPPushBO config =
        parse(
            new UPPushConfigService(),
            """
            {
              "externalMqtt": {
                "brokerUrl": "tcp://broker:1883",
                "topic": "devices/up",
                "enable": true
              },
              "tcp": {
                "host": "127.0.0.1",
                "port": 9000,
                "enable": true
              }
            }
            """);

    assertTrue(config.getExternalMqtt().isSupport());
    assertTrue(config.getExternalMqtt().isEnable());
    assertTrue(config.getExternalMqtt().isClusterClientId());
    assertTrue(config.getExternalMqtt().getQos() == 1);
    assertTrue(config.getTcp().isSupport());
    assertTrue(config.getTcp().isEnable());
    assertTrue("PERSISTENT".equals(config.getTcp().getConnectionMode()));
    assertTrue("LINE".equals(config.getTcp().getFrameMode()));
  }

  @Test
  void shouldParseKafkaAndRocketMqSecurityOptions() throws Exception {
    UPPushBO config =
        parse(
            new UPPushConfigService(),
            """
            {
              "kafka": {
                "bootstrapServers": "kafka:9093",
                "topic": "events",
                "securityProtocol": "SASL_SSL",
                "saslMechanism": "SCRAM-SHA-512",
                "username": "app",
                "password": "secret"
              },
              "rocketMQ": {
                "namesrvAddr": "rocketmq:9876",
                "topic": "events",
                "group": "app",
                "namespace": "tenant-a",
                "useTLS": true
              }
            }
            """);

    assertEquals("SASL_SSL", config.getKafka().getSecurityProtocol());
    assertEquals("SCRAM-SHA-512", config.getKafka().getSaslMechanism());
    assertEquals("tenant-a", config.getRocketMQ().getNamespace());
    assertTrue(config.getRocketMQ().isUseTLS());
  }

  @Test
  void shouldLoadApplicationConfigAtomicallyAndInvalidateExplicitly() {
    UPPushConfigService service = new UPPushConfigService();
    AtomicInteger calls = new AtomicInteger();
    setMapper(
        service,
        applicationMapper(
            calls,
            IoTUserApplication.builder()
                .appUniqueId("app-1")
                .appStatus(0)
                .cfg("{\"http\":{\"url\":\"http://localhost\",\"enable\":true}}")
                .build()));

    assertTrue(service.isApplicationEnabled("app-1"));
    assertTrue(service.getPushConfig("app-1").getHttp().isEnable());
    assertTrue(calls.get() == 1);

    service.refreshCache("app-1");
    assertTrue(service.isApplicationEnabled("app-1"));
    assertTrue(calls.get() == 2);
  }

  @Test
  void disabledApplicationShouldNeverExposeEnabledPushChannel() {
    UPPushConfigService service = new UPPushConfigService();
    setMapper(
        service,
        applicationMapper(
            new AtomicInteger(),
            IoTUserApplication.builder()
                .appUniqueId("app-disabled")
                .appStatus(1)
                .cfg("{\"http\":{\"url\":\"http://localhost\",\"enable\":true}}")
                .build()));

    assertFalse(service.isApplicationEnabled("app-disabled"));
    assertFalse(service.getPushConfig("app-disabled").getHttp().isEnable());
  }

  @Test
  void preloadResolvesEnabledAndDisabledApplicationsBeforeAnyUplink() {
    var service = new UPPushConfigService();
    var calls = new AtomicInteger();
    var applications =
        new java.util.concurrent.atomic.AtomicReference<>(
            java.util.List.of(
                IoTUserApplication.builder().appUniqueId("enabled").appStatus(0).cfg("{}").build(),
                IoTUserApplication.builder()
                    .appUniqueId("disabled")
                    .appStatus(1)
                    .cfg("{}")
                    .build()));
    setMapper(
        service,
        (IoTUserApplicationMapper)
            Proxy.newProxyInstance(
                IoTUserApplicationMapper.class.getClassLoader(),
                new Class<?>[] {IoTUserApplicationMapper.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("selectAll")) {
                    calls.incrementAndGet();
                    return applications.get();
                  }
                  throw new AssertionError("Unexpected hot-path mapper call: " + method.getName());
                }));
    service.preloadConfigurations();
    for (int i = 0; i < 1000; i++) {
      assertTrue(service.isApplicationEnabled("enabled"));
      assertFalse(service.isApplicationEnabled("disabled"));
      service.getPushConfig("enabled");
    }
    assertEquals(1, calls.get());
    applications.get().getFirst().setAppStatus(1);
    service.preloadConfigurations();
    assertFalse(service.isApplicationEnabled("enabled"));
  }

  private UPPushBO parse(UPPushConfigService service, String json) throws Exception {
    Method method = UPPushConfigService.class.getDeclaredMethod("parsePushConfig", String.class);
    method.setAccessible(true);
    return (UPPushBO) method.invoke(service, json);
  }

  private IoTUserApplicationMapper applicationMapper(
      AtomicInteger calls, IoTUserApplication application) {
    return (IoTUserApplicationMapper)
        Proxy.newProxyInstance(
            IoTUserApplicationMapper.class.getClassLoader(),
            new Class<?>[] {IoTUserApplicationMapper.class},
            (proxy, method, args) -> {
              if ("selectIotUserApplicationById".equals(method.getName())) {
                calls.incrementAndGet();
                return application;
              }
              return null;
            });
  }

  private void setMapper(UPPushConfigService service, IoTUserApplicationMapper mapper) {
    try {
      Field field = UPPushConfigService.class.getDeclaredField("ioTUserApplicationMapper");
      field.setAccessible(true);
      field.set(service, mapper);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }
}
