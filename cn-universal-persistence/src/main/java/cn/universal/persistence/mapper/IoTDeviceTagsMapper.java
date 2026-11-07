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

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTDeviceTags;
import java.util.List;

public interface IoTDeviceTagsMapper extends BaseMapper<IoTDeviceTags> {

  /**
   * 根据分组id查询设备id集合
   *
   * @param groupId
   * @return
   */
  int selectDevIds(String groupId);

  int deleteByValueId(String groupId);

  IoTDeviceTags getOne(String iotId);

  List<String> selectDevGroupName(String iotId);
}
