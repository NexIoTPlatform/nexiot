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

package cn.universal.databridge.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.cache.annotation.MultiLevelCacheable;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.mapper.ResourceConnectionMapper;
import cn.universal.databridge.util.ConfigValidator;
import cn.universal.databridge.util.ConnectionTester;
import cn.universal.databridge.util.ConnectionTester.ConnectionTestResult;
import cn.universal.databridge.util.ResourceConnectionUtils;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资源连接服务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Service
@Slf4j
public class ResourceConnectionService {

  @Resource private ResourceConnectionMapper resourceConnectionMapper;

  @Resource private DataBridgeConfigService dataBridgeConfigService;

  @Resource private ConnectionTester connectionTester;

  /** 创建资源连接 */
  @Transactional(rollbackFor = Exception.class)
  public Long createResourceConnection(ResourceConnection connection) {
    // 1. 设置默认插件类型（如果未设置）
    ResourceConnectionUtils.setDefaultPluginTypeIfMissing(connection);

    // 2. 验证连接（使用ConfigValidator进行基本验证）
    ConfigValidator.validateResourceConnection(connection);

    // 3. 检查重名
    if (isNameExists(connection.getName(), null)) {
      throw new RuntimeException("资源连接名称已存在");
    }

    // 4. 设置默认值
    if (connection.getStatus() == null) {
      connection.setStatus(1); // 默认启用
    }
    connection.setCreateTime(LocalDateTime.now());
    connection.setUpdateTime(LocalDateTime.now());
    if (StrUtil.isNotBlank(connection.getExtraConfig())
        && JSONUtil.isTypeJSON(connection.getExtraConfig())) {
      JSONObject extraConfig = JSONUtil.parseObj(connection.getExtraConfig());
      connection.setHost(extraConfig.getStr("host", ""));
      connection.setPort(extraConfig.getInt("port", 0));
      connection.setUsername(extraConfig.getStr("username", ""));
      String password = extraConfig.getStr("password", "");
      connection.setPassword(password);
      connection.setDatabaseName(extraConfig.getStr("databaseName", ""));
    }
    // 4. 保存连接
    int result = resourceConnectionMapper.insertSelective(connection);
    if (result <= 0) {
      throw new RuntimeException("创建资源连接失败");
    }

    log.info("创建资源连接成功: {}", connection.getName());
    return connection.getId();
  }

  /** 更新资源连接 */
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(
      cacheNames = {"data_bridage_resource_connection"},
      allEntries = true)
  public void updateResourceConnection(ResourceConnection connection) {
    // 1. 验证连接（使用ConfigValidator进行基本验证）
    ConfigValidator.validateResourceConnection(connection);

    // 2. 检查重名
    if (isNameExists(connection.getName(), connection.getId())) {
      throw new RuntimeException("资源连接名称已存在");
    }

    // 3. 设置更新时间
    connection.setUpdateTime(LocalDateTime.now());
    if (StrUtil.isNotBlank(connection.getExtraConfig())
        && JSONUtil.isTypeJSON(connection.getExtraConfig())) {
      JSONObject extraConfig = JSONUtil.parseObj(connection.getExtraConfig());
      connection.setHost(extraConfig.getStr("host", ""));
      connection.setPort(extraConfig.getInt("port", 0));
      connection.setUsername(extraConfig.getStr("username", ""));
      String password = extraConfig.getStr("password", "");
      connection.setPassword(password);
      connection.setDatabaseName(extraConfig.getStr("databaseName", ""));
    }
    // 4. 更新连接
    int result = resourceConnectionMapper.updateByPrimaryKeySelective(connection);
    if (result <= 0) {
      throw new RuntimeException("更新资源连接失败");
    }

    log.info("更新资源连接成功: {}", connection.getName());
  }

  public ResourceConnection getById(Long id) {
    return resourceConnectionMapper.selectByPrimaryKey(id);
  }

  /** 根据ID获取连接 */
  @MultiLevelCacheable(
      cacheNames = "data_bridage_resource_connection",
      key = "#id",
      l1Expire = 900,
      l2Expire = 3600)
  public ResourceConnection getResouceForRunning(Long id) {
    return resourceConnectionMapper.selectByPrimaryKey(id);
  }

  /** 获取所有连接 */
  public List<ResourceConnection> getAllConnections() {
    return resourceConnectionMapper.selectAll();
  }

  /** 根据类型获取活跃连接 */
  public List<ResourceConnection> getActiveConnectionsByType(ResourceConnection.ResourceType type) {
    return resourceConnectionMapper.selectActiveConnectionsByType(type.name());
  }

  /** 根据方向获取资源连接列表 */
  public List<ResourceConnection> getConnectionsByDirection(
      ResourceConnection.Direction direction) {
    ResourceConnection condition = new ResourceConnection();
    condition.setDirection(direction);
    condition.setStatus(1); // 只查询启用的连接
    return resourceConnectionMapper.select(condition);
  }

  /** 更新连接状态 */
  @Transactional(rollbackFor = Exception.class)
  public void updateConnectionStatus(Long id, Integer status, String updateBy) {
    ResourceConnection connection = new ResourceConnection();
    connection.setId(id);
    connection.setStatus(status);
    connection.setUpdateBy(updateBy);
    connection.setUpdateTime(LocalDateTime.now());

    int result = resourceConnectionMapper.updateByPrimaryKeySelective(connection);
    if (result <= 0) {
      throw new RuntimeException("更新连接状态失败");
    }

    log.info("更新资源连接状态: ID={}, 状态={}", id, status);
  }

  /** 删除连接 */
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(
      cacheNames = {"data_bridage_resource_connection"},
      allEntries = true)
  public void deleteConnection(Long id) {
    // 检查是否被桥接配置使用
    List<DataBridgeConfig> configs = dataBridgeConfigService.getConfigsByTargetResourceId(id);
    if (!configs.isEmpty()) {
      StringBuilder configNames = new StringBuilder();
      for (DataBridgeConfig config : configs) {
        if (configNames.length() > 0) {
          configNames.append(", ");
        }
        configNames.append(config.getName());
      }
      throw new RuntimeException("无法删除资源连接，该连接正在被以下桥接配置使用: " + configNames.toString());
    }

    int result = resourceConnectionMapper.deleteByPrimaryKey(id);
    if (result <= 0) {
      throw new RuntimeException("删除资源连接失败");
    }

    log.info("删除资源连接: ID={}", id);
  }

  /** 测试连接 */
  public ConnectionTestResult testConnection(Long id) {
    try {
      ResourceConnection connection = getById(id);
      if (connection == null) {
        log.error("资源连接不存在，ID: {}", id);
        return ConnectionTestResult.failure("资源连接不存在");
      }

      ConnectionTester.ConnectionTestResult result = connectionTester.testConnection(connection);
      if (result.isSuccess()) {
        log.info("连接测试成功: {}", connection.getName());
        return result;
      } else {
        log.error("连接测试失败: {} - {}", connection.getName(), result.getMessage());
        return result;
      }
    } catch (Exception e) {
      log.error("测试连接异常: ID={} - {}", id, e.getMessage());
      return ConnectionTestResult.failure("测试连接失败");
    }
  }

  /** 检查名称是否存在 */
  private boolean isNameExists(String name, Long excludeId) {
    ResourceConnection existing = resourceConnectionMapper.selectByName(name, excludeId);
    return existing != null;
  }
}
