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

package cn.universal.dm.device.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.bo.UPPushBO;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 上行推送配置服务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Service
public class UPPushConfigService {

  @Resource private IoTUserApplicationMapper ioTUserApplicationMapper;

  // 本地缓存：应用ID -> 推送配置
  private static final long POSITIVE_CACHE_NANOS = TimeUnit.MINUTES.toNanos(10);
  private static final long NEGATIVE_CACHE_NANOS = TimeUnit.SECONDS.toNanos(30);

  private final Cache<String, CachedPushConfig> pushConfigCache =
      Caffeine.newBuilder()
          .maximumSize(5000)
          .expireAfter(
              new Expiry<String, CachedPushConfig>() {
                @Override
                public long expireAfterCreate(
                    String key, CachedPushConfig value, long currentTime) {
                  return value.exists() ? POSITIVE_CACHE_NANOS : NEGATIVE_CACHE_NANOS;
                }

                @Override
                public long expireAfterUpdate(
                    String key, CachedPushConfig value, long currentTime, long currentDuration) {
                  return expireAfterCreate(key, value, currentTime);
                }

                @Override
                public long expireAfterRead(
                    String key, CachedPushConfig value, long currentTime, long currentDuration) {
                  return currentDuration;
                }
              })
          .build();

  /**
   * 获取推送配置（带缓存）
   *
   * @param applicationId 应用ID
   * @return 推送配置
   */
  public UPPushBO getPushConfig(String applicationId) {
    if (StrUtil.isBlank(applicationId)) {
      log.warn("[推送配置] 应用ID为空，返回默认配置");
      return getDefaultPushConfig();
    }
    return cachedPushConfig(applicationId).config();
  }

  public boolean isApplicationEnabled(String applicationId) {
    return StrUtil.isNotBlank(applicationId) && cachedPushConfig(applicationId).enabled();
  }

  private CachedPushConfig cachedPushConfig(String applicationId) {
    return pushConfigCache.get(applicationId, this::loadCachedPushConfig);
  }

  /**
   * 批量获取推送配置
   *
   * @param applicationIds 应用ID列表
   * @return 推送配置映射
   */
  public Map<String, UPPushBO> getPushConfigs(List<String> applicationIds) {
    return applicationIds.stream()
        .collect(java.util.stream.Collectors.toMap(appId -> appId, this::getPushConfig));
  }

  /**
   * 刷新缓存
   *
   * @param applicationId 应用ID
   */
  public void refreshCache(String applicationId) {
    if (StrUtil.isNotBlank(applicationId)) {
      pushConfigCache.invalidate(applicationId);
      log.info("[推送配置] 缓存已刷新，应用ID: {}", applicationId);
    }
  }

  /** 清空所有缓存 */
  public void clearCache() {
    pushConfigCache.invalidateAll();
    log.info("[推送配置] 所有缓存已清空");
  }

  /**
   * 从数据库加载推送配置
   *
   * @param applicationId 应用ID
   * @return 推送配置
   */
  private CachedPushConfig loadCachedPushConfig(String applicationId) {
    try {
      IoTUserApplication application =
          ioTUserApplicationMapper.selectIotUserApplicationById(applicationId);
      return configurationOf(application, applicationId);
    } catch (Exception e) {
      log.error("[推送配置] 加载配置异常，应用ID: {}", applicationId, e);
      return new CachedPushConfig(getDefaultPushConfig(), false, false);
    }
  }

  /**
   * Warm all application configurations outside message handling, including disabled applications.
   */
  @jakarta.annotation.PostConstruct
  @org.springframework.scheduling.annotation.Scheduled(
      fixedDelayString = "${nexiot.push.config-refresh-ms:30000}",
      initialDelay = 30000)
  public void preloadConfigurations() {
    try {
      var applications = ioTUserApplicationMapper.selectAll();
      if (applications == null) return;
      java.util.Set<String> found = new java.util.HashSet<>();
      for (var application : applications) {
        String id = application.getAppUniqueId();
        if (StrUtil.isBlank(id)) continue;
        found.add(id);
        pushConfigCache.put(id, configurationOf(application, id));
      }
      pushConfigCache.asMap().keySet().removeIf(id -> !found.contains(id));
    } catch (RuntimeException error) {
      log.warn("[推送配置] 预加载失败，保留现有缓存", error);
    }
  }

  private CachedPushConfig configurationOf(IoTUserApplication application, String applicationId) {
    if (application == null) {
      log.warn("[推送配置] 应用不存在，应用ID: {}", applicationId);
      return new CachedPushConfig(getDefaultPushConfig(), false, false);
    }
    if (application.getAppStatus() != null
        && !IoTConstant.NORMAL.equals(application.getAppStatus())) {
      log.debug("[推送配置] 应用已停用，应用ID: {}", applicationId);
      return new CachedPushConfig(getDefaultPushConfig(), true, false);
    }

    // 解析推送配置JSON
    String pushConfigJson = application.getCfg();
    if (StrUtil.isBlank(pushConfigJson)) {
      log.warn("[推送配置] 推送配置为空，应用ID: {}", applicationId);
      return new CachedPushConfig(getDefaultPushConfig(), true, true);
    }
    return new CachedPushConfig(parsePushConfig(pushConfigJson), true, true);
  }

  /**
   * 解析推送配置JSON
   *
   * @param pushConfigJson 推送配置JSON字符串
   * @return 推送配置对象
   */
  private UPPushBO parsePushConfig(String pushConfigJson) {
    try {
      JSONObject config = JSONUtil.parseObj(pushConfigJson);

      return UPPushBO.builder()
          .http(parseHttpConfig(config.getJSONObject("http")))
          .mqtt(parseMqttConfig(config.getJSONObject("mqtt")))
          .externalMqtt(parseExternalMqttConfig(config.getJSONObject("externalMqtt")))
          .tcp(parseTcpConfig(config.getJSONObject("tcp")))
          .kafka(parseKafkaConfig(config.getJSONObject("kafka")))
          .rocketMQ(
              parseRocketMQConfig(
                  config.getJSONObject("rocketMQ") != null
                      ? config.getJSONObject("rocketMQ")
                      : config.getJSONObject("rocketmq")))
          .transform(parseTransformConfig(config.getJSONObject("transform")))
          .build();
    } catch (Exception e) {
      log.error("[推送配置] 解析配置JSON异常: {}", pushConfigJson, e);
      return getDefaultPushConfig();
    }
  }

  /** 解析HTTP配置 */
  private UPPushBO.HttpPushConfig parseHttpConfig(JSONObject httpConfig) {
    if (httpConfig == null) {
      return UPPushBO.HttpPushConfig.builder().enable(false).support(false).build();
    }

    return UPPushBO.HttpPushConfig.builder()
        .url(httpConfig.getStr("url"))
        .enable(httpConfig.getBool("enable", false))
        .header(httpConfig.getStr("header"))
        .secret(httpConfig.getStr("secret"))
        .support(httpConfig.getBool("support", true))
        .build();
  }

  /** 解析MQTT配置 */
  private UPPushBO.MqttPushConfig parseMqttConfig(JSONObject mqttConfig) {
    if (mqttConfig == null) {
      return UPPushBO.MqttPushConfig.builder().enable(false).support(false).build();
    }

    return UPPushBO.MqttPushConfig.builder()
        .url(mqttConfig.getStr("url"))
        .topic(mqttConfig.getStr("topic"))
        .enable(mqttConfig.getBool("enable", false))
        .support(mqttConfig.getBool("support", true))
        .password(mqttConfig.getStr("password"))
        .username(mqttConfig.getStr("username"))
        .build();
  }

  private UPPushBO.ExternalMqttPushConfig parseExternalMqttConfig(JSONObject config) {
    if (config == null) {
      return UPPushBO.ExternalMqttPushConfig.builder().enable(false).support(false).build();
    }
    return UPPushBO.ExternalMqttPushConfig.builder()
        .brokerUrl(config.getStr("brokerUrl"))
        .clientId(config.getStr("clientId"))
        .username(config.getStr("username"))
        .password(config.getStr("password"))
        .topic(config.getStr("topic"))
        .qos(config.getInt("qos", 1))
        .retained(config.getBool("retained", false))
        .keepAliveSeconds(config.getInt("keepAliveSeconds", 60))
        .connectTimeoutSeconds(config.getInt("connectTimeoutSeconds", 10))
        .publishTimeoutSeconds(config.getInt("publishTimeoutSeconds", 10))
        .clusterClientId(config.getBool("clusterClientId", true))
        .enable(config.getBool("enable", false))
        .support(config.getBool("support", true))
        .build();
  }

  private UPPushBO.TcpPushConfig parseTcpConfig(JSONObject config) {
    if (config == null) {
      return UPPushBO.TcpPushConfig.builder().enable(false).support(false).build();
    }
    return UPPushBO.TcpPushConfig.builder()
        .host(config.getStr("host"))
        .port(config.getInt("port"))
        .tls(config.getBool("tls", false))
        .connectionMode(config.getStr("connectionMode", "PERSISTENT"))
        .frameMode(config.getStr("frameMode", "LINE"))
        .delimiter(config.getStr("delimiter", "\n"))
        .charset(config.getStr("charset", "UTF-8"))
        .connectTimeoutMillis(config.getInt("connectTimeoutMillis", 5000))
        .writeTimeoutMillis(config.getInt("writeTimeoutMillis", 3000))
        .enable(config.getBool("enable", false))
        .support(config.getBool("support", true))
        .build();
  }

  /** 解析Kafka配置 */
  private UPPushBO.KafkaPushConfig parseKafkaConfig(JSONObject kafkaConfig) {
    if (kafkaConfig == null) {
      return UPPushBO.KafkaPushConfig.builder().enable(false).support(false).build();
    }

    return UPPushBO.KafkaPushConfig.builder()
        .bootstrapServers(kafkaConfig.getStr("bootstrapServers"))
        .topic(kafkaConfig.getStr("topic"))
        .username(kafkaConfig.getStr("username"))
        .password(kafkaConfig.getStr("password"))
        .securityProtocol(kafkaConfig.getStr("securityProtocol"))
        .saslMechanism(kafkaConfig.getStr("saslMechanism"))
        .enable(kafkaConfig.getBool("enable", false))
        .support(kafkaConfig.getBool("support", true))
        .build();
  }

  /** 解析RocketMQ配置 */
  private UPPushBO.RocketMQPushConfig parseRocketMQConfig(JSONObject rocketMQConfig) {
    if (rocketMQConfig == null) {
      return UPPushBO.RocketMQPushConfig.builder().enable(false).support(false).build();
    }

    return UPPushBO.RocketMQPushConfig.builder()
        .namesrvAddr(rocketMQConfig.getStr("namesrvAddr"))
        .topic(rocketMQConfig.getStr("topic"))
        .group(rocketMQConfig.getStr("group"))
        .accessKey(rocketMQConfig.getStr("accessKey"))
        .secretKey(rocketMQConfig.getStr("secretKey"))
        .securityToken(rocketMQConfig.getStr("securityToken"))
        .namespace(rocketMQConfig.getStr("namespace"))
        .useTLS(rocketMQConfig.getBool("useTLS", false))
        .enable(rocketMQConfig.getBool("enable", false))
        .support(rocketMQConfig.getBool("support", true))
        .build();
  }

  /** 解析数据转换配置 */
  private UPPushBO.TransformConfig parseTransformConfig(JSONObject transformConfig) {
    if (transformConfig == null) {
      return UPPushBO.TransformConfig.builder()
          .enable(false)
          .support(true)
          .fallbackMode("ORIGINAL")
          .build();
    }

    return UPPushBO.TransformConfig.builder()
        .enable(transformConfig.getBool("enable", false))
        .support(transformConfig.getBool("support", true))
        .defaultRuleId(transformConfig.getLong("defaultRuleId"))
        .fallbackMode(transformConfig.getStr("fallbackMode", "ORIGINAL"))
        .build();
  }

  /**
   * 获取默认推送配置
   *
   * @return 默认配置
   */
  UPPushBO getDefaultPushConfig() {
    return UPPushBO.builder()
        .http(UPPushBO.HttpPushConfig.builder().enable(false).support(false).build())
        .mqtt(UPPushBO.MqttPushConfig.builder().enable(false).support(false).build())
        .externalMqtt(
            UPPushBO.ExternalMqttPushConfig.builder().enable(false).support(false).build())
        .tcp(UPPushBO.TcpPushConfig.builder().enable(false).support(false).build())
        .kafka(UPPushBO.KafkaPushConfig.builder().enable(false).support(false).build())
        .rocketMQ(UPPushBO.RocketMQPushConfig.builder().enable(false).support(false).build())
        .transform(
            UPPushBO.TransformConfig.builder()
                .enable(false)
                .support(true)
                .fallbackMode("ORIGINAL")
                .build())
        .build();
  }

  private record CachedPushConfig(UPPushBO config, boolean exists, boolean enabled) {}
}
