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

import cn.universal.persistence.entity.admin.SysConfig;
import java.util.List;

/** 参数配置服务层接口 */
public interface ISysConfigService {

  /**
   * 查询参数配置信息
   *
   * @param configId 参数配置ID
   * @return 参数配置信息
   */
  public SysConfig selectConfigById(Long configId);

  /**
   * 根据键名查询参数配置信息
   *
   * @param configKey 参数键名
   * @return 参数键值
   */
  public String selectConfigByKey(String configKey);

  /**
   * 获取验证码开关
   *
   * @return true开启，false关闭
   */
  public boolean selectCaptchaOnOff();

  /**
   * 查询参数配置列表
   *
   * @param config 参数配置信息
   * @return 参数配置集合
   */
  public List<SysConfig> selectConfigList(SysConfig config);

  /**
   * 新增参数配置
   *
   * @param config 参数配置信息
   * @return 结果
   */
  public int insertConfig(SysConfig config);

  /**
   * 修改参数配置
   *
   * @param config 参数配置信息
   * @return 结果
   */
  public int updateConfig(SysConfig config);

  /**
   * 批量删除参数信息
   *
   * @param configIds 需要删除的参数ID
   * @return 结果
   */
  public void deleteConfigByIds(Long[] configIds);

  /** 加载参数缓存数据 */
  public void loadingConfigCache();

  /** 清空参数缓存数据 */
  public void clearConfigCache();

  /** 重置参数缓存数据 */
  public void resetConfigCache();

  /**
   * 校验参数键名是否唯一
   *
   * @param config 参数信息
   * @return 结果
   */
  public String checkConfigKeyUnique(SysConfig config);
}
