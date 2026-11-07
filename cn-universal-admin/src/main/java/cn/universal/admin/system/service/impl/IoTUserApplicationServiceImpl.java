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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.system.service.IIoTUserApplicationService;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.device.service.push.transform.AppPushConfigChangeNotifier;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.entity.vo.IoTUserApplicationVO;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

/**
 * 用户应用信息Service业务层处理
 *
 * @since 2025-12-30
 */
@Service
@Slf4j
public class IoTUserApplicationServiceImpl extends BaseServiceImpl
    implements IIoTUserApplicationService {

  @Resource private IoTUserApplicationMapper iotUserApplicationMapper;
  @Resource private IoTUserService ioTUserService;

  @Resource private OauthClientDetailsMapper oauthClientDetailsMapper;

  @Resource private AppPushConfigChangeNotifier appPushConfigChangeNotifier;
  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  @Value("${mqtt.cfg.enable:true}")
  private boolean sysMqttEnabled;

  //  @Value("${mqtt.cfg.host:tcp://localhost:1883}")
  //  private String sysMqttHost;

  // MQTT配置 - 复用现有配置
  @Value("${mqtt.cfg.host.out:tcp://localhost:1883}")
  private String mqttHostOut;

  @Value("${mqtt.cfg.host:tcp://localhost:1883}")
  private String mqttHost;

  @Value("${mqtt.cfg.ws.url:}")
  private String sysMqttWsUrl;

  @Value("${mqtt.cfg.wss.url:}")
  private String sysMqttWssUrl;

  private static final String DEFAULT_HTTP_PUSH_HEADER = "X-Nexiot-Secret";
  private static final Set<String> KAFKA_SECURITY_PROTOCOLS =
      Set.of("PLAINTEXT", "SSL", "SASL_PLAINTEXT", "SASL_SSL");
  private static final Set<String> KAFKA_SASL_MECHANISMS =
      Set.of("PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512");

  @Override
  @Transactional(rollbackFor = Exception.class)
  @CacheEvict(
      cacheNames = {"selectUserByUserName"},
      allEntries = true)
  public boolean enDisableIoTUser(String unionId, boolean isEnable) {
    List<IoTUserApplication> select =
        iotUserApplicationMapper.select(IoTUserApplication.builder().unionId(unionId).build());
    if (CollectionUtil.isNotEmpty(select)) {
      // 处理应用
      for (IoTUserApplication iotUserApplication : select) {
        if (isEnable) {
          iotUserApplication.setAppStatus(IoTConstant.NORMAL);
        } else {
          iotUserApplication.setAppStatus(IoTConstant.UN_NORMAL);
        }
        if (iotUserApplicationMapper.updateByPrimaryKeySelective(iotUserApplication) > 0) {
          appPushConfigChangeNotifier.notifyAppConfigChanged(
              iotUserApplication.getAppUniqueId(), "APP_STATUS_UPDATED");
        }
      }
      cacheInvalidationPublisher.publishApplicationStatusChange("application-status-updated");
    }
    // 处理oauth2鉴权
    List<OAuth2ClientDetails> oauth2 =
        oauthClientDetailsMapper.select(OAuth2ClientDetails.builder().iotUnionId(unionId).build());
    if (CollectionUtil.isNotEmpty(oauth2)) {
      for (OAuth2ClientDetails details : oauth2) {
        OAuth2ClientDetails build = OAuth2ClientDetails.builder().build();
        if (isEnable) {
          build.setAuthorizedGrantTypes("client_credentials");
        } else {
          build.setAuthorizedGrantTypes("");
        }
        Example example = new Example(OAuth2ClientDetails.class);
        example.createCriteria().andEqualTo("clientId", details.getClientId());
        oauthClientDetailsMapper.updateByExampleSelective(build, example);
      }
    }
    return true;
  }

  @Override
  @Cacheable(cacheNames = "iot_app_user_name", key = "#applicationId")
  public String selectApplicationName(String applicationId) {
    IoTUserApplication iotUserApplication =
        iotUserApplicationMapper.selectOne(
            IoTUserApplication.builder().appUniqueId(applicationId).build());
    return iotUserApplication == null ? "" : iotUserApplication.getAppName();
  }

  /**
   * 查询用户应用信息
   *
   * @param appUniqueId 用户应用信息ID
   * @return 用户应用信息
   */
  @Override
  public IoTUserApplication selectIotUserApplicationById(String appUniqueId) {
    return checkSelf(appUniqueId);
  }

  public IoTUserApplication checkSelf(String appUniqueId) {
    IoTUserApplication iotUserApplication =
        iotUserApplicationMapper.selectIotUserApplicationById(appUniqueId);
    if (Objects.isNull(iotUserApplication)) {
      throw new IoTException("应用不存在！");
    }
    assertTenantResourceAccess(
        iotUserApplication.getTenantId(), iotUserApplication.getUnionId(), "您没有权限操作此应用！");
    synchronizeLegacyPushSwitches(iotUserApplication);
    return iotUserApplication;
  }

  /**
   * 查询用户应用信息列表
   *
   * @param iotUserApplication 用户应用信息
   * @return 用户应用信息
   */
  @Override
  public List<IoTUserApplicationVO> selectIotUserApplicationList(
      IoTUserApplication iotUserApplication, IoTUser iotUser) {
    applyApplicationScope(iotUserApplication, iotUser);
    List<IoTUserApplicationVO> applications =
        iotUserApplicationMapper.selectIotUserApplicationList(iotUserApplication);
    applications.forEach(this::synchronizeLegacyPushSwitches);
    return applications;
  }

  /**
   * 新增用户应用信息
   *
   * @param iotUserApplication 用户应用信息
   * @return 结果
   */
  @Override
  public int insertIotAppUser(IoTUserApplication iotUserApplication) {
    IoTUser iotUser = queryIotUser(iotUserApplication.getUnionId());
    iotUserApplication.setUnionId(iotUser.getUnionId());
    if (StrUtil.isBlank(iotUserApplication.getTenantId())) {
      iotUserApplication.setTenantId(
          StrUtil.blankToDefault(iotUser.getTenantId(), iotUser.getUnionId()));
    }
    normalizePushCfg(iotUserApplication);
    synchronizeLegacyPushSwitches(iotUserApplication);
    int result = iotUserApplicationMapper.insertIotUserApplication(iotUserApplication);
    if (result > 0) {
      appPushConfigChangeNotifier.notifyAppConfigChanged(
          iotUserApplication.getAppUniqueId(), "APP_CREATED");
    }
    return result;
  }

  /**
   * 修改用户应用信息
   *
   * @param iotUserApplication 用户应用信息
   * @return 结果
   */
  @Override
  public int updateIotUserApplication(IoTUserApplication iotUserApplication) {
    IoTUserApplication stored = selectIotUserApplicationById(iotUserApplication.getAppUniqueId());
    boolean applicationStatusChanged =
        iotUserApplication.getAppStatus() != null
            && !Objects.equals(stored.getAppStatus(), iotUserApplication.getAppStatus());
    iotUserApplication.setUnionId(stored.getUnionId());
    iotUserApplication.setTenantId(
        StrUtil.blankToDefault(iotUserApplication.getTenantId(), stored.getTenantId()));
    normalizePushCfg(iotUserApplication);
    if (StrUtil.isNotBlank(iotUserApplication.getCfg())) {
      synchronizeLegacyPushSwitches(iotUserApplication);
    }
    int result = iotUserApplicationMapper.updateIotUserApplication(iotUserApplication);
    if (result > 0) {
      appPushConfigChangeNotifier.notifyAppConfigChanged(
          iotUserApplication.getAppUniqueId(), "APP_UPDATED");
      if (applicationStatusChanged) {
        cacheInvalidationPublisher.publishApplicationStatusChange("application-status-updated");
      }
    }
    return result;
  }

  /**
   * 删除用户应用信息对象
   *
   * @param appUniqueId 需要删除的数据ID
   * @return 结果
   */
  @Override
  public int deleteIotUserApplicationByIds(String[] appUniqueId) {
    if (appUniqueId != null) {
      for (String id : appUniqueId) {
        selectIotUserApplicationById(id);
      }
    }
    int result = iotUserApplicationMapper.deleteIotUserApplicationByIds(appUniqueId);
    if (result > 0 && appUniqueId != null) {
      for (String id : appUniqueId) {
        appPushConfigChangeNotifier.notifyAppConfigChanged(id, "APP_DELETED");
      }
      cacheInvalidationPublisher.publishApplicationStatusChange("application-deleted");
    }
    return result;
  }

  @Override
  public List<IoTUserApplication> selectIotUserApplicationByIds(String[] appUniqueId) {
    return iotUserApplicationMapper.selectIotUserApplicationsByIds(appUniqueId);
  }

  /**
   * 删除用户应用信息信息 x
   *
   * @param appUniqueId 用户应用信息ID
   * @return 结果
   */
  @Override
  public int deleteIotUserApplicationById(String appUniqueId) {
    selectIotUserApplicationById(appUniqueId);
    int result = iotUserApplicationMapper.deleteIotUserApplicationById(appUniqueId);
    if (result > 0) {
      appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "APP_DELETED");
      cacheInvalidationPublisher.publishApplicationStatusChange("application-deleted");
    }
    return result;
  }

  @Override
  public List<IoTUserApplicationVO> selectApplicationList(
      IoTUserApplication application, IoTUser iotUser) {
    applyApplicationScope(application, iotUser);
    List<IoTUserApplicationVO> applications =
        iotUserApplicationMapper.selectApplicationList(application);
    applications.forEach(this::synchronizeLegacyPushSwitches);
    return applications;
  }

  @Override
  public boolean enableOrDisablePushCfg(String appUniqueId, String pushType, boolean isEnable) {
    IoTUserApplication application = selectIotUserApplicationById(appUniqueId);
    if (application == null) {
      return false;
    }
    // 获取当前推送配置
    String pushCfgStr = application.getCfg();
    JSONObject pushCfg;
    if (StrUtil.isBlank(pushCfgStr)) {
      pushCfg = new JSONObject();
    } else {
      pushCfg = JSONUtil.parseObj(pushCfgStr);
    }

    switch (pushType) {
      case "mqtt":
        if (isEnable && !sysMqttEnabled) {
          throw new IoTException("系统 MQTT 服务未启用");
        }
        JSONObject existingMqttConfig = pushCfg.getJSONObject("mqtt");
        validatePushEnable("MQTT", existingMqttConfig, isEnable);
        application.setMqttEnable(isEnable ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE);
        if (isEnable) {
          // MQTT启用：获取已有配置或创建新配置，保留用户自定义值
          JSONObject mqttConfig = pushCfg.getJSONObject("mqtt");
          if (mqttConfig == null) {
            mqttConfig = new JSONObject();
          }
          mqttConfig.set("enable", true);
          mqttConfig.set("support", true);
          // 只有当配置为空时才用默认值填充，保留用户已修改的值
          if (StrUtil.isBlank(mqttConfig.getStr("url"))) {
            mqttConfig.set("url", StrUtil.blankToDefault(mqttHostOut, mqttHost));
          }
          if (StrUtil.isBlank(mqttConfig.getStr("wsUrl")) && StrUtil.isNotBlank(sysMqttWsUrl)) {
            mqttConfig.set("wsUrl", sysMqttWsUrl);
          }
          if (StrUtil.isBlank(mqttConfig.getStr("wssUrl")) && StrUtil.isNotBlank(sysMqttWssUrl)) {
            mqttConfig.set("wssUrl", sysMqttWssUrl);
          }
          if (StrUtil.isBlank(mqttConfig.getStr("password"))) {
            mqttConfig.set("password", application.getAppSecret());
          }
          if (StrUtil.isBlank(mqttConfig.getStr("username"))) {
            mqttConfig.set("username", application.getAppId());
          }
          if (StrUtil.isBlank(mqttConfig.getStr("clientId"))) {
            mqttConfig.set("clientId", application.getAppId());
          }
          pushCfg.set("mqtt", mqttConfig);
        } else {
          // MQTT禁用：只设置enable为false
          if (pushCfg.containsKey("mqtt")) {
            JSONObject mqttConfig = pushCfg.getJSONObject("mqtt");
            mqttConfig.set("enable", false);
            pushCfg.set("mqtt", mqttConfig);
          }
        }
        break;
      case "http":
        JSONObject httpConfig = pushCfg.getJSONObject("http");
        if (httpConfig != null && StrUtil.isBlank(httpConfig.getStr("header"))) {
          httpConfig.set("header", DEFAULT_HTTP_PUSH_HEADER);
        }
        validatePushEnable("HTTP", httpConfig, isEnable);
        application.setHttpEnable(isEnable ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE);
        updatePushSwitch(pushCfg, "http", isEnable);
        break;
      case "kafka":
        validatePushEnable("Kafka", pushCfg.getJSONObject("kafka"), isEnable);
        updatePushSwitch(pushCfg, "kafka", isEnable);
        break;
      case "externalMqtt":
      case "externalmqtt":
        validatePushEnable("ExternalMQTT", pushCfg.getJSONObject("externalMqtt"), isEnable);
        updatePushSwitch(pushCfg, "externalMqtt", isEnable);
        break;
      case "tcp":
        validatePushEnable("TCP", pushCfg.getJSONObject("tcp"), isEnable);
        updatePushSwitch(pushCfg, "tcp", isEnable);
        break;
      case "rocketmq":
      case "rocketMQ":
        JSONObject rocketMqConfig = pushCfg.getJSONObject("rocketMQ");
        if (rocketMqConfig == null && pushCfg.containsKey("rocketmq")) {
          rocketMqConfig = pushCfg.getJSONObject("rocketmq");
          pushCfg.remove("rocketmq");
          pushCfg.set("rocketMQ", rocketMqConfig);
        }
        validatePushEnable("RocketMQ", rocketMqConfig, isEnable);
        updatePushSwitch(pushCfg, "rocketMQ", isEnable);
        break;
      default:
        return false;
    }
    // 更新推送配置
    application.setCfg(pushCfg.toString());
    int result = iotUserApplicationMapper.updateIotUserApplication(application);
    if (result > 0) {
      appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "PUSH_SWITCH_UPDATED");
    }
    return result > 0;
  }

  private void updatePushSwitch(JSONObject pushCfg, String configKey, boolean isEnable) {
    JSONObject config = pushCfg.getJSONObject(configKey);
    if (config == null) {
      config = new JSONObject();
    }
    config.set("enable", isEnable);
    config.set("support", true);
    pushCfg.set(configKey, config);
  }

  private void validatePushEnable(String pushType, JSONObject config, boolean isEnable) {
    if (!isEnable) {
      return;
    }
    if (config != null && Boolean.FALSE.equals(config.getBool("support"))) {
      throw new IoTException(pushType + " 推送当前不受支持");
    }
    if ("MQTT".equals(pushType)) {
      return;
    }
    if (config == null) {
      throw new IoTException("请先完成 " + pushType + " 推送配置");
    }
    if ("HTTP".equals(pushType)) {
      String url = config.getStr("url");
      if (StrUtil.isBlank(url)
          || !(url.startsWith("http://") || url.startsWith("https://"))
          || StrUtil.isBlank(config.getStr("header"))
          || StrUtil.isBlank(config.getStr("secret"))) {
        throw new IoTException("HTTP 推送地址、密钥请求头和推送密钥不能为空");
      }
      return;
    }
    if ("Kafka".equals(pushType)) {
      if (StrUtil.isBlank(config.getStr("bootstrapServers"))
          || StrUtil.isBlank(config.getStr("topic"))) {
        throw new IoTException("Kafka 服务地址和 Topic 不能为空");
      }
      boolean hasCredentials =
          StrUtil.isNotBlank(config.getStr("username"))
              || StrUtil.isNotBlank(config.getStr("password"));
      String defaultProtocol = hasCredentials ? "SASL_PLAINTEXT" : "PLAINTEXT";
      String securityProtocol =
          StrUtil.blankToDefault(config.getStr("securityProtocol"), defaultProtocol)
              .trim()
              .toUpperCase(Locale.ROOT);
      if (!KAFKA_SECURITY_PROTOCOLS.contains(securityProtocol)) {
        throw new IoTException("Kafka 安全协议不受支持: " + securityProtocol);
      }
      if (securityProtocol.startsWith("SASL_")) {
        if (StrUtil.isBlank(config.getStr("username"))
            || StrUtil.isBlank(config.getStr("password"))) {
          throw new IoTException("Kafka SASL 鉴权必须同时配置用户名和密码");
        }
        String saslMechanism =
            StrUtil.blankToDefault(config.getStr("saslMechanism"), "PLAIN")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (!KAFKA_SASL_MECHANISMS.contains(saslMechanism)) {
          throw new IoTException("Kafka SASL 机制不受支持: " + saslMechanism);
        }
      }
      return;
    }
    if ("ExternalMQTT".equals(pushType)) {
      String brokerUrl = config.getStr("brokerUrl");
      if (StrUtil.isBlank(brokerUrl)
          || !(brokerUrl.startsWith("tcp://")
              || brokerUrl.startsWith("ssl://")
              || brokerUrl.startsWith("ws://")
              || brokerUrl.startsWith("wss://"))
          || StrUtil.isBlank(config.getStr("topic"))) {
        throw new IoTException("外部 MQTT Broker 地址和 Topic 不能为空");
      }
      int qos = config.getInt("qos", 1);
      if (qos < 0 || qos > 2) {
        throw new IoTException("MQTT QoS 必须为 0、1 或 2");
      }
      return;
    }
    if ("TCP".equals(pushType)) {
      Integer port = config.getInt("port");
      if (StrUtil.isBlank(config.getStr("host"))
          || port == null
          || port < 1
          || port > 65535) {
        throw new IoTException("TCP 服务器地址不能为空，端口必须在 1-65535 之间");
      }
      return;
    }
    if (StrUtil.isBlank(config.getStr("namesrvAddr"))
        || StrUtil.isBlank(config.getStr("topic"))
        || StrUtil.isBlank(config.getStr("group"))) {
      throw new IoTException("RocketMQ NameServer、Topic 和 Producer Group 不能为空");
    }
    boolean accessKeyMissing = StrUtil.isBlank(config.getStr("accessKey"));
    boolean secretKeyMissing = StrUtil.isBlank(config.getStr("secretKey"));
    if (accessKeyMissing != secretKeyMissing) {
      throw new IoTException("RocketMQ AccessKey 和 SecretKey 必须同时配置");
    }
    if (StrUtil.isNotBlank(config.getStr("securityToken"))
        && (accessKeyMissing || secretKeyMissing)) {
      throw new IoTException("RocketMQ SecurityToken 必须配合 AccessKey 和 SecretKey 使用");
    }
  }

  private void normalizePushCfg(IoTUserApplication application) {
    if (application == null || StrUtil.isBlank(application.getCfg())) {
      return;
    }
    JSONObject cfg = JSONUtil.parseObj(application.getCfg());
    normalizeConfiguredChannel(cfg, "http", "url", "secret");
    normalizeConfiguredChannel(cfg, "rocketMQ", "namesrvAddr", "topic", "group");
    normalizeConfiguredChannel(cfg, "kafka", "bootstrapServers", "topic");
    normalizeConfiguredChannel(cfg, "externalMqtt", "brokerUrl", "topic");
    normalizeConfiguredChannel(cfg, "tcp", "host", "port");
    application.setCfg(cfg.toString());
  }

  private void normalizeConfiguredChannel(
      JSONObject cfg, String configKey, String... requiredFields) {
    JSONObject config = cfg.getJSONObject(configKey);
    if (config == null || config.isEmpty()) {
      return;
    }
    boolean configured = true;
    for (String field : requiredFields) {
      if (StrUtil.isBlank(config.getStr(field))) {
        configured = false;
        break;
      }
    }
    if (!configured) {
      return;
    }
    boolean support = config.getBool("support", true);
    config.set("support", support);
    config.set("enable", support && config.getBool("enable", false));
    if ("http".equals(configKey) && StrUtil.isBlank(config.getStr("header"))) {
      config.set("header", DEFAULT_HTTP_PUSH_HEADER);
    }
    cfg.set(configKey, config);
  }

  private void synchronizeLegacyPushSwitches(IoTUserApplication application) {
    if (application == null) {
      return;
    }
    JSONObject cfg = parseCfg(application.getCfg());
    application.setHttpEnable(channelEnabled(cfg, "http") ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE);
    application.setMqttEnable(channelEnabled(cfg, "mqtt") ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE);
  }

  private void synchronizeLegacyPushSwitches(IoTUserApplicationVO application) {
    if (application == null) {
      return;
    }
    JSONObject cfg = parseCfg(application.getCfg());
    application.setHttpEnable(channelEnabled(cfg, "http") ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE);
    application.setMqttEnable(channelEnabled(cfg, "mqtt") ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE);
  }

  private JSONObject parseCfg(String cfg) {
    if (StrUtil.isBlank(cfg) || !JSONUtil.isTypeJSON(cfg)) {
      return new JSONObject();
    }
    return JSONUtil.parseObj(cfg);
  }

  private boolean channelEnabled(JSONObject cfg, String channel) {
    JSONObject channelConfig = cfg.getJSONObject(channel);
    return channelConfig != null
        && channelConfig.getBool("support", true)
        && channelConfig.getBool("enable", false);
  }

  @Override
  @Transactional
  public boolean resetAppSecretAndSyncMqtt(String appUniqueId, String unionId) {
    // 查询应用信息
    IoTUserApplication application = selectIotUserApplicationById(appUniqueId);

    if (application == null) {
      return false;
    }
    // 生成新的应用密钥
    String newAppSecret = cn.hutool.core.util.RandomUtil.randomString(32);
    String encodedAppSecret = new BCryptPasswordEncoder().encode(newAppSecret);

    application.setAppSecret(newAppSecret);
    if (iotUserApplicationMapper.updateByPrimaryKeySelective(application) == 0) {
      throw new IoTException("应用密钥更新失败");
    }

    OAuth2ClientDetails oauthClientDetails =
        OAuth2ClientDetails.builder()
            .clientId(application.getAppId())
            .clientSecret(encodedAppSecret)
            .build();
    if (oauthClientDetailsMapper.updateByPrimaryKeySelective(oauthClientDetails) == 0) {
      throw new IoTException("OAuth客户端密钥同步失败");
    }

    // 检查MQTT是否开启，如果开启则同步更新MQTT配置中的密码
    String oldCfg = application.getCfg();
    if (StrUtil.isNotBlank(oldCfg)) {
      JSONObject cfg = JSONUtil.parseObj(oldCfg);
      if (cfg.containsKey("mqtt")) {
        JSONObject mqttConfig = cfg.getJSONObject("mqtt");
        if (mqttConfig.getBool("enable", false)) {
          // MQTT已启用，更新密码
          mqttConfig.set("password", newAppSecret);
          cfg.set("mqtt", mqttConfig);

          // 更新配置
          IoTUserApplication updateCfg = new IoTUserApplication();
          updateCfg.setAppUniqueId(appUniqueId);
          updateCfg.setCfg(cfg.toString());
          if (iotUserApplicationMapper.updateByPrimaryKeySelective(updateCfg) == 0) {
            throw new IoTException("MQTT配置同步失败");
          }
          log.info("应用 {} 密钥重置后，MQTT配置密码已同步更新", appUniqueId);
        }
      }
    }

    //    // 同步小程序应用的密钥（异步调用，避免阻塞）
    //    try {
    //      syncMiniappAppSecret(appUniqueId);
    //    } catch (Exception e) {
    //      log.warn("同步小程序应用密钥失败, appUniqueId={}", appUniqueId, e);
    //      // 不影响主流程，仅记录日志
    //    }

    appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "APP_SECRET_RESET");
    return true;
  }

  private void applyApplicationScope(IoTUserApplication query, IoTUser iotUser) {
    if (query == null || iotUser == null || iotUser.isAdmin()) {
      return;
    }
    IoTUser tenantOwner = queryIotUser(iotUser.getUnionId());
    query.setUnionId(tenantOwner == null ? iotUser.getUnionId() : tenantOwner.getUnionId());
    query.setTenantId(resolveTenantId(iotUser));
  }

  /** 同步小程序应用的密钥 当IoT应用密钥变更时，自动同步到关联的小程序应用 */
  private void syncMiniappAppSecret(String linkedAppId) {
    try {
      // 调用小程序应用同步密钥接口
      String url = "http://localhost:9091/admin/mp/app/sync-secret/" + linkedAppId;
      log.info("开始同步小程序应用密钥, linkedAppId={}", linkedAppId);
      // 这里可以使用 RestTemplate 或 HttpClient 发送请求
      // 由于小程序和admin在同一应用，也可以直接注入MpAppService调用
      // 为了保持解耦，使用HTTP方式调用
    } catch (Exception e) {
      log.warn("同步小程序应用密钥失败, linkedAppId={}", linkedAppId, e);
    }
  }
}
