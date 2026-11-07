package cn.universal.dm.device.service;

import cn.universal.dm.device.dto.*;
import cn.universal.dm.device.service.push.processor.PushStatisticsProcessor;
import cn.universal.persistence.entity.IoTDashboardStatistics;
import cn.universal.persistence.entity.vo.DashboardMapDeviceVO;
import cn.universal.persistence.mapper.IoTDashboardStatisticsMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 仪表盘服务 - 重构版本，使用DTO和真实数据库查询
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Slf4j
@Service
public class DashboardServiceV2 {

  @Autowired private IoTDashboardStatisticsMapper dashboardStatisticsMapper;
  @Autowired private StringRedisTemplate redisTemplate;
  @Autowired private SystemMonitorService systemMonitorService;
  @Autowired private PushStatisticsProcessor pushStatisticsProcessor;
  @Autowired private IoTDeviceMapper ioTDeviceMapper;
  @Autowired private DashboardScopeSupport dashboardScopeSupport;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String CACHE_PREFIX = "dashboard:cache:";
  private static final long CACHE_EXPIRE_SECONDS = 300; // 5分钟

  // ==================== 公共方法 ====================

  /**
   * 获取今日概览数据（带用户权限过滤）
   */
  @Cacheable(
      cacheNames = "dashboard:overview",
      key = "'overview:' + #unionId + ':' + #isAdmin",
      unless = "#result == null")
  public DashboardOverviewDTO getTodayOverview(String unionId, boolean isAdmin) {
    try {
      LocalDate today = LocalDate.now();

      DeviceStatsDTO deviceStats = getDeviceStats(unionId, isAdmin);
      ApplicationStatsDTO applicationStats = getApplicationStats(unionId, isAdmin);
      PerformanceStatsDTO performanceStats = getPerformanceStats(today);
      ChannelStatsDTO channelStats = getChannelStats(today);
      ProductStatsDTO productStats = getProductStats(unionId, isAdmin);

      return DashboardOverviewDTO.builder()
          .deviceStats(deviceStats)
          .applicationStats(applicationStats)
          .performanceStats(performanceStats)
          .channelStats(channelStats)
          .productStats(productStats)
          .timestamp(System.currentTimeMillis())
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取今日概览数据失败", e);
      return DashboardOverviewDTO.builder()
          .timestamp(System.currentTimeMillis())
          .build();
    }
  }

  /**
   * 获取设备统计数据
   */
  @Cacheable(
      cacheNames = "dashboard:deviceStats",
      key = "'deviceStats:' + #unionId + ':' + #isAdmin",
      unless = "#result == null")
  public DeviceStatsDTO getDeviceStats(String unionId, boolean isAdmin) {
    try {
      DeptDataPermissionScope scope = resolveDashboardScope(unionId, isAdmin);
      long totalDevices = dashboardScopeSupport.countDevices(scope, unionId, isAdmin);
      long onlineDevices = dashboardScopeSupport.countOnlineDevices(scope, unionId, isAdmin);

      long offlineDevices = totalDevices - onlineDevices;
      double onlineRate = totalDevices > 0
          ? Math.round((double) onlineDevices / totalDevices * 100 * 100.0) / 100.0
          : 0.0;

      log.debug("[仪表盘] 设备统计: 总数={}, 在线={}, 离线={}, 在线率={}%",
          totalDevices, onlineDevices, offlineDevices, onlineRate);

      return DeviceStatsDTO.builder()
          .totalDevices(totalDevices)
          .onlineDevices(onlineDevices)
          .offlineDevices(offlineDevices)
          .onlineRate(onlineRate)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取设备统计数据失败", e);
      return DeviceStatsDTO.builder()
          .totalDevices(0L)
          .onlineDevices(0L)
          .offlineDevices(0L)
          .onlineRate(0.0)
          .build();
    }
  }

  /**
   * 获取应用统计数据
   */
  @Cacheable(
      cacheNames = "dashboard:applicationStats",
      key = "'appStats:' + #unionId + ':' + #isAdmin",
      unless = "#result == null")
  public ApplicationStatsDTO getApplicationStats(String unionId, boolean isAdmin) {
    try {
      DeptDataPermissionScope scope = resolveDashboardScope(unionId, isAdmin);
      long totalApplications = dashboardScopeSupport.countApplications(scope, unionId, isAdmin);

      log.debug("[仪表盘] 应用统计: 总数={}, 用户={}, 管理员={}",
          totalApplications, unionId, isAdmin);

      return ApplicationStatsDTO.builder()
          .totalApplications(totalApplications)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取应用统计数据失败", e);
      return ApplicationStatsDTO.builder()
          .totalApplications(0L)
          .build();
    }
  }

  /**
   * 获取地图摘要数据
   */
  @Cacheable(
      cacheNames = "dashboard:mapSummary",
      key = "'mapSummary:' + #unionId + ':' + #isAdmin",
      unless = "#result == null")
  public DashboardMapSummaryDTO getDashboardMapSummary(String unionId, boolean isAdmin) {
    try {
      DeptDataPermissionScope scope = resolveDashboardScope(unionId, isAdmin);
      DeviceStatsDTO deviceStats = getDeviceStats(unionId, isAdmin);
      long locatedDevices =
          ioTDeviceMapper.countDashboardLocatedDevices(
              dashboardScopeSupport.scopeTenantId(scope),
              dashboardScopeSupport.scopeDeptIds(scope),
              dashboardScopeSupport.scopeUnionIds(scope),
              dashboardScopeSupport.scopeDenied(scope, isAdmin));
      long onlineLocatedDevices =
          ioTDeviceMapper.countDashboardOnlineLocatedDevices(
              dashboardScopeSupport.scopeTenantId(scope),
              dashboardScopeSupport.scopeDeptIds(scope),
              dashboardScopeSupport.scopeUnionIds(scope),
              dashboardScopeSupport.scopeDenied(scope, isAdmin));

      return DashboardMapSummaryDTO.builder()
          .totalDevices(deviceStats.getTotalDevices())
          .onlineDevices(deviceStats.getOnlineDevices())
          .locatedDevices(locatedDevices)
          .onlineLocatedDevices(onlineLocatedDevices)
          .unlocatedDevices(Math.max(deviceStats.getTotalDevices() - locatedDevices, 0L))
          .build();
    } catch (Exception e) {
      log.error("[仪表盘] 获取地图摘要失败", e);
      return DashboardMapSummaryDTO.builder()
          .totalDevices(0L)
          .onlineDevices(0L)
          .locatedDevices(0L)
          .onlineLocatedDevices(0L)
          .unlocatedDevices(0L)
          .matchedDevices(0L)
          .build();
    }
  }

  /**
   * 获取地图设备数据
   */
  public DashboardMapResponseDTO getDashboardMapDevices(
      String unionId, boolean isAdmin, DashboardMapRequestDTO request) {
    DashboardMapRequestDTO safeRequest =
        request == null ? new DashboardMapRequestDTO() : request;

    DeptDataPermissionScope scope = resolveDashboardScope(unionId, isAdmin);
    String keyword = StringUtils.trimToNull(safeRequest.getKeyword());
    String productKey = StringUtils.trimToNull(safeRequest.getProductKey());
    Integer state =
        safeRequest.getState() != null && (safeRequest.getState() == 0 || safeRequest.getState() == 1)
            ? safeRequest.getState()
            : null;
    boolean previewMode = Boolean.TRUE.equals(safeRequest.getPreview()) || !safeRequest.hasBounds();
    int limit = safeRequest.resolveLimit();

    try {
      Double westLng = previewMode ? null : safeRequest.getWestLng();
      Double southLat = previewMode ? null : safeRequest.getSouthLat();
      Double eastLng = previewMode ? null : safeRequest.getEastLng();
      Double northLat = previewMode ? null : safeRequest.getNorthLat();

      List<DashboardMapDeviceVO> rows =
          ioTDeviceMapper.selectDashboardMapDevices(
              dashboardScopeSupport.scopeTenantId(scope),
              dashboardScopeSupport.scopeDeptIds(scope),
              dashboardScopeSupport.scopeUnionIds(scope),
              dashboardScopeSupport.scopeDenied(scope, isAdmin),
              keyword,
              productKey,
              state,
              westLng,
              southLat,
              eastLng,
              northLat,
              limit);
      long matchedDevices =
          ioTDeviceMapper.countDashboardMapDevices(
              dashboardScopeSupport.scopeTenantId(scope),
              dashboardScopeSupport.scopeDeptIds(scope),
              dashboardScopeSupport.scopeUnionIds(scope),
              dashboardScopeSupport.scopeDenied(scope, isAdmin),
              keyword,
              productKey,
              state,
              westLng,
              southLat,
              eastLng,
              northLat);

      List<DashboardMapDeviceDTO> list = new ArrayList<>();
      for (DashboardMapDeviceVO row : rows) {
        list.add(
            DashboardMapDeviceDTO.builder()
                .id(row.getId())
                .iotId(row.getIotId())
                .deviceId(row.getDeviceId())
                .deviceName(row.getDeviceName())
                .productKey(row.getProductKey())
                .productName(row.getProductName())
                .state(row.getState())
                .onlineTime(row.getOnlineTime())
                .coordinate(row.getCoordinate())
                .longitude(row.getLongitude())
                .latitude(row.getLatitude())
                .thirdPlatform(row.getThirdPlatform())
                .deviceNode(row.getDeviceNode())
                .build());
      }

      DashboardMapSummaryDTO summary =
          getDashboardMapSummary(unionId, isAdmin).toBuilder().matchedDevices(matchedDevices).build();

      return DashboardMapResponseDTO.builder()
          .previewMode(previewMode)
          .hasMore(matchedDevices > list.size())
          .limit(limit)
          .list(list)
          .summary(summary)
          .timestamp(System.currentTimeMillis())
          .build();
    } catch (Exception e) {
      log.error(
          "[仪表盘] 获取地图设备失败, unionId={}, isAdmin={}, request={}",
          unionId,
          isAdmin,
          safeRequest,
          e);
      return DashboardMapResponseDTO.builder()
          .previewMode(previewMode)
          .hasMore(false)
          .limit(limit)
          .list(new ArrayList<>())
          .summary(getDashboardMapSummary(unionId, isAdmin).toBuilder().matchedDevices(0L).build())
          .timestamp(System.currentTimeMillis())
          .build();
    }
  }

  /**
   * 获取消息统计数据
   */
  public MessageStatsDTO getMessageStats(LocalDate date, String unionId, boolean isAdmin) {
    try {
      // 目前只支持管理员查看全局消息统计
      if (!isAdmin) {
        return MessageStatsDTO.builder()
            .totalMessages(0L)
            .successMessages(0L)
            .failedMessages(0L)
            .pushMessages(0L)
            .retryMessages(0L)
            .successRate(0.0)
            .avgPerHour(0.0)
            .build();
      }

      Map<String, Object> dbStats = dashboardStatisticsMapper.selectMessageStatsByDate(date);

      if (dbStats != null && !dbStats.isEmpty()) {
        long totalMessages = getLongValue(dbStats.get("totalMessages"));
        long successMessages = getLongValue(dbStats.get("successMessages"));
        long failedMessages = getLongValue(dbStats.get("failedMessages"));
        long pushMessages = getLongValue(dbStats.get("pushMessages"));
        long retryMessages = getLongValue(dbStats.get("retryMessages"));

        double successRate = totalMessages > 0
            ? Math.round((double) successMessages / totalMessages * 100 * 100.0) / 100.0
            : 0.0;
        double avgPerHour = totalMessages / 24.0;

        log.debug("[仪表盘] 消息统计: 总数={}, 成功={}, 失败={}",
            totalMessages, successMessages, failedMessages);

        return MessageStatsDTO.builder()
            .totalMessages(totalMessages)
            .successMessages(successMessages)
            .failedMessages(failedMessages)
            .pushMessages(pushMessages)
            .retryMessages(retryMessages)
            .successRate(successRate)
            .avgPerHour(avgPerHour)
            .build();
      }

      return MessageStatsDTO.builder()
          .totalMessages(0L)
          .successMessages(0L)
          .failedMessages(0L)
          .pushMessages(0L)
          .retryMessages(0L)
          .successRate(0.0)
          .avgPerHour(0.0)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取消息统计数据失败", e);
      return MessageStatsDTO.builder().build();
    }
  }

  /**
   * 获取产品统计数据
   */
  @Cacheable(
      cacheNames = "dashboard:productStats",
      key = "'productStats:' + #unionId + ':' + #isAdmin",
      unless = "#result == null")
  public ProductStatsDTO getProductStats(String unionId, boolean isAdmin) {
    try {
      DeptDataPermissionScope scope = resolveDashboardScope(unionId, isAdmin);
      long totalProducts = dashboardScopeSupport.countProducts(scope, unionId, isAdmin);
      long myProducts = dashboardScopeSupport.countSelfProducts(scope, unionId, isAdmin);
      long publicProducts = Math.max(totalProducts - myProducts, 0L);

      log.debug("[仪表盘] 产品统计: 总数={}, 我的={}, 公共={}",
          totalProducts, myProducts, publicProducts);

      return ProductStatsDTO.builder()
          .totalProducts(totalProducts)
          .myProducts(myProducts)
          .publicProducts(publicProducts)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取产品统计数据失败", e);
      return ProductStatsDTO.builder()
          .totalProducts(0L)
          .myProducts(0L)
          .publicProducts(0L)
          .build();
    }
  }

  /**
   * 获取性能统计数据
   */
  @Cacheable(
      cacheNames = "dashboard:performanceStats",
      key = "'performanceStats:' + #date",
      unless = "#result == null")
  public PerformanceStatsDTO getPerformanceStats(LocalDate date) {
    try {
      double avgResponseTime = 0.0; // 暂时没有响应时间统计
      double messageProcessingSpeed = calculateMessageProcessingSpeed(date);
      double systemThroughput = calculateSystemThroughput(date);
      double connectionStability = calculateConnectionStability();

      return PerformanceStatsDTO.builder()
          .avgResponseTime(avgResponseTime)
          .messageProcessingSpeed(messageProcessingSpeed)
          .systemThroughput(systemThroughput)
          .connectionStability(connectionStability)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取性能统计数据失败", e);
      return PerformanceStatsDTO.builder().build();
    }
  }

  /**
   * 获取渠道统计数据
   */
  @Cacheable(
      cacheNames = "dashboard:channelStats",
      key = "'channelStats:' + #date",
      unless = "#result == null")
  public ChannelStatsDTO getChannelStats(LocalDate date) {
    try {
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

      log.debug("[仪表盘] 渠道统计: {}", channelData);

      return ChannelStatsDTO.builder()
          .channels(channelData)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取渠道统计数据失败", e);
      return ChannelStatsDTO.builder()
          .channels(new HashMap<>())
          .build();
    }
  }

  /**
   * 获取消息趋势数据
   */
  @Cacheable(
      cacheNames = "dashboard:messageTrend",
      key = "'messageTrend:' + #period",
      unless = "#result == null")
  public MessageTrendDTO getMessageTrend(String period) {
    try {
      List<String> labels = new ArrayList<>();
      List<Long> totalData = new ArrayList<>();
      List<Long> successData = new ArrayList<>();

      LocalDate endDate = LocalDate.now();
      LocalDate startDate;
      int dataPoints;

      switch (period) {
        case "14d":
          dataPoints = 14;
          startDate = endDate.minusDays(13);
          break;
        case "30d":
          dataPoints = 30;
          startDate = endDate.minusDays(29);
          break;
        case "7d":
        default:
          dataPoints = 7;
          startDate = endDate.minusDays(6);
          break;
      }

      // 从数据库查询按渠道分组的实际数据
      List<Map<String, Object>> dailyStatsByChannel =
          dashboardStatisticsMapper.selectDailyMessageStatsByChannel(startDate, endDate);

      // 构建数据结构：日期 -> 渠道 -> 数据
      Map<LocalDate, Map<String, Long>> dateChannelMap = new HashMap<>();
      Map<String, List<Long>> channelDataMap = new LinkedHashMap<>();
      Set<String> allChannels = new LinkedHashSet<>();

      // 解析数据库查询结果
      if (dailyStatsByChannel != null) {
        for (Map<String, Object> stat : dailyStatsByChannel) {
          // 处理 java.sql.Date 到 java.time.LocalDate 的转换
          Object dateObj = stat.get("statDate");
          LocalDate date = null;
          if (dateObj instanceof java.sql.Date) {
            date = ((java.sql.Date) dateObj).toLocalDate();
          } else if (dateObj instanceof LocalDate) {
            date = (LocalDate) dateObj;
          }
          
          String channel = (String) stat.get("channel");
          Long totalMessages = getLongValue(stat.get("totalMessages"));

          if (channel != null && date != null) {
            allChannels.add(channel);
            dateChannelMap.computeIfAbsent(date, k -> new HashMap<>())
                .put(channel, totalMessages);
          }
        }
      }

      // 初始化每个渠道的数据列表
      for (String channel : allChannels) {
        channelDataMap.put(channel, new ArrayList<>());
      }

      // 生成完整的日期序列和数据
      for (int i = 0; i < dataPoints; i++) {
        LocalDate date = startDate.plusDays(i);
        labels.add(String.format("%d/%d", date.getMonthValue(), date.getDayOfMonth()));

        // 计算当天的总计（所有渠道相加）
        long dayTotal = 0L;
        Map<String, Long> channelMap = dateChannelMap.get(date);

        if (channelMap != null) {
          for (Long value : channelMap.values()) {
            dayTotal += value;
          }
        }

        totalData.add(dayTotal);
        successData.add(dayTotal); // 暂时使用total作为success，如需要可以单独统计

        // 填充每个渠道的数据
        for (String channel : allChannels) {
          Long channelValue = 0L;
          if (channelMap != null && channelMap.containsKey(channel)) {
            channelValue = channelMap.get(channel);
          }
          channelDataMap.get(channel).add(channelValue);
        }
      }

      log.debug("[仪表盘] 消息趋势数据: 周期={}, 渠道数={}, 数据点={}", 
          period, allChannels.size(), dataPoints);

      return MessageTrendDTO.builder()
          .labels(labels)
          .total(totalData)
          .success(successData)
          .channelData(channelDataMap)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取消息趋势数据失败", e);
      return MessageTrendDTO.builder()
          .labels(new ArrayList<>())
          .total(new ArrayList<>())
          .success(new ArrayList<>())
          .channelData(new HashMap<>())
          .build();
    }
  }

  /**
   * 获取渠道分布数据
   */
  @Cacheable(
      cacheNames = "dashboard:channelDistribution",
      key = "'channelDistribution'",
      unless = "#result == null")
  public List<ChannelDistributionDTO> getChannelDistribution() {
    try {
      LocalDate today = LocalDate.now();
      List<Map<String, Object>> dbDistribution = dashboardStatisticsMapper.selectChannelDistribution(today);

      List<ChannelDistributionDTO> distribution = new ArrayList<>();

      if (dbDistribution != null && !dbDistribution.isEmpty()) {
        for (Map<String, Object> item : dbDistribution) {
          String channel = (String) item.get("channel");
          Long count = getLongValue(item.get("messageCount"));

          if (channel != null && count > 0) {
            distribution.add(ChannelDistributionDTO.builder()
                .channel(channel)
                .count(count)
                .build());
          }
        }
      }

      log.debug("[仪表盘] 渠道分布: {}", distribution);
      return distribution;

    } catch (Exception e) {
      log.error("[仪表盘] 获取渠道分布失败", e);
      return new ArrayList<>();
    }
  }

  /**
   * 获取设备趋势数据 - 使用真实历史数据
   * 支持7天、14天、30天
   */
  @Cacheable(
      cacheNames = "dashboard:deviceTrend",
      key = "'deviceTrend:' + #period + ':' + #unionId + ':' + #isAdmin",
      unless = "#result == null")
  public DeviceTrendDTO getDeviceTrend(String period, String unionId, boolean isAdmin) {
    try {
      List<String> labels = new ArrayList<>();
      List<Long> onlineData = new ArrayList<>();
      List<Long> offlineData = new ArrayList<>();

      int days;
      switch (period) {
        case "14d":
          days = 14;
          break;
        case "30d":
          days = 30;
          break;
        case "7d":
        default:
          days = 7;
          break;
      }

      LocalDate endDate = LocalDate.now();
      LocalDate startDate = endDate.minusDays(days - 1);

      // 从数据库查询历史设备统计数据
      List<Map<String, Object>> dailyStats =
          dashboardStatisticsMapper.selectDailyDeviceStats(startDate, endDate);

      // 创建日期到数据的映射
      Map<LocalDate, Map<String, Object>> statsMap = new HashMap<>();
      if (dailyStats != null) {
        for (Map<String, Object> stat : dailyStats) {
          LocalDate date = (LocalDate) stat.get("statDate");
          statsMap.put(date, stat);
        }
      }

      // 获取当前实际设备数（用于没有历史数据的日期）
      DeptDataPermissionScope scope = resolveDashboardScope(unionId, isAdmin);
      long currentTotal = dashboardScopeSupport.countDevices(scope, unionId, isAdmin);
      long currentOnline = dashboardScopeSupport.countOnlineDevices(scope, unionId, isAdmin);
      long currentOffline = currentTotal - currentOnline;

      // 生成完整的日期序列
      for (int i = 0; i < days; i++) {
        LocalDate date = startDate.plusDays(i);
        labels.add(String.format("%d/%d", date.getMonthValue(), date.getDayOfMonth()));

        Map<String, Object> stat = statsMap.get(date);
        if (stat != null) {
          // 使用历史数据
          long total = getLongValue(stat.get("totalDevices"));
          long online = getLongValue(stat.get("onlineDevices"));
          long offline = total - online;

          onlineData.add(online);
          offlineData.add(offline);
        } else {
          // 没有历史数据，使用当前值
          onlineData.add(currentOnline);
          offlineData.add(currentOffline);
        }
      }

      log.debug("[仪表盘] 设备趋势: 周期={}, 数据点={}", period, days);

      return DeviceTrendDTO.builder()
          .labels(labels)
          .online(onlineData)
          .offline(offlineData)
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取设备趋势失败", e);
      return DeviceTrendDTO.builder()
          .labels(new ArrayList<>())
          .online(new ArrayList<>())
          .offline(new ArrayList<>())
          .build();
    }
  }

  // ==================== 私有辅助方法 ====================

  /**
   * 计算消息处理速度（消息/秒）
   */
  private double calculateMessageProcessingSpeed(LocalDate date) {
    try {
      Map<String, Object> dbStats = dashboardStatisticsMapper.selectMessageStatsByDate(date);

      if (dbStats != null && !dbStats.isEmpty()) {
        long totalMessages = getLongValue(dbStats.get("totalMessages"));
        return totalMessages > 0 ? (double) totalMessages / 86400 : 0.0; // 86400秒/天
      }
    } catch (Exception e) {
      log.error("[仪表盘] 计算消息处理速度失败", e);
    }
    return 0.0;
  }

  /**
   * 计算系统吞吐量（MB/s）
   */
  private double calculateSystemThroughput(LocalDate date) {
    try {
      Map<String, Object> dbStats = dashboardStatisticsMapper.selectMessageStatsByDate(date);

      if (dbStats != null && !dbStats.isEmpty()) {
        long totalMessages = getLongValue(dbStats.get("totalMessages"));
        // 假设每条消息平均1KB，计算吞吐量
        double bytesPerSecond = (totalMessages * 1024.0) / 86400;
        return bytesPerSecond / (1024 * 1024); // 转换为MB/s
      }
    } catch (Exception e) {
      log.error("[仪表盘] 计算系统吞吐量失败", e);
    }
    return 0.0;
  }

  /**
   * 计算连接稳定性（%）
   */
  private double calculateConnectionStability() {
    try {
      long totalDevices = ioTDeviceMapper.countAllDevices();
      long onlineDevices = ioTDeviceMapper.countOnlineDevices();

      return totalDevices > 0
          ? Math.round((double) onlineDevices / totalDevices * 100 * 100.0) / 100.0
          : 0.0;
    } catch (Exception e) {
      log.error("[仪表盘] 计算连接稳定性失败", e);
    }
    return 0.0;
  }

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
   * 安全地将Object转换为Double
   */
  private double getDoubleValue(Object value) {
    if (value == null) {
      return 0.0;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    try {
      return Double.parseDouble(value.toString());
    } catch (NumberFormatException e) {
      log.warn("[仪表盘] 转换Double失败: {}", value);
      return 0.0;
    }
  }

  /**
   * 安全地将Object转换为Integer
   */
  private int getIntegerValue(Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      log.warn("[仪表盘] 转换Integer失败: {}", value);
      return 0;
    }
  }

  /**
   * 清除缓存
   */
  private void clearCache(LocalDate date) {
    try {
      String dateKey = date.format(DATE_FORMATTER);
      redisTemplate.delete(CACHE_PREFIX + "message_stats:" + dateKey);
      redisTemplate.delete(CACHE_PREFIX + "channel_stats:" + dateKey);
      log.debug("[仪表盘] 清除缓存: {}", dateKey);
    } catch (Exception e) {
      log.error("[仪表盘] 清除缓存失败", e);
    }
  }

  // ==================== 其他方法（保持兼容） ====================

  /**
   * 记录统计数据
   */
  public void recordStatistics(IoTDashboardStatistics statistics) {
    try {
      IoTDashboardStatistics existing =
          dashboardStatisticsMapper.selectByUniqueKey(
              statistics.getStatDate(),
              statistics.getProductKey(),
              statistics.getChannel(),
              statistics.getMetricType());

      if (existing == null) {
        statistics.setCreateTime(LocalDateTime.now());
        statistics.setUpdateTime(LocalDateTime.now());
        dashboardStatisticsMapper.insert(statistics);
        log.debug("[仪表盘统计] 插入新记录: {}", statistics);
      } else {
        dashboardStatisticsMapper.updateByUniqueKey(
            statistics.getStatDate(),
            statistics.getProductKey(),
            statistics.getChannel(),
            statistics.getMetricType(),
            statistics.getMetricValue(),
            LocalDateTime.now());
        log.debug("[仪表盘统计] 更新现有记录: {}", statistics);
      }

      clearCache(statistics.getStatDate());

    } catch (Exception e) {
      log.error("[仪表盘统计] 记录统计数据失败: {}", statistics, e);
    }
  }

  /**
   * 获取系统监控数据
   */
  @Cacheable(
      cacheNames = "dashboard:systemMonitor",
      key = "'systemMonitor'",
      unless = "#result == null")
  public SystemMonitorDTO getSystemMonitorData() {
    try {
      Map<String, Object> systemMetrics = systemMonitorService.getSystemMetrics();
      Map<String, Object> systemInfo = systemMonitorService.getSystemInfo();

      // 转换系统指标
      SystemMonitorDTO.SystemMetricsDTO metricsDTO = SystemMonitorDTO.SystemMetricsDTO.builder()
          .cpuUsage(getDoubleValue(systemMetrics.get("cpuUsage")))
          .memoryUsage(getDoubleValue(systemMetrics.get("memoryUsage")))
          .diskUsage(getDoubleValue(systemMetrics.get("diskUsage")))
          .networkReceiveRate(getDoubleValue(systemMetrics.get("networkReceiveRate")))
          .networkSendRate(getDoubleValue(systemMetrics.get("networkSendRate")))
          .jvmHeapUsage(getDoubleValue(systemMetrics.get("jvmHeapUsage")))
          .jvmNonHeapUsage(getDoubleValue(systemMetrics.get("jvmNonHeapUsage")))
          .threadCount(getIntegerValue(systemMetrics.get("threadCount")))
          .build();

      // 转换系统信息
      SystemMonitorDTO.SystemInfoDTO infoDTO = SystemMonitorDTO.SystemInfoDTO.builder()
          .osName((String) systemInfo.get("osName"))
          .osVersion((String) systemInfo.get("osVersion"))
          .osArch((String) systemInfo.get("osArch"))
          .cpuCores(getIntegerValue(systemInfo.get("cpuCores")))
          .totalMemory(getLongValue(systemInfo.get("totalMemory")))
          .availableMemory(getLongValue(systemInfo.get("availableMemory")))
          .totalDiskSpace(getLongValue(systemInfo.get("totalDiskSpace")))
          .availableDiskSpace(getLongValue(systemInfo.get("availableDiskSpace")))
          .jvmVersion((String) systemInfo.get("jvmVersion"))
          .jvmVendor((String) systemInfo.get("jvmVendor"))
          .systemStartTime(getLongValue(systemInfo.get("systemStartTime")))
          .systemUptime(getLongValue(systemInfo.get("systemUptime")))
          .build();

      return SystemMonitorDTO.builder()
          .systemMetrics(metricsDTO)
          .systemInfo(infoDTO)
          .isLinuxSystem(systemMonitorService.isLinuxSystem())
          .timestamp(System.currentTimeMillis())
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取系统监控数据失败", e);
      return SystemMonitorDTO.builder()
          .timestamp(System.currentTimeMillis())
          .build();
    }
  }

  /**
   * 获取统计管理器状态
   */
  public StatisticsStatusDTO getStatisticsManagerStatus() {
    try {
      Map<String, Object> status = pushStatisticsProcessor.getManagerStatus();

      return StatisticsStatusDTO.builder()
          .running((Boolean) status.get("running"))
          .pendingMessages(getLongValue(status.get("pendingMessages")))
          .processedMessages(getLongValue(status.get("processedMessages")))
          .failedMessages(getLongValue(status.get("failedMessages")))
          .lastFlushTime(getLongValue(status.get("lastFlushTime")))
          .lastFlushTimeFormatted((String) status.get("lastFlushTimeFormatted"))
          .cacheSize(getIntegerValue(status.get("cacheSize")))
          .timestamp(System.currentTimeMillis())
          .build();

    } catch (Exception e) {
      log.error("[仪表盘] 获取统计管理器状态失败", e);
      return StatisticsStatusDTO.builder()
          .running(false)
          .timestamp(System.currentTimeMillis())
          .build();
    }
  }

  /**
   * 强制刷新统计到Redis
   */
  @CacheEvict(cacheNames = {"dashboard:overview", "dashboard:deviceStats", "dashboard:applicationStats", 
      "dashboard:productStats", "dashboard:messageTrend", "dashboard:performanceStats", 
      "dashboard:channelStats", "dashboard:channelDistribution", "dashboard:deviceTrend", 
      "dashboard:systemMonitor"}, allEntries = true)
  public void forceFlushStatistics() {
    pushStatisticsProcessor.forceFlushToRedis();
  }

  /**
   * 获取系统监控服务
   */
  public SystemMonitorService getSystemMonitorService() {
    return systemMonitorService;
  }

  private DeptDataPermissionScope resolveDashboardScope(String unionId, boolean isAdmin) {
    return dashboardScopeSupport.resolveScope(unionId, isAdmin);
  }
}
