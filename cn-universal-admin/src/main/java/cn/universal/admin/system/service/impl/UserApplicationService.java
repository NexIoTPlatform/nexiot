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

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 用户APP鉴权服务类
 *
 * <p>提供IoT平台中用户应用程序的认证和授权功能，包括： - OAuth2客户端信息查询 - 用户应用关联关系管理 - 应用ID和UnionID的映射查询
 *
 * <p>所有查询方法都使用了Spring Cache缓存，提高查询性能
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/12 16:10
 */
@Component
public class UserApplicationService {

  @Resource private IoTUserApplicationMapper applicationMapper;

  @Resource private IoTUserMapper iotUserMapper;

  @Resource private OauthClientDetailsMapper oauthClientDetailsMapper;

  /**
   * 根据客户端ID查询OAuth2客户端详细信息
   *
   * <p>用于验证客户端身份，获取客户端的授权信息、重定向URI等配置 结果会被缓存，提高频繁查询的性能
   *
   * @param clientId OAuth2客户端ID，不能为空
   * @return OAuth2客户端详细信息，如果不存在返回null
   * @throws IoTException 当clientId为空时抛出业务异常
   */
  @Cacheable(cacheNames = "iot_oauth_client", key = "''+#clientId", unless = "#result == null")
  public OAuth2ClientDetails getOauthUnionId(String clientId) {
    if (StrUtil.isBlank(clientId)) {
      throw new IoTException("clientId can't be null");
    }
    OAuth2ClientDetails clientDetails = OAuth2ClientDetails.builder().clientId(clientId).build();
    clientDetails = oauthClientDetailsMapper.selectOne(clientDetails);
    if (clientDetails == null) {
      return null;
    }
    return clientDetails;
  }

  /**
   * 根据UnionID查询用户关联的所有应用程序
   *
   * <p>UnionID是用户在IoT平台中的唯一标识，一个用户可以关联多个应用 用于获取用户在所有应用中的权限和配置信息
   *
   * @param unionId 用户在IoT平台中的唯一标识，不能为空
   * @return 用户关联的应用程序列表
   * @throws IoTException 当unionId为空或查询失败时抛出业务异常
   */
  @Cacheable(cacheNames = "iot_unionId", key = "''+#unionId", unless = "#result == null")
  public List<IoTUserApplication> getUserAppByUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      throw new IoTException("platform application user unionId can't be null");
    }
    List<IoTUserApplication> applications =
        applicationMapper.selectIotUserApplicationByUnionId(unionId);
    if (applications == null) {
      throw new IoTException("inner error,please connect admin");
    }
    return applications;
  }

  /**
   * 根据应用ID查询应用详细信息（包含删除状态检查）
   *
   * <p>用于验证应用的有效性，只返回未删除的应用信息 如果应用不存在或已删除，会抛出业务异常
   *
   * @param appId 应用ID，不能为空
   * @return 应用详细信息
   * @throws IoTException 当appId为空或应用不存在时抛出业务异常
   */
  @Cacheable(cacheNames = "iot_appid", key = "''+#appId", unless = "#result == null")
  public IoTUserApplication getUserAppByAppid(String appId) {
    if (StrUtil.isBlank(appId)) {
      throw new IoTException("appId can't be null");
    }
    IoTUserApplication queryObject = IoTUserApplication.builder().appId(appId).deleted(0).build();
    IoTUserApplication iotUser = applicationMapper.selectOne(queryObject);

    if (iotUser == null) {
      throw new IoTException("inner error,please connect admin");
    }
    return iotUser;
  }

  /**
   * 根据应用ID查询应用详细信息（不包含删除状态检查）
   *
   * <p>与getUserAppByAppid的区别是不检查删除状态，返回所有应用信息 适用于需要查看已删除应用的场景
   *
   * @param appId 应用ID，不能为空
   * @return 应用详细信息，可能为null
   * @throws IoTException 当appId为空时抛出业务异常
   */
  @Cacheable(cacheNames = "iot_appid", key = "''+#appId", unless = "#result == null")
  public IoTUserApplication getIotUserByAppid(String appId) {
    if (StrUtil.isBlank(appId)) {
      throw new IoTException("appId can't be null");
    }
    IoTUserApplication queryObject = IoTUserApplication.builder().appId(appId).deleted(0).build();
    IoTUserApplication iotUser = applicationMapper.selectOne(queryObject);
    return iotUser;
  }
}
