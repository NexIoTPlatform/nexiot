package cn.universal.core.config;

import static org.junit.Assert.*;

import cn.hutool.json.JSONUtil;
import cn.universal.common.config.InstanceIdProvider;
import cn.universal.common.plugin.PlatformPluginDescriptor;
import cn.universal.core.protocol.ProtocolModuleInfo;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class InstanceDiagnosticsRegistryTest {
  @Test
  public void preservesStartupAndRetainsStoppedSnapshotWithoutLeakingSecrets() {
    var redis = new RecordingRedis();
    var environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
        "tcp.protocol.enabled", "true", "udp.protocol.enabled", "false",
        "spring.datasource.password", "secret", "server.port", "9092")));
    var registry = new InstanceDiagnosticsRegistry(
        redis, new InstanceIdProvider(), environment, List.of(), List.of());
    registry.onReady();
    var first = JSONUtil.toBean(redis.value, InstanceDiagnosticsRegistry.NodeSnapshot.class);
    registry.publish();
    var next = JSONUtil.toBean(redis.value, InstanceDiagnosticsRegistry.NodeSnapshot.class);
    assertEquals(first.getStartedAt(), next.getStartedAt());
    assertEquals(first.getBootId(), next.getBootId());
    assertEquals(Integer.valueOf(9092), next.getPort());
    assertEquals(Map.of("tcp.protocol.enabled", true, "udp.protocol.enabled", false), next.getServices());
    assertFalse(redis.value.contains("secret"));
    assertEquals(TimeUnit.DAYS, redis.unit);
    assertEquals(7L, redis.ttl);
    registry.onClosed();
    assertTrue(redis.value.contains("STOPPED"));
    String stoppedValue = redis.value;
    registry.publish();
    registry.onReady();
    assertEquals(stoppedValue, redis.value);
  }

  @Test
  public void derivesTimeoutFromEachNodesThresholdAndKeepsGracefulStop() {
    var node = new InstanceDiagnosticsRegistry.NodeSnapshot();
    node.setLastHeartbeatAt(100_000);
    node.setOfflineThresholdSeconds(120);
    assertEquals("ONLINE", InstanceDiagnosticsRegistry.resolveStatus(node, 219_999));
    assertEquals("OFFLINE", InstanceDiagnosticsRegistry.resolveStatus(node, 220_000));
    assertEquals("OFFLINE", InstanceDiagnosticsRegistry.resolveStatus(node, 99_999));
    node.setStatus("STOPPED");
    assertEquals("STOPPED", InstanceDiagnosticsRegistry.resolveStatus(node, 100_001));
  }

  @Test
  public void retainsModuleNamesWithConfigurationAliasesAndDisabledServices() {
    var redis = new RecordingRedis();
    var environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
        "ctaiot.protocol.enable", true,
        "ezviz.protocol.enabled", false,
        "dahua-cloud.protocol.enabled", true,
        "hik.isc.protocol.enabled", true,
        "dahua.icc.protocol.enabled", true,
        "hik.sdk.protocol.enabled", true,
        "ai.plugin.enabled", true,
        "custom.protocol.enabled", true)));
    var registry = new InstanceDiagnosticsRegistry(redis, new InstanceIdProvider(), environment,
        List.of(module("ctaiot", "CT-AIoT (天翼物联)"), module("ezviz", "Ezviz (萤石)"),
            module("dahua_cloud", "大华云"), module("isc", "海康isc视频平台"),
            module("icc", "大华ICC视频平台"), module("hik_sdk", "Hikvision Direct SDK Gateway")),
        List.of(new PlatformPluginDescriptor() {
          public String getPluginCode() { return "platform-ai"; }
          public String getPluginName() { return "平台AI能力"; }
          public boolean isEnabled() { return true; }
        }));
    registry.onReady();
    registry.onClosed();
    var snapshot = JSONUtil.toBean(redis.value, InstanceDiagnosticsRegistry.NodeSnapshot.class);
    assertEquals("STOPPED", snapshot.getStatus());
    assertEquals(Boolean.FALSE, snapshot.getServices().get("ezviz.protocol.enabled"));
    assertEquals(Boolean.TRUE, snapshot.getServices().get("ctaiot.protocol.enable"));
    assertEquals(Map.of(
        "ctaiot.protocol.enable", "CT-AIoT (天翼物联)",
        "ezviz.protocol.enabled", "Ezviz (萤石)",
        "dahua-cloud.protocol.enabled", "大华云",
        "hik.isc.protocol.enabled", "海康isc视频平台",
        "dahua.icc.protocol.enabled", "大华ICC视频平台",
        "hik.sdk.protocol.enabled", "Hikvision Direct SDK Gateway",
        "ai.plugin.enabled", "平台AI能力"), snapshot.getServiceNames());
    assertTrue(snapshot.getServices().containsKey("custom.protocol.enabled"));
  }

  @Test
  public void readsOlderSnapshotsWithoutModuleNames() {
    var snapshot = JSONUtil.toBean("{\"services\":{\"ezviz.protocol.enabled\":true}}",
        InstanceDiagnosticsRegistry.NodeSnapshot.class);
    assertEquals(Boolean.TRUE, snapshot.getServices().get("ezviz.protocol.enabled"));
    assertTrue(snapshot.getServiceNames().isEmpty());
  }

  private static ProtocolModuleInfo module(String code, String name) {
    return new ProtocolModuleInfo() {
      public String getCode() { return code; }
      public String getName() { return name; }
      public String getDescription() { return ""; }
    };
  }

  private static class RecordingRedis extends StringRedisTemplate {
    String value;
    long ttl;
    TimeUnit unit;

    @Override
    @SuppressWarnings("unchecked")
    public ValueOperations<String, String> opsForValue() {
      return (ValueOperations<String, String>) Proxy.newProxyInstance(
          getClass().getClassLoader(), new Class<?>[] {ValueOperations.class}, (proxy, method, args) -> {
            if (method.getName().equals("set")) {
              value = (String) args[1];
              ttl = (long) args[2];
              unit = (TimeUnit) args[3];
            }
            return null;
          });
    }
  }
}
