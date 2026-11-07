/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 设备日志Controller */
@RestController
@RequestMapping("/admin/v2/device/log")
@Tag(name = "设备日志管理", description = "设备日志管理接口")
public class IoTDeviceLogV2Controller extends BaseController {

  @Autowired private IIoTDeviceDataService iIoTDeviceDataService;
  @Autowired private DeviceHistoryAccess historyAccess;

  @Value("${iot.export.max-size:20000}")
  private Integer maxExportSize;

  /** 查询设备日志列表 */
  @GetMapping("/list")
  public TableDataInfo<IoTDeviceLogVO> list(@Validated LogQuery logQuery) {
    requireHistoryAccess(logQuery);
    logQuery.setCursorMode(false);
    PageBean<IoTDeviceLogVO> devLogVoPageBean = iIoTDeviceDataService.pageList(logQuery);
    return getDataTable(
        devLogVoPageBean.getList(), Integer.parseInt(devLogVoPageBean.getTotalCount() + ""));
  }

  @GetMapping("/slice")
  public PageBean<IoTDeviceLogVO> logSlice(LogQuery query) {
    requireHistoryAccess(query);
    query.setCursorMode(true);
    return iIoTDeviceDataService.pageList(query);
  }

  @GetMapping("/meta/slice")
  public PageBean<IoTDeviceLogMetadataVO> metadataSlice(LogQuery query) {
    requireHistoryAccess(query);
    query.setCursorMode(true);
    return iIoTDeviceDataService.queryLogMeta(query);
  }

  /**
   * 属性消息
   *
   * @param logQuery 日志查询条件
   * @return 属性消息列表
   */
  @Operation(summary = "属性消息", description = "获取属性消息列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取属性消息列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageBean.class)))
  @GetMapping("/meta/list")
  public PageBean<IoTDeviceLogMetadataVO> logMeta(LogQuery logQuery) {
    requireHistoryAccess(logQuery);
    logQuery.setCursorMode(false);
    PageBean<IoTDeviceLogMetadataVO> devLogMetaVoPageBean =
        iIoTDeviceDataService.queryLogMeta(logQuery);
    return devLogMetaVoPageBean;
  }

  @Operation(summary = "属性趋势", description = "默认按时间窗口有界采样，返回样本统计及截断标记；samplingMode=exact时全量聚合，最多1000个时间段")
  @GetMapping("/meta/trend")
  public PropertyTrendVO propertyTrend(PropertyTrendQuery query) {
    requireHistoryAccess(query);
    return iIoTDeviceDataService.queryPropertyTrend(query);
  }

  /**
   * 获取设备事件的统计信息
   *
   * @param productKey 产品key
   * @param iotId 设备id
   * @return 设备事件统计信息
   */
  @Operation(summary = "获取设备事件的统计信息", description = "返回各事件最近发生时间和展示次数，达到100条显示99+，成功结果缓存30秒")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备事件统计信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "productKey", description = "产品key", required = true)
  @Parameter(name = "iotId", description = "设备id", required = true)
  @GetMapping("/event/total/{productKey}/{iotId}")
  public TableDataInfo<IoTDeviceEvents> getEventTotal(
      @PathVariable("productKey") String productKey, @PathVariable("iotId") String iotId) {
    requireHistoryAccess(LogQuery.builder().productKey(productKey).iotId(iotId).build());
    PageBean<IoTDeviceEvents> devEventsPageBean =
        iIoTDeviceDataService.queryEventTotal(productKey, iotId);
    return getDataTable(
        devEventsPageBean.getList(), Integer.parseInt(devEventsPageBean.getTotalCount() + ""));
  }

  /**
   * 导出设备元数据日志
   *
   * @param response HTTP响应
   * @param logQuery 日志查询条件
   */
  @Operation(
      summary = "导出设备元数据日志",
      description = "导出设备元数据日志到Excel文件，支持多数据源（MySQL/ClickHouse/IoTDB）")
  @ApiResponse(responseCode = "200", description = "成功导出设备元数据日志")
  @PostMapping("/meta/export")
  @Log(
      title = "导出设备元数据日志",
      businessType = BusinessType.EXPORT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = false)
  public void exportDeviceMetaLog(HttpServletResponse response, LogQuery logQuery) {
    requireHistoryAccess(logQuery);
    List<IoTDeviceLogMetadataVO> rows = exportRows(logQuery, iIoTDeviceDataService::queryLogMeta);
    new ExcelUtil<>(IoTDeviceLogMetadataVO.class).exportExcel(response, rows, "设备数据导出");
  }

  @PostMapping("/export")
  @Log(
      title = "导出设备日志",
      businessType = BusinessType.EXPORT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = false)
  public void exportDeviceLog(HttpServletResponse response, LogQuery query) {
    requireHistoryAccess(query);
    List<IoTDeviceLogVO> rows = exportRows(query, iIoTDeviceDataService::pageList);
    new ExcelUtil<>(IoTDeviceLogVO.class).exportExcel(response, rows, "设备日志导出");
  }

  private <T> List<T> exportRows(LogQuery query, Function<LogQuery, PageBean<T>> fetch) {
    query.setCursorMode(true);
    query.setCursor(null);
    query.setBeforeTime(null);
    query.setBeforeId(null);
    query.setPageNum(1);
    query.setPageSize(200);
    List<T> rows = new ArrayList<>();
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
    while (true) {
      if (System.nanoTime() >= deadline) throw new IoTException("导出查询超时，请缩小时间范围分段导出", 400);
      PageBean<T> page = fetch.apply(query);
      rows.addAll(page.getList());
      if (rows.size() > maxExportSize || (rows.size() == maxExportSize && page.isHasMore())) {
        throw new IoTException("单次最多导出" + maxExportSize + "条，请缩小时间范围分段导出", 400);
      }
      if (!page.isHasMore()) return rows;
      if (page.getNextCursor() == null || page.getNextCursor().equals(query.getCursor())) {
        throw new IoTException("导出分页位置异常，请稍后重试", 500);
      }
      query.setCursor(page.getNextCursor());
    }
  }

  protected String currentHistoryTenant() {
    return StrUtil.blankToDefault(
        SecurityUtils.getTenantIdQuietly(), resolveTenantId(currentTenantUser()));
  }

  protected boolean currentHistoryAdmin() {
    var user = currentTenantUser();
    return user != null && user.isAdmin();
  }

  private void requireHistoryAccess(LogQuery query) {
    historyAccess.requireAccess(currentHistoryTenant(), currentHistoryAdmin(), query);
  }
}
