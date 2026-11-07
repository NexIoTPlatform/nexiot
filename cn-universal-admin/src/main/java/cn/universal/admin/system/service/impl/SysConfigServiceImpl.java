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

package cn.universal.admin.system.service.impl;

import cn.hutool.core.convert.Convert;
import cn.universal.admin.system.service.ISysConfigService;
import cn.universal.common.constant.Constants;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.StringUtils;
import cn.universal.persistence.entity.admin.SysConfig;
import cn.universal.persistence.mapper.admin.SysConfigMapper;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** 参数配置 服务层实现 @Author ruoyi */
@Service
public class SysConfigServiceImpl implements ISysConfigService {

  @Resource private SysConfigMapper configMapper;

  /** 项目启动时，初始化参数到缓存 */
  @PostConstruct
  public void init() {
    loadingConfigCache();
  }

  /**
   * 查询参数配置信息
   *
   * @param configId 参数配置ID
   * @return 参数配置信息
   */
  @Override
  public SysConfig selectConfigById(Long configId) {
    SysConfig config = new SysConfig();
    config.setConfigId(configId);
    return configMapper.selectConfig(config);
  }

  /**
   * 根据键名查询参数配置信息
   *
   * @param configKey 参数key
   * @return 参数键值
   */
  @Override
  @Cacheable(cacheNames = "selectConfigByKey", key = "#configKey", unless = "#result == null")
  public String selectConfigByKey(String configKey) {
    //        String configValue = Convert.toStr(redisCache.getCacheObject(getCacheKey(configKey)));
    //        if (StringUtils.isNotEmpty(configValue))
    //        {
    //            return configValue;
    //        }
    SysConfig config = new SysConfig();
    config.setConfigKey(configKey);
    SysConfig retConfig = configMapper.selectConfig(config);
    if (StringUtils.isNotNull(retConfig)) {
      //            redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
      return retConfig.getConfigValue();
    }
    return null;
  }

  /**
   * 获取验证码开关
   *
   * @return true开启，false关闭
   */
  @Override
  public boolean selectCaptchaOnOff() {
    String captchaOnOff = selectConfigByKey("sys.account.captchaOnOff");
    if (StringUtils.isEmpty(captchaOnOff)) {
      return true;
    }
    return Convert.toBool(captchaOnOff);
  }

  /**
   * 查询参数配置列表
   *
   * @param config 参数配置信息
   * @return 参数配置集合
   */
  @Override
  public List<SysConfig> selectConfigList(SysConfig config) {
    return configMapper.selectConfigList(config);
  }

  /**
   * 新增参数配置
   *
   * @param config 参数配置信息
   * @return 结果
   */
  @Override
  @CacheEvict(cacheNames = "selectConfigByKey", allEntries = true)
  public int insertConfig(SysConfig config) {
    int row = configMapper.insertConfig(config);
    //        if (row > 0)
    //        {
    //            redisCache.setCacheObject(getCacheKey(config.getConfigKey()),
    // config.getConfigValue());
    //        }
    return row;
  }

  /**
   * 修改参数配置
   *
   * @param config 参数配置信息
   * @return 结果
   */
  @Override
  @CacheEvict(cacheNames = "selectConfigByKey", allEntries = true)
  public int updateConfig(SysConfig config) {
    int row = configMapper.updateConfig(config);
    //        if (row > 0)
    //        {
    //            redisCache.setCacheObject(getCacheKey(config.getConfigKey()),
    // config.getConfigValue());
    //        }
    return row;
  }

  /**
   * 批量删除参数信息
   *
   * @param configIds 需要删除的参数ID
   * @return 结果
   */
  @Override
  @CacheEvict(cacheNames = "selectConfigByKey", allEntries = true)
  public void deleteConfigByIds(Long[] configIds) {
    for (Long configId : configIds) {
      SysConfig config = selectConfigById(configId);
      if (IoTUserConstants.YES.equals(config.getConfigType())) {
        throw new IoTException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
      }
      configMapper.deleteConfigById(configId);
      //            redisCache.deleteObject(getCacheKey(config.getConfigKey()));
    }
  }

  /** 加载参数缓存数据 */
  @Override
  public void loadingConfigCache() {
    //        List<SysConfig> configsList = configMapper.selectConfigList(new SysConfig());
    //        for (SysConfig config : configsList)
    //        {
    //            redisCache.setCacheObject(getCacheKey(config.getConfigKey()),
    // config.getConfigValue());
    //        }
  }

  /** 清空参数缓存数据 */
  @Override
  public void clearConfigCache() {
    //        Collection<String> keys = redisCache.keys(Constants.SYS_CONFIG_KEY + "*");
    //        redisCache.deleteObject(keys);
  }

  /** 重置参数缓存数据 */
  @Override
  @CacheEvict(cacheNames = "selectConfigByKey", allEntries = true)
  public void resetConfigCache() {
    clearConfigCache();
    loadingConfigCache();
  }

  /**
   * 校验参数键名是否唯一
   *
   * @param config 参数配置信息
   * @return 结果
   */
  @Override
  public String checkConfigKeyUnique(SysConfig config) {
    Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
    SysConfig info = configMapper.checkConfigKeyUnique(config.getConfigKey());
    if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue()) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }

  /**
   * 设置cache key
   *
   * @param configKey 参数键
   * @return 缓存键key
   */
  private String getCacheKey(String configKey) {
    return Constants.SYS_CONFIG_KEY + configKey;
  }
}
