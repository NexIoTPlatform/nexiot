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

package cn.universal.jar.driver.demo;

import cn.universal.core.protocol.jar.JarDriverCodecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 测试用的 JarDriverCodecService Bean 用于测试 JarDriverCodecService 接口（三个参数）的加载和调用
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/02
 */
@Service("testJarCodec")
@Slf4j
public class TestJarDriverCodecService implements JarDriverCodecService {

  @Override
  public String productKey() {
    return "testJarCodec";
  }

  @Override
  public String preDecode(String payload, Object context) {
    log.info(
        "TestJarDriverCodecService preDecode called with  payload: {}, context: {}",
        payload,
        context);
    return "preDecode:" + payload;
  }

  @Override
  public String encode(String payload, Object context) {
    log.info(
        "TestJarDriverCodecService encode called with  payload: {}, context: {}", payload, context);
    return "encode:" + payload;
  }

  @Override
  public String decode(String payload, Object context) {
    log.info(
        "TestJarDriverCodecService decode called with  payload: {}, context: {}", payload, context);
    return "decode:" + payload;
  }
}
