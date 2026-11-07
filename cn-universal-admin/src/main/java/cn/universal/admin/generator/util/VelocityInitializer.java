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

package cn.universal.admin.generator.util;

import java.util.Properties;
import org.apache.velocity.app.Velocity;

/** VelocityEngine工厂 @Author ruoyi */
public class VelocityInitializer {

  /** 初始化vm方法 */
  public static void initVelocity() {
    Properties p = new Properties();
    try {
      // 加载classpath目录下的vm文件
      p.setProperty(
          "resource.loader.file.class",
          "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
      // 定义字符集
      p.setProperty(Velocity.INPUT_ENCODING, "utf-8");
      // 初始化Velocity引擎，指定配置Properties
      Velocity.init(p);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
