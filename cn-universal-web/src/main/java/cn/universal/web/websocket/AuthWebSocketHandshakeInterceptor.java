package cn.universal.web.websocket;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.service.UserWebSocketPushSettingService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.service.IoTUserService;
import jakarta.annotation.Resource;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class AuthWebSocketHandshakeInterceptor implements HandshakeInterceptor {

  @Resource private JwtDecoder jwtDecoder;

  @Resource private IoTUserService ioTUserService;

  @Resource private UserWebSocketPushSettingService userWebSocketPushSettingService;

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    String token = resolveToken(request);
    if (StrUtil.isBlank(token)) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }
    token = StrUtil.removePrefixIgnoreCase(token, "Bearer ").trim();
    try {
      Jwt jwt = jwtDecoder.decode(token);
      String unionId = StrUtil.blankToDefault(jwt.getSubject(), jwt.getClaimAsString("unionId"));
      if (StrUtil.isBlank(unionId)) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }
      IoTUser user = ioTUserService.selectUserByUnionId(unionId);
      if (user == null) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }
      attributes.put("unionId", unionId);
      attributes.put("admin", user.isAdmin());
      attributes.put("tenantId", StrUtil.blankToDefault(user.getTenantId(), user.getUnionId()));
      attributes.put(
          "tenantAdmin",
          !user.isAdmin() && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN"));
      attributes.put("parentUnionId", user.getParentUnionId());
      attributes.put("settings", userWebSocketPushSettingService.getSettings(user));
      return true;
    } catch (Exception e) {
//      log.warn("WebSocket握手鉴权失败: uri={}", request.getURI(), e);
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // no-op
  }

  private String resolveToken(ServerHttpRequest request) {
    HttpHeaders headers = request.getHeaders();
    String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (StrUtil.isNotBlank(authorization)) {
      return authorization;
    }

    URI uri = request.getURI();
    return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
  }
}
