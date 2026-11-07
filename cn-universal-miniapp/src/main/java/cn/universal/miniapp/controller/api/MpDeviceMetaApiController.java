/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.api;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.miniapp.security.MpAppIdContextHolder;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序API-数据接口 提供历史数据查询功能
 *
 * @author NexIoT
 * @since 2025/12/17
 */
@Slf4j
@RestController
@RequestMapping("/mp/api/data")
@Tag(name = "小程序-数据接口", description = "小程序API-数据接口，提供历史数据查询功能")
public class MpDeviceMetaApiController {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Autowired private IIoTDeviceDataService deviceDataService;
  @Autowired private IoTDeviceService iotDeviceService;

  /**
   * 获取属性历史数据
   *
   * @param iotId 设备ID
   * @param property 属性ID
   * @param beginTime 开始时间
   * @param endTime 结束时间
   * @return 属性历史数据
   */
  @GetMapping("/history/{iotId}/{property}")
  @Operation(summary = "获取属性历史数据", description = "根据设备ID和属性ID获取历史数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取属性历史数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<List<Map<String, Object>>> getPropertyHistory(
      @PathVariable @Parameter(description = "设备ID") String iotId,
      @PathVariable @Parameter(description = "属性ID") String property,
      @RequestParam(required = false) @Parameter(description = "开始时间") String beginTime,
      @RequestParam(required = false) @Parameter(description = "结束时间") String endTime) {

    String appId = MpAppIdContextHolder.getAppId();
    log.info("查询属性历史数据: appId={}, iotId={}, property={}", appId, iotId, property);

    try {
      // 获取设备信息，以获取productKey
      IoTDeviceDTO deviceDTO = iotDeviceService.selectDevInstanceBO(iotId);
      if (deviceDTO == null) {
        log.warn("设备不存在: iotId={}", iotId);
        return AjaxResult.success(new ArrayList<>());
      }

      // 构建查询参数
      LogQuery logQuery =
          LogQuery.builder()
              .iotId(iotId)
              .productKey(deviceDTO.getProductKey())
              .deviceId(deviceDTO.getDeviceId())
              .property(property)
              .messageType(MessageType.PROPERTIES.name())
              .build();

      // 设置时间范围（转换为秒级Unix时间戳）
      if (beginTime != null && !beginTime.isEmpty()) {
        LocalDateTime begin = LocalDateTime.parse(beginTime, DATE_FORMATTER);
        logQuery.setBeginCreateTime(begin.toInstant(ZoneOffset.of("+8")).getEpochSecond());
      }
      if (endTime != null && !endTime.isEmpty()) {
        LocalDateTime end = LocalDateTime.parse(endTime, DATE_FORMATTER);
        logQuery.setEndCreateTime(end.toInstant(ZoneOffset.of("+8")).getEpochSecond());
      }

      // 设置分页
      logQuery.setPageNum(1);
      logQuery.setPageSize(500);

      // 调用服务查询
      PageBean<IoTDeviceLogMetadataVO> pageBean = deviceDataService.queryLogMeta(logQuery);

      List<Map<String, Object>> result = new ArrayList<>();
      if (pageBean != null && pageBean.getList() != null) {
        for (IoTDeviceLogMetadataVO vo : pageBean.getList()) {
          Map<String, Object> item = new HashMap<>();
          item.put("timestamp", vo.getCreateTime());
          // content字段包含属性值
          item.put("value", parseValue(vo.getContent()));
          item.put("property", vo.getProperty());
          result.add(item);
        }
      }

      return AjaxResult.success(result);
    } catch (Exception e) {
      log.error("查询属性历史数据失败", e);
      return AjaxResult.success(new ArrayList<>());
    }
  }

  /**
   * 获取设备历史数据（所有属性）
   *
   * @param iotId 设备ID
   * @param beginTime 开始时间
   * @param endTime 结束时间
   * @return 设备历史数据
   */
  @GetMapping("/history/{iotId}")
  @Operation(summary = "获取设备历史数据", description = "根据设备ID获取所有属性的历史数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备历史数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<List<Map<String, Object>>> getDeviceHistory(
      @PathVariable @Parameter(description = "设备ID") String iotId,
      @RequestParam(required = false) @Parameter(description = "开始时间") String beginTime,
      @RequestParam(required = false) @Parameter(description = "结束时间") String endTime) {

    String appId = MpAppIdContextHolder.getAppId();
    log.info("查询设备历史数据: appId={}, iotId={}", appId, iotId);

    try {
      // 获取设备信息，以获取productKey
      IoTDeviceDTO deviceDTO = iotDeviceService.selectDevInstanceBO(iotId);
      if (deviceDTO == null) {
        log.warn("设备不存在: iotId={}", iotId);
        return AjaxResult.success(new ArrayList<>());
      }

      LogQuery logQuery =
          LogQuery.builder()
              .iotId(iotId)
              .productKey(deviceDTO.getProductKey())
              .deviceId(deviceDTO.getDeviceId())
              .messageType(MessageType.PROPERTIES.name())
              .build();

      // 设置时间范围（转换为秒级Unix时间戳）
      if (beginTime != null && !beginTime.isEmpty()) {
        LocalDateTime begin = LocalDateTime.parse(beginTime, DATE_FORMATTER);
        logQuery.setBeginCreateTime(begin.toInstant(ZoneOffset.of("+8")).getEpochSecond());
      }
      if (endTime != null && !endTime.isEmpty()) {
        LocalDateTime end = LocalDateTime.parse(endTime, DATE_FORMATTER);
        logQuery.setEndCreateTime(end.toInstant(ZoneOffset.of("+8")).getEpochSecond());
      }

      logQuery.setPageNum(1);
      logQuery.setPageSize(100);

      PageBean<IoTDeviceLogMetadataVO> pageBean = deviceDataService.queryLogMeta(logQuery);

      List<Map<String, Object>> result = new ArrayList<>();
      if (pageBean != null && pageBean.getList() != null) {
        for (IoTDeviceLogMetadataVO vo : pageBean.getList()) {
          Map<String, Object> item = new HashMap<>();
          item.put("timestamp", vo.getCreateTime());
          item.put("value", parseValue(vo.getContent()));
          item.put("property", vo.getProperty());
          result.add(item);
        }
      }

      return AjaxResult.success(result);
    } catch (Exception e) {
      log.error("查询设备历史数据失败", e);
      return AjaxResult.success(new ArrayList<>());
    }
  }

  /** 解析属性值 */
  private Object parseValue(String content) {
    if (content == null || content.isEmpty()) {
      return null;
    }
    try {
      // 尝试解析为数字
      if (content.matches("-?\\d+(\\.\\d+)?")) {
        if (content.contains(".")) {
          return Double.parseDouble(content);
        }
        return Long.parseLong(content);
      }
      return content;
    } catch (Exception e) {
      return content;
    }
  }
}
