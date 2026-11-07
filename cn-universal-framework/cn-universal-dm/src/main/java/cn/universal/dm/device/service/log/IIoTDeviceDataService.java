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

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/23
 */
public interface IIoTDeviceDataService {

  default Set<String> getEnabledStorePolicies() {
    return Collections.emptySet();
  }

  default boolean isStorePolicyEnabled(String storePolicy) {
    String normalized =
        storePolicy == null || storePolicy.trim().isEmpty()
            ? "none"
            : storePolicy.trim().toLowerCase(Locale.ROOT);
    return getEnabledStorePolicies().contains(normalized);
  }

  default Map<String, Object> metrics() {
    return Collections.emptyMap();
  }

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
   * <p>用于工业采集等已经具备有界队列和背压控制的入口，避免再进入全局异步执行器造成无界数据库并发。
   */
  default void saveDeviceLogDirect(
      BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    saveDeviceLog(upRequest, ioTDeviceDTO, ioTProduct);
  }

  default void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {}

  /**
   * 查询设备日志
   *
   * @param logQuery 查询条件
   * @return list
   */
  default PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery) {
    return null;
  }

  /** 日志详情 */
  default IoTDeviceLogVO queryById(LogQuery logQuery) {
    return null;
  }

  /**
   * 事件详情
   *
   * @param productKey 产品标志
   */
  default PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    return null;
  }

  default PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    return null;
  }

  PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query);

  /**
   * 查询最近的指令记录
   *
   * @param product 产品信息
   * @param iotId 设备唯一标识
   * @param commandId 指令ID
   * @param messageType 消息类型
   * @param createTime 创建时间（秒级时间戳）
   * @param commandStatus 指令状态
   * @return 设备日志记录
   */
  default IoTDeviceLog getRecentCommand(
      IoTProduct product,
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    return null;
  }

  /**
   * 查询最近的指令记录
   *
   * @param product 产品信息
   * @param iotId 设备唯一标识
   * @param commandId 指令ID
   * @param messageType 消息类型
   * @param createTime 创建时间（秒级时间戳）
   * @param commandStatus 指令状态
   * @return 设备日志记录
   */
  default IoTDeviceLog getRecentCommand(
      IoTProduct product,
      String iotId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    return null;
  }
}
