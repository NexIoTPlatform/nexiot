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

package cn.universal.mqtt.protocol.third;

import cn.universal.common.enums.NetworkType;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/**
 * MQTT配置数据库加载器
 *
 * <p>只负责从数据库加载Network实体，不做任何解析和业务拼装。 支持按类型、按productKey、全部启用等多种查询方式。 @Author gitee.com/NexIoT
 *
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Service
public class ThirdMQTTConfigLoader {

  @Autowired private NetworkMapper networkMapper;
  @Autowired private IoTProductMapper ioTProductMapper;

  @Autowired private MQTTTopicManager mqttTopicManager;

  @Autowired private ThirdMQTTConfigParser configParser;

  /** 按类型加载所有Network */
  public List<Network> loadByType(String type) {
    Example example = new Example(Network.class);
    example.createCriteria().andEqualTo("type", type).andEqualTo("state", Network.STATE_ENABLED);
    return networkMapper.selectByExample(example);
  }

  /** 加载所有MQTT_CLIENT类型 */
  public List<Network> loadAllMqttClient() {
    return loadByType(NetworkType.MQTT_CLIENT.getId());
  }

  /** 加载所有MQTT_SERVER类型 */
  public List<Network> loadAllMqttServer() {
    return loadByType(NetworkType.MQTT_SERVER.getId());
  }

  /** 获取所有已启用的productKey */
  public List<String> getAllEnabledProductKeys() {
    return ioTProductMapper.selectAllEnableNetworkProductKey();
  }
}
