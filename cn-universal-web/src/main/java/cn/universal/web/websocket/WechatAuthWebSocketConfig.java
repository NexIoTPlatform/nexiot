package cn.universal.web.websocket;

import cn.universal.security.wechat.WechatAuthService;
import java.net.URI;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class WechatAuthWebSocketConfig implements WebSocketConfigurer {
  private final WechatAuthWebSocketHandler handler;
  private final WechatAuthService auth;

  public WechatAuthWebSocketConfig(WechatAuthWebSocketHandler handler, WechatAuthService auth) {
    this.handler = handler;
    this.auth = auth;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(handler, "/ws/wechat-auth")
        .addInterceptors(new TicketInterceptor(auth))
        .setAllowedOriginPatterns("*");
  }

  static class TicketInterceptor implements HandshakeInterceptor {
    private final WechatAuthService auth;

    TicketInterceptor(WechatAuthService auth) {
      this.auth = auth;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes) {
      try {
        String origin = request.getHeaders().getOrigin();
        String host = request.getHeaders().getFirst("Host");
        if (!sameOrigin(origin, host)) {
          response.setStatusCode(HttpStatus.FORBIDDEN);
          return false;
        }
        var parameters = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        var values = parameters.get("ticket");
        if (values == null || values.size() != 1) throw new IllegalArgumentException();
        attributes.put("wechatSessionId", auth.consumeWebSocketTicket(values.getFirst()));
        return true;
      } catch (RuntimeException e) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler handler,
        Exception exception) {}

    static boolean sameOrigin(String value, String host) {
      if (value == null || host == null) return false;
      try {
        URI origin = URI.create(value);
        if (!("http".equalsIgnoreCase(origin.getScheme())
                || "https".equalsIgnoreCase(origin.getScheme()))
            || origin.getUserInfo() != null
            || origin.getHost() == null
            || origin.getQuery() != null
            || origin.getFragment() != null
            || (origin.getPath() != null && !origin.getPath().isEmpty())) return false;
        URI authority = URI.create("//" + host);
        if (authority.getHost() == null
            || authority.getUserInfo() != null
            || authority.getQuery() != null
            || authority.getFragment() != null
            || (authority.getPath() != null && !authority.getPath().isEmpty())
            || authority.getPort() > 65535
            || origin.getPort() > 65535
            || host.endsWith(":")) return false;
        if (!origin.getHost().equalsIgnoreCase(authority.getHost())) return false;
        int defaultPort = "https".equalsIgnoreCase(origin.getScheme()) ? 443 : 80;
        int originPort = origin.getPort() < 0 ? defaultPort : origin.getPort();
        int requestPort = authority.getPort() < 0 ? defaultPort : authority.getPort();
        return originPort == requestPort;
      } catch (RuntimeException e) {
        return false;
      }
    }
  }
}
