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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.databridge.processor.DataBridgeAfterPushProcessor;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.push.UPProcessorManager;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.base.IoTDeviceExtendService;
import cn.universal.persistence.base.IoTUPWrapper;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.entity.bo.UPPushBO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/6 11:56
 */
@Slf4j
public abstract class IoTUPPushAdapter<T extends BaseUPRequest> {

  private static final long DROP_WARNING_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);

  @Resource private IoTDeviceExtendService deviceExtTemplate;

  @Resource private UPProcessorManager upProcessorManager;

  @Resource private UPPushConfigService upPushConfigService;

  @Autowired @Lazy private UPPushExecutor upPushExecutor;

  @Autowired @Lazy private UPPushDispatcher upPushDispatcher;

  @Autowired private UPRequestSnapshotter upRequestSnapshotter;

  @Autowired(required = false)
  private DeviceUplinkPublisher deviceUplinkPublisher;

  @Autowired(required = false)
  private DataBridgeAfterPushProcessor dataBridgeAfterPushProcessor;

  @Autowired(required = false)
  private List<IoTUPWrapper> upWrappers = List.of();

  private final AtomicLong nextDropWarningNanos = new AtomicLong();

  protected void sendMqtt(String topic, String msg) {
    //    mqttUPWrapper.mqttPush(topic, msg);
  }

  /**
   * 推送前钩子方法，子类可重写进行消息转换、规则引擎等处理
   *
   * @param upRequests 上行请求列表
   */
  protected void beforePush(List<T> upRequests) {
    // 调用现有的beforePush逻辑
    callExistingBeforePush(upRequests);

    // 执行插件化处理器
    if (upProcessorManager != null) {
      List<T> ts = upProcessorManager.executeBeforePush(upRequests);
      if (ts != null && ts != upRequests) {
        upRequests.clear();
        upRequests.addAll(ts);
      }
    }

    // 子类可重写此方法进行扩展
    onBeforePush(upRequests);
  }

  /**
   * 推送后钩子方法，子类可重写进行结果处理、日志记录等
   *
   * @param upRequests 上行请求列表
   * @param pushResults 推送结果列表
   */
  protected void afterPush(List<T> upRequests, List<IoTPushResult> pushResults) {
    // 执行插件化处理器（带推送结果）
    if (upProcessorManager != null) {
      upProcessorManager.executeAfterPush(upRequests, pushResults);
    }

    // 数据桥接处理（异步）
    try {
      if (dataBridgeAfterPushProcessor != null) {
        dataBridgeAfterPushProcessor.executeAfterPush((List<BaseUPRequest>) upRequests);
      }
    } catch (Exception e) {
      log.error("[数据桥接] afterPush 调用异常", e);
    }

    // 子类可重写此方法进行扩展
    onAfterPush(upRequests, pushResults);
  }

  /**
   * 推送前扩展点，子类可重写
   *
   * @param upRequests 上行请求列表
   */
  protected void onBeforePush(List<T> upRequests) {
    // 默认空实现，子类可重写
  }

  /**
   * 推送后扩展点，子类可重写
   *
   * @param upRequests 上行请求列表
   * @param pushResults 推送结果列表
   */
  protected void onAfterPush(List<T> upRequests, List<IoTPushResult> pushResults) {
    // 默认空实现，子类可重写
  }

  /**
   * 实际的推送逻辑
   *
   * @param upRequests 上行请求列表
   * @return 推送结果列表
   */
  protected List<IoTPushResult> executePush(List<T> upRequests) {
    // 按应用分组处理推送
    Map<String, List<T>> appGroups = groupByApplication(upRequests);
    List<IoTPushResult> allResults = new java.util.ArrayList<>();

    for (Map.Entry<String, List<T>> entry : appGroups.entrySet()) {
      String appId = entry.getKey();
      List<T> requests = entry.getValue();

      try {
        // 获取推送配置
        if (!SUBSCRIPTION_ONLY_GROUP.equals(appId)
            && !upPushConfigService.isApplicationEnabled(appId)) {
          log.debug("[推送执行] 应用已停用或不存在，跳过: {}", appId);
          continue;
        }
        UPPushBO pushConfig =
            SUBSCRIPTION_ONLY_GROUP.equals(appId)
                ? upPushConfigService.getDefaultPushConfig()
                : upPushConfigService.getPushConfig(appId);
        List<T> pushableRequests =
            requests.stream()
                .filter(request -> upPushExecutor.hasEnabledChannel(request, pushConfig))
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        if (pushableRequests.isEmpty()) {
          continue;
        }
        pushableRequests.forEach(deviceExtTemplate::upExt);
        List<IoTPushResult> results =
            upPushExecutor.executeBatchPush((List<BaseUPRequest>) pushableRequests, pushConfig);
        log.debug(
            "[推送执行] 应用 {} 推送完成，消息数量: {}, 结果数量: {}",
            appId,
            pushableRequests.size(),
            results != null ? results.size() : 0);

        // 收集推送结果
        if (results != null) {
          allResults.addAll(results);
        }
      } catch (Exception e) {
        log.error("[推送执行] 应用 {} 推送异常", appId, e);
      }
    }

    return allResults;
  }

  /**
   * 按应用ID分组，过滤掉无效的请求
   *
   * @param upRequests 上行请求列表
   * @return 分组后的请求映射
   */
  private Map<String, List<T>> groupByApplication(List<T> upRequests) {
    return upRequests.stream()
        .filter(
            request -> {
              IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
              if (deviceDTO == null) {
                log.warn("[分组] 设备DTO为空，丢弃消息: {}", request);
                return false;
              }

              String applicationId = resolveApplicationId(request);
              if (StrUtil.isBlank(applicationId)) {
                if (hasEnabledSubscription(request)) {
                  return true;
                }
                log.debug(
                    "[分组] 应用ID为空，设备ID: {}, requestApplicationId: {}, 丢弃消息",
                    deviceDTO.getDeviceId(),
                    request.getApplicationId());
                return false;
              }
              request.setApplicationId(applicationId);
              return true;
            })
        .collect(
            Collectors.groupingBy(
                request ->
                    StrUtil.blankToDefault(
                        resolveApplicationId(request), SUBSCRIPTION_ONLY_GROUP)));
  }

  private static final String SUBSCRIPTION_ONLY_GROUP = "__subscription_only__";

  private boolean hasEnabledSubscription(T request) {
    if (!(request.getDevSubscribe() instanceof List<?> subscriptions)) {
      return false;
    }
    return subscriptions.stream()
        .filter(IoTDeviceSubscribe.class::isInstance)
        .map(IoTDeviceSubscribe.class::cast)
        .anyMatch(subscribe -> subscribe.isEnabledFlag() && StrUtil.isNotBlank(subscribe.getUrl()));
  }

  private String resolveApplicationId(T request) {
    if (request == null) {
      return null;
    }
    IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
    return StrUtil.blankToDefault(
        deviceDTO == null ? null : StrUtil.trim(deviceDTO.getApplicationId()),
        StrUtil.trim(request.getApplicationId()));
  }

  /**
   * 模板方法：统一的上行消息处理流程
   *
   * @param upRequests 上行请求列表
   * @return 处理结果
   */
  protected String doUp(List<T> upRequests) {
    submitUp(upRequests);
    return null;
  }

  /** Best-effort submission used by protocol processors that need queue acceptance metrics. */
  protected boolean submitUp(List<T> upRequests) {
    if (CollectionUtil.isEmpty(upRequests)) {
      return true;
    }

    if (deviceUplinkPublisher != null) deviceUplinkPublisher.publish(upRequests);

    // Independent side-effect dispatch happens before application filtering/queue admission.
    List<T> processedRequests = new ArrayList<>(upRequests);
    beforePush(processedRequests);
    if (CollectionUtil.isEmpty(processedRequests)) {
      return true;
    }

    Map<String, List<T>> deviceGroups = new LinkedHashMap<>();
    for (T request : processedRequests) {
      deviceGroups
          .computeIfAbsent(resolveOrderingKey(request), ignored -> new java.util.ArrayList<>())
          .add(request);
    }

    boolean accepted = true;
    for (Map.Entry<String, List<T>> entry : deviceGroups.entrySet()) {
      List<T> sourceRequests = entry.getValue();
      if (!upPushDispatcher.canAccept(entry.getKey())) {
        upPushDispatcher.recordRejected(sourceRequests.size());
        accepted = false;
        logRejectedGroup(sourceRequests);
        continue;
      }

      List<T> requests = upRequestSnapshotter.snapshot(sourceRequests);
      if (requests.isEmpty()) {
        upPushDispatcher.recordRejected(sourceRequests.size());
        log.error("[推送调度] 请求快照失败，已丢弃 {} 条消息", sourceRequests.size());
        accepted = false;
        continue;
      }
      boolean submitted =
          upPushDispatcher.dispatch(
              entry.getKey(), requests.size(), () -> executePushPipeline(requests));
      if (!submitted) {
        accepted = false;
        logRejectedGroup(requests);
      }
    }
    return accepted;
  }

  private void logRejectedGroup(List<T> requests) {
    if (!shouldLogDropWarning()) {
      return;
    }
    T first = requests.get(0);
    log.warn(
        "[推送调度] 队列已满，best-effort 丢弃，后续同类日志将限频: productKey={}, deviceId={}, count={}",
        first.getProductKey(),
        first.getDeviceId(),
        requests.size());
  }

  /** Explicit synchronous entry for callers whose configured execution mode is synchronous. */
  protected String doUpDirect(List<T> upRequests) {
    if (CollectionUtil.isNotEmpty(upRequests)) {
      if (deviceUplinkPublisher != null) deviceUplinkPublisher.publish(upRequests);
      List<T> processedRequests = new ArrayList<>(upRequests);
      beforePush(processedRequests);
      if (CollectionUtil.isNotEmpty(processedRequests)) {
        executePushPipeline(processedRequests);
      }
    }
    return null;
  }

  private void executePushPipeline(List<T> upRequests) {
    List<IoTPushResult> pushResults = executePush(upRequests);
    afterPush(upRequests, pushResults);
  }

  private boolean shouldLogDropWarning() {
    long now = System.nanoTime();
    long next = nextDropWarningNanos.get();
    return now >= next
        && nextDropWarningNanos.compareAndSet(next, now + DROP_WARNING_INTERVAL_NANOS);
  }

  private String resolveOrderingKey(T request) {
    if (request == null) {
      return "unknown";
    }
    if (StrUtil.isNotBlank(request.getIotId())) {
      return request.getIotId();
    }
    if (StrUtil.isNotBlank(request.getProductKey()) || StrUtil.isNotBlank(request.getDeviceId())) {
      return StrUtil.nullToEmpty(request.getProductKey())
          + ':'
          + StrUtil.nullToEmpty(request.getDeviceId());
    }
    return StrUtil.blankToDefault(request.getRequestId(), "unknown");
  }

  /**
   * 调用现有的beforePush逻辑
   *
   * @param upRequests 上行请求列表
   */
  private void callExistingBeforePush(List<T> upRequests) {
    for (IoTUPWrapper wrapper : upWrappers) {
      wrapper.beforePush(upRequests);
      log.debug("调用推送前置处理,实现名称={}", wrapper.getClass().getSimpleName());
    }
  }
}
