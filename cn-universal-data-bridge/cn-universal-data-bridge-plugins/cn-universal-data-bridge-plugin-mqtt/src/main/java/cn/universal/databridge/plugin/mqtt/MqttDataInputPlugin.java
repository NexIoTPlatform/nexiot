package cn.universal.databridge.plugin.mqtt;

import cn.hutool.core.util.IdUtil;
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
import org.eclipse.paho.mqttv5.client.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** MQTT数据输入插件 支持从MQTT订阅数据 */
@Component("mqttInput")
@ConditionalOnProperty(
    prefix = "databridge.plugins.mqtt-input",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class MqttDataInputPlugin extends AbstractDataInputPlugin {

  // TODO: 存储正在运行的MQTT客户端，用于管理连接生命周期
  // private final Map<Long, MqttClient> runningClients = new ConcurrentHashMap<>();

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("MQTT数据输入插件")
        .version("1.0.0")
        .description("支持从MQTT订阅数据，实现数据输入功能")
        .author("gitee.com/NexIoT")
        .pluginType("MQTT_INPUT")
        .supportedResourceTypes(List.of("MQTT"))
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
    try {
      String broker = String.format("tcp://%s:%d", connection.getHost(), connection.getPort());
      MqttClient client = new MqttClient(broker, IdUtil.objectId());
      MqttConnectionOptions options = buildOptions(connection);
      client.connect(options);
      client.disconnect();
      client.close();
      return true;
    } catch (Exception e) {
      log.error("MQTT连接测试失败: {}", e.getMessage(), e);
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
      log.error("验证MQTT输入配置失败: {}", e.getMessage(), e);
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

  private MqttConnectionOptions buildOptions(ResourceConnection connection) {
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setCleanStart(true);
    options.setConnectionTimeout(30);
    options.setKeepAliveInterval(60);

    if (StrUtil.isNotBlank(connection.getUsername())) {
      options.setUserName(connection.getUsername());
    }
    if (StrUtil.isNotBlank(connection.getPassword())) {
      options.setPassword(connection.getPassword().getBytes());
    }

    return options;
  }

  // TODO: 提取topic的方法，用于MQTT订阅
  // private String extractTopic(DataBridgeConfig config) {
  //     try {
  //         JSONObject configJson = JSONUtil.parseObj(config.getConfig());
  //         return configJson.getStr("topic", "/iot/input");
  //     } catch (Exception e) {
  //         log.warn("解析MQTT输入配置失败，使用默认topic: {}", e.getMessage());
  //         return "/iot/input";
  //     }
  // }
}
