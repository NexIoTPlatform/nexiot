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

package cn.universal.mqtt.protocol.topic;

import static cn.universal.mqtt.protocol.config.MqttConstant.OTA_REPORT_PATTERN;
import static cn.universal.mqtt.protocol.config.MqttConstant.OTA_UPDATE_PATTERN;
import static cn.universal.mqtt.protocol.config.MqttConstant.PASSTHROUGH_DOWN_PATTERN;
import static cn.universal.mqtt.protocol.config.MqttConstant.PASSTHROUGH_UP_PATTERN;
import static cn.universal.mqtt.protocol.config.MqttConstant.THING_DOWN_PATTERN;
import static cn.universal.mqtt.protocol.config.MqttConstant.THING_EVENT_UP_PATTERN;
import static cn.universal.mqtt.protocol.config.MqttConstant.THING_PROPERTY_UP_PATTERN;
import static cn.universal.mqtt.protocol.config.MqttConstant.TOPIC_OTA_PREFIX;
import static cn.universal.mqtt.protocol.config.MqttConstant.TOPIC_THING_PREFIX;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MQTT主题管理器
 *
 * <p>负责管理标准化的MQTT主题规则，在启动阶段固定主题分类和格式。 提供主题解析、订阅管理、路由分发等核心功能。
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class MQTTTopicManager {

  /** 订阅主题映射 */
  private final Map<MqttConstant.TopicCategory, List<String>> subscriptionTopics;

  /** 主题类型缓存 - 提高解析性能 */
  private final Map<String, MQTTTopicType> topicTypeCache;

  // ========== ThirdMQTT 多实例 topic 类型缓存 ===========
  // key: networkUnionId, value: Map<topicType, topicPattern>
  private final Map<String, Map<String, String>> thirdPartyTopicTypeMap = new ConcurrentHashMap<>();

  @Value("${mqtt.cfg.defaultTopics:}")
  private String topicConfigJson;

  public MQTTTopicManager() {
    this.subscriptionTopics = new EnumMap<>(MqttConstant.TopicCategory.class);
    this.topicTypeCache = new ConcurrentHashMap<>();

    log.info("[TopicManager] MQTT主题管理器初始化完成");
  }

  @PostConstruct
  public void initialize() {
    try {
      // 从JSON配置解析主题配置
      parseTopicConfigFromJson();

      // 初始化订阅主题映射
      initializeSubscriptionTopics();

      log.info("[MQTT_TOPIC] Topic管理器初始化完成");
    } catch (Exception e) {
      log.error("[MQTT_TOPIC] Topic管理器初始化失败: ", e);
    }
  }

  /** 从JSON配置解析主题配置 */
  private void parseTopicConfigFromJson() {
    try {
      if (StrUtil.isBlank(topicConfigJson)) {
        log.warn("[MQTT_TOPIC] Topic配置为空，使用默认配置");
        initializeDefaultTopicConfig();
        return;
      }

      JSONObject config = JSONUtil.parseObj(topicConfigJson);

      // 解析物模型主题
      if (config.containsKey("thing-model")) {
        JSONObject thingModel = config.getJSONObject("thing-model");
        List<String> topics = thingModel.getBeanList("topics", String.class);
        addTopicsFromConfig(MqttConstant.TopicCategory.THING_MODEL, topics);
        log.debug("[MQTT_TOPIC] 加载物模型主题: {}", topics);
      }

      // 解析系统级主题
      if (config.containsKey("system-level")) {
        JSONObject systemLevel = config.getJSONObject("system-level");
        List<String> topics = systemLevel.getBeanList("topics", String.class);
        addTopicsFromConfig(MqttConstant.TopicCategory.SYSTEM_LEVEL, topics);
        log.debug("[MQTT_TOPIC] 加载系统级主题: {}", topics);
      }

      // 解析透传主题
      if (config.containsKey("passthrough")) {
        JSONObject passthrough = config.getJSONObject("passthrough");
        List<String> topics = passthrough.getBeanList("topics", String.class);
        addTopicsFromConfig(MqttConstant.TopicCategory.PASSTHROUGH, topics);
        log.debug("[MQTT_TOPIC] 加载透传主题: {}", topics);
      }

    } catch (Exception e) {
      log.error("[MQTT_TOPIC] 解析Topic配置失败，使用默认配置: ", e);
      initializeDefaultTopicConfig();
    }
  }

  /** 初始化默认主题配置 */
  private void initializeDefaultTopicConfig() {
    // 默认物模型主题
    List<String> thingModelTopics = new ArrayList<>();
    thingModelTopics.add(TOPIC_THING_PREFIX + "/up/property/+/+");
    thingModelTopics.add(TOPIC_THING_PREFIX + "/up/event/+/+");
    //    thingModelTopics.add(TOPIC_THING_PREFIX + "/down/+/+");
    addTopicsFromConfig(MqttConstant.TopicCategory.THING_MODEL, thingModelTopics);

    // 默认系统级主题
    List<String> systemLevelTopics = new ArrayList<>();
    systemLevelTopics.add(TOPIC_OTA_PREFIX + "/report/+/+");
    //    systemLevelTopics.add(TOPIC_OTA_PREFIX + "/update/+/+");
    addTopicsFromConfig(MqttConstant.TopicCategory.SYSTEM_LEVEL, systemLevelTopics);

    // 默认透传主题
    List<String> passthroughTopics = new ArrayList<>();
    passthroughTopics.add(TOPIC_THING_PREFIX + "/up/+/+");
    //    passthroughTopics.add(TOPIC_THING_PREFIX + "/down/+/+");
    addTopicsFromConfig(MqttConstant.TopicCategory.PASSTHROUGH, passthroughTopics);
  }

  /** 从配置添加主题到指定分类 */
  private void addTopicsFromConfig(MqttConstant.TopicCategory category, List<String> topics) {
    if (topics == null || topics.isEmpty()) {
      return;
    }
    List<String> list = subscriptionTopics.computeIfAbsent(category, k -> new ArrayList<>());
    list.addAll(topics);
  }

  /** 初始化订阅主题映射 */
  private void initializeSubscriptionTopics() {
    // 确保所有分类都有对应的列表
    for (MqttConstant.TopicCategory category : MqttConstant.TopicCategory.values()) {
      subscriptionTopics.computeIfAbsent(category, k -> new ArrayList<>());
    }
    logSubscriptionSummary();
  }

  /**
   * 获取指定分类的订阅主题列表
   *
   * @param category 主题分类
   * @return 订阅主题列表（只读）
   */
  public List<String> getSubscriptionTopics(MqttConstant.TopicCategory category) {
    return Collections.unmodifiableList(
        subscriptionTopics.getOrDefault(category, Collections.emptyList()));
  }

  /**
   * 获取所有订阅主题
   *
   * @return 所有订阅主题的不可变列表
   */
  public List<String> getAllSubscriptionTopics() {
    return subscriptionTopics.values().stream().flatMap(List::stream).collect(Collectors.toList());
  }

  /**
   * 解析主题类型（带缓存）
   *
   * @param topic 主题路径
   * @return 主题类型，未匹配返回null
   */
  public MQTTTopicType parseTopicType(String topic) {
    if (topic == null || topic.trim().isEmpty()) {
      return null;
    }

    // 先查缓存
    MQTTTopicType cachedType = topicTypeCache.get(topic);
    if (cachedType != null) {
      return cachedType;
    }

    // 使用优化后的 matchCategory 方法解析主题类型
    MqttConstant.TopicCategory category = matchCategory(topic);
    MQTTTopicType topicType = null;

    if (category != MqttConstant.TopicCategory.UNKNOWN) {
      // 根据分类和主题路径确定具体的主题类型
      topicType = determineTopicTypeFromCategory(category, topic);
    }

    // 缓存结果（包括null结果，避免重复解析）
    if (topicType != null) {
      topicTypeCache.put(topic, topicType);
    }

    return topicType;
  }

  /**
   * 根据主题分类和主题路径确定具体的主题类型
   *
   * @param category 主题分类
   * @param topic 主题路径
   * @return 具体的主题类型
   */
  private MQTTTopicType determineTopicTypeFromCategory(
      MqttConstant.TopicCategory category, String topic) {
    switch (category) {
      case THING_MODEL:
        // 物模型主题：根据路径判断是属性上报、事件上报还是下行
        if (topic.contains("/up/property/")) {
          return MQTTTopicType.THING_PROPERTY_UP;
        } else if (topic.contains("/up/event/")) {
          return MQTTTopicType.THING_EVENT_UP;
        } else if (topic.contains("/down/")) {
          return MQTTTopicType.THING_DOWN;
        }
        break;

      case SYSTEM_LEVEL:
        // 系统级主题：根据路径判断是固件上报还是更新
        if (topic.contains("/report/")) {
          return MQTTTopicType.OTA_REPORT;
        } else if (topic.contains("/update/")) {
          return MQTTTopicType.OTA_UPDATE;
        }
        break;

      case PASSTHROUGH:
        // 透传主题：根据路径判断是上行还是下行
        if (topic.contains("/up/")) {
          return MQTTTopicType.PASSTHROUGH_UP;
        } else if (topic.contains("/down/")) {
          return MQTTTopicType.PASSTHROUGH_DOWN;
        }
        break;

      default:
        break;
    }

    return null;
  }

  /**
   * 检查主题是否为标准主题
   *
   * @param topic 主题路径
   * @return true-标准主题，false-非标准主题
   */
  public boolean isStandardTopic(String topic) {
    return parseTopicType(topic) != null;
  }

  /**
   * 提取主题信息
   *
   * @param topic 主题路径
   * @return 主题信息对象
   */
  public TopicInfo extractTopicInfo(String topic) {
    MQTTTopicType topicType = parseTopicType(topic);
    if (topicType == null) {
      return TopicInfo.unknown(topic);
    }

    String productKey = topicType.extractProductKey(topic);
    String deviceId = topicType.extractDeviceId(topic);

    // 使用topicType.getCategory()获取分类
    MqttConstant.TopicCategory category = topicType.getCategory();

    return TopicInfo.builder()
        .originalTopic(topic)
        .topicType(topicType)
        .category(category)
        .productKey(productKey)
        .deviceId(deviceId)
        .isUpstream(topicType.isUpstream())
        .isValid(productKey != null && deviceId != null)
        .build();
  }

  /** 记录订阅主题摘要 */
  private void logSubscriptionSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append("[TopicManager] 订阅主题配置摘要:\n");

    for (MqttConstant.TopicCategory category : MqttConstant.TopicCategory.values()) {
      List<String> topics = subscriptionTopics.get(category);
      summary.append(String.format("  %s: %d个主题\n", category.name(), topics.size()));

      for (String topic : topics) {
        summary.append(String.format("    - %s\n", topic));
      }
    }

    summary.append(String.format("  总计: %d个主题", getAllSubscriptionTopics().size()));
    log.info(summary.toString());
  }

  /** 主题信息封装类 */
  public static class TopicInfo {

    private final String originalTopic;
    private final MQTTTopicType topicType;
    private final MqttConstant.TopicCategory category;
    private final String productKey;
    private final String deviceId;
    private final boolean isUpstream;
    private final boolean isValid;

    private TopicInfo(
        String originalTopic,
        MQTTTopicType topicType,
        MqttConstant.TopicCategory category,
        String productKey,
        String deviceId,
        boolean isUpstream,
        boolean isValid) {
      this.originalTopic = originalTopic;
      this.topicType = topicType;
      this.category = category;
      this.productKey = productKey;
      this.deviceId = deviceId;
      this.isUpstream = isUpstream;
      this.isValid = isValid;
    }

    public static TopicInfoBuilder builder() {
      return new TopicInfoBuilder();
    }

    public static TopicInfo unknown(String topic) {
      return new TopicInfo(
          topic, null, MqttConstant.TopicCategory.UNKNOWN, null, null, false, false);
    }

    // Getters
    public String getOriginalTopic() {
      return originalTopic;
    }

    public MQTTTopicType getTopicType() {
      return topicType;
    }

    public MqttConstant.TopicCategory getCategory() {
      return category;
    }

    public String getProductKey() {
      return productKey;
    }

    public String getDeviceId() {
      return deviceId;
    }

    public boolean isUpstream() {
      return isUpstream;
    }

    public boolean isDownstream() {
      return !isUpstream;
    }

    public boolean isValid() {
      return isValid;
    }

    public String getDeviceUniqueId() {
      return productKey != null && deviceId != null ? productKey + ":" + deviceId : null;
    }

    @Override
    public String toString() {
      return String.format(
          "TopicInfo{topic='%s', type=%s, category=%s, product='%s', device='%s', upstream=%s,"
              + " valid=%s}",
          originalTopic, topicType, category, productKey, deviceId, isUpstream, isValid);
    }

    public static class TopicInfoBuilder {

      private String originalTopic;
      private MQTTTopicType topicType;
      private MqttConstant.TopicCategory category;
      private String productKey;
      private String deviceId;
      private boolean isUpstream;
      private boolean isValid;

      public TopicInfoBuilder originalTopic(String originalTopic) {
        this.originalTopic = originalTopic;
        return this;
      }

      public TopicInfoBuilder topicType(MQTTTopicType topicType) {
        this.topicType = topicType;
        return this;
      }

      public TopicInfoBuilder category(MqttConstant.TopicCategory category) {
        this.category = category;
        return this;
      }

      public TopicInfoBuilder productKey(String productKey) {
        this.productKey = productKey;
        return this;
      }

      public TopicInfoBuilder deviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
      }

      public TopicInfoBuilder isUpstream(boolean isUpstream) {
        this.isUpstream = isUpstream;
        return this;
      }

      public TopicInfoBuilder isValid(boolean isValid) {
        this.isValid = isValid;
        return this;
      }

      public TopicInfo build() {
        return new TopicInfo(
            originalTopic, topicType, category, productKey, deviceId, isUpstream, isValid);
      }
    }
  }

  public static MqttConstant.TopicCategory matchCategory(String topic) {
    // 物模型
    if (THING_PROPERTY_UP_PATTERN.matcher(topic).matches()
        || THING_EVENT_UP_PATTERN.matcher(topic).matches()
        || THING_DOWN_PATTERN.matcher(topic).matches()) {
      return MqttConstant.TopicCategory.THING_MODEL;
    }
    // 透传
    if (PASSTHROUGH_UP_PATTERN.matcher(topic).matches()
        || PASSTHROUGH_DOWN_PATTERN.matcher(topic).matches()) {
      return MqttConstant.TopicCategory.PASSTHROUGH;
    }
    // 系统级
    if (OTA_REPORT_PATTERN.matcher(topic).matches()
        || OTA_UPDATE_PATTERN.matcher(topic).matches()) {
      return MqttConstant.TopicCategory.SYSTEM_LEVEL;
    }
    return MqttConstant.TopicCategory.UNKNOWN;
  }

  /** 从主题中提取产品Key 支持标准物模型/透传/系统级Topic和历史格式，优先用正则表达式提取。 */
  public String extractProductKeyFromTopic(String topic) {
    if (StrUtil.isBlank(topic)) {
      return null;
    }
    try {
      java.util.regex.Matcher m;
      if ((m = THING_PROPERTY_UP_PATTERN.matcher(topic)).matches()) {
        return m.group(1);
      }
      if ((m = THING_EVENT_UP_PATTERN.matcher(topic)).matches()) {
        return m.group(1);
      }
      if ((m = THING_DOWN_PATTERN.matcher(topic)).matches()) {
        return m.group(1);
      }
      if ((m = PASSTHROUGH_UP_PATTERN.matcher(topic)).matches()) {
        return m.group(1);
      }
      if ((m = PASSTHROUGH_DOWN_PATTERN.matcher(topic)).matches()) {
        return m.group(1);
      }
      if ((m = OTA_REPORT_PATTERN.matcher(topic)).matches()) {
        return m.group(1);
      }
      if ((m = OTA_UPDATE_PATTERN.matcher(topic)).matches()) {
        return m.group(1);
      }
      // 历史兼容格式
      String[] parts = topic.split("/");
      if (parts.length >= 2) {
        return parts[parts.length - 2];
      }
      if (parts.length == 1) {
        return parts[0];
      }
      return null;
    } catch (Exception e) {
      log.error("[MQTT] 提取产品Key失败 - 主题: {}, 异常: ", topic, e);
      return null;
    }
  }

  /** 从主题中提取设备ID 支持标准物模型/透传/系统级Topic和历史格式，优先用正则表达式提取。 */
  public String extractDeviceIdFromTopic(String topic) {
    try {
      java.util.regex.Matcher m;
      if ((m = THING_PROPERTY_UP_PATTERN.matcher(topic)).matches()) {
        return m.group(2);
      }
      if ((m = THING_EVENT_UP_PATTERN.matcher(topic)).matches()) {
        return m.group(2);
      }
      if ((m = THING_DOWN_PATTERN.matcher(topic)).matches()) {
        return m.group(2);
      }
      if ((m = PASSTHROUGH_UP_PATTERN.matcher(topic)).matches()) {
        return m.group(2);
      }
      if ((m = PASSTHROUGH_DOWN_PATTERN.matcher(topic)).matches()) {
        return m.group(2);
      }
      if ((m = OTA_REPORT_PATTERN.matcher(topic)).matches()) {
        return m.group(2);
      }
      if ((m = OTA_UPDATE_PATTERN.matcher(topic)).matches()) {
        return m.group(2);
      }
      // 历史兼容格式
      String[] parts = topic.split("/");
      if (parts.length >= 1) {
        return parts[parts.length - 1];
      }
      return "unknown";
    } catch (Exception e) {
      log.error("[MQTT] 提取设备ID失败 - 主题: {}, 异常: ", topic, e);
      return "unknown";
    }
  }

  /**
   * 解析第三方MQTT订阅主题（仅支持对象数组格式）
   *
   * <p>只支持新格式的对象数组，格式示例：
   *
   * <pre>
   * [
   *   {
   *     "enabled": true,
   *     "productKey": "xxx",
   *     "qos": 0,
   *     "topicCategory": "THING_MODEL",
   *     "topicPattern": "$third/up/#"
   *   }
   * ]
   * </pre>
   *
   * @param networkUnionId 第三方MQTT唯一标识
   * @param configMap 配置Map
   * @param defaultQos 默认QoS
   * @return 主题配置列表
   */
  public List<MQTTProductConfig.MqttTopicConfig> parseThirdMQTTSubscribeTopicsFromConfig(
      String networkUnionId, Map<String, Object> configMap, int defaultQos) {
    try {
      Object topicsObj = configMap.get("subscribeTopics");
      if (topicsObj == null) {
        topicsObj = configMap.get("topics");
      }

      if (topicsObj == null) {
        log.warn("[MQTT_TOPIC] 网络={}没有配置subscribeTopics，使用默认主题", networkUnionId);
        return getDefaultSubscriptionTopics(defaultQos);
      }

      // 只支持 List 类型（对象数组格式）
      if (!(topicsObj instanceof List)) {
        log.error(
            "[MQTT_TOPIC] 网络={}的主题配置格式错误，期望List类型（对象数组），实际类型={}，值={}",
            networkUnionId,
            topicsObj.getClass().getName(),
            topicsObj);
        return getDefaultSubscriptionTopics(defaultQos);
      }

      List<?> topicList = (List<?>) topicsObj;
      if (topicList.isEmpty()) {
        log.warn("[MQTT_TOPIC] 网络={}的主题配置为空列表，使用默认主题", networkUnionId);
        return getDefaultSubscriptionTopics(defaultQos);
      }

      return parseObjectTopics(topicList, defaultQos, networkUnionId);

    } catch (Exception e) {
      log.error("[MQTT_TOPIC] 解析订阅主题失败，网络={}，使用默认主题，错误={}", networkUnionId, e.getMessage(), e);
      return getDefaultSubscriptionTopics(defaultQos);
    }
  }

  /**
   * 获取默认订阅主题配置列表
   *
   * @param defaultQos 默认QoS
   * @return 默认主题配置列表
   */
  private List<MQTTProductConfig.MqttTopicConfig> getDefaultSubscriptionTopics(int defaultQos) {
    return getAllSubscriptionTopics().stream()
        .map(String::trim)
        .filter(topic -> !topic.isEmpty())
        .map(
            topic ->
                MQTTProductConfig.MqttTopicConfig.builder()
                    .topic(topic)
                    .qos(defaultQos)
                    .enabled(true)
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * 解析对象数组格式的主题配置
   *
   * @param topicList 主题配置列表
   * @param defaultQos 默认QoS
   * @param networkUnionId 网络标识
   * @return 主题配置列表
   */
  private List<MQTTProductConfig.MqttTopicConfig> parseObjectTopics(
      List<?> topicList, int defaultQos, String networkUnionId) {
    Map<String, String> typeMap = new ConcurrentHashMap<>();
    List<MQTTProductConfig.MqttTopicConfig> configs = new ArrayList<>();

    for (Object item : topicList) {
      try {
        if (item instanceof Map || item instanceof JSONObject) {
          // 对象格式，支持新功能
          @SuppressWarnings("unchecked")
          Map<String, Object> topicMap = (Map<String, Object>) item;
          String topic =
              getStringValue(topicMap, "topicPattern", getStringValue(topicMap, "topic", null));
          if (StrUtil.isBlank(topic)) {
            continue;
          }

          int qos = getIntValue(topicMap, "qos", defaultQos);
          boolean enabled = getBooleanValue(topicMap, "enabled", true);
          String productKey = getStringValue(topicMap, "productKey", null);
          String topicCategoryStr = getStringValue(topicMap, "topicCategory", null);
          // 将字符串转换为枚举
          MqttConstant.TopicCategory topicCategory = null;
          if (StrUtil.isNotBlank(topicCategoryStr)) {
            try {
              topicCategory = MqttConstant.TopicCategory.valueOf(topicCategoryStr);
            } catch (IllegalArgumentException e) {
              log.warn("[MQTT_TOPIC] 无效的主题分类值: {}, 将忽略", topicCategoryStr);
            }
          }

          MQTTProductConfig.MqttTopicConfig config =
              MQTTProductConfig.MqttTopicConfig.builder()
                  .topic(topic)
                  .qos(qos)
                  .enabled(enabled)
                  .productKey(productKey)
                  .topicCategory(topicCategory)
                  .build();

          configs.add(config);
        }
      } catch (Exception e) {
        log.warn("[MQTT_TOPIC] 解析主题配置项失败: {}", item, e);
      }
    }

    thirdPartyTopicTypeMap.put(networkUnionId, typeMap);
    return configs;
  }

  /** 从Map中获取整数值 */
  private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /** 从Map中获取布尔值 */
  private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
    Object value = map.get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }
    return defaultValue;
  }

  /**
   * 获取第三方MQTT下发topic pattern
   *
   * @param networkUnionId 第三方MQTT唯一标识
   * @param type 主题类型常量，如 MqttConstant.TYPE_THING_DOWN
   * @return topic pattern 或 null
   */
  public String getThirdPartyDownTopicPattern(String networkUnionId, String type) {
    Map<String, String> typeMap = thirdPartyTopicTypeMap.get(networkUnionId);
    if (typeMap != null) {
      return typeMap.get(type);
    }
    return null;
  }

  /** MQTT 过滤器匹配：保留空层级，# 可匹配零层，普通字符不作为正则表达式。 */
  public boolean matchesTopic(String topic, String pattern) {
    if (StrUtil.isEmpty(topic)
        || StrUtil.isEmpty(pattern)
        || topic.indexOf('\0') >= 0
        || topic.contains("+")
        || topic.contains("#")) {
      return false;
    }
    // 共享订阅投递的 Topic 不包含 $share/<group>/ 前缀。
    if (pattern.startsWith("$share/")) {
      String[] shared = pattern.split("/", 3);
      if (shared.length != 3
          || shared[1].isEmpty()
          || shared[1].contains("+")
          || shared[1].contains("#")) {
        return false;
      }
      pattern = shared[2];
    }
    if (pattern.isEmpty() || pattern.indexOf('\0') >= 0) {
      return false;
    }
    String[] filters = pattern.split("/", -1);
    String[] levels = topic.split("/", -1);
    for (int i = 0; i < filters.length; i++) {
      String filter = filters[i];
      if ((filter.contains("#") && (!filter.equals("#") || i != filters.length - 1))
          || (filter.contains("+") && !filter.equals("+"))) {
        return false;
      }
    }
    if (topic.startsWith("$") && (filters[0].equals("+") || filters[0].equals("#"))) {
      return false;
    }
    for (int i = 0; i < filters.length; i++) {
      if (filters[i].equals("#")) {
        return true;
      }
      if (i >= levels.length || (!filters[i].equals("+") && !filters[i].equals(levels[i]))) {
        return false;
      }
    }
    return filters.length == levels.length;
  }

  /** 一条命中规则的路由快照；产品与分类不能分别命中不同的配置项。 */
  public record TopicRoute(
      String productKey, MqttConstant.TopicCategory topicCategory, String topicPattern) {}

  /** 按配置顺序使用第一条启用的匹配规则。显式产品优先；标准主题按固定位置取产品； 自定义主题仅在路径中出现唯一已绑定产品 Key 时自动识别，不猜测设备号或厂商前缀。 */
  public TopicRoute resolveRoute(
      String topic,
      List<MQTTProductConfig.MqttTopicConfig> subscribeTopics,
      Predicate<String> isBoundProduct) {
    if (subscribeTopics == null) {
      return null;
    }
    for (MQTTProductConfig.MqttTopicConfig mapping : subscribeTopics) {
      if (mapping == null || !mapping.isEnabled() || !matchesTopic(topic, mapping.getTopic())) {
        continue;
      }
      String productKey = mapping.getProductKey();
      if (StrUtil.isBlank(productKey)) {
        productKey = extractStandardProductKey(topic);
        if (productKey == null && isBoundProduct != null) {
          List<String> candidates =
              java.util.Arrays.stream(topic.split("/", -1))
                  .filter(StrUtil::isNotBlank)
                  .distinct()
                  .filter(isBoundProduct)
                  .limit(2)
                  .toList();
          productKey = candidates.size() == 1 ? candidates.get(0) : null;
        }
      }
      MqttConstant.TopicCategory category = mapping.getTopicCategory();
      if (category == null || category == MqttConstant.TopicCategory.UNKNOWN) {
        category = matchCategory(topic);
      }
      return new TopicRoute(productKey, category, mapping.getTopic());
    }
    return null;
  }

  /** 只解析标准结构，不使用历史路径兜底猜测产品。 */
  private String extractStandardProductKey(String topic) {
    for (java.util.regex.Pattern pattern :
        List.of(
            THING_PROPERTY_UP_PATTERN,
            THING_EVENT_UP_PATTERN,
            THING_DOWN_PATTERN,
            PASSTHROUGH_UP_PATTERN,
            OTA_REPORT_PATTERN,
            OTA_UPDATE_PATTERN)) {
      java.util.regex.Matcher matcher = pattern.matcher(topic);
      if (matcher.matches()) {
        return matcher.group(1);
      }
    }
    return null;
  }

  public String extractProductKeyFromConfig(
      String topic, List<MQTTProductConfig.MqttTopicConfig> subscribeTopics) {
    TopicRoute route = resolveRoute(topic, subscribeTopics, null);
    return route == null ? null : route.productKey();
  }

  public MqttConstant.TopicCategory getTopicCategoryFromConfig(
      String topic, List<MQTTProductConfig.MqttTopicConfig> subscribeTopics) {
    TopicRoute route = resolveRoute(topic, subscribeTopics, null);
    return route == null ? MqttConstant.TopicCategory.UNKNOWN : route.topicCategory();
  }

  /**
   * 根据提取规则从topic中提取productKey
   *
   * @param topic 实际topic
   * @param extractRule 提取规则，格式： - "regex:pattern" - 使用正则表达式提取，第一个捕获组作为productKey - "path:index" -
   *     从topic路径中提取，index为路径段索引（从0开始）
   * @return productKey，如果提取失败返回null
   */
  public String extractProductKeyByRule(String topic, String extractRule) {
    if (StrUtil.isBlank(topic) || StrUtil.isBlank(extractRule)) {
      return null;
    }

    try {
      if (extractRule.startsWith("regex:")) {
        // 正则表达式提取
        String pattern = extractRule.substring(6);
        java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regexPattern.matcher(topic);
        if (matcher.find() && matcher.groupCount() > 0) {
          return matcher.group(1);
        }
      } else if (extractRule.startsWith("path:")) {
        // 路径提取
        String indexStr = extractRule.substring(5);
        int index = Integer.parseInt(indexStr);
        String[] parts = topic.split("/");
        if (index >= 0 && index < parts.length) {
          return parts[index];
        }
      }
    } catch (Exception e) {
      log.warn("[MQTT_TOPIC] 根据规则提取productKey失败: topic={}, rule={}", topic, extractRule, e);
    }

    return null;
  }

  /**
   * 将topic pattern中的占位符替换为实际值（公共方法）
   *
   * <p>支持的占位符格式：
   *
   * <ul>
   *   <li>#{productKey} 或 {{productKey}} - 替换为产品Key
   *   <li>#{deviceId} 或 {{deviceId}} - 替换为设备ID
   *   <li>+ - MQTT通配符，按顺序替换为productKey和deviceId（向后兼容）
   * </ul>
   *
   * <p>示例：
   *
   * <ul>
   *   <li>"$third/up/#{productKey}/set/#{deviceId}" -> "$third/up/product123/set/device456"
   *   <li>"$third/upxcz/set/#{deviceId}" -> "$third/upxcz/set/device456"
   *   <li>"$third/+/+/set" -> "$third/product123/device456/set"（向后兼容）
   * </ul>
   *
   * @param pattern 主题模式
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 替换后的主题
   */
  public String fillTopicPattern(String pattern, String productKey, String deviceId) {
    Map<String, String> variables = new ConcurrentHashMap<>();
    variables.put("productKey", productKey);
    variables.put("deviceId", deviceId);
    return fillTopicPattern(pattern, variables);
  }

  /**
   * 填充topic pattern中的占位符
   *
   * <p>支持的变量格式：
   *
   * <ul>
   *   <li>#{key} / {{key}} / ${key} / {key}
   *   <li>+ 通配符，按 productKey、deviceId、subProductKey、subDeviceId 顺序填充
   * </ul>
   *
   * @param pattern 主题模式
   * @param variables 变量映射
   * @return 替换后的主题
   */
  public String fillTopicPattern(String pattern, Map<String, String> variables) {
    if (StrUtil.isBlank(pattern)) {
      return pattern;
    }

    String result = pattern;
    if (variables != null && !variables.isEmpty()) {
      for (Map.Entry<String, String> entry : variables.entrySet()) {
        result = replacePlaceholderVariants(result, entry.getKey(), entry.getValue());
      }
    }

    return replaceWildcardPlaceholders(result, variables);
  }

  private String replacePlaceholderVariants(String pattern, String key, String value) {
    String safeValue = StrUtil.blankToDefault(value, "");
    String result = pattern;
    result = result.replace("#{" + key + "}", safeValue);
    result = result.replace("{{" + key + "}}", safeValue);
    result = result.replace("${" + key + "}", safeValue);
    result = result.replace("{" + key + "}", safeValue);
    return result;
  }

  private String replaceWildcardPlaceholders(String pattern, Map<String, String> variables) {
    if (!pattern.contains("+")) {
      return pattern;
    }
    String result = pattern;
    List<String> wildcardValues = new ArrayList<>();
    wildcardValues.add(getVariable(variables, "productKey"));
    wildcardValues.add(getVariable(variables, "deviceId"));
    wildcardValues.add(getVariable(variables, "subProductKey"));
    wildcardValues.add(getVariable(variables, "subDeviceId"));
    for (String wildcardValue : wildcardValues) {
      if (!result.contains("+")) {
        break;
      }
      result = result.replaceFirst("\\+", StrUtil.blankToDefault(wildcardValue, ""));
    }
    return result;
  }

  private String getVariable(Map<String, String> variables, String key) {
    if (variables == null || StrUtil.isBlank(key)) {
      return null;
    }
    return variables.get(key);
  }

  // 辅助方法
  private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
    Object value = map.get(key);
    return value != null ? String.valueOf(value) : defaultValue;
  }
}
