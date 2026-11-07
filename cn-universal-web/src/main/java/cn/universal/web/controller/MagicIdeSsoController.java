package cn.universal.web.controller;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.TenantAccessPolicy;
import cn.universal.security.support.TenantDomainResolver;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import nexiot.web.ide.debug.plugins.auth.MagicSsoSessionService;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.exception.MagicLoginException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exchange an authenticated platform session for a short-lived Magic IDE credential. */
@RestController
public class MagicIdeSsoController {
  private final JwtDecoder decoder;
  private final IoTUserService users;
  private final SysPermissionService permissions;
  private final TenantDomainResolver domains;
  private final MagicSsoSessionService sessions;

  public MagicIdeSsoController(
      JwtDecoder decoder,
      IoTUserService users,
      SysPermissionService permissions,
      TenantDomainResolver domains,
      MagicSsoSessionService sessions) {
    this.decoder = decoder;
    this.users = users;
    this.permissions = permissions;
    this.domains = domains;
    this.sessions = sessions;
  }

  @PostMapping("/admin/v1/magic-ide/session")
  public ResponseEntity<Map<String, Object>> exchange(@AuthenticationPrincipal Jwt jwt)
      throws MagicLoginException {
    if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录平台");
    IoTUser user = validate(jwt);
    String unionId = user.getUnionId();
    String tenantId = TenantAccessPolicy.resolveTenantId(user);
    boolean admin = user.isAdmin();
    IoTTenant hostTenant = domains.resolveCurrentTenant();
    if (hostTenant != null && !StrUtil.equals(hostTenant.getTenantId(), tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前登录与访问域名的租户不一致");
    }
    MagicUser magicUser =
        sessions.issue(
            unionId,
            tenantId,
            admin,
            jwt.getExpiresAt(),
            () -> {
              // Reuse the real platform decoder (signature, expiry, account and tenant validation).
              IoTUser current = validate(decoder.decode(jwt.getTokenValue()));
              if (!StrUtil.equals(unionId, current.getUnionId())
                  || !StrUtil.equals(tenantId, TenantAccessPolicy.resolveTenantId(current))
                  || admin != current.isAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "平台身份已变更");
              }
            });
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            Map.of(
                "token",
                magicUser.getToken(),
                "userId",
                unionId,
                "tenantId",
                tenantId,
                "workspaceAdmin",
                admin,
                "expiresIn",
                magicUser.getTimeout(),
                "expiresAt",
                Instant.now().plusSeconds(magicUser.getTimeout()).toEpochMilli()));
  }

  private IoTUser validate(Jwt jwt) {
    // A client_credentials/application token must never be promoted to an interactive IDE login.
    if (StrUtil.isNotBlank(jwt.getClaimAsString("clientId"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "应用令牌不能用于调试器登录");
    }
    String unionId = StrUtil.blankToDefault(jwt.getSubject(), jwt.getClaimAsString("unionId"));
    IoTUser user = StrUtil.isBlank(unionId) ? null : users.selectUserByUnionId(unionId);
    if (jwt.getExpiresAt() == null
        || !jwt.getExpiresAt().isAfter(Instant.now())
        || user == null
        || !Integer.valueOf(0).equals(user.getStatus())
        || Integer.valueOf(1).equals(user.getDeleted())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "平台登录已失效");
    }
    String claimedTenant = jwt.getClaimAsString("tenantId");
    if (StrUtil.isNotBlank(claimedTenant)
        && !StrUtil.equals(claimedTenant, TenantAccessPolicy.resolveTenantId(user))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "平台租户已变更");
    }
    Set<String> grants = permissions.getMenuPermission(user);
    if (!grants.contains("*:*:*") && !grants.contains("protocol:protocol:codec")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "没有驱动调试权限");
    }
    return user;
  }
}
