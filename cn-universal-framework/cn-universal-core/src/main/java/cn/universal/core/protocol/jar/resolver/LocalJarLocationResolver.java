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

import cn.universal.common.exception.CodecException;
import cn.universal.core.protocol.jar.ProtocolJarClassLoader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地 JAR Location 解析器 处理 location 为本地 JAR 文件路径的情况
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/02
 */
@Slf4j
public class LocalJarLocationResolver implements LocationResolver {

  @Override
  public boolean supports(String location) {
    if (location == null) {
      return false;
    }
    // 判断是否为本地文件路径
    return location.endsWith(".jar")
        || location.startsWith("/")
        || location.startsWith("\\")
        || location.contains(File.separator);
  }

  @Override
  public Object resolve(String location, String provider) throws CodecException {
    // 1. 检查文件是否存在
    File jarFile = new File(location);
    if (!jarFile.exists()) {
      throw new CodecException("JAR 文件不存在: " + location);
    }

    // 2. 创建 URL
    URL url;
    try {
      if (!location.contains("://")) {
        url = jarFile.toURI().toURL();
      } else {
        url = new java.net.URI("jar:" + location + "!/").toURL();
      }
    } catch (MalformedURLException | URISyntaxException e) {
      throw new CodecException("无效的 JAR URL: " + location, e);
    }

    // 3. 创建 ClassLoader 并加载类
    ProtocolJarClassLoader loader = createClassLoader(url);
    try {
      Class<?> clazz = Class.forName(provider, true, loader);
      Object instance = clazz.getDeclaredConstructor().newInstance();
      log.info(
          "成功加载 JAR 类: provider={}, location={}, class={}", provider, location, clazz.getName());
      return instance;
    } catch (Exception e) {
      throw new CodecException("无法实例化类: " + provider + ", location: " + location, e);
    }
  }

  /**
   * 创建 ClassLoader
   *
   * @param url JAR URL
   * @return ClassLoader
   */
  private ProtocolJarClassLoader createClassLoader(URL url) {
    return new ProtocolJarClassLoader(new URL[] {url}, this.getClass().getClassLoader());
  }
}
