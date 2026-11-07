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

package cn.universal.admin.system.service;

import cn.universal.admin.common.service.BaseService;
import cn.universal.common.domain.R;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import java.util.List;

/** 设备订阅 */
public interface IoTDeviceSubscribeService extends BaseService {

  List<IoTDeviceSubscribe> selectDevSubscribeList(IoTDeviceSubscribe sub);

  IoTDeviceSubscribe selectDevInstanceById(String id);

  R updateDevSubscribe(IoTDeviceSubscribe sub);

  R insertDevSubscribe(IoTDeviceSubscribe sub);

  R deleteDevSubscribe(String productKey, String iotId, Long[] ids);
}
