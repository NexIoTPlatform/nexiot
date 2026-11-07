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
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
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
 * 小程序API-日志接口 提供设备日志查询功能
 *
 * @author NexIoT
 * @since 2025/12/17
 */
@Slf4j
@RestController
@RequestMapping("/mp/api/log")
@Tag(name = "小程序-日志接口", description = "小程序API-日志接口，提供设备日志查询功能")
public class MpLogApiController {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Autowired private IoTDeviceService iotDeviceService;
  @Autowired private IIoTDeviceDataService deviceDataService;

  /**
   * 获取设备日志列表
   *
   * @param iotId 设备ID
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param messageType 消息类型
   * @param beginTime 开始时间
   * @param endTime 结束时间
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @return 设备日志列表
   */
  @GetMapping("/{iotId}")
  @Operation(summary = "获取设备日志列表", description = "根据设备ID获取设备日志列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备日志列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> getDeviceLog(
      @PathVariable @Parameter(description = "设备ID") String iotId,
      @RequestParam(required = false) @Parameter(description = "产品Key") String productKey,
      @RequestParam(required = false) @Parameter(description = "设备ID") String deviceId,
      @RequestParam(required = false) @Parameter(description = "消息类型") String messageType,
      @RequestParam(required = false) @Parameter(description = "开始时间") String beginTime,
      @RequestParam(required = false) @Parameter(description = "结束时间") String endTime,
      @RequestParam(defaultValue = "1") @Parameter(description = "页码") int pageNum,
      @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") int pageSize) {

    String appId = MpAppIdContextHolder.getAppId();
    log.info(
        "查询设备日志: appId={}, iotId={}, messageType={}, pageNum={}, pageSize={}",
        appId,
        iotId,
        messageType,
        pageNum,
        pageSize);

    Map<String, Object> result = new HashMap<>();
    result.put("code", 200);

    try {
      // 构建查询参数
      LogQuery logQuery =
          LogQuery.builder().iotId(iotId).productKey(productKey).deviceId(deviceId).build();

      // 设置消息类型
      if (messageType != null && !messageType.isEmpty() && !"ALL".equals(messageType)) {
        logQuery.setMessageType(messageType);
      }

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
      logQuery.setPageNum(pageNum);
      logQuery.setPageSize(pageSize);

      // 调用服务查询
      PageBean<IoTDeviceLogVO> pageBean = deviceDataService.pageList(logQuery);

      List<Map<String, Object>> rows = new ArrayList<>();
      if (pageBean != null && pageBean.getList() != null) {
        for (IoTDeviceLogVO vo : pageBean.getList()) {
          Map<String, Object> item = new HashMap<>();
          item.put("id", vo.getId());
          item.put("iotId", vo.getIotId());
          item.put("messageType", vo.getMessageType());
          item.put("createTime", vo.getCreateTime());
          item.put("content", vo.getContent());
          item.put("event", vo.getEvent());
          // 使用point作为功能标识
          item.put("function", vo.getPoint());
          rows.add(item);
        }
      }

      result.put("rows", rows);
      result.put("total", pageBean != null ? pageBean.getTotalCount() : 0);
    } catch (Exception e) {
      log.error("查询设备日志失败", e);
      result.put("rows", new ArrayList<>());
      result.put("total", 0);
    }

    return result;
  }

  /**
   * 获取属性历史数据
   *
   * @param iotId 设备ID
   * @param property 属性ID
   * @param beginTime 开始时间
   * @param endTime 结束时间
   * @return 属性历史数据
   */
  @GetMapping("/meta/{iotId}/{property}")
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
  @GetMapping("/meta/{iotId}")
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
