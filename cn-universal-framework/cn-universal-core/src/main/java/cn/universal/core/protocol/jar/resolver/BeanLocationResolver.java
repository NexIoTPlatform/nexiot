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

package cn.universal.core.protocol.jar.resolver;

import cn.hutool.extra.spring.SpringUtil;
import cn.universal.common.exception.CodecException;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

/**
 * Spring Bean Location 解析器 处理 location 为 Spring Bean 名称的情况
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/02
 */
@Slf4j
public class BeanLocationResolver implements LocationResolver {

  @Override
  public boolean supports(String location) {
    if (location == null || location.contains("://")) {
      return false;
    }
    // 排除文件路径和 JAR 文件
    if (location.endsWith(".jar")
        || location.startsWith("/")
        || location.startsWith("\\")
        || location.contains(File.separator)) {
      return false;
    }
    // 检查 Spring 容器中是否存在该 bean
    try {
      return SpringUtil.getBean(location) != null;
    } catch (Exception e) {
      log.debug("检查 Bean 是否存在时出错: location={}, error={}", location, e.getMessage());
      return false;
    }
  }

  @Override
  public Object resolve(String location, String provider) throws CodecException {
    try {
      // 从 Spring 容器获取 Bean
      Object bean = SpringUtil.getBean(location);
      log.info(
          "从 Spring 容器获取 Bean: location={}, provider={}, beanClass={}",
          location,
          provider,
          bean.getClass().getName());
      return bean;
    } catch (BeansException e) {
      throw new CodecException("无法从 Spring 容器获取 Bean: " + location, e);
    }
  }
}
