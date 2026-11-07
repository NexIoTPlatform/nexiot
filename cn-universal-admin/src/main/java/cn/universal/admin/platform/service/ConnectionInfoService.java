package cn.universal.admin.platform.service;

import cn.universal.admin.platform.dto.ConnectionInfoDTO;

/** 连接信息服务接口 */
public interface ConnectionInfoService {

  /**
   * 获取产品的连接信息
   *
   * @param productKey 产品Key
   * @return 连接信息
   */
  ConnectionInfoDTO getConnectionInfo(String productKey);

  /**
   * 获取产品的MQTT密码信息
   *
   * @param productKey 产品Key
   * @return 密码信息
   */
  ConnectionInfoDTO getMqttPasswordInfo(String productKey);
}
