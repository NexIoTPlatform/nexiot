package cn.universal.web.websocket;

import cn.universal.admin.system.service.UserWebSocketPushSettingService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class DeviceEventWebSocketConfig implements WebSocketConfigurer {

  @Resource private DeviceEventWebSocketHandler deviceEventWebSocketHandler;

  @Resource private VideoShareOverlayWebSocketHandler videoShareOverlayWebSocketHandler;

  @Resource private AuthWebSocketHandshakeInterceptor authWebSocketHandshakeInterceptor;

  @Resource private VideoSdkTalkWebSocketHandler videoSdkTalkWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // A 20-second single-use ticket is issued only after REST permission/tenant checks.
    registry.addHandler(videoSdkTalkWebSocketHandler, "/ws/sdk-talk").setAllowedOriginPatterns("*");
    registry
        .addHandler(
            deviceEventWebSocketHandler, UserWebSocketPushSettingService.DEFAULT_WEBSOCKET_PATH)
        .addInterceptors(authWebSocketHandshakeInterceptor)
        .setAllowedOriginPatterns("*");

    // 公开视频分享不走后台登录鉴权，token 在 VideoShareOverlayWebSocketHandler 内部校验。
    registry
        .addHandler(videoShareOverlayWebSocketHandler, "/ws/video-share", "/ws/video-share/**")
        .setAllowedOriginPatterns("*");
  }
}
