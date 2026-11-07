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

package cn.universal.admin.platform.service;

import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.bo.IoTDeviceLogBO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import java.util.List;

/**
 * 设备日志Service接口
 *
 * @since 2025-12-30
 */
public interface IIoTDevLogService {

  /** 分页查询设备日志列表 */
  List<IoTDeviceLogVO> queryPageList(IoTDeviceLogBO bo);

  /** 根据日志id查询日志详情 */
  IoTDeviceLogVO queryById(LogQuery logQuery);

  /**
   * 获取设备事件的统计信息
   *
   * @param productId 产品Id
   * @param iotId 设备id
   */
  List<IoTDeviceEvents> queryEventTotal(String productId, String iotId);
}
