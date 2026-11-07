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

package cn.universal.mqtt.protocol.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTPublishMessage;
import cn.universal.mqtt.protocol.system.SysMQTTManager;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * MQTT Mock数据服务
 *
 * <p>提供Mock数据自动发送功能，用于测试环境
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Service
@Profile({"test2"})
public class MqttMockDataService {

  @Autowired private ThirdMQTTServerManager thirdMQTTServerManager;

  @Autowired private SysMQTTManager sysMQTTManager;

  @Autowired private IoTProductDeviceService ioTProductDeviceService;

  @Value("${mock.size:30}")
  private int sendSize;

  // Mock数据发送状态控制
  private volatile boolean mockDataSending = false;
  private volatile Thread mockDataThread = null;

  // Mock数据配置
  private static final String PRODUCT_KEY = "681c0775c2dc427d0480ab5f";
  private static final String DEVICE_ID_PREFIX = "ov0001411";
  private static final String TOPIC_PREFIX = "$thing/up/property/";

  public R<?> startMockDataSending() {
    if (mockDataSending) {
      return R.error("Mock数据发送已在运行中");
    }

    mockDataSending = true;

    // 使用虚拟线程替代传统线程
    mockDataThread =
        Thread.startVirtualThread(
            () -> {
              log.debug("[MQTT_MOCK] 开始自动发送Mock数据，每秒发送10条");

              while (mockDataSending && !Thread.currentThread().isInterrupted()) {
                try {
                  long startTime = System.currentTimeMillis();
                  // 每秒发送30条数据
                  for (int i = 0; i < sendSize; i++) {
                    if (!mockDataSending || Thread.currentThread().isInterrupted()) {
                      break;
                    }
                    // 发送单条Mock数据
                    sendSingleMockDataInternal();
                    //                    TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(300,
                    // 800));
                  }
                  long endTime = System.currentTimeMillis();
                  long elapsedTime = endTime - startTime;

                  // 计算需要休眠的时间，确保每秒发送10条
                  if (elapsedTime < 1000) {
                    Thread.sleep(1000 - elapsedTime);
                  }
                } catch (InterruptedException e) {
                  log.debug("[MQTT_MOCK] Mock数据发送线程被中断");
                  break;
                } catch (Exception e) {
                  log.error("[MQTT_MOCK] 自动发送Mock数据异常: ", e);
                }
              }

              log.debug("[MQTT_MOCK] 自动发送Mock数据已停止");
            });

    mockDataThread.setName("MockDataSender");
    // 虚拟线程不需要设置为守护线程，会随着载体线程退出

    return R.ok("Mock数据自动发送已启动，每秒发送10条");
  }

  /**
   * 启动自动发送Mock数据（每2秒发送一次）
   *
   * @return 启动结果
   */
  public R<?> startMockDataSendingx() {
    if (mockDataSending) {
      return R.error("Mock数据发送已在运行中");
    }

    mockDataSending = true;

    mockDataThread =
        new Thread(
            () -> {
              log.debug("[MQTT_MOCK] 开始自动发送Mock数据，每2秒发送一次");

              while (mockDataSending && !Thread.currentThread().isInterrupted()) {
                try {
                  // 发送一次Mock数据
                  sendSingleMockDataInternal();

                  // 等待2秒
                  Thread.sleep(1000);
                } catch (InterruptedException e) {
                  log.debug("[MQTT_MOCK] Mock数据发送线程被中断");
                  break;
                } catch (Exception e) {
                  log.error("[MQTT_MOCK] 自动发送Mock数据异常: ", e);
                }
              }

              log.debug("[MQTT_MOCK] 自动发送Mock数据已停止");
            });

    mockDataThread.setName("MockDataSender");
    mockDataThread.setDaemon(true);
    mockDataThread.start();

    return R.ok("Mock数据自动发送已启动，每2秒发送一次");
  }

  /**
   * 停止自动发送Mock数据
   *
   * @return 停止结果
   */
  public R<?> stopMockDataSending() {
    if (!mockDataSending) {
      return R.error("Mock数据发送未在运行");
    }

    mockDataSending = false;

    if (mockDataThread != null) {
      mockDataThread.interrupt();
      mockDataThread = null;
    }

    log.debug("[MQTT_MOCK] 已停止自动发送Mock数据");
    return R.ok("Mock数据自动发送已停止");
  }

  /**
   * 获取Mock数据发送状态
   *
   * @return 发送状态
   */
  public R<?> getMockDataStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("sending", mockDataSending);
    status.put("threadAlive", mockDataThread != null && mockDataThread.isAlive());
    status.put("timestamp", System.currentTimeMillis());

    return R.ok(status);
  }

  /**
   * 发送单次Mock数据
   *
   * @return 发送结果
   */
  public R<?> sendSingleMockData() {
    try {
      // 随机生成设备ID后缀 (0-9)
      int randomSuffix = (int) (Math.random() * 10);
      String finalDeviceId = DEVICE_ID_PREFIX + randomSuffix;

      // 构建主题
      String topic = TOPIC_PREFIX + PRODUCT_KEY + "/" + finalDeviceId;

      // 生成随机数据
      int battery = 80 + (int) (Math.random() * 20); // 80-99
      int ecl = 90 + (int) (Math.random() * 10); // 90-99
      int switchStatus = (int) (Math.random() * 2); // 0或1
      int csq = 15 + (int) (Math.random() * 10); // 15-24

      // 构建payload
      String payload =
          String.format(
              "{\"battery\":\"%d\",\"ecl\":\"%d\",\"switchStatus\":%d,\"csq\":%d}",
              battery, ecl, switchStatus, csq);

      log.debug(
          "[MQTT_MOCK] 发送Mock数据 - 产品: {}, 设备: {}, 主题: {}, 数据: {}",
          PRODUCT_KEY,
          finalDeviceId,
          topic,
          payload);

      // 发送消息
      return sendMockMessage(PRODUCT_KEY, finalDeviceId, topic, payload);

    } catch (Exception e) {
      log.error("[MQTT_MOCK] 发送Mock数据异常: ", e);
      return R.error("Mock数据发送异常: " + e.getMessage());
    }
  }

  /** 发送单次Mock数据（内部方法，用于自动发送） */
  private void sendSingleMockDataInternal() {
    try {
      // 随机生成设备ID后缀 (0-9)
      int randomSuffix = (int) (Math.random() * 10);
      String finalDeviceId = DEVICE_ID_PREFIX + randomSuffix;

      // 构建主题
      String topic = TOPIC_PREFIX + PRODUCT_KEY + "/" + finalDeviceId;

      // 生成随机数据
      int battery = RandomUtil.randomInt(0, 99); // 80-99
      int ecl = RandomUtil.randomInt(0, 99); // 90-99
      int switchStatus = (int) (Math.random() * 2); // 0或1
      int csq = RandomUtil.randomInt(0, 31);
      ; // 15-24

      // 构建payload
      String payload =
          String.format(
              "{\"battery\":\"%d\",\"ecl\":\"%d\",\"switchStatus\":%d,\"csq\":%d}",
              battery, ecl, switchStatus, csq);

      log.debug(
          "[MQTT_MOCK] 自动发送Mock数据 - 产品: {}, 设备: {}, 主题: {}, 数据: {}",
          PRODUCT_KEY,
          finalDeviceId,
          topic,
          payload);

      // 发送消息
      sendMockMessage(PRODUCT_KEY, finalDeviceId, topic, payload);

    } catch (Exception e) {
      log.error("[MQTT_MOCK] 自动发送Mock数据异常: ", e);
    }
  }

  /**
   * 发送Mock消息到MQTT
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param topic 主题
   * @param payload 消息内容
   * @return 发送结果
   */
  private R<?> sendMockMessage(String productKey, String deviceId, String topic, String payload) {
    try {
      String networkUnionId = ioTProductDeviceService.selectNetworkUnionId(productKey);
      // 构建发布消息
      MQTTPublishMessage publishMessage =
          MQTTPublishMessage.builder()
              .topic(topic)
              .payload(StrUtil.bytes(payload))
              .qos(0)
              .retained(false)
              .productKey(productKey)
              .deviceId(deviceId)
              .messageType("MOCK_DATA")
              .build();

      boolean success;
      // 优先推送到内置MQTT（System MQTT）
      if (sysMQTTManager.isEnabled() && StrUtil.isBlank(networkUnionId)) {
        success =
            sysMQTTManager.publishMessage(
                topic,
                publishMessage.getPayload(),
                publishMessage.getQos(),
                publishMessage.isRetained());
        log.info("通过sys-mqtt推送消息,topic={},payload={}", topic, payload);
      } else {
        success = thirdMQTTServerManager.publishMessage(networkUnionId, publishMessage);
        log.info("通过third-mqtt推送消息,topic={},payload={}", topic, payload);
      }

      if (success) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Mock数据发送成功");
        result.put("productKey", productKey);
        result.put("deviceId", deviceId);
        result.put("topic", topic);
        result.put("payloadLength", payload.length());
        result.put("timestamp", System.currentTimeMillis());

        return R.ok(result);
      } else {
        return R.error("Mock数据发送失败，请检查MQTT连接状态");
      }

    } catch (Exception e) {
      log.error("[MQTT_MOCK] 发送Mock消息异常 - 产品: {}, 设备: {}, 异常: ", productKey, deviceId, e);
      return R.error("Mock消息发送异常: " + e.getMessage());
    }
  }
}
