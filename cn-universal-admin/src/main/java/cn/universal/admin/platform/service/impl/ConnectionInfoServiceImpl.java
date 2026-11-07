package cn.universal.admin.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.config.ConnectionConfig;
import cn.universal.admin.platform.dto.ConnectionInfoDTO;
import cn.universal.admin.platform.service.ConnectionInfoService;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 连接信息服务实现 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionInfoServiceImpl implements ConnectionInfoService {

  private final IoTProductMapper productMapper;
  private final NetworkMapper networkMapper;
  private final ConnectionConfig connectionConfig;

  @Override
  public ConnectionInfoDTO getConnectionInfo(String productKey) {
    log.debug("获取产品连接信息: productKey={}", productKey);

    // 查询产品信息
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null) {
      throw new RuntimeException("产品不存在: " + productKey);
    }

    // 构建内置连接信息
    ConnectionInfoDTO.BuiltinConnection builtin = buildBuiltinConnection(product);

    // 构建网络组件连接信息
    ConnectionInfoDTO.NetworkConnection network = buildNetworkConnection(product);

    return ConnectionInfoDTO.builder().builtin(builtin).network(network).build();
  }

  @Override
  public ConnectionInfoDTO getMqttPasswordInfo(String productKey) {
    log.debug("获取产品MQTT密码信息: productKey={}", productKey);

    // 查询产品信息
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null) {
      throw new RuntimeException("产品不存在: " + productKey);
    }

    // 构建内置MQTT密码信息
    ConnectionInfoDTO.BuiltinConnection builtin = buildBuiltinMqttPassword(product);

    // 构建网络组件MQTT密码信息
    ConnectionInfoDTO.NetworkConnection network = buildNetworkMqttPassword(product);

    return ConnectionInfoDTO.builder().builtin(builtin).network(network).build();
  }

  /** 构建内置连接信息 */
  private ConnectionInfoDTO.BuiltinConnection buildBuiltinConnection(IoTProduct product) {
    String platform = product.getThirdPlatform();

    ConnectionInfoDTO.BuiltinConnection.BuiltinConnectionBuilder builder =
        ConnectionInfoDTO.BuiltinConnection.builder();

    switch (platform) {
      case "tcp":
        return builder
            .connectionType(connectionConfig.getTcpConnectionType())
            .host(connectionConfig.getTcpHost(product.getProductKey()))
            .port(connectionConfig.getTcpSniPort())
            .description(connectionConfig.getTcpDescription())
            .build();

      case "mqtt":
        ConnectionInfoDTO.BuiltinConnection builtinMqtt =
            builder
                .connectionType(connectionConfig.getMqttConnectionType())
                .host(connectionConfig.getMqttHost())
                .port(connectionConfig.extractPortFromHost(connectionConfig.getMqttHost()))
                .username(product.getProductKey())
                .password("点击查看密码")
                .description(connectionConfig.getMqttDescription())
                .topics(buildDefaultTopics(product.getProductKey()))
                .build();
        return builtinMqtt;

      case "udp":
        return builder
            .connectionType(connectionConfig.getUdpConnectionType())
            .host(connectionConfig.getTcpHost(product.getProductKey()))
            .port(connectionConfig.getUdpPort())
            .description(connectionConfig.getUdpDescription())
            .build();
      default:
        return builder.connectionType("请联系管理员").host("请联系管理员").port(0).description("暂无").build();
    }
  }

  /** 构建网络组件连接信息 */
  private ConnectionInfoDTO.NetworkConnection buildNetworkConnection(IoTProduct product) {
    if (StrUtil.isBlank(product.getNetworkUnionId())) {
      return ConnectionInfoDTO.NetworkConnection.builder()
          .enabled(false)
          .message("未绑定网络组件")
          .build();
    }

    // 查询网络组件
    Network network = Network.builder().unionId(product.getNetworkUnionId()).build();
    network = networkMapper.selectOne(network);
    if (network == null) {
      return ConnectionInfoDTO.NetworkConnection.builder()
          .enabled(false)
          .message("绑定的网络组件不存在")
          .build();
    }

    JSONObject config = JSONUtil.parseObj(network.getConfiguration());

    ConnectionInfoDTO.NetworkConnection.NetworkConnectionBuilder builder =
        ConnectionInfoDTO.NetworkConnection.builder()
            .enabled(true)
            .name(network.getName())
            .type(network.getType())
            .state(network.isStateEnabled())
            .host(config.getStr("host"))
            .port(config.getInt("port"))
            .username(config.getStr("username"))
            .password("点击查看密码");

    if ("tcp".equalsIgnoreCase(product.getThirdPlatform())) {
      builder.publicHost(connectionConfig.getTcpPublicHost());
    }

    // 处理MQTT主题信息
    if (isMqttNetwork(product, network)) {
      builder
          .clientId(config.getStr("clientId"))
          .clusterEnabled(config.getBool("clusterEnabled", false));
    }

    if (isMqttProduct(product)) {
      ConnectionInfoDTO.TopicInfo subscribeTopics = buildThirdMQTTSubscribeTopics(config, product);
      builder.subscribeTopics(subscribeTopics);
    }

    return builder.build();
  }

  /** 构建内置MQTT密码信息 */
  private ConnectionInfoDTO.BuiltinConnection buildBuiltinMqttPassword(IoTProduct product) {
    if (!"mqtt".equals(product.getThirdPlatform())) {
      return null;
    }

    return ConnectionInfoDTO.BuiltinConnection.builder()
        .connectionType(connectionConfig.getMqttConnectionType())
        .host(connectionConfig.getMqttHost())
        .port(connectionConfig.extractPortFromHost(connectionConfig.getMqttHost()))
        .username(product.getProductKey())
        .password(product.getProductSecret())
        .description(connectionConfig.getMqttDescription())
        .build();
  }

  /** 构建网络组件MQTT密码信息 */
  private ConnectionInfoDTO.NetworkConnection buildNetworkMqttPassword(IoTProduct product) {
    if (StrUtil.isBlank(product.getNetworkUnionId())) {
      return ConnectionInfoDTO.NetworkConnection.builder()
          .enabled(false)
          .message("未绑定网络组件")
          .build();
    }

    List<Network> networks = networkMapper.selectByUnionId(product.getNetworkUnionId());
    if (networks.isEmpty()) {
      return ConnectionInfoDTO.NetworkConnection.builder()
          .enabled(false)
          .message("绑定的网络组件不存在")
          .build();
    }

    Network network = networks.get(0);
    JSONObject config = JSONUtil.parseObj(network.getConfiguration());

    ConnectionInfoDTO.NetworkConnection.NetworkConnectionBuilder builder =
        ConnectionInfoDTO.NetworkConnection.builder()
            .enabled(true)
            .name(network.getName())
            .type(network.getType())
            .host(config.getStr("host"))
            .port(config.getInt("port"))
            .username(config.getStr("username"))
            .password(config.getStr("password"));

    if (isMqttNetwork(product, network)) {
      builder
          .clientId(config.getStr("clientId"))
          .clusterEnabled(config.getBool("clusterEnabled", false));
    }

    return builder.build();
  }

  private boolean isMqttNetwork(IoTProduct product, Network network) {
    return isMqttProduct(product)
        || (network != null && StrUtil.containsIgnoreCase(network.getType(), "MQTT"));
  }

  private boolean isMqttProduct(IoTProduct product) {
    return product != null && "mqtt".equalsIgnoreCase(product.getThirdPlatform());
  }

  /** 构建默认主题信息 */
  private ConnectionInfoDTO.TopicInfo buildDefaultTopics(String productKey) {
    ConnectionInfoDTO.ThingTopics thingTopics =
        ConnectionInfoDTO.ThingTopics.builder()
            .propertyUp(replaceProductKey(connectionConfig.getMqttThingPropertyUp(), productKey))
            .eventUp(replaceProductKey(connectionConfig.getMqttThingEventUp(), productKey))
            .commandDown(replaceProductKey(connectionConfig.getMqttThingCommandDown(), productKey))
            .build();

    ConnectionInfoDTO.PassthroughTopics passthroughTopics =
        ConnectionInfoDTO.PassthroughTopics.builder()
            .dataUp(replaceProductKey(connectionConfig.getMqttPassthroughDataUp(), productKey))
            .commandDown(
                replaceProductKey(connectionConfig.getMqttPassthroughCommandDown(), productKey))
            .build();

    return ConnectionInfoDTO.TopicInfo.builder()
        .thingTopics(thingTopics)
        .passthroughTopics(passthroughTopics)
        .build();
  }

  /** 构建订阅主题信息 */
  private ConnectionInfoDTO.TopicInfo buildThirdMQTTSubscribeTopics(
      JSONObject config, IoTProduct product) {
    String productKey = product.getProductKey();
    Object subscribeTopicsObj = config.get("subscribeTopics");

    if (subscribeTopicsObj == null) {
      return buildDefaultTopics(productKey);
    }

    // 检测是否为数组格式（第三方MQTT新格式）
    if (subscribeTopicsObj instanceof List || subscribeTopicsObj instanceof JSONArray) {
      return buildThirdPartyTopics(subscribeTopicsObj, product);
    }

    // 检测是否为对象格式（旧格式）
    if (subscribeTopicsObj instanceof JSONObject) {
      JSONObject subscribeTopics = (JSONObject) subscribeTopicsObj;
      if (subscribeTopics.isEmpty()) {
        return buildDefaultTopics(productKey);
      }

      ConnectionInfoDTO.ThingTopics thingTopics = null;
      ConnectionInfoDTO.PassthroughTopics passthroughTopics = null;

      JSONObject thingTopicsConfig = subscribeTopics.getJSONObject("thingTopics");
      if (thingTopicsConfig != null) {
        thingTopics =
            ConnectionInfoDTO.ThingTopics.builder()
                .propertyUp(thingTopicsConfig.getStr("propertyUp"))
                .eventUp(thingTopicsConfig.getStr("eventUp"))
                .commandDown(thingTopicsConfig.getStr("commandDown"))
                .build();
      }

      JSONObject passthroughTopicsConfig = subscribeTopics.getJSONObject("passthroughTopics");
      if (passthroughTopicsConfig != null) {
        passthroughTopics =
            ConnectionInfoDTO.PassthroughTopics.builder()
                .dataUp(passthroughTopicsConfig.getStr("dataUp"))
                .commandDown(passthroughTopicsConfig.getStr("commandDown"))
                .build();
      }

      return ConnectionInfoDTO.TopicInfo.builder()
          .thingTopics(thingTopics)
          .passthroughTopics(passthroughTopics)
          .build();
    }

    // 其他情况，回退到默认主题
    return buildDefaultTopics(productKey);
  }

  /**
   * 构建第三方MQTT主题信息（数组格式）
   *
   * @param subscribeTopicsObj 订阅主题配置（数组格式）
   * @param product 产品对象，用于过滤匹配的主题和获取下行主题配置
   * @return 主题信息
   */
  @SuppressWarnings("unchecked")
  private ConnectionInfoDTO.TopicInfo buildThirdPartyTopics(
      Object subscribeTopicsObj, IoTProduct product) {
    String productKey = product.getProductKey();
    List<Object> topicList;

    // 转换为List
    if (subscribeTopicsObj instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) subscribeTopicsObj;
      topicList = jsonArray.toList(Object.class);
    } else if (subscribeTopicsObj instanceof List) {
      topicList = (List<Object>) subscribeTopicsObj;
    } else {
      return buildDefaultTopics(productKey);
    }

    if (topicList == null || topicList.isEmpty()) {
      return buildDefaultTopics(productKey);
    }

    // 使用List收集相同类型的主题，最后用逗号连接
    List<String> propertyUpList = new java.util.ArrayList<>();
    List<String> eventUpList = new java.util.ArrayList<>();
    List<String> thingCommandDownList = new java.util.ArrayList<>();
    List<String> passthroughDataUpList = new java.util.ArrayList<>();
    List<String> passthroughCommandDownList = new java.util.ArrayList<>();

    boolean hasThingTopics = false;
    boolean hasPassthroughTopics = false;

    // 遍历主题配置，过滤出与当前productKey匹配的主题
    for (Object item : topicList) {
      if (!(item instanceof JSONObject)) {
        continue;
      }

      JSONObject topicConfig = (JSONObject) item;

      // 检查是否启用
      if (!topicConfig.getBool("enabled", true)) {
        continue;
      }

      // 检查productKey匹配（如果配置的productKey为空，视为通用主题）
      String configProductKey = topicConfig.getStr("productKey");
      if (StrUtil.isNotBlank(configProductKey) && !configProductKey.equals(productKey)) {
        continue; // 不匹配，跳过
      }

      String topicPattern = topicConfig.getStr("topicPattern");
      if (StrUtil.isBlank(topicPattern)) {
        continue;
      }

      // 根据topicCategory分类
      String topicCategory = topicConfig.getStr("topicCategory");

      if ("THING_MODEL".equals(topicCategory)) {
        // 物模型主题：根据topic pattern判断是propertyUp、eventUp还是commandDown
        String topicType = inferThingTopicType(topicPattern);
        switch (topicType) {
          case "propertyUp":
            propertyUpList.add(topicPattern);
            hasThingTopics = true;
            break;
          case "eventUp":
            eventUpList.add(topicPattern);
            hasThingTopics = true;
            break;
          case "commandDown":
            thingCommandDownList.add(topicPattern);
            hasThingTopics = true;
            break;
          default:
            // 如果无法判断，默认作为propertyUp
            propertyUpList.add(topicPattern);
            hasThingTopics = true;
            break;
        }
      } else if ("PASSTHROUGH".equals(topicCategory)) {
        // 透传主题：根据topic pattern判断是dataUp还是commandDown
        String topicType = inferPassthroughTopicType(topicPattern);
        switch (topicType) {
          case "dataUp":
            passthroughDataUpList.add(topicPattern);
            hasPassthroughTopics = true;
            break;
          case "commandDown":
            passthroughCommandDownList.add(topicPattern);
            hasPassthroughTopics = true;
            break;
          default:
            // 如果无法判断，默认作为dataUp
            passthroughDataUpList.add(topicPattern);
            hasPassthroughTopics = true;
            break;
        }
      } else {
        // 未配置topicCategory，尝试自动判断
        // 如果包含/down/，可能是下行主题；否则默认为上行主题
        if (topicPattern.contains("/down/") || topicPattern.contains("/down")) {
          // 无法确定是物模型还是透传，优先作为物模型的commandDown
          thingCommandDownList.add(topicPattern);
          hasThingTopics = true;
        } else {
          // 上行主题，无法确定类型，优先作为透传的dataUp
          passthroughDataUpList.add(topicPattern);
          hasPassthroughTopics = true;
        }
      }
    }

    // 构建物模型主题
    ConnectionInfoDTO.ThingTopics.ThingTopicsBuilder thingTopicsBuilder =
        ConnectionInfoDTO.ThingTopics.builder();

    if (!propertyUpList.isEmpty()) {
      thingTopicsBuilder.propertyUp(String.join(",", propertyUpList));
    }
    if (!eventUpList.isEmpty()) {
      thingTopicsBuilder.eventUp(String.join(",", eventUpList));
    }

    // 处理下行主题：优先从产品配置中读取downTopic
    String commandDownTopic = getProductDownTopic(product);
    if (StrUtil.isNotBlank(commandDownTopic)) {
      // 产品配置中有downTopic，直接使用
      thingTopicsBuilder.commandDown(commandDownTopic);
    } else if (!thingCommandDownList.isEmpty()) {
      // 如果产品配置中没有，使用收集到的下行主题（用逗号连接）
      thingTopicsBuilder.commandDown(String.join(",", thingCommandDownList));
    } else {
      // 如果都没有，使用默认的物模型下行主题格式（与BaseReplyProcessor中的规则一致）
      // 格式：$thing/down/${productKey}/${deviceId}
      String defaultDownTopic = "$thing/down/${productKey}/${deviceId}";
      thingTopicsBuilder.commandDown(defaultDownTopic);
    }

    // 构建透传主题
    ConnectionInfoDTO.PassthroughTopics.PassthroughTopicsBuilder passthroughTopicsBuilder =
        ConnectionInfoDTO.PassthroughTopics.builder();

    if (!passthroughDataUpList.isEmpty()) {
      passthroughTopicsBuilder.dataUp(String.join(",", passthroughDataUpList));
    }
    if (!passthroughCommandDownList.isEmpty()) {
      passthroughTopicsBuilder.commandDown(String.join(",", passthroughCommandDownList));
    }

    ConnectionInfoDTO.TopicInfo.TopicInfoBuilder builder = ConnectionInfoDTO.TopicInfo.builder();

    if (hasThingTopics) {
      builder.thingTopics(thingTopicsBuilder.build());
    }

    if (hasPassthroughTopics) {
      builder.passthroughTopics(passthroughTopicsBuilder.build());
    }

    return builder.build();
  }

  /**
   * 从产品配置中获取下行主题 如果产品配置中没有downTopic，返回null，使用默认规则
   *
   * @param product 产品对象
   * @return 下行主题，如果不存在返回null
   */
  private String getProductDownTopic(IoTProduct product) {
    if (product == null || StrUtil.isBlank(product.getConfiguration())) {
      return null;
    }

    try {
      JSONObject config = JSONUtil.parseObj(product.getConfiguration());
      String downTopic = config.getStr("downTopic");
      return StrUtil.isNotBlank(downTopic) ? downTopic : null;
    } catch (Exception e) {
      log.warn("解析产品配置失败，productKey={}", product.getProductKey(), e);
      return null;
    }
  }

  /** 推断物模型主题类型（propertyUp、eventUp、commandDown） */
  private String inferThingTopicType(String topicPattern) {
    if (StrUtil.isBlank(topicPattern)) {
      return "propertyUp";
    }

    String lowerPattern = topicPattern.toLowerCase();

    // 判断是否为下行主题
    if (lowerPattern.contains("/down/")
        || lowerPattern.contains("/down")
        || lowerPattern.contains("/set/")
        || lowerPattern.contains("/set")) {
      return "commandDown";
    }

    // 判断是否为事件主题
    if (lowerPattern.contains("/event/") || lowerPattern.contains("/event")) {
      return "eventUp";
    }

    // 判断是否为属性主题
    if (lowerPattern.contains("/property/") || lowerPattern.contains("/property")) {
      return "propertyUp";
    }

    // 默认作为属性上报
    return "propertyUp";
  }

  /** 推断透传主题类型（dataUp、commandDown） */
  private String inferPassthroughTopicType(String topicPattern) {
    if (StrUtil.isBlank(topicPattern)) {
      return "dataUp";
    }

    String lowerPattern = topicPattern.toLowerCase();

    // 判断是否为下行主题
    if (lowerPattern.contains("/down/")
        || lowerPattern.contains("/down")
        || lowerPattern.contains("/set/")
        || lowerPattern.contains("/set")
        || lowerPattern.contains("/command/")
        || lowerPattern.contains("/command")) {
      return "commandDown";
    }

    // 默认作为数据上报
    return "dataUp";
  }

  /** 替换主题中的产品Key占位符 */
  private String replaceProductKey(String topic, String productKey) {
    if (StrUtil.isBlank(topic)) {
      return topic;
    }
    // 支持两种格式：{productKey} 和 ${productKey}
    return topic
        .replace("{productKey}", productKey)
        .replace("${productKey}", productKey)
        .replace("{deviceId}", "${deviceId}")
        .replace("${deviceId}", "${deviceId}");
  }
}
