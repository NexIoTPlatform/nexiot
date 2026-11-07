package cn.universal.common.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 实例ID提供者 用于生成唯一的应用实例标识 支持心跳机制和TTL管理，确保实例状态可追踪
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/27
 */
@Component
@Slf4j
public class InstanceIdProvider {

  @Value("${server.instance.id:platform}")
  private String configInstanceId;

  @Value("${node.offline.threshold.minutes:2}")
  private int offlineThresholdMinutes;

  @Resource private ServerProperties serverProperties;

  @Autowired private Environment environment;

  @Autowired(required = false)
  private StringRedisTemplate redisTemplate;

  private String instanceId;
  private final long startTime = java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
  private final Object heartbeatLifecycleMonitor = new Object();
  private volatile boolean shuttingDown;
  private static final String HEARTBEAT_KEY_PREFIX = "instance:heartbeat:";
  private static final String INSTANCE_INFO_KEY_PREFIX = "instance:info:";
  private static final int HEARTBEAT_TTL_MINUTES = 10; // 心跳TTL时间

  public InstanceIdProvider() {
    // 构造函数中不生成实例ID，延迟到依赖注入完成后
  }

  /** 初始化实例ID 在依赖注入完成后调用 */
  @jakarta.annotation.PostConstruct
  public void init() {
    this.instanceId = generateInstanceId();
    log.info("实例ID初始化完成: {}", instanceId);
  }

  /**
   * 生成实例ID 格式: hostname-ip-port-stableId 使用基于机器特征的稳定ID，确保同一台机器每次启动的实例ID一致
   *
   * @return 实例ID
   */
  private String generateInstanceId() {
    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      String ip = InetAddress.getLocalHost().getHostAddress();
      String port = environment != null ? environment.getProperty("server.port", "8080") : "8080";

      // 使用配置的实例ID或生成基于机器特征的稳定ID
      String stableId;
      if (StrUtil.isNotBlank(configInstanceId)) {
        stableId = configInstanceId;
      } else {
        // 基于机器特征生成稳定ID：hostname + ip + port 的hash值
        String machineSignature = hostname + "-" + ip + "-" + port;
        int hash = Math.abs(machineSignature.hashCode());
        stableId = String.format("%08x", hash & 0xFFFFFFFF); // 8位十六进制
      }

      return String.format("%s-%s-%s-%s", hostname, ip, port, stableId);
    } catch (UnknownHostException e) {
      log.warn("无法获取主机信息，使用默认实例ID", e);
      String fallbackId = StrUtil.isNotBlank(configInstanceId) ? configInstanceId : "default";
      return "univ-" + fallbackId;
    }
  }

  /**
   * 获取当前实例ID
   *
   * @return 实例ID
   */
  public String getInstanceId() {
    if (instanceId == null) {
      // 如果实例ID还未初始化，立即生成
      this.instanceId = generateInstanceId();
      log.info("实例ID延迟初始化完成: {}", instanceId);
    }
    return instanceId;
  }

  /**
   * 检查消息是否来自当前实例
   *
   * @param sourceId 消息源ID
   * @return true表示来自当前实例
   */
  public boolean isOwnMessage(String sourceId) {
    return getInstanceId().equals(sourceId);
  }

  /**
   * 获取简化的实例标识（用于日志等场景）
   *
   * @return 简化实例标识
   */
  public String getSimpleInstanceId() {
    String id = getInstanceId();
    return id.substring(0, Math.min(id.length(), 20)) + "...";
  }

  /** 更新实例心跳 每15秒执行一次，保持实例活跃状态 */
  @Scheduled(fixedRate = 15 * 1000) // 15秒（从30秒改为15秒）
  public void updateHeartbeat() {
    synchronized (heartbeatLifecycleMonitor) {
      if (shuttingDown || redisTemplate == null) {
        return;
      }
      updateHeartbeatInternal();
    }
  }

  private void updateHeartbeatInternal() {
    try {
      String heartbeatKey = HEARTBEAT_KEY_PREFIX + getInstanceId();
      String infoKey = INSTANCE_INFO_KEY_PREFIX + getInstanceId();

      long currentTime = System.currentTimeMillis();

      // 更新心跳时间戳
      redisTemplate
          .opsForValue()
          .set(heartbeatKey, String.valueOf(currentTime), HEARTBEAT_TTL_MINUTES, TimeUnit.MINUTES);

      // 更新实例信息
      String instanceInfo =
          String.format(
              "hostname:%s,ip:%s,port:%s,startTime:%d",
              InetAddress.getLocalHost().getHostName(),
              InetAddress.getLocalHost().getHostAddress(),
              environment != null ? environment.getProperty("server.port", "8080") : "8080",
              startTime);
      redisTemplate
          .opsForValue()
          .set(infoKey, instanceInfo, HEARTBEAT_TTL_MINUTES, TimeUnit.MINUTES);

      log.debug("实例心跳已更新: instanceId={}, time={}", getInstanceId(), currentTime);
    } catch (Exception e) {
      log.warn("更新实例心跳失败: instanceId={}, error={}", getInstanceId(), e.getMessage());
    }
  }

  @EventListener(ContextClosedEvent.class)
  public void onContextClosed(ContextClosedEvent event) {
    synchronized (heartbeatLifecycleMonitor) {
      shuttingDown = true;
    }
  }

  /**
   * 检查实例是否活跃 使用可配置的时间差额判断，而不是依赖TTL时间
   *
   * @param targetInstanceId 目标实例ID
   * @return true表示实例活跃
   */
  public boolean isInstanceActive(String targetInstanceId) {
    if (redisTemplate == null || StrUtil.isBlank(targetInstanceId)) {
      return false;
    }

    try {
      String heartbeatKey = HEARTBEAT_KEY_PREFIX + targetInstanceId;
      String lastHeartbeat = redisTemplate.opsForValue().get(heartbeatKey);

      if (lastHeartbeat == null) {
        log.debug("实例无心跳记录: instanceId={}", targetInstanceId);
        return false;
      }

      // 检查心跳时间是否在合理范围内
      long lastTime = Long.parseLong(lastHeartbeat);
      long currentTime = System.currentTimeMillis();
      long timeDiff = currentTime - lastTime;

      // 使用可配置的离线阈值，而不是依赖TTL时间
      long offlineThreshold = offlineThresholdMinutes * 60 * 1000;
      boolean isActive = timeDiff < offlineThreshold;

      log.debug(
          "实例活跃性检查: instanceId={}, lastHeartbeat={}, timeDiff={}ms, threshold={}ms, isActive={}",
          targetInstanceId,
          lastTime,
          timeDiff,
          offlineThreshold,
          isActive);

      return isActive;
    } catch (Exception e) {
      log.warn("检查实例活跃性失败: instanceId={}, error={}", targetInstanceId, e.getMessage());
      return false;
    }
  }

  /**
   * 获取实例信息
   *
   * @param targetInstanceId 目标实例ID
   * @return 实例信息
   */
  public String getInstanceInfo(String targetInstanceId) {
    if (redisTemplate == null || StrUtil.isBlank(targetInstanceId)) {
      return null;
    }

    try {
      String infoKey = INSTANCE_INFO_KEY_PREFIX + targetInstanceId;
      return redisTemplate.opsForValue().get(infoKey);
    } catch (Exception e) {
      log.warn("获取实例信息失败: instanceId={}, error={}", targetInstanceId, e.getMessage());
      return null;
    }
  }

  /**
   * 清理实例心跳数据
   *
   * @param targetInstanceId 目标实例ID
   */
  public void cleanupInstanceHeartbeat(String targetInstanceId) {
    if (redisTemplate == null || StrUtil.isBlank(targetInstanceId)) {
      return;
    }

    try {
      String heartbeatKey = HEARTBEAT_KEY_PREFIX + targetInstanceId;
      String infoKey = INSTANCE_INFO_KEY_PREFIX + targetInstanceId;

      redisTemplate.delete(heartbeatKey);
      redisTemplate.delete(infoKey);

      log.info("实例心跳数据已清理: instanceId={}", targetInstanceId);
    } catch (Exception e) {
      log.warn("清理实例心跳数据失败: instanceId={}, error={}", targetInstanceId, e.getMessage());
    }
  }

  /**
   * 获取所有活跃实例ID
   *
   * @return 活跃实例ID列表
   */
  public java.util.Set<String> getActiveInstanceIds() {
    if (redisTemplate == null) {
      return new java.util.HashSet<>();
    }

    try {
      java.util.Set<String> activeInstances = new java.util.HashSet<>();

      // 使用SCAN命令替代KEYS命令，兼容禁用了KEYS的Redis服务器
      String pattern = HEARTBEAT_KEY_PREFIX + "*";
      java.util.Set<String> heartbeatKeys = scanKeys(pattern);

      for (String heartbeatKey : heartbeatKeys) {
        String instanceId = heartbeatKey.substring(HEARTBEAT_KEY_PREFIX.length());
        if (isInstanceActive(instanceId)) {
          activeInstances.add(instanceId);
        }
      }

      log.debug("获取活跃实例列表: count={}, instances={}", activeInstances.size(), activeInstances);
      return activeInstances;
    } catch (Exception e) {
      log.warn("获取活跃实例列表失败: error={}", e.getMessage());
      return new java.util.HashSet<>();
    }
  }

  /**
   * 使用SCAN命令扫描匹配的键
   *
   * @param pattern 匹配模式
   * @return 匹配的键集合
   */
  private java.util.Set<String> scanKeys(String pattern) {
    java.util.Set<String> keys = new java.util.HashSet<>();

    try {
      // 使用SCAN命令替代KEYS命令
      redisTemplate.execute(
          (org.springframework.data.redis.core.RedisCallback<java.util.Set<String>>)
              connection -> {
                org.springframework.data.redis.core.Cursor<byte[]> cursor =
                    connection.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern)
                            .count(100)
                            .build());

                while (cursor.hasNext()) {
                  byte[] keyBytes = cursor.next();
                  String key = new String(keyBytes, java.nio.charset.StandardCharsets.UTF_8);
                  keys.add(key);
                }

                return keys;
              });

      log.debug("SCAN命令扫描完成: pattern={}, foundKeys={}", pattern, keys.size());

    } catch (Exception e) {
      log.warn("SCAN命令执行失败: pattern={}, error={}", pattern, e.getMessage());

      // 如果SCAN也失败，尝试使用KEYS命令（作为备选方案）
      try {
        java.util.Set<String> keysResult = redisTemplate.keys(pattern);
        if (keysResult != null) {
          keys.addAll(keysResult);
          log.debug("使用KEYS命令作为备选方案: pattern={}, foundKeys={}", pattern, keys.size());
        }
      } catch (Exception keysException) {
        log.error("KEYS命令也失败: pattern={}, error={}", pattern, keysException.getMessage());
      }
    }

    return keys;
  }
}
