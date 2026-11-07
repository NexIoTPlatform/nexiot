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

package cn.universal.databridge.processor;

import cn.hutool.core.collection.CollectionUtil;
import cn.universal.databridge.manager.DataBridgeManager;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * 数据桥接推送后处理器 在推送后执行数据桥接
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@ConditionalOnProperty(prefix = "databridge", name = "enabled", havingValue = "true")
@Slf4j
public class DataBridgeAfterPushProcessor {

  private final AtomicLong rejectedCount = new AtomicLong();

  @Resource private DataBridgeManager dataBridgeManager;

  @Value("${databridge.enabled:false}")
  private boolean dataBridgeEnabled;

  /** 在推送后执行数据桥接 */
  public void executeAfterPush(List<BaseUPRequest> upRequests) {
    if (CollectionUtil.isEmpty(upRequests) || !dataBridgeEnabled) {
      return;
    }

    log.debug("[数据桥接] 开始处理 {} 条消息", upRequests.size());

    try {
      // 异步执行数据桥接
      dataBridgeManager.processDeviceData(upRequests);
    } catch (TaskRejectedException e) {
      long count = rejectedCount.incrementAndGet();
      if (count == 1 || count % 1000 == 0) {
        log.warn("[数据桥接] 队列已满，丢弃桥接任务, rejectedCount={}", count);
      }
    } catch (Exception e) {
      log.error("[数据桥接] 处理异常: {}", e.getMessage(), e);
    }
  }
}
