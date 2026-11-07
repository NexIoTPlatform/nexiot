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

package cn.universal.admin.network.config;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 网络组件配置
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "network")
public class NetworkConfig {

  /** TCP配置模板 */
  private Map<String, Object> tcpTemplate;

  /** MQTT配置模板 */
  private Map<String, Object> mqttTemplate;

  /** 默认配置 */
  private DefaultConfig defaults = new DefaultConfig();

  /** 配置 RestTemplate Bean */
  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000); // 连接超时5秒
    factory.setReadTimeout(10000); // 读取超时10秒
    return new RestTemplate(factory);
  }

  @Data
  public static class DefaultConfig {

    /** 默认端口 */
    private int defaultPort = 18080;

    /** 默认超时时间 */
    private int defaultTimeout = 30;

    /** 默认心跳间隔 */
    private int defaultKeepAlive = 60;
  }
}
