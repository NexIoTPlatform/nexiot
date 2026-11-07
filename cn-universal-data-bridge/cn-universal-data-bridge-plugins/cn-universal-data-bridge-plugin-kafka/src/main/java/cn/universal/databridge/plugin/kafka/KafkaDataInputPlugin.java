package cn.universal.databridge.plugin.kafka;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataInputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Kafka数据输入插件 支持从Kafka消费数据 */
@Component("kafkaInput")
@ConditionalOnProperty(
    prefix = "databridge.plugins.kafka-input",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class KafkaDataInputPlugin extends AbstractDataInputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("Kafka数据输入插件")
        .version("1.0.0")
        .description("支持从Kafka消费数据，实现数据输入功能")
        .author("gitee.com/NexIoT")
        .pluginType("KAFKA_INPUT")
        .supportedResourceTypes(List.of("KAFKA"))
        .dataDirection(PluginInfo.DataDirection.INPUT)
        .category("消息队列")
        .icon("message")
        .build();
  }

  @Override
  public List<SourceScope> getSupportedSourceScopes() {
    return List.of(
        SourceScope.ALL_PRODUCTS, SourceScope.SPECIFIC_PRODUCTS, SourceScope.APPLICATION);
  }

  @Override
  protected void sendToIoTPlatform(Object processedData, DataBridgeConfig config) {
    // TODO: 实现推送到IoT平台的逻辑
    // 这里可以通过消息队列、HTTP API等方式将数据推送到IoT平台
    log.debug("推送数据到IoT平台: 配置ID={}", config.getId());
  }

  @Override
  protected void processExternalDataWithDefaultLogic(
      Object externalData, DataBridgeConfig config, ResourceConnection connection) {
    try {
      // 处理外部数据，转换为IoT平台格式
      BaseUPRequest request = transformToIoTData(externalData, config);
      if (request != null) {
        sendToIoTPlatform(request, config);
      }
    } catch (Exception e) {
      log.error("处理外部数据失败: {}", e.getMessage(), e);
    }
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try (AdminClient adminClient = createAdminClient(connection)) {
      // 尝试列出topics来测试连接
      adminClient.listTopics().names().get();
      return true;
    } catch (Exception e) {
      log.error("Kafka连接测试失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Boolean validateConfig(DataBridgeConfig config) {
    // 验证统一配置
    if (StrUtil.isBlank(config.getConfig())) {
      return false;
    }

    try {
      JSONObject configJson = JSONUtil.parseObj(config.getConfig());
      // 验证必要的配置项
      return configJson.containsKey("topic") && StrUtil.isNotBlank(configJson.getStr("topic"));
    } catch (Exception e) {
      log.error("验证Kafka输入配置失败: {}", e.getMessage(), e);
      return false;
    }
  }

  private BaseUPRequest transformToIoTData(Object externalData, DataBridgeConfig config) {
    try {
      if (externalData instanceof String) {
        String payload = (String) externalData;
        JSONObject data = JSONUtil.parseObj(payload);

        // 构建BaseUPRequest
        BaseUPRequest request = new BaseUPRequest();
        request.setMessageType(MessageType.PROPERTIES);
        request.setProperties(new HashMap<>());

        // 设置设备信息
        IoTDeviceDTO deviceDTO = new IoTDeviceDTO();
        deviceDTO.setDeviceId(data.getStr("deviceId", "unknown"));
        deviceDTO.setProductKey(data.getStr("productKey", "unknown"));
        request.setIoTDeviceDTO(deviceDTO);

        // 设置属性
        if (data.containsKey("properties")) {
          Object properties = data.get("properties");
          if (properties instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) properties;
            request.setProperties(props);
          }
        }

        return request;
      }
    } catch (Exception e) {
      log.error("转换外部数据为IoT格式失败: {}", e.getMessage(), e);
    }
    return null;
  }

  private AdminClient createAdminClient(ResourceConnection connection) {
    Properties props = new Properties();
    props.put(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
        String.format("%s:%d", connection.getHost(), connection.getPort()));
    props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

    if (StrUtil.isNotBlank(connection.getUsername())) {
      props.put("security.protocol", "SASL_PLAINTEXT");
      props.put("sasl.mechanism", "PLAIN");
      props.put(
          "sasl.jaas.config",
          String.format(
              "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
              connection.getUsername(), connection.getPassword()));
    }

    return AdminClient.create(props);
  }

  // TODO: Kafka消费者创建方法，用于数据输入
  // private KafkaConsumer<String, String> createKafkaConsumer(ResourceConnection connection, String
  // topic) {
  //     Properties props = new Properties();
  //     props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
  //         String.format("%s:%d", connection.getHost(), connection.getPort()));
  //     props.put(ConsumerConfig.GROUP_ID_CONFIG, "databridge-input-group");
  //     props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
  // StringDeserializer.class.getName());
  //     props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
  // StringDeserializer.class.getName());
  //     props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
  //     props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
  //     props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
  //
  //     if (StrUtil.isNotBlank(connection.getUsername())) {
  //         props.put("security.protocol", "SASL_PLAINTEXT");
  //         props.put("sasl.mechanism", "PLAIN");
  //         props.put("sasl.jaas.config",
  //             String.format("org.apache.kafka.common.security.plain.PlainLoginModule required
  // username=\"%s\" password=\"%s\";",
  //                 connection.getUsername(), connection.getPassword()));
  //     }
  //
  //     KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
  //     consumer.subscribe(Collections.singletonList(topic));
  //     return consumer;
  // }

  // TODO: 提取topic的方法，用于Kafka订阅
  // private String extractTopic(DataBridgeConfig config) {
  //     try {
  //         JSONObject configJson = JSONUtil.parseObj(config.getConfig());
  //         return configJson.getStr("topic", "iot-input-data");
  //     } catch (Exception e) {
  //         log.warn("解析Kafka输入配置失败，使用默认topic: {}", e.getMessage());
  //         return "iot-input-data";
  //     }
  // }
}
