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

package cn.universal.dm.device.service.push;

import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 性能监控处理器 - 推送性能统计
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class PerformanceMonitorProcessor implements UPProcessor<BaseUPRequest> {

  private final ConcurrentHashMap<String, AtomicLong> pushCountMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> pushSuccessMap = new ConcurrentHashMap<>();

  @Override
  public String getName() {
    return "PerformanceMonitorProcessor";
  }

  @Override
  public String getDescription() {
    return "性能监控处理器";
  }

  @Override
  public int getOrder() {
    return 300; // 最后执行，用于统计
  }

  @Override
  public boolean isAfterPushEnabled() {
    return false;
  }

  @Override
  public List<BaseUPRequest> beforePush(List<BaseUPRequest> upRequests) {
    log.debug("[性能监控处理器] 开始监控 {} 条消息", upRequests.size());

    // 保留协议层/采集层上报时间；只有缺失时才补当前时间。
    upRequests.forEach(
        request -> {
          if (request.getTime() == null || request.getTime() <= 0) {
            request.setTime(System.currentTimeMillis());
          }
        });

    return upRequests;
  }

  /**
   * 获取推送统计信息
   *
   * @return 统计信息
   */
  public ConcurrentHashMap<String, AtomicLong> getPushCountMap() {
    return pushCountMap;
  }

  /**
   * 获取成功推送统计信息
   *
   * @return 统计信息
   */
  public ConcurrentHashMap<String, AtomicLong> getPushSuccessMap() {
    return pushSuccessMap;
  }
}
