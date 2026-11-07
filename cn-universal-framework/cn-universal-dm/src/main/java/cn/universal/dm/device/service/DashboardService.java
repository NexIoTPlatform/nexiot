package cn.universal.dm.device.service;

import cn.universal.dm.device.service.push.processor.PushStatisticsProcessor;
import cn.universal.persistence.entity.IoTDashboardStatistics;
import cn.universal.persistence.mapper.IoTDashboardStatisticsMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 仪表盘服务 - 重构版本，使用真实数据库查询
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Slf4j
@Service
public class DashboardService {

  @Autowired private IoTDashboardStatisticsMapper dashboardStatisticsMapper;

  @Autowired private StringRedisTemplate redisTemplate;

  @Autowired private SystemMonitorService systemMonitorService;

  @Autowired private PushStatisticsProcessor pushStatisticsProcessor;

  @Autowired private IoTDeviceMapper ioTDeviceMapper;

  @Autowired private DashboardScopeSupport dashboardScopeSupport;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  
  // 缓存键前缀
  private static final String CACHE_PREFIX = "dashboard:cache:";
  
  // 缓存过期时间（秒）
  private static final long CACHE_EXPIRE_SECONDS = 300; // 5分钟

  /**
   * 记录统计数据
   *
   * @param statistics 统计数据
   */
  public void recordStatistics(IoTDashboardStatistics statistics) {
    try {
      // 先查找是否存在记录
      IoTDashboardStatistics existing =
          dashboardStatisticsMapper.selectByUniqueKey(
              statistics.getStatDate(),
              statistics.getProductKey(),
              statistics.getChannel(),
              statistics.getMetricType());

      if (existing == null) {
        // 不存在则插入新记录
        statistics.setCreateTime(LocalDateTime.now());
        statistics.setUpdateTime(LocalDateTime.now());
        dashboardStatisticsMapper.insert(statistics);
        log.debug("[仪表盘统计] 插入新记录: {}", statistics);
      } else {
        // 存在则更新记录
        dashboardStatisticsMapper.updateByUniqueKey(
            statistics.getStatDate(),
            statistics.getProductKey(),
            statistics.getChannel(),
            statistics.getMetricType(),
            statistics.getMetricValue(),
            LocalDateTime.now());
        log.debug("[仪表盘统计] 更新现有记录: {}", statistics);
      }
      
      // 清除相关缓存
      clearCache(statistics.getStatDate());
      
    } catch (Exception e) {
      log.error("[仪表盘统计] 记录统计数据失败: {}", statistics, e);
    }
  }

  /**
   * 批量记录统计数据
   *
   * @param statisticsList 统计数据列表
   */
  public void batchRecordStatistics(List<IoTDashboardStatistics> statisticsList) {
    if (statisticsList == null || statisticsList.isEmpty()) {
      return;
    }

    for (IoTDashboardStatistics statistics : statisticsList) {
      recordStatistics(statistics);
    }
  }

  /** 调试方法：查看数据库中的统计记录 */
  public List<IoTDashboardStatistics> debugStatistics(LocalDate date) {
    log.info("[仪表盘调试] 查询日期: {}", date);

    // 查询所有记录
    List<IoTDashboardStatistics> allRecords =
        dashboardStatisticsMapper.selectByDateAndMetric(date, null, null, null);

    log.info("[仪表盘调试] 找到 {} 条记录", allRecords.size());

    for (IoTDashboardStatistics record : allRecords) {
      log.info(
          "[仪表盘调试] 记录: date={}, product={}, channel={}, type={}, value={}",
          record.getStatDate(),
          record.getProductKey(),
          record.getChannel(),
          record.getMetricType(),
          record.getMetricValue());
    }

    return allRecords;
  }

  /** 获取实时数据 */
  public Map<String, Object> getRealtimeData() {
    return getTodayOverview(); // 实时数据就是今日概览
  }

  /** 获取图表数据 */
  public Map<String, Object> getChartData(LocalDate date, String type, String channel) {
    Map<String, Object> chartData = new HashMap<>();

    try {
      String dateKey = date.format(DATE_FORMATTER);

      switch (type) {
        case "device_trend":
          chartData = getDeviceTrendData(dateKey);
          break;
        case "message_trend":
          chartData = getMessageTrendData(dateKey, channel);
          break;
        case "channel_distribution":
          chartData = getChannelDistributionData(dateKey);
          break;
        case "performance_metrics":
          chartData = getPerformanceMetricsData(dateKey);
          break;
        default:
          chartData.put("error", "不支持的图表类型: " + type);
      }
    } catch (Exception e) {
      log.error("[仪表盘] 获取图表数据失败，类型: {}", type, e);
      chartData.put("error", "获取图表数据失败");
    }

    return chartData;
  }

  /** 获取系统监控数据 */
  public Map<String, Object> getSystemMonitorData() {
    Map<String, Object> monitorData = new HashMap<>();

    try {
      // 获取系统监控数据
      Map<String, Object> systemMetrics = systemMonitorService.getSystemMetrics();

      // 获取系统信息
      Map<String, Object> systemInfo = systemMonitorService.getSystemInfo();

      monitorData.put("systemMetrics", systemMetrics);
      monitorData.put("systemInfo", systemInfo);
      monitorData.put("isLinuxSystem", systemMonitorService.isLinuxSystem());
      monitorData.put("timestamp", System.currentTimeMillis());

    } catch (Exception e) {
      log.error("[仪表盘] 获取系统监控数据失败", e);
      monitorData.put("error", "获取系统监控数据失败");
    }

    return monitorData;
  }

  /** 获取统计管理器状态 */
  public Map<String, Object> getStatisticsManagerStatus() {
    return pushStatisticsProcessor.getManagerStatus();
  }

  /** 强制刷新统计到Redis */
  public void forceFlushStatistics() {
    pushStatisticsProcessor.forceFlushToRedis();
  }

  /** 获取系统监控服务 */
  public SystemMonitorService getSystemMonitorService() {
    return systemMonitorService;
  }

  /** 获取今日概览数据 */
  public Map<String, Object> getTodayOverview() {
    Map<String, Object> overview = new HashMap<>();

    try {
      LocalDate today = LocalDate.now();
      String dateKey = today.format(DATE_FORMATTER);

      // 设备统计
      Map<String, Object> deviceStats = getDeviceStats(dateKey);
      overview.put("deviceStats", deviceStats);

      // 消息统计
      Map<String, Object> messageStats = getMessageStats(dateKey);
      overview.put("messageStats", messageStats);

      // 性能统计
      Map<String, Object> performanceStats = getPerformanceStats(dateKey);
      overview.put("performanceStats", performanceStats);

      // 渠道统计
      Map<String, Object> channelStats = getChannelStats(dateKey);
      overview.put("channelStats", channelStats);

      overview.put("timestamp", System.currentTimeMillis());

    } catch (Exception e) {
      log.error("[仪表盘] 获取今日概览数据失败", e);
      overview.put("error", "获取今日概览数据失败");
    }

    return overview;
  }

  /** 获取今日概览数据（带用户权限过滤） */
  public Map<String, Object> getTodayOverviewWithUserFilter(String unionId, boolean isAdmin) {
    Map<String, Object> overview = new HashMap<>();

    try {
      LocalDate today = LocalDate.now();
      String dateKey = today.format(DATE_FORMATTER);

      // 设备统计（根据用户权限过滤）
      Map<String, Object> deviceStats = getDeviceStatsWithUserFilter(dateKey, unionId, isAdmin);
      overview.put("deviceStats", deviceStats);

      // 消息统计（根据用户权限过滤）
      Map<String, Object> messageStats = getMessageStatsWithUserFilter(dateKey, unionId, isAdmin);
      overview.put("messageStats", messageStats);

      // 性能统计
      Map<String, Object> performanceStats = getPerformanceStats(dateKey);
      overview.put("performanceStats", performanceStats);

      // 渠道统计（根据用户权限过滤）
      Map<String, Object> channelStats = getChannelStatsWithUserFilter(dateKey, unionId, isAdmin);
      overview.put("channelStats", channelStats);

      overview.put("timestamp", System.currentTimeMillis());

    } catch (Exception e) {
      log.error("[仪表盘] 获取今日概览数据失败", e);
      overview.put("error", "获取今日概览数据失败");
    }

    return overview;
  }

  /** 从数据库获取设备统计数据（带用户权限过滤） */
  public Map<String, Object> getDeviceStatsFromDB(String unionId, boolean isAdmin) {
    Map<String, Object> deviceStats = new HashMap<>();

    try {
      DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(unionId, isAdmin);
      long totalDevices = dashboardScopeSupport.countDevices(scope, unionId, isAdmin);
      long onlineDevices = dashboardScopeSupport.countOnlineDevices(scope, unionId, isAdmin);

      long offlineDevices = totalDevices - onlineDevices;
      double onlineRate =
          totalDevices > 0
              ? Math.round((double) onlineDevices / totalDevices * 100 * 100.0) / 100.0
              : 0.0;

      deviceStats.put("totalDevices", totalDevices);
      deviceStats.put("onlineDevices", onlineDevices);
      deviceStats.put("offlineDevices", offlineDevices);
      deviceStats.put("onlineRate", onlineRate);

      log.debug(
          "[仪表盘] 设备统计: 总数={}, 在线={}, 离线={}, 在线率={}%",
          totalDevices, onlineDevices, offlineDevices, onlineRate);

    } catch (Exception e) {
      log.error("[仪表盘] 获取设备统计数据失败", e);
      deviceStats.put("error", "获取设备统计数据失败");
    }

    return deviceStats;
  }

  // ==================== 私有方法 ====================

  /** 获取设备统计数据（带用户权限过滤） */
  private Map<String, Object> getDeviceStatsWithUserFilter(
      String dateKey, String unionId, boolean isAdmin) {
    Map<String, Object> deviceStats = new HashMap<>();

    try {
      DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(unionId, isAdmin);
      long totalDevices = dashboardScopeSupport.countDevices(scope, unionId, isAdmin);
      long onlineDevices = dashboardScopeSupport.countOnlineDevices(scope, unionId, isAdmin);

      long offlineDevices = totalDevices - onlineDevices;
      double onlineRate =
          totalDevices > 0
              ? Math.round((double) onlineDevices / totalDevices * 100 * 100.0) / 100.0
              : 0.0;

      deviceStats.put("totalDevices", totalDevices);
      deviceStats.put("onlineDevices", onlineDevices);
      deviceStats.put("offlineDevices", offlineDevices);
      deviceStats.put("onlineRate", onlineRate);

      log.debug(
          "[仪表盘] 设备统计(用户过滤): 总数={}, 在线={}, 离线={}, 在线率={}%",
          totalDevices, onlineDevices, offlineDevices, onlineRate);

    } catch (Exception e) {
      log.error("[仪表盘] 获取设备统计数据失败", e);
      deviceStats.put("totalDevices", 0L);
      deviceStats.put("onlineDevices", 0L);
      deviceStats.put("offlineDevices", 0L);
      deviceStats.put("onlineRate", 0.0);
    }

    return deviceStats;
  }

  /** 获取消息统计数据（带用户权限过滤） */
  private Map<String, Object> getMessageStatsWithUserFilter(
      String dateKey, String unionId, boolean isAdmin) {
    Map<String, Object> messageStats = new HashMap<>();

    try {
      if (isAdmin) {
        // 管理员：获取所有消息统计
        return getMessageStats(dateKey);
      } else {
        // 普通用户：获取自己的消息统计
        // 这里需要根据用户权限过滤，暂时返回空数据
        messageStats.put("totalMessages", 0L);
        messageStats.put("successMessages", 0L);
        messageStats.put("failedMessages", 0L);
        messageStats.put("successRate", 0.0);
        messageStats.put("avgPerHour", 0.0);
      }
    } catch (Exception e) {
      log.error("[仪表盘] 获取消息统计数据失败", e);
    }

    return messageStats;
  }

  /** 获取渠道统计数据（带用户权限过滤） */
  private Map<String, Object> getChannelStatsWithUserFilter(
      String dateKey, String unionId, boolean isAdmin) {
    Map<String, Object> channelStats = new HashMap<>();

    try {
      if (isAdmin) {
        // 管理员：获取所有渠道统计
        return getChannelStats(dateKey);
      } else {
        // 普通用户：获取自己的渠道统计
        // 这里需要根据用户权限过滤，暂时返回空数据
        channelStats.put("channels", new HashMap<>());
      }
    } catch (Exception e) {
      log.error("[仪表盘] 获取渠道统计数据失败", e);
    }

    return channelStats;
  }

  /** 获取设备统计数据 */
  private Map<String, Object> getDeviceStats(String dateKey) {
    Map<String, Object> deviceStats = new HashMap<>();

    try {
      // 直接从数据库查询设备总数
      long totalDevices = ioTDeviceMapper.countAllDevices();

      // 直接从数据库查询在线设备数
      long onlineDevices = ioTDeviceMapper.countOnlineDevices();

      long offlineDevices = totalDevices - onlineDevices;
      double onlineRate =
          totalDevices > 0
              ? Math.round((double) onlineDevices / totalDevices * 100 * 100.0) / 100.0
              : 0.0;

      deviceStats.put("totalDevices", totalDevices);
      deviceStats.put("onlineDevices", onlineDevices);
      deviceStats.put("offlineDevices", offlineDevices);
      deviceStats.put("onlineRate", onlineRate);

      log.debug(
          "[仪表盘] 设备统计: 总数={}, 在线={}, 离线={}, 在线率={}%",
          totalDevices, onlineDevices, offlineDevices, onlineRate);

    } catch (Exception e) {
      log.error("[仪表盘] 获取设备统计数据失败", e);
      deviceStats.put("totalDevices", 0L);
      deviceStats.put("onlineDevices", 0L);
      deviceStats.put("offlineDevices", 0L);
      deviceStats.put("onlineRate", 0.0);
    }

    return deviceStats;
  }

  /** 获取消息统计数据 - 使用真实数据库查询 */
  private Map<String, Object> getMessageStats(String dateKey) {
    Map<String, Object> messageStats = new HashMap<>();

    try {
      // 尝试从缓存获取
      String cacheKey = CACHE_PREFIX + "message_stats:" + dateKey;
      String cachedValue = redisTemplate.opsForValue().get(cacheKey);
      
      if (cachedValue != null) {
        log.debug("[仪表盘] 从缓存获取消息统计数据: {}", dateKey);
        // 这里简化处理，实际应该反序列化JSON
        // 为了简单，直接查询数据库
      }
      
      // 从数据库查询
      LocalDate date = LocalDate.parse(dateKey, DATE_FORMATTER);
      Map<String, Object> dbStats = dashboardStatisticsMapper.selectMessageStatsByDate(date);
      
      if (dbStats != null && !dbStats.isEmpty()) {
        long totalMessages = getLongValue(dbStats.get("totalMessages"));
        long successMessages = getLongValue(dbStats.get("successMessages"));
        long failedMessages = getLongValue(dbStats.get("failedMessages"));
        
        messageStats.put("totalMessages", totalMessages);
        messageStats.put("successMessages", successMessages);
        messageStats.put("failedMessages", failedMessages);
        messageStats.put(
            "successRate", totalMessages > 0 ? (double) successMessages / totalMessages * 100 : 0.0);
        messageStats.put("avgPerHour", totalMessages / 24.0);
        
        log.debug("[仪表盘] 消息统计: 总数={}, 成功={}, 失败={}", 
            totalMessages, successMessages, failedMessages);
      } else {
        // 没有数据，返回0
        messageStats.put("totalMessages", 0L);
        messageStats.put("successMessages", 0L);
        messageStats.put("failedMessages", 0L);
        messageStats.put("successRate", 0.0);
        messageStats.put("avgPerHour", 0.0);
      }

    } catch (Exception e) {
      log.error("[仪表盘] 获取消息统计数据失败", e);
      messageStats.put("totalMessages", 0L);
      messageStats.put("successMessages", 0L);
      messageStats.put("failedMessages", 0L);
      messageStats.put("successRate", 0.0);
      messageStats.put("avgPerHour", 0.0);
    }

    return messageStats;
  }

  /** 获取性能统计数据 */
  private Map<String, Object> getPerformanceStats(String dateKey) {
    Map<String, Object> performanceStats = new HashMap<>();

    try {
      // 平均响应时间
      String responseTimeKey =
          String.format("dashboard:metric:%s:all:all:all:response_time_total", dateKey);
      String responseCountKey =
          String.format("dashboard:metric:%s:all:all:all:response_count", dateKey);

      String totalTime = redisTemplate.opsForValue().get(responseTimeKey);
      String count = redisTemplate.opsForValue().get(responseCountKey);

      double avgResponseTime = 0.0;
      if (totalTime != null && count != null) {
        long total = Long.valueOf(totalTime);
        long responseCount = Long.valueOf(count);
        avgResponseTime = responseCount > 0 ? (double) total / responseCount : 0.0;
      }

      performanceStats.put("avgResponseTime", avgResponseTime);
      performanceStats.put("messageProcessingSpeed", getMessageProcessingSpeed(dateKey));
      performanceStats.put("systemThroughput", getSystemThroughput());
      performanceStats.put("connectionStability", getConnectionStability(dateKey));

    } catch (Exception e) {
      log.error("[仪表盘] 获取性能统计数据失败", e);
    }

    return performanceStats;
  }

  /** 获取渠道统计数据 - 使用真实数据库查询 */
  private Map<String, Object> getChannelStats(String dateKey) {
    Map<String, Object> channelStats = new HashMap<>();

    try {
      // 从数据库查询
      LocalDate date = LocalDate.parse(dateKey, DATE_FORMATTER);
      List<Map<String, Object>> distribution = dashboardStatisticsMapper.selectChannelDistribution(date);
      
      Map<String, Long> channelData = new HashMap<>();
      if (distribution != null && !distribution.isEmpty()) {
        for (Map<String, Object> item : distribution) {
          String channel = (String) item.get("channel");
          Long count = getLongValue(item.get("messageCount"));
          if (channel != null) {
            channelData.put(channel, count);
          }
        }
      }

      channelStats.put("channels", channelData);
      log.debug("[仪表盘] 渠道统计: {}", channelData);

    } catch (Exception e) {
      log.error("[仪表盘] 获取渠道统计数据失败", e);
      channelStats.put("channels", new HashMap<>());
    }

    return channelStats;
  }

  /** 获取消息处理速度（消息/秒） */
  private double getMessageProcessingSpeed(String dateKey) {
    try {
      LocalDate date = LocalDate.parse(dateKey, DATE_FORMATTER);
      Map<String, Object> dbStats = dashboardStatisticsMapper.selectMessageStatsByDate(date);
      
      if (dbStats != null && !dbStats.isEmpty()) {
        long totalMessages = getLongValue(dbStats.get("totalMessages"));
        return totalMessages > 0 ? (double) totalMessages / 86400 : 0.0; // 86400秒/天
      }
    } catch (Exception e) {
      log.error("[仪表盘] 获取消息处理速度失败", e);
    }
    return 0.0;
  }

  /** 获取系统吞吐量（MB/s） - 暂时返回估算值 */
  private double getSystemThroughput() {
    // 这里可以集成系统监控数据
    // 暂时基于消息数估算
    try {
      LocalDate today = LocalDate.now();
      Map<String, Object> dbStats = dashboardStatisticsMapper.selectMessageStatsByDate(today);
      
      if (dbStats != null && !dbStats.isEmpty()) {
        long totalMessages = getLongValue(dbStats.get("totalMessages"));
        // 假设每条消息平均1KB，计算吞吐量
        double bytesPerSecond = (totalMessages * 1024.0) / 86400;
        return bytesPerSecond / (1024 * 1024); // 转换为MB/s
      }
    } catch (Exception e) {
      log.error("[仪表盘] 获取系统吞吐量失败", e);
    }
    return 0.0;
  }

  /** 获取连接稳定性（%） */
  private double getConnectionStability(String dateKey) {
    try {
      // 直接从数据库查询设备在线率
      long totalDevices = ioTDeviceMapper.countAllDevices();
      long onlineDevices = ioTDeviceMapper.countOnlineDevices();
      
      return totalDevices > 0 ? (double) onlineDevices / totalDevices * 100 : 0.0;
    } catch (Exception e) {
      log.error("[仪表盘] 获取连接稳定性失败", e);
    }
    return 0.0;
  }

  /** 获取设备趋势数据 - 暂时返回空，前端使用模拟数据 */
  private Map<String, Object> getDeviceTrendData(String dateKey) {
    Map<String, Object> data = new HashMap<>();
    data.put("title", "设备在线趋势");
    data.put("series", new Object[0]);
    return data;
  }

  /** 获取消息趋势数据 - 暂时返回空，前端使用模拟数据 */
  private Map<String, Object> getMessageTrendData(String dateKey, String channel) {
    Map<String, Object> data = new HashMap<>();
    data.put("title", "消息量趋势");
    data.put("series", new Object[0]);
    return data;
  }

  /** 获取渠道分布数据 - 暂时返回空，前端使用模拟数据 */
  private Map<String, Object> getChannelDistributionData(String dateKey) {
    Map<String, Object> data = new HashMap<>();
    data.put("title", "渠道消息分布");
    data.put("series", new Object[0]);
    return data;
  }

  /** 获取性能指标数据 - 暂时返回空，前端使用模拟数据 */
  private Map<String, Object> getPerformanceMetricsData(String dateKey) {
    Map<String, Object> data = new HashMap<>();
    data.put("title", "系统性能指标");
    data.put("series", new Object[0]);
    return data;
  }
  
  // ==================== 辅助方法 ====================
  
  /**
   * 安全地将Object转换为Long
   */
  private long getLongValue(Object value) {
    if (value == null) {
      return 0L;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      log.warn("[仪表盘] 转换Long失败: {}", value);
      return 0L;
    }
  }
  
  /**
   * 清除缓存
   */
  private void clearCache(LocalDate date) {
    try {
      String dateKey = date.format(DATE_FORMATTER);
      String pattern = CACHE_PREFIX + "*:" + dateKey;
      // 这里简化处理，实际应该使用scan命令
      redisTemplate.delete(CACHE_PREFIX + "message_stats:" + dateKey);
      redisTemplate.delete(CACHE_PREFIX + "channel_stats:" + dateKey);
      log.debug("[仪表盘] 清除缓存: {}", dateKey);
    } catch (Exception e) {
      log.error("[仪表盘] 清除缓存失败", e);
    }
  }

  // ==================== 新增前端仪表盘方法 ====================

  /**
   * 获取设备类型分布
   *
   * @param unionId 用户ID
   * @param isAdmin 是否管理员
   * @return 设备类型分布数据
   */
  public Map<String, Object> getDeviceTypeDistribution(String unionId, boolean isAdmin) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(unionId, isAdmin);
      List<Map<String, Object>> distribution =
          dashboardScopeSupport.getDeviceTypeDistribution(scope, unionId, isAdmin);
      
      result.put("data", distribution);
      result.put("success", true);
      
      log.debug("[仪表盘] 设备类型分布: {}", distribution);
      
    } catch (Exception e) {
      log.error("[仪表盘] 获取设备类型分布失败", e);
      result.put("success", false);
      result.put("message", "获取设备类型分布失败");
      result.put("data", new ArrayList<>());
    }
    
    return result;
  }

  /**
   * 获取设备在线趋势 - 使用真实数据，不添加随机波动
   * 注意：由于数据库没有历史在线数据，所有时间点使用当前实际在线数
   *
   * @param period 时间周期：24h、7d、30d
   * @param unionId 用户ID
   * @param isAdmin 是否管理员
   * @return 设备在线趋势数据
   */
  public Map<String, Object> getDeviceOnlineTrend(String period, String unionId, boolean isAdmin) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      List<String> labels = new ArrayList<>();
      List<Long> onlineData = new ArrayList<>();
      List<Long> offlineData = new ArrayList<>();
      
      int dataPoints;
      switch (period) {
        case "7d":
          dataPoints = 7;
          break;
        case "30d":
          dataPoints = 30;
          break;
        case "24h":
        default:
          dataPoints = 24;
          break;
      }
      
      // 查询当前实际在线数（只查询一次）
      DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(unionId, isAdmin);
      long totalDevices = dashboardScopeSupport.countDevices(scope, unionId, isAdmin);
      long onlineDevices = dashboardScopeSupport.countOnlineDevices(scope, unionId, isAdmin);
      long offlineDevices = totalDevices - onlineDevices;
      
      // 生成时间标签，所有时间点使用相同的实际在线数
      for (int i = dataPoints - 1; i >= 0; i--) {
        if ("24h".equals(period)) {
          LocalDateTime time = LocalDateTime.now().minusHours(i);
          labels.add(String.format("%02d:00", time.getHour()));
        } else {
          LocalDate date = LocalDate.now().minusDays(i);
          labels.add(String.format("%d/%d", date.getMonthValue(), date.getDayOfMonth()));
        }
        
        // 使用当前实际在线数，不添加随机波动
        onlineData.add(onlineDevices);
        offlineData.add(offlineDevices);
      }
      
      Map<String, Object> data = new HashMap<>();
      data.put("labels", labels);
      data.put("online", onlineData);
      data.put("offline", offlineData);
      
      result.put("data", data);
      result.put("success", true);
      
      log.debug("[仪表盘] 设备在线趋势: 总数={}, 在线={}, 离线={}", 
          totalDevices, onlineDevices, offlineDevices);
      
    } catch (Exception e) {
      log.error("[仪表盘] 获取设备在线趋势失败", e);
      result.put("success", false);
      result.put("message", "获取设备在线趋势失败");
    }
    
    return result;
  }

  /**
   * 获取产品接入方式分布
   *
   * @param unionId 用户ID
   * @param isAdmin 是否管理员
   * @return 产品接入方式分布数据
   */
  public Map<String, Object> getProductPlatformDistribution(String unionId, boolean isAdmin) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(unionId, isAdmin);
      List<Map<String, Object>> distribution =
          dashboardScopeSupport.getProductPlatformDistribution(scope, unionId, isAdmin);
      
      result.put("data", distribution);
      result.put("success", true);
      
      log.debug("[仪表盘] 产品接入方式分布: {}", distribution);
      
    } catch (Exception e) {
      log.error("[仪表盘] 获取产品接入方式分布失败", e);
      result.put("success", false);
      result.put("message", "获取产品接入方式分布失败");
      result.put("data", new ArrayList<>());
    }
    
    return result;
  }

  /**
   * 获取消息统计数据 - 使用真实数据库查询，不再使用模拟数据
   *
   * @param period 时间周期：24h、7d、30d
   * @return 消息统计数据
   */
  public Map<String, Object> getMessageStatistics(String period) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      // 尝试从缓存获取
      String cacheKey = CACHE_PREFIX + "message_statistics:" + period;
      String cachedValue = redisTemplate.opsForValue().get(cacheKey);
      
      if (cachedValue != null) {
        log.debug("[仪表盘] 从缓存获取消息统计数据: {}", period);
        // 简化处理，直接查询数据库
      }
      
      List<String> labels = new ArrayList<>();
      List<Long> totalData = new ArrayList<>();
      List<Long> successData = new ArrayList<>();
      
      int dataPoints;
      LocalDate endDate = LocalDate.now();
      LocalDate startDate;
      
      switch (period) {
        case "7d":
          dataPoints = 7;
          startDate = endDate.minusDays(6);
          break;
        case "30d":
          dataPoints = 30;
          startDate = endDate.minusDays(29);
          break;
        case "24h":
        default:
          // 24小时的数据，由于数据库没有按小时统计，我们按天显示并平均分配
          dataPoints = 24;
          startDate = endDate;
          break;
      }
      
      if ("24h".equals(period)) {
        // 24小时：获取今天的数据，平均分配到24小时
        // 注意：由于数据库没有按小时统计，这里将今天的总数平均分配到24小时
        Map<String, Object> todayStats = dashboardStatisticsMapper.selectMessageStatsByDate(endDate);
        
        long todayTotal = todayStats != null ? getLongValue(todayStats.get("totalMessages")) : 0L;
        long todaySuccess = todayStats != null ? getLongValue(todayStats.get("successMessages")) : 0L;
        
        // 平均分配到24小时（不添加随机波动）
        long hourTotal = todayTotal / 24;
        long hourSuccess = todaySuccess / 24;
        
        for (int i = 0; i < 24; i++) {
          labels.add(String.format("%02d:00", i));
          totalData.add(hourTotal);
          successData.add(hourSuccess);
        }
      } else {
        // 7天或30天：从数据库查询实际数据
        List<Map<String, Object>> dailyStats = 
            dashboardStatisticsMapper.selectDailyMessageStats(startDate, endDate);
        
        // 创建日期到数据的映射
        Map<LocalDate, Map<String, Object>> statsMap = new HashMap<>();
        if (dailyStats != null) {
          for (Map<String, Object> stat : dailyStats) {
            LocalDate date = (LocalDate) stat.get("statDate");
            statsMap.put(date, stat);
          }
        }
        
        // 生成完整的日期序列
        for (int i = 0; i < dataPoints; i++) {
          LocalDate date = startDate.plusDays(i);
          labels.add(String.format("%d/%d", date.getMonthValue(), date.getDayOfMonth()));
          
          Map<String, Object> stat = statsMap.get(date);
          if (stat != null) {
            totalData.add(getLongValue(stat.get("totalMessages")));
            successData.add(getLongValue(stat.get("successMessages")));
          } else {
            totalData.add(0L);
            successData.add(0L);
          }
        }
      }
      
      Map<String, Object> data = new HashMap<>();
      data.put("labels", labels);
      data.put("total", totalData);
      data.put("success", successData);
      
      result.put("data", data);
      result.put("success", true);
      
      // 缓存结果
      try {
        // 这里简化处理，实际应该序列化为JSON
        redisTemplate.opsForValue().set(cacheKey, "cached", CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
      } catch (Exception e) {
        log.warn("[仪表盘] 缓存消息统计数据失败", e);
      }
      
    } catch (Exception e) {
      log.error("[仪表盘] 获取消息统计数据失败", e);
      result.put("success", false);
      result.put("message", "获取消息统计数据失败");
    }
    
    return result;
  }

  /**
   * 获取消息渠道分布 - 使用真实数据库查询
   *
   * @return 消息渠道分布数据
   */
  public Map<String, Object> getMessageChannelDistribution() {
    Map<String, Object> result = new HashMap<>();
    
    try {
      LocalDate today = LocalDate.now();
      
      // 尝试从缓存获取
      String cacheKey = CACHE_PREFIX + "channel_distribution:" + today.format(DATE_FORMATTER);
      String cachedValue = redisTemplate.opsForValue().get(cacheKey);
      
      if (cachedValue != null) {
        log.debug("[仪表盘] 从缓存获取渠道分布数据");
        // 简化处理，直接查询数据库
      }
      
      // 从数据库查询实际数据
      List<Map<String, Object>> dbDistribution = dashboardStatisticsMapper.selectChannelDistribution(today);
      
      List<Map<String, Object>> distribution = new ArrayList<>();
      
      if (dbDistribution != null && !dbDistribution.isEmpty()) {
        // 使用实际数据
        for (Map<String, Object> item : dbDistribution) {
          String channel = (String) item.get("channel");
          Long count = getLongValue(item.get("messageCount"));
          
          if (channel != null && count > 0) {
            Map<String, Object> channelItem = new HashMap<>();
            channelItem.put("channel", channel);
            channelItem.put("count", count);
            distribution.add(channelItem);
          }
        }
      }
      
      // 如果没有数据，返回空列表而不是模拟数据
      result.put("data", distribution);
      result.put("success", true);
      
      log.debug("[仪表盘] 消息渠道分布: {}", distribution);
      
      // 缓存结果
      try {
        redisTemplate.opsForValue().set(cacheKey, "cached", CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
      } catch (Exception e) {
        log.warn("[仪表盘] 缓存渠道分布数据失败", e);
      }
      
    } catch (Exception e) {
      log.error("[仪表盘] 获取消息渠道分布失败", e);
      result.put("success", false);
      result.put("message", "获取消息渠道分布失败");
      result.put("data", new ArrayList<>());
    }
    
    return result;
  }

  /**
   * 获取协议分布统计
   *
   * @param unionId 用户ID
   * @param isAdmin 是否管理员
   * @return 协议分布统计数据
   */
  public Map<String, Object> getProtocolDistribution(String unionId, boolean isAdmin) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(unionId, isAdmin);
      List<Map<String, Object>> transportProtocol =
          dashboardScopeSupport.getTransportProtocolDistribution(scope, unionId, isAdmin);
      List<Map<String, Object>> messageProtocol =
          dashboardScopeSupport.getMessageProtocolDistribution(scope, unionId, isAdmin);
      
      Map<String, Object> data = new HashMap<>();
      data.put("transport", transportProtocol);
      data.put("message", messageProtocol);
      
      result.put("data", data);
      result.put("success", true);
      
      log.debug("[仪表盘] 协议分布统计 - 传输协议: {}, 消息协议: {}", transportProtocol, messageProtocol);
      
    } catch (Exception e) {
      log.error("[仪表盘] 获取协议分布统计失败", e);
      result.put("success", false);
      result.put("message", "获取协议分布统计失败");
      
      Map<String, Object> emptyData = new HashMap<>();
      emptyData.put("transport", new ArrayList<>());
      emptyData.put("message", new ArrayList<>());
      result.put("data", emptyData);
    }
    
    return result;
  }

  /**
   * 获取产品活跃度排行
   *
   * @param limit 返回数量
   * @param unionId 用户ID
   * @param isAdmin 是否管理员
   * @return 产品活跃度排行数据
   */
  public Map<String, Object> getProductRanking(Integer limit, String unionId, boolean isAdmin) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      DeptDataPermissionScope scope = dashboardScopeSupport.resolveScope(unionId, isAdmin);
      List<Map<String, Object>> ranking =
          dashboardScopeSupport.getProductRanking(scope, limit, unionId, isAdmin);
      
      // 计算活跃度评分和趋势
      for (Map<String, Object> product : ranking) {
        // 活跃度评分 = 在线率 * 0.4 + 消息活跃度 * 0.4 + 设备规模 * 0.2
        double onlineRate = ((Number) product.getOrDefault("onlineRate", 0)).doubleValue();
        long deviceCount = ((Number) product.getOrDefault("deviceCount", 0)).longValue();
        long todayMessages = ((Number) product.getOrDefault("todayMessages", 0)).longValue();
        
        double messageActivity = deviceCount > 0 ? (double) todayMessages / deviceCount : 0;
        double messageScore = Math.min(messageActivity / 500 * 100, 100) * 0.4;
        double scaleScore = Math.min(deviceCount / 1000.0 * 100, 100) * 0.2;
        
        int activityScore = (int) (onlineRate * 0.4 + messageScore + scaleScore);
        product.put("activityScore", activityScore);
        
        // 趋势（这里简化处理，实际应该对比历史数据）
        String trend = activityScore > 70 ? "up" : activityScore > 50 ? "stable" : "down";
        product.put("trend", trend);
      }
      
      result.put("data", ranking);
      result.put("success", true);
      
      log.debug("[仪表盘] 产品活跃度排行: {}", ranking);
      
    } catch (Exception e) {
      log.error("[仪表盘] 获取产品活跃度排行失败", e);
      result.put("success", false);
      result.put("message", "获取产品活跃度排行失败");
      result.put("data", new ArrayList<>());
    }
    
    return result;
  }
}
