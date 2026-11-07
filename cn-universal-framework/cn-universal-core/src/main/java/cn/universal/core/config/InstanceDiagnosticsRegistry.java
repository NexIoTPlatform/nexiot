package cn.universal.core.config;

import cn.hutool.json.JSONUtil;
import cn.universal.common.config.InstanceIdProvider;
import cn.universal.common.plugin.PlatformPluginDescriptor;
import cn.universal.core.protocol.ProtocolModuleInfo;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 保留节点最后一次诊断快照。独立于协议心跳，避免改变旧节点及清理任务的契约。 */
@Component
@Slf4j
public class InstanceDiagnosticsRegistry {
  static final String PREFIX = "instance:metadata:";
  private final StringRedisTemplate redis;
  private final InstanceIdProvider instanceIdProvider;
  private final ConfigurableEnvironment environment;
  private final List<ProtocolModuleInfo> protocolModules;
  private final List<PlatformPluginDescriptor> plugins;
  private final long startedAt = ManagementFactory.getRuntimeMXBean().getStartTime();
  private final String bootId = java.util.UUID.randomUUID().toString();
  private volatile boolean ready;
  private volatile boolean stopped;
  private int port;
  private NodeSnapshot snapshot;

  @Value("${node.diagnostics.retention-days:7}")
  private int retentionDays = 7;

  @Value("${node.offline.threshold.minutes:2}")
  private int offlineThresholdMinutes = 2;

  public InstanceDiagnosticsRegistry(
      StringRedisTemplate redis, InstanceIdProvider instanceIdProvider,
      ConfigurableEnvironment environment, List<ProtocolModuleInfo> protocolModules,
      List<PlatformPluginDescriptor> plugins) {
    this.redis = redis;
    this.instanceIdProvider = instanceIdProvider;
    this.environment = environment;
    this.protocolModules = protocolModules;
    this.plugins = plugins;
  }

  @EventListener
  public void onWebServerReady(WebServerInitializedEvent event) {
    if (event.getApplicationContext().getServerNamespace() == null) {
      port = event.getWebServer().getPort();
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public synchronized void onReady() {
    if (stopped) return;
    snapshot = describeNode();
    ready = true;
    publish();
  }

  @Scheduled(fixedDelay = 15000)
  public synchronized void publish() {
    if (!ready || stopped) return;
    long now = System.currentTimeMillis();
    snapshot.setLastHeartbeatAt(now);
    snapshot.setUptimeSeconds(Math.max(0, (now - startedAt) / 1000));
    var memory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    snapshot.setHeapUsedBytes(memory.getUsed());
    snapshot.setHeapMaxBytes(memory.getMax());
    snapshot.setThreadCount(ManagementFactory.getThreadMXBean().getThreadCount());
    snapshot.setSystemLoad(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
    saveSnapshot();
  }

  @EventListener(ContextClosedEvent.class)
  public synchronized void onClosed() {
    stopped = true;
    if (snapshot == null) return;
    snapshot.setStatus("STOPPED");
    snapshot.setStoppedAt(System.currentTimeMillis());
    saveSnapshot();
  }

  private void saveSnapshot() {
    try {
      redis.opsForValue().set(PREFIX + snapshot.getInstanceId(), JSONUtil.toJsonStr(snapshot),
          Math.max(1, retentionDays), TimeUnit.DAYS);
    } catch (Exception e) {
      log.warn("节点诊断快照写入失败: {}", e.getMessage());
    }
  }

  private NodeSnapshot describeNode() {
    NodeSnapshot node = new NodeSnapshot();
    node.setInstanceId(instanceIdProvider.getInstanceId());
    node.setBootId(bootId);
    node.setStartedAt(startedAt);
    node.setStatus("ONLINE");
    node.setOfflineThresholdSeconds(Math.max(1, offlineThresholdMinutes) * 60L);
    node.setPort(port > 0 ? port : environment.getProperty("server.port", Integer.class, 8080));
    node.setApplication(environment.getProperty("spring.application.name", "NexIoT"));
    node.setProfiles(List.of(environment.getActiveProfiles()));
    node.setPid(ProcessHandle.current().pid());
    node.setJavaVersion(System.getProperty("java.version"));
    node.setOs(System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
    node.setProcessors(Runtime.getRuntime().availableProcessors());
    try {
      node.setHostname(InetAddress.getLocalHost().getHostName());
      List<String> addresses = new ArrayList<>();
      for (var network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (!network.isUp() || network.isLoopback()) continue;
        for (var address : Collections.list(network.getInetAddresses())) {
          if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
            addresses.add(address.getHostAddress());
          }
        }
      }
      addresses.sort(Comparator.naturalOrder());
      node.setAddresses(addresses);
    } catch (Exception e) {
      node.setHostname("unknown");
      node.setAddresses(List.of());
    }
    // 只读取服务开关白名单，不上传环境变量、连接串或认证配置。
    Map<String, Boolean> services = new TreeMap<>();
    for (var source : environment.getPropertySources()) {
      if (!(source instanceof EnumerablePropertySource<?> enumerable)) continue;
      for (String key : enumerable.getPropertyNames()) {
        if (key.matches("[a-z0-9.-]+\\.(protocol|plugin)\\.enabled")
            || List.of("ctaiot.protocol.enable", "mqtt.cfg.third.enabled", "databridge.enabled")
                .contains(key)) {
          services.put(key, environment.getProperty(key, Boolean.class, false));
        }
      }
    }
    node.setServices(services);
    node.setServiceNames(describeServices(services));
    return node;
  }

  /** 名称来自上报节点自身的模块元数据，随快照保留，离线后仍能查看。 */
  private Map<String, String> describeServices(Map<String, Boolean> services) {
    Map<String, String> protocolNames = new LinkedHashMap<>();
    protocolModules.forEach(module -> protocolNames.put(module.getCode(), module.getName()));
    Map<String, String> pluginNames = new LinkedHashMap<>();
    plugins.forEach(plugin -> pluginNames.put(plugin.getPluginCode(), plugin.getPluginName()));
    // 仅兼容配置前缀与模块编码的差异；展示名称始终由模块维护。
    Map<String, String> protocolAliases = Map.of("hik.isc", "isc", "dahua.icc", "icc");
    Map<String, String> pluginAliases = Map.of(
        "ai", "platform-ai", "analysis", "analysis-lite", "video.ai", "video-ai-factory");
    Map<String, String> names = new TreeMap<>();
    for (String key : services.keySet()) {
      String name = null;
      int protocolSuffix = key.indexOf(".protocol.enable");
      int pluginSuffix = key.indexOf(".plugin.enabled");
      if (protocolSuffix > 0) {
        String prefix = key.substring(0, protocolSuffix);
        String code = protocolAliases.getOrDefault(prefix, prefix.replace('.', '_').replace('-', '_'));
        name = protocolNames.get(code);
      } else if (pluginSuffix > 0) {
        String prefix = key.substring(0, pluginSuffix);
        String code = pluginAliases.getOrDefault(prefix, prefix.replace('.', '-'));
        name = pluginNames.get(code);
      }
      if (name != null && !name.isBlank()) names.put(key, name);
    }
    return names;
  }

  /** SCAN 有界游标且正确关闭连接；Redis 故障向上抛出，页面不能将其显示成零节点。 */
  public List<NodeSnapshot> listNodes() {
    Map<String, NodeSnapshot> nodes = new LinkedHashMap<>();
    for (String key : scan(PREFIX + "*")) {
      String json = redis.opsForValue().get(key);
      if (json == null) continue;
      try {
        NodeSnapshot node = JSONUtil.toBean(json, NodeSnapshot.class);
        node.setStatus(resolveStatus(node, System.currentTimeMillis()));
        nodes.put(node.getInstanceId(), node);
      } catch (Exception e) {
        log.warn("忽略损坏的节点诊断记录: {}", key);
      }
    }
    // 混合版本集群仍显示旧节点，缺失的诊断字段不伪造。
    for (String key : scan("instance:heartbeat:*")) {
      String id = key.substring("instance:heartbeat:".length());
      if (nodes.containsKey(id)) continue;
      String value = redis.opsForValue().get(key);
      if (value == null) continue;
      try {
        NodeSnapshot node = new NodeSnapshot();
        node.setInstanceId(id);
        node.setHostname(id);
        node.setLegacy(true);
        node.setLastHeartbeatAt(Long.parseLong(value));
        node.setOfflineThresholdSeconds(Math.max(1, offlineThresholdMinutes) * 60L);
        node.setStatus(resolveStatus(node, System.currentTimeMillis()));
        nodes.put(id, node);
      } catch (NumberFormatException ignored) {
        // 已过期或非时间戳心跳不参与展示。
      }
    }
    return nodes.values().stream()
        .sorted(Comparator.comparing((NodeSnapshot node) -> "ONLINE".equals(node.getStatus()))
            .thenComparingLong(NodeSnapshot::getLastHeartbeatAt).reversed())
        .toList();
  }

  static String resolveStatus(NodeSnapshot node, long now) {
    if ("STOPPED".equals(node.getStatus())) return "STOPPED";
    long age = now - node.getLastHeartbeatAt();
    return node.getLastHeartbeatAt() > 0 && age >= 0
        && age < node.getOfflineThresholdSeconds() * 1000 ? "ONLINE" : "OFFLINE";
  }

  private List<String> scan(String pattern) {
    return redis.execute((RedisCallback<List<String>>) connection -> {
      List<String> keys = new ArrayList<>();
      try (var cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
        while (cursor.hasNext()) keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
      }
      return keys;
    });
  }

  @Data
  public static class NodeSnapshot {
    private String instanceId;
    private String bootId;
    private String hostname;
    private List<String> addresses = List.of();
    private Integer port;
    private String application;
    private List<String> profiles = List.of();
    private Map<String, Boolean> services = Map.of();
    private Map<String, String> serviceNames = Map.of();
    private String status;
    private long startedAt;
    private long lastHeartbeatAt;
    private Long stoppedAt;
    private long offlineThresholdSeconds;
    private long uptimeSeconds;
    private Long pid;
    private String javaVersion;
    private String os;
    private Integer processors;
    private Long heapUsedBytes;
    private Long heapMaxBytes;
    private Integer threadCount;
    private Double systemLoad;
    private boolean legacy;
  }
}
