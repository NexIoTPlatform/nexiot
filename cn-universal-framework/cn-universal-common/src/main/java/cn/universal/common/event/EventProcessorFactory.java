package cn.universal.common.event;

import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.universal.common.event.processer.AppPushConfigProcessor;
import cn.universal.common.event.processer.DeviceWebSocketPushProcessor;
import cn.universal.common.event.processer.DeviceCacheInvalidationProcessor;
import cn.universal.common.event.processer.FenceEventProcessor;
import cn.universal.common.event.processer.ProductConfigProcessor;
import cn.universal.common.event.processer.TcpDownProcessor;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 事件处理器工厂类 统一管理所有Redis事件处理器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class EventProcessorFactory {

  // 电子围栏事件处理器
  @Autowired(required = false)
  private FenceEventProcessor fenceEventProcessor;

  // TCP下行指令处理器
  @Autowired(required = false)
  private TcpDownProcessor tcpDownProcessor;

  /** 处理电子围栏事件 */
  public void handleFenceEvent(EventMessage message) {
    if (fenceEventProcessor != null) {
      try {
        fenceEventProcessor.handleFenceEvent(message);
      } catch (Exception e) {
        log.error("[事件处理器] 电子围栏事件处理失败", e);
      }
    } else {
      log.warn("[事件处理器] FenceEventProcessor未找到，跳过电子围栏事件处理");
    }
  }

  /** 处理TCP下行指令事件 */
  public void handleTcpDown(EventMessage message) {
    if (tcpDownProcessor != null) {
      try {
        tcpDownProcessor.handleTcpDownEvent(message);
      } catch (Exception e) {
        log.error("[事件处理器] TCP下行指令处理失败", e);
      }
    } else {
      log.warn("[事件处理器] TcpDownProcessor未找到，跳过TCP下行指令处理");
    }
  }

  /** 处理产品配置更新事件 */
  public void handleProductConfigUpdated(EventMessage message) {
    try {
      Map<String, ProductConfigProcessor> beans =
          SpringUtil.getBeansOfType(ProductConfigProcessor.class);
      if (MapUtil.isNotEmpty(beans)) {
        beans.forEach(
            (k, v) -> {
              v.handleProductConfigUpdated(message);
            });
      }
    } catch (Exception e) {
      log.error("[事件处理器] 产品配置更新处理失败", e);
    }
  }

  /** 处理产品协议更新事件 */
  public void handleProtocolUpdated(EventMessage message) {
    try {
      Map<String, ProductConfigProcessor> beans =
          SpringUtil.getBeansOfType(ProductConfigProcessor.class);
      if (MapUtil.isNotEmpty(beans)) {
        beans.forEach(
            (k, v) -> {
              v.handleProtocolUpdated(message);
            });
      }
    } catch (Exception e) {
      log.error("[事件处理器] 产品协议更新处理失败", e);
    }
  }

  /** 处理应用推送配置更新事件 */
  public void handleAppPushConfigUpdated(EventMessage message) {
    try {
      Map<String, AppPushConfigProcessor> beans =
          SpringUtil.getBeansOfType(AppPushConfigProcessor.class);
      if (MapUtil.isNotEmpty(beans)) {
        beans.forEach((key, value) -> value.handleAppPushConfigUpdated(message));
      }
    } catch (Exception e) {
      log.error("[事件处理器] 应用推送配置更新处理失败", e);
    }
  }

  /** 处理设备缓存失效事件 */
  public void handleDeviceCacheInvalidated(EventMessage message) {
    try {
      Map<String, DeviceCacheInvalidationProcessor> beans =
          SpringUtil.getBeansOfType(DeviceCacheInvalidationProcessor.class);
      if (MapUtil.isNotEmpty(beans)) {
        beans.forEach((key, value) -> value.handleDeviceCacheInvalidated(message));
      }
    } catch (Exception e) {
      log.error("[事件处理器] 设备缓存失效处理失败", e);
    }
  }

  /** 处理设备 WebSocket 推送事件 */
  public void handleDeviceWebSocketPush(EventMessage message) {
    try {
      Map<String, DeviceWebSocketPushProcessor> beans =
          SpringUtil.getBeansOfType(DeviceWebSocketPushProcessor.class);
      if (MapUtil.isNotEmpty(beans)) {
        beans.forEach((key, value) -> value.handleDeviceWebSocketPush(message));
      }
    } catch (Exception e) {
      log.error("[事件处理器] 设备 WebSocket 推送处理失败", e);
    }
  }
}
