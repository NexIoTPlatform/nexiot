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

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP订阅推送策略实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class SubscribePushStrategy implements PushStrategy<Void> {

  /** URL黑名单缓存 */
  private static final Cache<String, Integer> blockUrl =
      Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(256).build();

  /** 超过失败次数则入黑名单 */
  private static final Integer BLOCK_COUNT = 30;

  private final ApplicationPushHttpClient httpClient;

  public SubscribePushStrategy(ApplicationPushHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public IoTPushResult execute(BaseUPRequest request, String messageJson, Void config) {

    // 订阅的推送
    try {
      boolean failed = false;
      Object devSubObj = request.getDevSubscribe();
      if (devSubObj instanceof List<?> subscriptionObjects) {
        for (Object subscriptionObject : subscriptionObjects) {
          if (!(subscriptionObject instanceof IoTDeviceSubscribe subscribe)
              || !subscribe.isEnabledFlag()) {
            continue;
          }
          if (StrUtil.isBlank(subscribe.getUrl())) {
            log.debug("[HTTP订阅推送] URL为空，跳过推送");
            continue;
          }

          // 检查URL是否被冻结
          if (isUrlBlocked(subscribe.getUrl())) {
            log.debug("[HTTP订阅推送] URL {} 因连续失败暂时跳过", subscribe.getUrl());
            continue;
          }

          try {
            int status =
                httpClient.postJson(
                    subscribe.getUrl(),
                    messageJson,
                    Map.of("X-Timestamp", String.valueOf(System.currentTimeMillis())));
            if (status < 200 || status >= 300) {
              FailureState failure = countFail(subscribe.getUrl());
              logFailure(subscribe.getUrl(), request.getIotId(), "HTTP status=" + status, failure);
              failed = true;
              continue;
            }

            log.debug(
                "[HTTP订阅推送] 请求已送达, url={}, deviceId={}, status={}",
                subscribe.getUrl(),
                request.getIotId(),
                status);
            removeSuccess(subscribe.getUrl());
          } catch (Exception e) {
            FailureState failure = countFail(subscribe.getUrl());
            logFailure(
                subscribe.getUrl(),
                request.getIotId(),
                String.valueOf(ExceptionUtil.getRootCause(e)),
                failure);
            failed = true;
          }
        }
      }

      if (failed) {
        return IoTPushResult.failed(
            request.getIoTDeviceDTO().getThirdPlatform(),
            request.getProductKey(),
            request.getIotId(),
            "Subscribe",
            messageJson,
            "部分订阅地址推送失败",
            "PARTIAL_FAILURE");
      }

      // 如果没有订阅或所有订阅都成功，返回成功
      return IoTPushResult.success(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "Subscribe",
          messageJson,
          System.currentTimeMillis());

    } catch (Exception e) {
      log.error("[HTTP订阅推送] 推送失败: {}", request.getIotId(), e);
      return IoTPushResult.failed(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "Subscribe",
          messageJson,
          "推送异常: " + e.getMessage(),
          "PUSH_EXCEPTION");
    }
  }

  @Override
  public boolean isSupported(Void config) {
    return true;
  }

  /** 检查URL是否被冻结 */
  private boolean isUrlBlocked(String url) {
    Integer failures = blockUrl.getIfPresent(url);
    return failures != null && failures >= BLOCK_COUNT;
  }

  /** 记录失败次数 */
  private FailureState countFail(String url) {
    int[] previous = new int[1];
    int current =
        blockUrl
            .asMap()
            .compute(
                url,
                (key, failures) -> {
                  previous[0] = failures == null ? 0 : failures;
                  return Math.min(BLOCK_COUNT, previous[0] + 1);
                });
    return new FailureState(
        current, previous[0] == 0 || (previous[0] < BLOCK_COUNT && current == BLOCK_COUNT));
  }

  private void logFailure(String url, String deviceId, String error, FailureState failure) {
    if (failure.warn()) {
      log.warn(
          "[HTTP订阅推送] 上层地址发送失败, url={}, deviceId={}, consecutiveFailures={}, error={}",
          url,
          deviceId,
          failure.count(),
          error);
      return;
    }
    log.debug(
        "[HTTP订阅推送] 上层地址发送失败, url={}, deviceId={}, consecutiveFailures={}, error={}",
        url,
        deviceId,
        failure.count(),
        error);
  }

  /** 移除成功记录 */
  private void removeSuccess(String url) {
    blockUrl.invalidate(url);
  }

  private record FailureState(int count, boolean warn) {}
}
