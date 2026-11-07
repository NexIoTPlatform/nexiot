package cn.universal.web.controller;

import cn.universal.core.config.InstanceDiagnosticsRegistry;
import cn.universal.dm.device.dto.DashboardMapRequestDTO;
import cn.universal.dm.device.service.DashboardOperationsService;
import cn.universal.dm.device.service.DashboardService;
import cn.universal.dm.device.service.DashboardServiceV2;
import cn.universal.dm.device.service.DashboardStatisticsTask;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘控制器 根据仪表盘界面功能设计接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@Tag(name = "仪表盘管理", description = "提供设备统计、系统监控等仪表盘相关接口")
public class DashboardController extends BaseController {
  @Autowired private DashboardService dashboardService;

  @Autowired private DashboardServiceV2 dashboardServiceV2;

  @Autowired private DashboardStatisticsTask dashboardStatisticsTask;

  @Autowired private DashboardOperationsService operationsService;
  @Autowired private InstanceDiagnosticsRegistry instanceDiagnosticsRegistry;

  @GetMapping("/operations/statistics")
  @Operation(summary = "获取平台消息统计快照")
  public Object getOperationsStatistics(@RequestParam(defaultValue = "7") int days) {
    SecurityUtils.getUnionId();
    return operationsService.getStatistics(days);
  }

  @GetMapping("/operations/nodes")
  @Operation(summary = "获取运行节点及离线诊断快照")
  public Object getOperationsNodes() {
    SecurityUtils.getUnionId();
    return Map.of("nodes", instanceDiagnosticsRegistry.listNodes(), "timestamp", System.currentTimeMillis());
  }

  /**
   * 获取仪表盘首页数据 包含：设备总数、在线设备、今日消息、成功率等概览数据 对应图片中的4个顶部概览卡片
   *
   * @return 仪表盘首页数据
   */
  @GetMapping("/overview")
  @Operation(summary = "获取仪表盘首页数据", description = "包含设备总数、在线设备、今日消息、成功率等概览数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取仪表盘首页数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getDashboardOverview() {
    log.info("[仪表盘] 获取首页概览数据");
    // 获取当前用户信息
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();

    return dashboardServiceV2.getTodayOverview(unionId, isAdmin);
  }

  /**
   * 获取地图首页设备数据
   *
   * @param request 地图查询参数
   * @return 地图设备数据
   */
  @GetMapping("/map/devices")
  @Operation(summary = "获取地图首页设备数据", description = "支持预览模式与视野模式的设备坐标懒加载查询")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取地图首页设备数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getDashboardMapDevices(DashboardMapRequestDTO request) {
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();
    return dashboardServiceV2.getDashboardMapDevices(unionId, isAdmin, request);
  }

  /**
   * 获取设备消息分类图表数据 对应图片中的"设备消息分类"图表 支持：今日、本周、本月时间范围
   *
   * @param timeRange 时间范围：today(今日)、week(本周)、month(本月)
   * @return 设备消息分类图表数据
   */
  @GetMapping("/message-classification")
  @Operation(summary = "获取设备消息分类图表数据", description = "获取设备消息分类图表数据，支持今日、本周、本月时间范围")
  @Parameter(
      name = "timeRange",
      description = "时间范围：today(今日)、week(本周)、month(本月)",
      example = "today")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备消息分类图表数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> getMessageClassification(
      @RequestParam(value = "timeRange", defaultValue = "today") String timeRange) {
    log.info("[仪表盘] 获取设备消息分类数据，时间范围: {}", timeRange);
    LocalDate date = LocalDate.now(); // 消息统计默认返回管理员数据
    return dashboardService.getChartData(date, "message_classification", timeRange);
  }

  /**
   * 获取系统性能监控数据 对应图片中的"系统性能监控"部分 包含：CPU使用率、内存使用率、磁盘使用率、网络流量 支持：5秒、10秒、30秒刷新间隔
   *
   * @param refreshInterval 刷新间隔：5、10、30（秒）
   * @return 系统性能监控数据
   */
  @GetMapping("/system-monitor")
  @Operation(summary = "获取系统性能监控数据", description = "获取系统性能监控数据，包含CPU使用率、内存使用率、磁盘使用率、网络流量等")
  @Parameter(name = "refreshInterval", description = "刷新间隔：5、10、30（秒）", example = "5")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取系统性能监控数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getSystemMonitor(
      @RequestParam(value = "refreshInterval", defaultValue = "5") Integer refreshInterval) {
    log.info("[仪表盘] 获取系统性能监控数据，刷新间隔: {}秒", refreshInterval);
    return dashboardServiceV2.getSystemMonitorData();
  }

  /**
   * 获取实时统计数据 用于实时更新仪表盘数据
   *
   * @return 实时统计数据
   */
  @GetMapping("/realtime")
  @Operation(summary = "获取实时统计数据", description = "获取实时统计数据，用于实时更新仪表盘数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取实时统计数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getRealtimeData() {
    log.info("[仪表盘] 获取实时数据");

    // 获取当前用户信息
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();

    return dashboardServiceV2.getTodayOverview(unionId, isAdmin);
  }

  /**
   * 检查统计数据状态
   *
   * @return 统计数据状态
   */
  @GetMapping("/statistics-status")
  @Operation(summary = "检查统计数据状态", description = "检查统计数据状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取统计数据状态",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getStatisticsStatus() {
    log.info("[仪表盘] 获取统计数据状态");
    return dashboardServiceV2.getStatisticsManagerStatus();
  }

  /**
   * 强制刷新统计数据到Redis
   *
   * @return 刷新结果
   */
  @PostMapping("/flush-statistics")
  @Operation(summary = "强制刷新统计数据到Redis", description = "强制刷新统计数据到Redis")
  @ApiResponse(
      responseCode = "200",
      description = "成功刷新统计数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> flushStatistics() {
    log.info("[仪表盘] 强制刷新统计数据到Redis");
    dashboardServiceV2.forceFlushStatistics();
    return Map.of("success", true, "message", "统计数据已强制刷新到Redis");
  }

  /**
   * 手动刷新统计数据
   *
   * @return 刷新结果
   */
  @PostMapping("/statistics/refresh")
  @Operation(summary = "手动刷新统计数据", description = "手动刷新统计数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功刷新统计数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> refreshStatistics() {
    log.info("[仪表盘] 手动刷新统计数据");
    dashboardStatisticsTask.manualRefreshStatistics();
    return Map.of("success", true, "message", "统计数据刷新完成");
  }

  /**
   * 检查统计数据是否存在
   *
   * @param date 日期（可选，默认为今天）
   * @return 检查结果
   */
  @GetMapping("/statistics/check")
  @Operation(summary = "检查统计数据是否存在", description = "检查指定日期的统计数据是否存在")
  @Parameter(name = "date", description = "日期（可选，默认为今天）", example = "2025-01-01")
  @ApiResponse(
      responseCode = "200",
      description = "成功检查统计数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> checkStatistics(@RequestParam(required = false) String date) {
    LocalDate checkDate;
    if (date != null && !date.isEmpty()) {
      checkDate = LocalDate.parse(date);
    } else {
      checkDate = LocalDate.now();
    }

    log.info("[仪表盘] 检查统计数据是否存在，日期: {}", checkDate);
    boolean exists = dashboardStatisticsTask.checkStatisticsExists(checkDate);
    return Map.of("date", checkDate.toString(), "exists", exists);
  }

  /**
   * 清理重复的统计数据
   *
   * @param date 日期（可选，默认为今天）
   * @return 清理结果
   */
  @PostMapping("/statistics/cleanup")
  @Operation(summary = "清理重复的统计数据", description = "清理指定日期的重复统计数据")
  @Parameter(name = "date", description = "日期（可选，默认为今天）", example = "2025-01-01")
  @ApiResponse(
      responseCode = "200",
      description = "成功清理重复统计数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> cleanupDuplicateStatistics(
      @RequestParam(required = false) String date) {
    LocalDate cleanupDate;
    if (date != null && !date.isEmpty()) {
      cleanupDate = LocalDate.parse(date);
    } else {
      cleanupDate = LocalDate.now();
    }

    log.info("[仪表盘] 清理重复统计数据，日期: {}", cleanupDate);
    dashboardStatisticsTask.cleanupDuplicateStatistics(cleanupDate);
    return Map.of("success", true, "message", "重复统计数据清理完成", "date", cleanupDate.toString());
  }

  /**
   * 获取系统监控调试信息 用于排查系统监控问题
   *
   * @return 调试信息
   */
  @GetMapping("/system-debug")
  @Operation(summary = "获取系统监控调试信息", description = "获取系统监控调试信息，用于排查系统监控问题")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取系统监控调试信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> getSystemDebugInfo() {
    log.info("[仪表盘] 获取系统监控调试信息");
    return dashboardServiceV2.getSystemMonitorService().getDebugInfo();
  }

  /**
   * 获取图表数据 支持多种图表类型
   *
   * @param date 统计日期
   * @param type 图表类型：device_trend(设备趋势)、message_trend(消息趋势)、
   *     channel_distribution(渠道分布)、performance_metrics(性能指标)
   * @param channel 渠道（可选）
   * @return 图表数据
   */
  @GetMapping("/chart-data")
  @Operation(summary = "获取图表数据", description = "获取图表数据，支持多种图表类型")
  @Parameter(name = "date", description = "统计日期", example = "2025-01-01")
  @Parameter(
      name = "type",
      description =
          "图表类型：device_trend(设备趋势)、message_trend(消息趋势)、channel_distribution(渠道分布)、performance_metrics(性能指标)",
      required = true)
  @Parameter(name = "channel", description = "渠道（可选）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取图表数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getChartData(
      @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
          LocalDate date,
      @RequestParam("type") String type,
      @RequestParam(value = "channel", required = false) String channel) {

    if (date == null) {
      date = LocalDate.now();
    }

    log.info("[仪表盘] 获取图表数据，类型: {}, 日期: {}, 渠道: {}", type, date, channel);

    // 根据图表类型使用 DashboardServiceV2
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();
    
    switch (type) {
      case "device_trend":
        return dashboardServiceV2.getDeviceTrend("7d", unionId, isAdmin);
      case "message_trend":
        return dashboardServiceV2.getMessageTrend("7d");
      case "channel_distribution":
        return dashboardServiceV2.getChannelDistribution();
      default:
        return Map.of("error", "不支持的图表类型: " + type);
    }
  }

  // ==================== 新增前端仪表盘接口 ====================

  /**
   * 获取设备类型分布
   * 按设备节点类型统计设备数量
   *
   * @return 设备类型分布数据
   */
  @GetMapping("/device-type-distribution")
  @Operation(summary = "获取设备类型分布", description = "按设备节点类型统计设备数量")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备类型分布",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> getDeviceTypeDistribution() {
    log.info("[仪表盘] 获取设备类型分布");
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();
    return dashboardService.getDeviceTypeDistribution(unionId, isAdmin);
  }

  /**
   * 获取设备在线趋势
   * 支持7天、14天、30天三种时间维度
   *
   * @param period 时间周期：7d(默认)、14d、30d
   * @return 设备在线趋势数据
   */
  @GetMapping("/device-online-trend")
  @Operation(summary = "获取设备在线趋势", description = "获取设备在线趋势数据，支持7d、14d、30d三种时间维度")
  @Parameter(name = "period", description = "时间周期：7d(默认)、14d、30d", example = "7d")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备在线趋势",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getDeviceOnlineTrend(
      @RequestParam(value = "period", defaultValue = "7d") String period) {
    log.info("[仪表盘] 获取设备在线趋势，周期: {}", period);
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();
    return dashboardServiceV2.getDeviceTrend(period, unionId, isAdmin);
  }

  /**
   * 获取产品接入方式分布
   * 按第三方平台统计产品数量
   *
   * @return 产品接入方式分布数据
   */
  @GetMapping("/product-platform-distribution")
  @Operation(summary = "获取产品接入方式分布", description = "按第三方平台统计产品数量")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品接入方式分布",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> getProductPlatformDistribution() {
    log.info("[仪表盘] 获取产品接入方式分布");
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();
    return dashboardService.getProductPlatformDistribution(unionId, isAdmin);
  }

  /**
   * 获取消息统计数据
   * 支持7天、14天、30天三种时间维度
   *
   * @param period 时间周期：7d(默认)、14d、30d
   * @return 消息统计数据
   */
  @GetMapping("/message-statistics")
  @Operation(summary = "获取消息统计数据", description = "获取消息统计数据，支持7d、14d、30d三种时间维度")
  @Parameter(name = "period", description = "时间周期：7d(默认)、14d、30d", example = "7d")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取消息统计数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getMessageStatistics(
      @RequestParam(value = "period", defaultValue = "7d") String period) {
    log.info("[仪表盘] 获取消息统计数据，周期: {}", period);
    return dashboardServiceV2.getMessageTrend(period);
  }

  /**
   * 获取消息渠道分布
   * 按推送渠道统计消息数量
   *
   * @return 消息渠道分布数据
   */
  @GetMapping("/message-channel-distribution")
  @Operation(summary = "获取消息渠道分布", description = "按推送渠道统计消息数量")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取消息渠道分布",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Object getMessageChannelDistribution() {
    log.info("[仪表盘] 获取消息渠道分布");
    return dashboardServiceV2.getChannelDistribution();
  }

  /**
   * 获取协议分布统计
   * 包含传输协议和消息协议分布
   *
   * @return 协议分布统计数据
   */
  @GetMapping("/protocol-distribution")
  @Operation(summary = "获取协议分布统计", description = "获取传输协议和消息协议分布统计")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取协议分布统计",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> getProtocolDistribution() {
    log.info("[仪表盘] 获取协议分布统计");
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();
    return dashboardService.getProtocolDistribution(unionId, isAdmin);
  }

  /**
   * 获取产品活跃度排行
   * 返回TOP N活跃产品列表
   *
   * @param limit 返回数量，默认10
   * @return 产品活跃度排行数据
   */
  @GetMapping("/product-ranking")
  @Operation(summary = "获取产品活跃度排行", description = "获取产品活跃度TOP排行榜")
  @Parameter(name = "limit", description = "返回数量，默认10", example = "10")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品活跃度排行",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> getProductRanking(
      @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
    log.info("[仪表盘] 获取产品活跃度排行，限制: {}", limit);
    String unionId = SecurityUtils.getUnionId();
    boolean isAdmin = isAdmin();
    return dashboardService.getProductRanking(limit, unionId, isAdmin);
  }
}
