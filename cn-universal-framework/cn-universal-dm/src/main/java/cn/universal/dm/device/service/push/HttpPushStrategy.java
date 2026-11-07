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
import cn.hutool.crypto.digest.MD5;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.bo.UPPushBO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP推送策略实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class HttpPushStrategy implements PushStrategy<UPPushBO.HttpPushConfig> {

  /** URL黑名单缓存 */
  private static final Cache<String, Integer> blockUrl =
      Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(256).build();

  /** 超过失败次数则入黑名单 */
  private static final Integer BLOCK_COUNT = 30;

  private final ApplicationPushHttpClient httpClient;

  public HttpPushStrategy(ApplicationPushHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public IoTPushResult execute(
      BaseUPRequest request, String messageJson, UPPushBO.HttpPushConfig config) {
    if (config == null) {
      log.warn("[HTTP推送] 配置为空，跳过推送");
      return IoTPushResult.failed(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "HTTP",
          messageJson,
          "配置为空",
          "CONFIG_NULL");
    }

    try {
      if (StrUtil.isBlank(config.getUrl())) {
        log.warn("[HTTP推送] URL为空，跳过推送");
        return IoTPushResult.failed(
            request.getIoTDeviceDTO().getThirdPlatform(),
            request.getProductKey(),
            request.getIotId(),
            "HTTP",
            messageJson,
            "URL为空",
            "URL_EMPTY");
      }

      // 检查URL是否被冻结
      if (isUrlBlocked(config.getUrl())) {
        log.debug("[HTTP推送] URL {} 因连续失败暂时跳过", config.getUrl());
        return IoTPushResult.failed(
            request.getIoTDeviceDTO().getThirdPlatform(),
            request.getProductKey(),
            request.getIotId(),
            "HTTP",
            messageJson,
            "URL已被冻结",
            "URL_BLOCKED");
      }

      // 添加时间戳和签名
      String timestamp = String.valueOf(System.currentTimeMillis());
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put("X-Timestamp", timestamp);

      // 添加自定义请求头（如果有配置）
      if (StrUtil.isNotBlank(config.getHeader())) {
        headers.put(
            config.getHeader(),
            StrUtil.isBlank(config.getSecret())
                ? MD5.create().digestHex(config.getHeader())
                : config.getSecret());
      }

      int status = httpClient.postJson(config.getUrl(), messageJson, headers);
      if (status < 200 || status >= 300) {
        FailureState failure = countFail(config.getUrl());
        logFailure(config.getUrl(), request.getIotId(), "HTTP status=" + status, failure);
        return IoTPushResult.failed(
            request.getIoTDeviceDTO().getThirdPlatform(),
            request.getProductKey(),
            request.getIotId(),
            "HTTP",
            messageJson,
            "HTTP状态码错误: " + status,
            "HTTP_ERROR");
      }
      log.debug("[HTTP推送] 请求已送达, url={}, status={}", config.getUrl(), status);
      removeSuccess(config.getUrl());

      return IoTPushResult.success(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "HTTP",
          messageJson,
          System.currentTimeMillis());

    } catch (IOException e) {
      FailureState failure = countFail(config.getUrl());
      logFailure(
          config.getUrl(),
          request.getIotId(),
          String.valueOf(ExceptionUtil.getRootCause(e)),
          failure);
      return IoTPushResult.failed(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "HTTP",
          messageJson,
          "HTTP异常: " + e.getMessage(),
          "HTTP_EXCEPTION");
    } catch (Exception e) {
      FailureState failure = countFail(config.getUrl());
      logFailure(
          config.getUrl(),
          request.getIotId(),
          String.valueOf(ExceptionUtil.getRootCause(e)),
          failure);
      return IoTPushResult.failed(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "HTTP",
          messageJson,
          "推送异常: " + e.getMessage(),
          "PUSH_EXCEPTION");
    }
  }

  @Override
  public boolean isSupported(UPPushBO.HttpPushConfig config) {
    return config != null && config.isSupport() && config.isEnable();
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
          "[HTTP推送] 上层地址发送失败, url={}, deviceId={}, consecutiveFailures={}, error={}",
          url,
          deviceId,
          failure.count(),
          error);
      return;
    }
    log.debug(
        "[HTTP推送] 上层地址发送失败, url={}, deviceId={}, consecutiveFailures={}, error={}",
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
