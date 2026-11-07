package cn.universal.admin.platform.service.push;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.push.ExternalMqttClientManager;
import cn.universal.dm.device.service.push.KafkaPushStrategy;
import cn.universal.dm.device.service.push.RocketMQPushStrategy;
import cn.universal.dm.device.service.push.TcpPushClientManager;
import cn.universal.persistence.entity.bo.UPPushBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ApplicationPushConnectionTestService {

  @Resource private ExternalMqttClientManager externalMqttClientManager;
  @Resource private TcpPushClientManager tcpPushClientManager;
  @Resource private KafkaPushStrategy kafkaPushStrategy;
  @Resource private RocketMQPushStrategy rocketMQPushStrategy;

  public void test(String pushType, JSONObject config) {
    if (config == null) {
      throw new IoTException("推送配置不能为空");
    }
    try {
      if ("externalMqtt".equalsIgnoreCase(pushType)) {
        externalMqttClientManager.testConnection(
            JSONUtil.toBean(config.toString(), UPPushBO.ExternalMqttPushConfig.class));
        return;
      }
      if ("tcp".equalsIgnoreCase(pushType)) {
        tcpPushClientManager.testConnection(
            JSONUtil.toBean(config.toString(), UPPushBO.TcpPushConfig.class));
        return;
      }
      if ("kafka".equalsIgnoreCase(pushType)) {
        kafkaPushStrategy.testConnection(
            JSONUtil.toBean(config.toString(), UPPushBO.KafkaPushConfig.class));
        return;
      }
      if ("rocketmq".equalsIgnoreCase(pushType)) {
        rocketMQPushStrategy.testConnection(
            JSONUtil.toBean(config.toString(), UPPushBO.RocketMQPushConfig.class));
        return;
      }
      throw new IoTException("该推送方式不支持连接测试");
    } catch (IoTException e) {
      throw e;
    } catch (Exception e) {
      throw new IoTException("连接失败: " + e.getMessage());
    }
  }
}
