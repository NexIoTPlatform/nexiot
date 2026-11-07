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

import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.bo.IoTDeviceGroupBO;
import cn.universal.persistence.entity.vo.IoTDeviceGroupVO;
import cn.universal.persistence.query.AjaxResult;
import java.util.List;

/**
 * 设备分组Service接口
 *
 * @since 2025-12-30
 */
public interface IIoTDeviceGroupService {

  /**
   * 查询设备分组
   *
   * @param id 设备分组ID
   * @return 设备分组
   */
  IoTDeviceGroupVO selectDevGroupById(Long id);

  /**
   * 查询设备分组列表
   *
   * @return 设备分组集合
   */
  List<IoTDeviceGroupVO> selectDevGroupList();

  /**
   * 新增设备分组
   *
   * @param devGroup 设备分组
   * @return 结果
   */
  AjaxResult<Void> insertDevGroup(IoTDeviceGroupBO devGroup);

  /**
   * 修改设备分组
   *
   * @param devGroup 设备分组
   * @return 结果
   */
  int updateDevGroup(IoTDeviceGroupBO devGroup);

  /**
   * 删除设备分组信息
   *
   * @param id 设备分组ID
   * @return 结果
   */
  AjaxResult<Void> deleteDevGroupById(Long id);

  AjaxResult<Void> bindDevToGroup(IoTDeviceGroupBO devGroup);

  List<IoTDevice> selectDevInstanceListByGroupId(String groupId);

  AjaxResult<Void> unBindDevByGroupId(String groupId, String[] devId);
}
