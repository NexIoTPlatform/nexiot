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

package cn.universal.dm.device.service.log;

import cn.hutool.json.JSONObject;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.log.writer.DeviceLogEntityWrite;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.util.Collections;
import java.util.List;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/23
 */
public interface IIoTDeviceLogService {

  /**
   * @return 存储策略
   */
  String getPolicy();

  /**
   * 保存日志消息
   *
   * @param upRequest 上行消息
   * @param ioTDeviceDTO 设备信息
   * @param ioTProduct 产品信息
   */
  void saveDeviceLog(BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct);

  /**
   * 保存日志消息，并在当前调用线程完成实际写入。
   *
   * <p>默认委托给原有异步入口，支持存量实现平滑迁移。高吞吐链路可以覆盖此方法，把并发控制交给上游有界管线。
   */
  default void saveDeviceLogDirect(
      BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    saveDeviceLog(upRequest, ioTDeviceDTO, ioTProduct);
  }

  /**
   * 批量刷写原始上行日志。
   *
   * <p>全局日志 writer 调用此入口完成实际写入。支持批量能力的存储实现应覆盖此方法；默认逐条 direct
   * 兼容存量实现。
   */
  default void flushDeviceLogRequests(List<DeviceLogWriteRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    for (DeviceLogWriteRequest request : requests) {
      if (request != null) {
        saveDeviceLogDirect(request.upRequest(), request.ioTDeviceDTO(), request.ioTProduct());
      }
    }
  }

  /**
   * 保存日志消息
   *
   * @param ioTDeviceLog 日志消息
   * @param ioTDeviceDTO 设备信息
   * @param ioTProduct 产品信息
   */
  default void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {}

  /**
   * 批量刷写已经构造好的日志实体。
   *
   * <p>全局日志 writer 调用此入口完成实际写入。支持批量能力的存储实现应覆盖此方法；默认逐条兼容存量实现。
   */
  default void flushDeviceLogEntities(List<DeviceLogEntityWrite> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    for (DeviceLogEntityWrite entity : entities) {
      if (entity != null) {
        saveDeviceLog(entity.ioTDeviceLog(), entity.ioTDeviceDTO(), entity.ioTProduct());
      }
    }
  }

  /**
   * 查询设备日志
   *
   * @param logQuery 查询条件
   * @return list
   */
  PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery);

  /** 日志详情 */
  IoTDeviceLogVO queryById(LogQuery logQuery);

  /**
   * 事件详情
   *
   * @param productKey 产品标志
   */
  PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId);

  /** 查询单属性或事件日志 */
  PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery);

  default PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query) {
    throw new IoTException("当前存储暂不支持全时段趋势，请通过数据记录查看原始数据");
  }

  /**
   * @return 存储配置信息
   */
  JSONObject configMetadata();

  /**
   * 查询最近的指令记录
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param commandId 指令ID
   * @param messageType 消息类型
   * @param createTime 创建时间
   * @param commandStatus 指令状态
   * @return 指令记录
   */
  default IoTDeviceLog getRecentCommand(
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    String iotId =
        productKey == null || productKey.isEmpty() || deviceId == null || deviceId.isEmpty()
            ? null
            : productKey + deviceId;
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .iotId(iotId)
            .productKey(productKey)
            .deviceId(deviceId)
            .commandId(commandId)
            .messageType(messageType)
            .startTime(createTime)
            .commandStatus(commandStatus)
            .limit(1)
            .build();

    List<IoTDeviceLog> logs = queryDeviceLogs(query);
    return logs.isEmpty() ? null : logs.get(0);
  }

  /**
   * 查询最近的指令记录
   *
   * @param iotId 产品Key
   * @param commandId 指令ID
   * @param messageType 消息类型
   * @param createTime 创建时间
   * @param commandStatus 指令状态
   * @return 指令记录
   */
  default IoTDeviceLog getRecentCommand(
      String iotId, String commandId, String messageType, Long createTime, Integer commandStatus) {
    // iotId = productKey + deviceId
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .iotId(iotId)
            .commandId(commandId)
            .messageType(messageType)
            .startTime(createTime)
            .commandStatus(commandStatus)
            .limit(1)
            .build();

    List<IoTDeviceLog> logs = queryDeviceLogs(query);
    return logs.isEmpty() ? null : logs.get(0);
  }

  /**
   * 通用设备日志查询接口
   *
   * @param query 查询条件
   * @return 符合条件的日志记录列表
   */
  default List<IoTDeviceLog> queryDeviceLogs(DeviceLogQuery query) {
    return Collections.emptyList();
  }
}
