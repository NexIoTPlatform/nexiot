package cn.universal.databridge.plugin.kafka;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataOutputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka数据桥接插件
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component("kafka")
@ConditionalOnProperty(
    prefix = "databridge.plugins.kafka",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class KafkaDataBridgePlugin extends AbstractDataOutputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("Kafka数据桥接插件")
        .version("1.0.0")
        .description("支持Kafka消息桥接")
        .author("gitee.com/NexIoT")
        .pluginType("KAFKA")
        .supportedResourceTypes(List.of("KAFKA"))
        .dataDirection(PluginInfo.DataDirection.BIDIRECTIONAL)
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
  protected void processProcessedData(
      Object processedData,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try (KafkaProducer<String, String> producer = createKafkaProducer(connection)) {
      String topic = extractTopic(config);

      String payload = renderProcessedPayload(processedData);

      ProducerRecord<String, String> record = new ProducerRecord<>(topic, payload);
      producer.send(
          record,
          (metadata, exception) -> {
            if (exception != null) {
              log.error("Kafka消息发送失败: {}", exception.getMessage(), exception);
            } else {
              log.debug(
                  "Kafka消息发送成功: topic={}, partition={}, offset={}",
                  metadata.topic(),
                  metadata.partition(),
                  metadata.offset());
            }
          });

    } catch (Exception e) {
      log.error("Kafka数据处理失败: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void processTemplateResult(
      String templateResult,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try (KafkaProducer<String, String> producer = createKafkaProducer(connection)) {
      String topic = extractTopic(config);

      ProducerRecord<String, String> record = new ProducerRecord<>(topic, templateResult);
      producer.send(
          record,
          (metadata, exception) -> {
            if (exception != null) {
              log.error("Kafka消息发送失败: {}", exception.getMessage(), exception);
            } else {
              log.debug(
                  "Kafka消息发送成功: topic={}, partition={}, offset={}",
                  metadata.topic(),
                  metadata.partition(),
                  metadata.offset());
            }
          });

    } catch (Exception e) {
      log.error("Kafka模板处理失败: {}", e.getMessage(), e);
      throw new RuntimeException(e);
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
    return StrUtil.isNotBlank(extractTopic(config)) && validateMagicScript(config.getMagicScript());
  }

  private KafkaProducer<String, String> createKafkaProducer(ResourceConnection connection) {
    Properties props = new Properties();
    props.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        String.format("%s:%d", connection.getHost(), connection.getPort()));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.ACKS_CONFIG, "1");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

    if (StrUtil.isNotBlank(connection.getUsername())) {
      props.put("security.protocol", "SASL_PLAINTEXT");
      props.put("sasl.mechanism", "PLAIN");
      props.put(
          "sasl.jaas.config",
          String.format(
              "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
              connection.getUsername(), connection.getPassword()));
    }

    return new KafkaProducer<>(props);
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

  private String extractTopic(DataBridgeConfig config) {
    // 从统一配置中提取topic
    if (StrUtil.isBlank(config.getConfig())) {
      return "iot-device-data"; // 默认topic
    }

    try {
      JSONObject configJson = JSONUtil.parseObj(config.getConfig());
      return configJson.getStr("topic", "iot-device-data");
    } catch (Exception e) {
      log.warn("解析Kafka配置失败，使用默认topic: {}", e.getMessage());
      return "iot-device-data";
    }
  }

  private Map<String, Object> buildTemplateVariables(
      BaseUPRequest request, DataBridgeConfig config) {
    Map<String, Object> variables = super.buildTemplateVariables(request, parseConfig(config));
    variables.put("timestamp", System.currentTimeMillis());
    variables.putIfAbsent("deviceKey", variables.get("deviceId"));
    return variables;
  }
}
