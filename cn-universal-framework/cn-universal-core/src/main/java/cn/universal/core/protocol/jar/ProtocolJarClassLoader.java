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

package cn.universal.core.protocol.jar;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 协议jar类加载器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/9 19:23
 */
public class ProtocolJarClassLoader extends URLClassLoader {

  private final URL[] urls;

  public ProtocolJarClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
    this.urls = urls;
  }

  @Override
  public void close() throws IOException {
    super.close();
  }

  public URL[] getUrls() {
    return this.urls;
  }
}
