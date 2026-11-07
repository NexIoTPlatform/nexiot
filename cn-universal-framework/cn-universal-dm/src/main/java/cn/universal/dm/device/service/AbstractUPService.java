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

package cn.universal.dm.device.service;

import cn.hutool.core.util.IdUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.core.message.UPRequest;
import cn.universal.core.service.ICodecService;
import cn.universal.core.service.IUP;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

/**
 * 上行消息工具抽象类，提供统一的消息编解码、协议处理及请求转发能力。 子类需实现消息解析逻辑，并处理特定类型的上行请求（如设备注册、数据上报等）。
 *
 * @param <T> 泛型参数，代表具体的上行请求类型，需继承自 {@link UPRequest}
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2020/10/21
 */
@Slf4j
public abstract class AbstractUPService<T extends UPRequest> extends AbstratIoTService
    implements IUP {

  @Autowired protected ICodecService codecService;

  /** 消息转换 */
  protected abstract List<T> convert(String request);

  @Override
  @Async
  public void asyncUP(String request) {
    String traceId = IdUtil.fastSimpleUUID();
    MDC.put(IoTConstant.TRACE_ID, traceId);

    Thread currentThread = Thread.currentThread();
    String threadInfo =
        String.format(
            "%s(id=%d)",
            currentThread.getName().isEmpty()
                ? "VirtualThread-" + currentThread.getId()
                : currentThread.getName(),
            currentThread.getId());

    long startTime = System.currentTimeMillis();
    log.info(
        "异步开始 - 组件名={}, 线程={}, TraceId={}, 开始时间={}, 上行消息={}",
        currentComponent(),
        threadInfo,
        traceId,
        startTime,
        request);

    try {
      // 模拟一些处理时间来观察并发效果
      //      Thread.sleep(3000); // 3秒处理时间
      realUPAction(request);

      long endTime = System.currentTimeMillis();
      log.info(
          "异步完成 - 组件名={}, 线程={}, TraceId={}, 结束时间={}, 耗时={}ms",
          currentComponent(),
          threadInfo,
          traceId,
          endTime,
          (endTime - startTime));
    } catch (Exception e) {
      log.warn("异步处理被中断 - TraceId={}", traceId);
    } finally {
      MDC.remove(IoTConstant.TRACE_ID);
    }
  }

  // 测试方法：模拟简单的异步处理
  @Async("virtualThreadExecutor")
  public void testAsync(String message) {
    Thread currentThread = Thread.currentThread();
    String threadInfo =
        String.format(
            "%s(id=%d)",
            currentThread.getName().isEmpty()
                ? "VirtualThread-" + currentThread.getId()
                : currentThread.getName(),
            currentThread.getId());

    log.info("测试异步开始 - 线程={}, 消息={}", threadInfo, message);

    try {
      // 模拟处理时间，但不阻塞
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    log.info("测试异步完成 - 线程={}, 消息={}", threadInfo, message);
  }

  protected abstract Object realUPAction(String upMsg);

  /** 当前iot组件名称 */
  protected abstract String currentComponent();
}
