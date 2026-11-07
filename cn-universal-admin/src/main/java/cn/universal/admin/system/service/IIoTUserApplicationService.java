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

import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.vo.IoTUserApplicationVO;
import java.util.List;

/**
 * 用户应用信息Service接口
 *
 * @since 2025-12-30
 */
public interface IIoTUserApplicationService {

  /** 启停用 */
  boolean enDisableIoTUser(String unionId, boolean isEnable);

  String selectApplicationName(String applicationId);

  /** 查询用户应用信息 */
  IoTUserApplication selectIotUserApplicationById(String appUniqueId);

  /** 查询用户应用信息列表 */
  List<IoTUserApplicationVO> selectIotUserApplicationList(
      IoTUserApplication iotUserApplication, IoTUser iotUser);

  /** 新增用户应用信息 */
  int insertIotAppUser(IoTUserApplication iotUserApplication);

  /**
   * 修改用户应用信息
   *
   * @param iotUserApplication 用户应用信息
   * @return 结果
   */
  int updateIotUserApplication(IoTUserApplication iotUserApplication);

  /**
   * 批量删除用户应用信息
   *
   * @param appUniqueId 需要删除的数据ID
   * @return 结果
   */
  int deleteIotUserApplicationByIds(String[] appUniqueId);

  /** 根据主键批量查询 */
  List<IoTUserApplication> selectIotUserApplicationByIds(String[] appUniqueId);

  /**
   * 删除用户应用信息信息
   *
   * @param appUniqueId 用户应用信息ID
   * @return 结果
   */
  int deleteIotUserApplicationById(String appUniqueId);

  List<IoTUserApplicationVO> selectApplicationList(IoTUserApplication application, IoTUser iotUser);

  boolean enableOrDisablePushCfg(String appUniqueId, String pushType, boolean isEnable);

  /**
   * 重置应用密钥并同步更新MQTT配置
   *
   * @param appUniqueId 应用唯一标识
   * @param unionId 用户唯一标识
   * @return 是否成功
   */
  boolean resetAppSecretAndSyncMqtt(String appUniqueId, String unionId);
}
