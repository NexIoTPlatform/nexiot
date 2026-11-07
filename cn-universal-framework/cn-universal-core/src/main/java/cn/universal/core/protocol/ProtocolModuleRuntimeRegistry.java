package cn.universal.core.protocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 协议模块运行时注册表
 *
 * <p>用于管理实际启动的协议模块，替代基于字符串的查找机制
 *
 * <p>协议模块启动时自动注册，关闭时自动注销
 *
 * @author Universal IoT
 * @since 2025/1/2
 */
@Component
@Slf4j
public class ProtocolModuleRuntimeRegistry {

  /** 运行时协议模块注册表 Key: 协议代码, Value: 协议模块信息 */
  private static final Map<String, ProtocolModuleInfo> runtimeRegistry = new ConcurrentHashMap<>();

  /** 协议模块启动状态 Key: 协议代码, Value: 启动状态 */
  private static final Map<String, ProtocolStartupStatus> startupStatusMap =
      new ConcurrentHashMap<>();

  /**
   * 注册协议模块（启动时调用）
   *
   * @param protocolInfo 协议模块信息
   */
  public static void registerProtocol(ProtocolModuleInfo protocolInfo) {
    if (protocolInfo == null || protocolInfo.getCode() == null) {
      log.warn("[协议注册表] 尝试注册无效的协议模块信息");
      return;
    }

    String code = protocolInfo.getCode();
    runtimeRegistry.put(code, protocolInfo);
    startupStatusMap.put(code, new ProtocolStartupStatus(code, true, System.currentTimeMillis()));

    log.info("[协议注册表] ✅ 协议模块已注册: {} - {}", code, protocolInfo.getName());
  }

  /**
   * 注销协议模块（关闭时调用）
   *
   * @param protocolCode 协议代码
   */
  public static void unregisterProtocol(String protocolCode) {
    if (protocolCode == null) {
      return;
    }

    ProtocolModuleInfo removed = runtimeRegistry.remove(protocolCode);
    startupStatusMap.remove(protocolCode);

    if (removed != null) {
      log.info("[协议注册表] ❌ 协议模块已注销: {} - {}", protocolCode, removed.getName());
    }
  }

  /**
   * 获取所有运行时协议模块
   *
   * @return 协议模块信息集合
   */
  public static Collection<ProtocolModuleInfo> getAllRuntimeProtocols() {
    return Collections.unmodifiableCollection(runtimeRegistry.values());
  }

  /**
   * 获取运行时协议模块代码集合
   *
   * @return 协议代码集合
   */
  public static Set<String> getRuntimeProtocolCodes() {
    return Collections.unmodifiableSet(runtimeRegistry.keySet());
  }

  /**
   * 检查协议模块是否运行中
   *
   * @param protocolCode 协议代码
   * @return 是否运行中
   */
  public static boolean isProtocolRunning(String protocolCode) {
    return runtimeRegistry.containsKey(protocolCode);
  }

  /**
   * 获取协议模块信息
   *
   * @param protocolCode 协议代码
   * @return 协议模块信息，如果未找到返回null
   */
  public static ProtocolModuleInfo getProtocolInfo(String protocolCode) {
    return runtimeRegistry.get(protocolCode);
  }

  /**
   * 获取核心协议模块
   *
   * @return 核心协议模块集合
   */
  public static List<ProtocolModuleInfo> getCoreProtocols() {
    return runtimeRegistry.values().stream()
        .filter(ProtocolModuleInfo::isCore)
        .sorted(Comparator.comparing(ProtocolModuleInfo::getCode))
        .toList();
  }

  /**
   * 获取可选协议模块
   *
   * @return 可选协议模块集合
   */
  public static List<ProtocolModuleInfo> getOptionalProtocols() {
    return runtimeRegistry.values().stream()
        .filter(info -> !info.isCore())
        .sorted(Comparator.comparing(ProtocolModuleInfo::getCode))
        .toList();
  }

  /**
   * 按分类获取协议模块
   *
   * @param category 协议分类
   * @return 指定分类的协议模块集合
   */
  public static List<ProtocolModuleInfo> getProtocolsByCategory(
      ProtocolModuleInfo.ProtocolCategory category) {
    return runtimeRegistry.values().stream()
        .filter(info -> info.getCategory() == category)
        .sorted(Comparator.comparing(ProtocolModuleInfo::getCode))
        .toList();
  }

  /**
   * 获取协议启动状态
   *
   * @param protocolCode 协议代码
   * @return 启动状态信息
   */
  public static ProtocolStartupStatus getStartupStatus(String protocolCode) {
    return startupStatusMap.get(protocolCode);
  }

  /**
   * 获取所有协议启动状态
   *
   * @return 启动状态映射
   */
  public static Map<String, ProtocolStartupStatus> getAllStartupStatus() {
    return Collections.unmodifiableMap(startupStatusMap);
  }

  /**
   * 获取运行时统计信息
   *
   * @return 统计信息
   */
  public static RuntimeStatistics getStatistics() {
    int totalRunning = runtimeRegistry.size();
    int coreCount =
        (int) runtimeRegistry.values().stream().filter(ProtocolModuleInfo::isCore).count();
    int optionalCount = totalRunning - coreCount;

    Map<ProtocolModuleInfo.ProtocolCategory, Integer> categoryStats = new HashMap<>();
    runtimeRegistry
        .values()
        .forEach(info -> categoryStats.merge(info.getCategory(), 1, Integer::sum));

    return new RuntimeStatistics(totalRunning, coreCount, optionalCount, categoryStats);
  }

  /** 协议启动状态 */
  public static class ProtocolStartupStatus {
    private final String protocolCode;
    private final boolean running;
    private final long startupTime;

    public ProtocolStartupStatus(String protocolCode, boolean running, long startupTime) {
      this.protocolCode = protocolCode;
      this.running = running;
      this.startupTime = startupTime;
    }

    public String getProtocolCode() {
      return protocolCode;
    }

    public boolean isRunning() {
      return running;
    }

    public long getStartupTime() {
      return startupTime;
    }
  }

  /** 运行时统计信息 */
  public static class RuntimeStatistics {
    private final int totalRunning;
    private final int coreCount;
    private final int optionalCount;
    private final Map<ProtocolModuleInfo.ProtocolCategory, Integer> categoryStats;

    public RuntimeStatistics(
        int totalRunning,
        int coreCount,
        int optionalCount,
        Map<ProtocolModuleInfo.ProtocolCategory, Integer> categoryStats) {
      this.totalRunning = totalRunning;
      this.coreCount = coreCount;
      this.optionalCount = optionalCount;
      this.categoryStats = categoryStats;
    }

    public int getTotalRunning() {
      return totalRunning;
    }

    public int getCoreCount() {
      return coreCount;
    }

    public int getOptionalCount() {
      return optionalCount;
    }

    public Map<ProtocolModuleInfo.ProtocolCategory, Integer> getCategoryStats() {
      return categoryStats;
    }
  }
}
