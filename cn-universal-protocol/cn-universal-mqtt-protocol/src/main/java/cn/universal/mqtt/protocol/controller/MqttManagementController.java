package cn.universal.mqtt.protocol.controller;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTPublishMessage;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MQTT客户端管理控制器
 *
 * <p>专门负责MQTT客户端的动态管理功能，包括： - 客户端启动/停止/重启 - 配置重新加载 - 消息发布 - 客户端状态查询
 *
 * @version 3.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@RestController
@RequestMapping("/monitor/mqtt/v2/management")
public class MqttManagementController {

  @Autowired private IoTProductDeviceService ioTProductDeviceService;

  @Autowired private ThirdMQTTServerManager mqttServerManager;

  // ==================== 客户端状态查询 ====================

  /** 获取所有MQTT客户端状态 */
  @GetMapping("/clients/status")
  public ResponseEntity<Map<String, Object>> getAllClientsStatus() {
    try {
      Map<String, String> status = mqttServerManager.getAllClientStatus();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取客户端状态成功");
      result.put("data", status);
      result.put("total", status.size());
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 获取客户端状态失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取客户端状态失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 获取指定产品的客户端状态 */
  @GetMapping("/clients/{unionId}/status")
  public ResponseEntity<Map<String, Object>> getClientStatus(@PathVariable String networkUnionId) {
    try {
      Map<String, String> allStatus = mqttServerManager.getAllClientStatus();
      String status = allStatus.get(networkUnionId);

      Map<String, Object> result = new HashMap<>();
      if (status != null) {
        result.put("success", true);
        result.put("message", "获取客户端状态成功");
        result.put("data", status);
        result.put("networkUnionId", networkUnionId);
      } else {
        result.put("success", false);
        result.put("message", "客户端不存在");
        result.put("networkUnionId", networkUnionId);
      }
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 获取客户端状态失败: networkUnionId={}", networkUnionId, e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取客户端状态失败: " + e.getMessage());
      result.put("networkUnionId", networkUnionId);

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 客户端管理操作 ====================

  /** 启动MQTT客户端 */
  @PostMapping("/clients/{networkUnionId}/start")
  public ResponseEntity<Map<String, Object>> startClient(@PathVariable String networkUnionId) {
    try {
      boolean success = mqttServerManager.startMqttClient(networkUnionId);

      Map<String, Object> result = new HashMap<>();
      result.put("success", success);
      result.put("message", success ? "客户端启动成功" : "客户端启动失败");
      result.put("networkUnionId", networkUnionId);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 启动客户端失败: networkUnionId={}", networkUnionId, e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "启动客户端失败: " + e.getMessage());
      result.put("productKey", networkUnionId);

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 停止MQTT客户端 */
  @PostMapping("/clients/{networkUnionId}/stop")
  public ResponseEntity<Map<String, Object>> stopClient(@PathVariable String networkUnionId) {
    try {
      boolean success = mqttServerManager.stopMqttClient(networkUnionId);

      Map<String, Object> result = new HashMap<>();
      result.put("success", success);
      result.put("message", success ? "客户端停止成功" : "客户端停止失败");
      result.put("networkUnionId", networkUnionId);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 停止客户端失败: networkUnionId={}", networkUnionId, e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "停止客户端失败: " + e.getMessage());
      result.put("networkUnionId", networkUnionId);

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 重启MQTT客户端 */
  @PostMapping("/clients/{networkUnionId}/restart")
  public ResponseEntity<Map<String, Object>> restartClient(@PathVariable String networkUnionId) {
    try {
      boolean success = mqttServerManager.restartMqttClient(networkUnionId);

      Map<String, Object> result = new HashMap<>();
      result.put("success", success);
      result.put("message", success ? "客户端重启成功" : "客户端重启失败");
      result.put("networkUnionId", networkUnionId);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 重启客户端失败: productKey={}", networkUnionId, e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "重启客户端失败: " + e.getMessage());
      result.put("networkUnionId", networkUnionId);

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 配置管理 ====================

  /** 重新加载所有配置 */
  @PostMapping("/config/reload")
  public ResponseEntity<Map<String, Object>> reloadAllConfigs() {
    try {
      mqttServerManager.reloadAllConfigs();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "配置重新加载成功");
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 重新加载配置失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "重新加载配置失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 消息发布 ====================

  /** 发布消息到指定主题 */
  @PostMapping("/clients/{productKey}/publish")
  public R<?> publishMessage(
      @PathVariable String productKey,
      @RequestParam String topic,
      @RequestParam String message,
      @RequestParam(defaultValue = "1") int qos,
      @RequestParam(defaultValue = "false") boolean retained) {

    try {
      String networkUnionId = ioTProductDeviceService.selectNetworkUnionId(productKey);
      if (StrUtil.isBlank(networkUnionId)) {
        return R.error("没有绑定网络");
      }
      // 构建发布消息对象
      MQTTPublishMessage publishMessage =
          MQTTPublishMessage.builder()
              .topic(topic)
              .payload(StrUtil.bytes(message))
              .qos(qos)
              .retained(retained)
              .productKey(productKey)
              .messageType("MANAGEMENT_PUBLISH")
              .build();

      boolean success = mqttServerManager.publishMessage(networkUnionId, publishMessage);

      Map<String, Object> result = new HashMap<>();
      result.put("success", success);
      result.put("message", success ? "消息发布成功" : "消息发布失败");
      result.put("productKey", productKey);
      result.put("topic", topic);
      result.put("messageLength", message.length());
      result.put("qos", qos);
      result.put("retained", retained);
      result.put("timestamp", System.currentTimeMillis());

      return R.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 发布消息失败: productKey={}, topic={}", productKey, topic, e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "发布消息失败: " + e.getMessage());
      result.put("productKey", productKey);
      result.put("topic", topic);

      return R.error("发布消息失败", result);
    }
  }

  // ==================== 系统管理 ====================

  /** 获取服务器管理信息 */
  @GetMapping("/server/info")
  public ResponseEntity<Map<String, Object>> getServerInfo() {
    try {
      String statistics = mqttServerManager.getStatistics();
      int activeConnections = mqttServerManager.getActiveConnectionCount();

      Map<String, Object> serverInfo = new HashMap<>();
      serverInfo.put("statistics", statistics);
      serverInfo.put("activeConnections", activeConnections);
      serverInfo.put("serverStatus", "RUNNING");

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取服务器信息成功");
      result.put("data", serverInfo);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 获取服务器信息失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取服务器信息失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 批量操作 - 启动所有客户端 */
  @PostMapping("/clients/start-all")
  public ResponseEntity<Map<String, Object>> startAllClients() {
    try {
      mqttServerManager.loadAndStartAllClients();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "所有客户端启动操作已执行");
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_MGMT] 启动所有客户端失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "启动所有客户端失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }
}
