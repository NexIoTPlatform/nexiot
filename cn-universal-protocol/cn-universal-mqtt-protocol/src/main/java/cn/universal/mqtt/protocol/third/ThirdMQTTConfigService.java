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

package cn.universal.mqtt.protocol.third;

import cn.hutool.core.util.StrUtil;
import cn.universal.cache.annotation.MultiLevelCacheable;
import cn.universal.cache.strategy.CacheStrategy;
import cn.universal.common.enums.NetworkType;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.mqtt.protocol.system.SysMQTTStatusProvider;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/**
 * MQTT配置服务
 *
 * <p>业务聚合入口，聚合Loader和Parser，统一对外暴露加载、统计、校验等接口。 不包含任何解析细节，全部委托给MqttConfigParser。 @Author
 * gitee.com/NexIoT
 *
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Service
public class ThirdMQTTConfigService implements ThirdMQTTConfigChecker {

  @Autowired private NetworkMapper networkMapper;
  @Resource private IoTProductMapper ioTProductMapper;
  @Autowired private ThirdMQTTConfigLoader thirdMQTTConfigLoader;

  @Autowired private SysMQTTStatusProvider sysMQTTStatusProvider;

  @Autowired private ThirdMQTTConfigParser thirdMQTTConfigParser;

  /** 加载所有MQTT配置（CLIENT+SERVER） */
  public Map<String, MQTTProductConfig> loadAllConfigs() {
    Map<String, MQTTProductConfig> allConfigs = new HashMap<>();
    // 1. 加载MQTT_CLIENT
    List<Network> clientNetworks = thirdMQTTConfigLoader.loadAllMqttClient();
    for (Network n : clientNetworks) {
      MQTTProductConfig config = thirdMQTTConfigParser.parse(n);
      if (config != null && config.isValid()) {
        allConfigs.put(config.getNetworkUnionId(), config);
      }
    }
    // 2. 加载MQTT_SERVER
    List<Network> serverNetworks = thirdMQTTConfigLoader.loadAllMqttServer();
    for (Network n : serverNetworks) {
      MQTTProductConfig config = thirdMQTTConfigParser.parse(n);
      if (config != null && config.isValid()) {
        allConfigs.put(config.getNetworkUnionId(), config);
      }
    }
    log.info(
        "[MqttConfig] 配置加载完成 CLIENT:{} SERVER:{} 总计:{}",
        clientNetworks.size(),
        serverNetworks.size(),
        allConfigs.size());
    return allConfigs;
  }

  /** 根据networkUnionId获取单个配置 */
  public MQTTProductConfig getConfig(String networkUnionId) {
    if (StrUtil.isBlank(networkUnionId)) {
      return null;
    }
    try {
      List<Network> networks = networkMapper.selectByUnionId(networkUnionId);
      if (networks == null || networks.isEmpty()) {
        return null;
      }
      // 取第一个（通常unionId是唯一的）
      Network network = networks.get(0);
      return thirdMQTTConfigParser.parse(network, false);
    } catch (Exception e) {
      log.warn("[MqttConfig] 获取配置失败: networkUnionId={}, error={}", networkUnionId, e.getMessage());
      return null;
    }
  }

  /** 检查产品是否有专用配置 */
  @Override
  public boolean supportMQTTNetwork(String productKey) {
    List<String> allKeys = thirdMQTTConfigLoader.getAllEnabledProductKeys();
    return allKeys.contains(productKey);
  }

  @Override
  @MultiLevelCacheable(
      cacheNames = "supportMQTTNetwork",
      key = "#productKey + ':' + #networkUnionId",
      l1Expire = 360,
      l2Expire = 720,
      strategy = CacheStrategy.WRITE_THROUGH)
  public boolean supportMQTTNetwork(String productKey, String networkUnionId) {
    String db = ioTProductMapper.selectNetworkUnionId(productKey);
    if (StrUtil.isBlank(networkUnionId)
        || StrUtil.isBlank(db)
        || !networkUnionId.equalsIgnoreCase(db)) {
      return false;
    }
    return true;
  }

  /** 获取所有已启用的productKey */
  public List<String> getAllProductKeys() {
    return thirdMQTTConfigLoader.getAllEnabledProductKeys();
  }

  /** 获取完整的配置统计信息（包括所有类型） */
  public String getCompleteConfigStatistics() {
    try {
      // MQTT_CLIENT统计
      Example clientTotalExample = new Example(Network.class);
      clientTotalExample
          .createCriteria()
          .andIn("type", Stream.of(NetworkType.MQTT_CLIENT.getId()).collect(Collectors.toList()));
      int clientTotal = networkMapper.selectCountByExample(clientTotalExample);

      Example clientEnabledExample = new Example(Network.class);
      clientEnabledExample
          .createCriteria()
          .andIn("type", Stream.of(NetworkType.MQTT_CLIENT.getId()).collect(Collectors.toList()))
          .andEqualTo("state", Network.STATE_ENABLED);
      int clientEnabled = networkMapper.selectCountByExample(clientEnabledExample);

      // MQTT_SERVER统计
      Example serverTotalExample = new Example(Network.class);
      serverTotalExample
          .createCriteria()
          .andIn("type", Arrays.asList(NetworkType.MQTT_SERVER.getId()));
      int serverTotal = networkMapper.selectCountByExample(serverTotalExample);

      Example serverEnabledExample = new Example(Network.class);
      serverEnabledExample
          .createCriteria()
          .andIn("type", Arrays.asList(NetworkType.MQTT_SERVER.getId()))
          .andEqualTo("state", Network.STATE_ENABLED);
      int serverEnabled = networkMapper.selectCountByExample(serverEnabledExample);

      // 系统MQTT统计
      int systemMqttCount = sysMQTTStatusProvider.isEnabled() ? 1 : 0;
      int systemMqttConnected = sysMQTTStatusProvider.isConnected() ? 1 : 0;

      StringBuilder stats = new StringBuilder();
      stats.append("MQTT完整配置统计:\n");
      stats.append("=== 数据库配置 ===\n");
      stats
          .append("MQTT_CLIENT - 总数: ")
          .append(clientTotal)
          .append(", 启用: ")
          .append(clientEnabled)
          .append("\n");
      stats
          .append("MQTT_SERVER - 总数: ")
          .append(serverTotal)
          .append(", 启用: ")
          .append(serverEnabled)
          .append("\n");
      stats.append("=== 系统MQTT ===\n");
      stats
          .append("系统MQTT - 配置: ")
          .append(systemMqttCount)
          .append(", 连接: ")
          .append(systemMqttConnected)
          .append("\n");
      stats.append("=== 总计 ===\n");
      stats.append("总配置数: ").append(clientTotal + serverTotal + systemMqttCount).append("\n");
      stats
          .append("启用数: ")
          .append(clientEnabled + serverEnabled + systemMqttConnected)
          .append("\n");

      return stats.toString();

    } catch (Exception e) {
      log.error("[MqttConfig] 获取完整配置统计失败: ", e);
      return "配置统计信息获取失败";
    }
  }

  /** 获取启用的产品总数（包括所有类型） */
  public int getTotalEnabledProductCount() {
    try {
      Example example = new Example(Network.class);
      example
          .createCriteria()
          .andIn(
              "type",
              Arrays.asList(NetworkType.MQTT_CLIENT.getId(), NetworkType.MQTT_SERVER.getId()))
          .andEqualTo("state", Network.STATE_ENABLED);

      int dbCount = networkMapper.selectCountByExample(example);
      int systemCount = sysMQTTStatusProvider.isEnabled() ? 1 : 0;

      return dbCount + systemCount;

    } catch (Exception e) {
      log.error("[MqttConfig] 获取启用产品总数失败: ", e);
      return 0;
    }
  }

  /** 获取配置详细信息列表 */
  public List<Map<String, Object>> getConfigDetails() {
    List<Map<String, Object>> details = Arrays.asList();

    try {
      Map<String, MQTTProductConfig> allConfigs = loadAllConfigs();

      details =
          allConfigs.entrySet().stream()
              .map(
                  entry -> {
                    MQTTProductConfig config = entry.getValue();
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("unionId", config.getNetworkUnionId());
                    detail.put("networkType", config.getNetworkType());
                    detail.put("host", config.getHost());
                    detail.put("username", config.getUsername());
                    detail.put("subscribeTopics", config.getSubscribeTopics());
                    detail.put("enabled", config.isEnabled());
                    return detail;
                  })
              .collect(Collectors.toList());

      // 添加系统MQTT详情
      if (sysMQTTStatusProvider.isEnabled()) {
        Map<String, Object> systemDetail = new HashMap<>();
        systemDetail.put("productKey", "SYSTEM_MQTT_BROKER");
        systemDetail.put("networkType", "SYSTEM_MQTT");
        systemDetail.put("host", sysMQTTStatusProvider.getConfig().getHost());
        systemDetail.put("username", sysMQTTStatusProvider.getConfig().getUsername());
        systemDetail.put(
            "subscribeTopics", sysMQTTStatusProvider.getConfig().getSubscribeTopics().size());
        systemDetail.put("enabled", true);
        details.add(systemDetail);
      }

    } catch (Exception e) {
      log.error("[MqttConfig] 获取配置详情失败: ", e);
    }

    return details;
  }
}
