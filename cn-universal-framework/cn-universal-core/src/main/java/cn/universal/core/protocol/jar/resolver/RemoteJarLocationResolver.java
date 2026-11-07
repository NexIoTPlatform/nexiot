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
import cn.universal.core.protocol.jar.downloader.JarDownloader;
import lombok.extern.slf4j.Slf4j;

/**
 * 远程 JAR Location 解析器 处理 location 为远程 JAR URL 的情况
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/02
 */
@Slf4j
public class RemoteJarLocationResolver implements LocationResolver {

  private final JarDownloader jarDownloader = new JarDownloader();

  @Override
  public boolean supports(String location) {
    if (location == null) {
      return false;
    }
    // 判断是否为远程 JAR URL
    return (location.startsWith("http://") || location.startsWith("https://"))
        && location.endsWith(".jar");
  }

  @Override
  public Object resolve(String location, String provider) throws CodecException {
    // 1. 下载远程 JAR
    String localPath = jarDownloader.download(location);

    // 2. 使用本地 JAR 解析器处理
    LocalJarLocationResolver localResolver = new LocalJarLocationResolver();
    return localResolver.resolve(localPath, provider);
  }
}
