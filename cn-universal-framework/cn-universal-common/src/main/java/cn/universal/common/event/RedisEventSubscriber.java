package cn.universal.common.event;

import cn.hutool.json.JSONUtil;
import cn.universal.common.config.InstanceIdProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

/** Redis事件订阅者 处理集群间的消息通信 */
@Slf4j
@Component
public class RedisEventSubscriber {

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private InstanceIdProvider instanceIdProvider;

  @Autowired private RedisMessageListenerContainer container;

  @Autowired private EventProcessorFactory eventProcessorFactory;

  // 定义一个通用的带异常处理的适配器创建方法
  private MessageListenerAdapter createAdapter(Object delegate, String methodName) {
    MessageListenerAdapter adapter =
        new MessageListenerAdapter(delegate) {
          @Override
          public void onMessage(Message message, byte[] pattern) {
            try {
              // 直接提取消息内容，只传递String参数
              String messageContent = new String(message.getBody());
              EventMessage eventMessage = JSONUtil.toBean(messageContent, EventMessage.class);
              log.debug(
                  "[Redis事件] 收到消息: channel={}, content={}",
                  new String(message.getChannel()),
                  messageContent);
              try {
                delegate
                    .getClass()
                    .getMethod(methodName, EventMessage.class)
                    .invoke(delegate, eventMessage);
              } catch (Exception e) {
                log.error("调用处理方法[{}]失败", methodName, e);
              }
            } catch (Exception e) {
              log.error(
                  "适配器方法[{}]处理消息失败，channel={}, pattern={}",
                  methodName,
                  new String(message.getChannel()),
                  pattern != null ? new String(pattern) : "null",
                  e);
            }
          }
        };
    // 显式设置字符串转换器，确保消息体被转为String
    return adapter;
  }

  @PostConstruct
  public void setupListeners() {
    log.info("当前实例类型: {}", this.getClass().getName());

    // 电子围栏事件
    MessageListenerAdapter fenceAdapter = createAdapter(this, "handleFenceEvent");
    fenceAdapter.setDefaultListenerMethod("handleFenceEvent");
    container.addMessageListener(fenceAdapter, new ChannelTopic(EventTopics.FENCE_EVENT));

    // TCP下行指令事件（使用Pattern订阅）
    MessageListenerAdapter tcpDownAdapter = createAdapter(this, "handleTcpDownEvent");
    tcpDownAdapter.setDefaultListenerMethod("handleTcpDownEvent");
    container.addMessageListener(tcpDownAdapter, new PatternTopic(EventTopics.TCP_DOWN));

    // 产品配置更新事件
    MessageListenerAdapter productConfigAdapter = createAdapter(this, "handleProductConfigUpdated");
    productConfigAdapter.setDefaultListenerMethod("handleProductConfigUpdated");
    container.addMessageListener(
        productConfigAdapter, new ChannelTopic(EventTopics.PRODUCT_CONFIG_UPDATED));

    // 协议配置更新事件
    MessageListenerAdapter protocolAdapter = createAdapter(this, "handleProtocolUpdated");
    protocolAdapter.setDefaultListenerMethod("handleProtocolUpdated");
    container.addMessageListener(protocolAdapter, new ChannelTopic(EventTopics.PROTOCOL_UPDATED));

    // 应用推送配置更新事件
    MessageListenerAdapter appPushConfigAdapter =
        createAdapter(this, "handleAppPushConfigUpdated");
    appPushConfigAdapter.setDefaultListenerMethod("handleAppPushConfigUpdated");
    container.addMessageListener(
        appPushConfigAdapter, new ChannelTopic(EventTopics.APP_PUSH_CONFIG_UPDATED));

    MessageListenerAdapter deviceCacheAdapter =
        createAdapter(this, "handleDeviceCacheInvalidated");
    deviceCacheAdapter.setDefaultListenerMethod("handleDeviceCacheInvalidated");
    container.addMessageListener(
        deviceCacheAdapter, new ChannelTopic(EventTopics.DEVICE_CACHE_INVALIDATED));

    MessageListenerAdapter websocketPushAdapter = createAdapter(this, "handleDeviceWebSocketPush");
    websocketPushAdapter.setDefaultListenerMethod("handleDeviceWebSocketPush");
    container.addMessageListener(
        websocketPushAdapter, new ChannelTopic(EventTopics.DEVICE_WEBSOCKET_PUSH));
    log.info("[Redis事件订阅] 已启动事件监听器");
  }

  /** 处理电子围栏事件 */
  public void handleFenceEvent(EventMessage message) {
    if (isOwnMessage(message)) {
      return;
    }
    log.info("[Redis事件] 收到电子围栏事件: {}", message);

    try {
      // 使用事件处理器工厂处理电子围栏事件
      eventProcessorFactory.handleFenceEvent(message);
    } catch (Exception e) {
      log.error("[Redis事件] 处理电子围栏事件失败", e);
    }
  }

  /** 处理TCP下行指令事件 */
  public void handleTcpDownEvent(EventMessage message) {
    if (isOwnMessage(message)) {
      return;
    }
    log.info("[Redis事件] 收到TCP下行指令事件: {}", message);

    try {
      // 使用事件处理器工厂处理TCP下行指令
      eventProcessorFactory.handleTcpDown(message);
    } catch (Exception e) {
      log.error("[Redis事件] 处理TCP下行指令事件失败", e);
    }
  }

  /** 处理产品配置更新事件 */
  public void handleProductConfigUpdated(EventMessage message) {
    if (isOwnMessage(message)) {
      return;
    }
    log.info("[Redis事件] 收到产品配置更新事件: {}", message);

    try {
      // 使用事件处理器工厂处理产品配置更新
      eventProcessorFactory.handleProductConfigUpdated(message);
    } catch (Exception e) {
      log.error("[Redis事件] 处理产品配置更新失败", e);
    }
  }

  /** 处理协议配置更新事件 */
  public void handleProtocolUpdated(EventMessage message) {
    if (isOwnMessage(message)) {
      return;
    }
    log.info("[Redis事件] 收到产品配置更新事件: {}", message);

    try {
      // 使用事件处理器工厂处理产品配置更新
      eventProcessorFactory.handleProtocolUpdated(message);
    } catch (Exception e) {
      log.error("[Redis事件] 处理产品配置更新失败", e);
    }
  }

  /** 处理应用推送配置更新事件 */
  public void handleAppPushConfigUpdated(EventMessage message) {
    if (isOwnMessage(message)) {
      return;
    }
    log.info("[Redis事件] 收到应用推送配置更新事件: {}", message);

    try {
      eventProcessorFactory.handleAppPushConfigUpdated(message);
    } catch (Exception e) {
      log.error("[Redis事件] 处理应用推送配置更新失败", e);
    }
  }

  /** 处理设备缓存失效事件 */
  public void handleDeviceCacheInvalidated(EventMessage message) {
    if (isOwnMessage(message)) {
      return;
    }
    try {
      eventProcessorFactory.handleDeviceCacheInvalidated(message);
    } catch (Exception e) {
      log.error("[Redis事件] 处理设备缓存失效失败", e);
    }
  }

  /** 处理设备 WebSocket 推送事件 */
  public void handleDeviceWebSocketPush(EventMessage message) {
    if (isOwnMessage(message)) {
      return;
    }
    log.info("[Redis事件] 收到设备 WebSocket 推送事件: {}", message);

    try {
      eventProcessorFactory.handleDeviceWebSocketPush(message);
    } catch (Exception e) {
      log.error("[Redis事件] 处理设备 WebSocket 推送失败", e);
    }
  }

  /** 判断是否是自己发送的消息 */
  private boolean isOwnMessage(EventMessage message) {
    try {
      String nodeId = message.getNodeId();
      String instanceId = instanceIdProvider.getInstanceId();
      final boolean b = instanceId.equalsIgnoreCase(nodeId);
      log.debug("判断是否自己的消息={},nodeId={}, instanceId={}", b, nodeId, instanceId);
      return b;
    } catch (Exception e) {
      return false;
    }
  }
}
