package cn.universal.manager.notice.service;

import cn.universal.manager.notice.model.NoticeOwnedResource;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.DeptDataPermissionSupport;
import cn.universal.security.support.TenantAccessPolicy;
import cn.universal.security.support.TenantIgnoreContext;
import cn.universal.security.utils.SecurityUtils;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tk.mybatis.mapper.entity.Example;

/** One policy for console, application tokens, queries and mutations. No identity means no CRUD. */
@Component
public class NoticeAccess {
  private static final ThreadLocal<IoTUser> EXECUTION_ACTOR = new ThreadLocal<>();
  private final IoTUserService users;
  private final OauthClientDetailsMapper clients;
  private final DeptDataPermissionSupport departments;

  public NoticeAccess(
      IoTUserService users,
      OauthClientDetailsMapper clients,
      DeptDataPermissionSupport departments) {
    this.users = users;
    this.clients = clients;
    this.departments = departments;
  }

  public IoTUser currentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken)
      return EXECUTION_ACTOR.get();
    if (!authentication.isAuthenticated()) throw denied();
    String subject = SecurityUtils.getUnionIdQuietly();
    if (subject == null) throw denied();
    Jwt jwt = authentication.getPrincipal() instanceof Jwt value ? value : null;
    String clientId = jwt == null ? null : jwt.getClaimAsString("clientId");
    IoTUser user;
    if (clientId != null) {
      if (!subject.equals(clientId)) throw denied();
      OAuth2ClientDetails client = unscoped(() -> clients.findByClientId(clientId));
      if (client == null || client.getIotUnionId() == null) throw denied();
      String owner = jwt.getClaimAsString("unionId");
      if (owner != null && !owner.equals(client.getIotUnionId())) throw denied();
      user = user(client.getIotUnionId());
    } else user = user(subject);
    if (user == null
        || Integer.valueOf(1).equals(user.getDeleted())
        || Integer.valueOf(1).equals(user.getStatus())) throw denied();
    return user;
  }

  /** Restore the trusted initiator for synchronous rule execution or a one-time OAuth callback. */
  public <T> T runAs(String unionId, Supplier<T> action) {
    IoTUser authenticated = currentUser();
    if (authenticated != null) {
      if (!Objects.equals(authenticated.getUnionId(), unionId)) throw denied();
      return action.get();
    }
    IoTUser actor = user(unionId);
    if (actor == null
        || Integer.valueOf(1).equals(actor.getDeleted())
        || Integer.valueOf(1).equals(actor.getStatus())) throw denied();
    IoTUser previous = EXECUTION_ACTOR.get();
    EXECUTION_ACTOR.set(actor);
    try {
      return action.get();
    } finally {
      if (previous == null) EXECUTION_ACTOR.remove();
      else EXECUTION_ACTOR.set(previous);
    }
  }

  public IoTUser requireUser() {
    IoTUser user = currentUser();
    if (user == null) throw denied();
    return user;
  }

  private IoTUser user(String creator) {
    return creator == null ? null : unscoped(() -> users.selectUserByUnionId(creator));
  }

  public void initialize(NoticeOwnedResource resource) {
    IoTUser user = requireUser();
    resource.setTenantId(TenantAccessPolicy.resolveTenantId(user));
    resource.setDeptId(user.getDeptId());
  }

  public void inherit(NoticeOwnedResource target, NoticeOwnedResource source) {
    target.setTenantId(source.getTenantId());
    target.setDeptId(source.getDeptId());
  }

  /** Legacy rows remain visible while the resumable ownership backfill is running. */
  public void hydrate(NoticeOwnedResource resource, String creator) {
    if (resource != null && (resource.getTenantId() == null || resource.getTenantId().isBlank())) {
      IoTUser owner = user(creator);
      if (owner != null) {
        resource.setTenantId(TenantAccessPolicy.resolveTenantId(owner));
        if (resource.getDeptId() == null) resource.setDeptId(owner.getDeptId());
      }
    }
  }

  public void check(NoticeOwnedResource resource, String creator) {
    hydrate(resource, creator);
    IoTUser actor = currentUser();
    // Internal rule execution and the one-time OAuth callback have no interactive identity.
    if (actor == null) return;
    checkAs(actor, resource, creator);
  }

  public void checkAs(IoTUser actor, NoticeOwnedResource resource, String creator) {
    hydrate(resource, creator);
    if (actor == null || resource == null) throw denied();
    if (actor.isAdmin()) return;
    if (!Objects.equals(TenantAccessPolicy.resolveTenantId(actor), resource.getTenantId()))
      throw denied();
    var scope = departments.resolveScope(actor.getUnionId());
    if (scope == null) throw denied();
    if (!scope.isAll()
        && !scope.isTenantAdmin()
        && (creator == null || !scope.getScopeUnionIds().contains(creator))
        && (resource.getDeptId() == null
            || !scope.getScopeDeptIds().contains(resource.getDeptId()))) throw denied();
  }

  public void checkCreator(String creator, NoticeOwnedResource resource, String resourceCreator) {
    IoTUser actor = user(creator);
    if (actor == null
        || Integer.valueOf(1).equals(actor.getDeleted())
        || Integer.valueOf(1).equals(actor.getStatus())) throw denied();
    checkAs(actor, resource, resourceCreator);
  }

  public void sameTenant(NoticeOwnedResource left, NoticeOwnedResource right) {
    if (left.getTenantId() == null || !Objects.equals(left.getTenantId(), right.getTenantId()))
      throw new IllegalArgumentException("通知模板与渠道必须属于同一租户");
  }

  /** Scope is part of SQL, before sorting/pagination; every value is a bound parameter. */
  public void scope(Example example) {
    IoTUser actor = requireUser();
    if (actor.isAdmin()) return;
    var scope = departments.resolveScope(actor.getUnionId());
    if (scope == null) throw denied();
    String table = example.getEntityClass().getAnnotation(jakarta.persistence.Table.class).name();
    String owner =
        "(SELECT COALESCE(NULLIF(u.tenant_id, ''), u.union_id) FROM iot_user u WHERE u.union_id = "
            + table
            + ".creator)";
    example
        .and()
        .andCondition(
            "COALESCE(NULLIF(" + table + ".tenant_id, ''), " + owner + ") =", scope.getTenantId());
    if (scope.isAll() || scope.isTenantAdmin()) return;
    Example.Criteria visible = example.and();
    if (!scope.getScopeDeptIds().isEmpty()) {
      String department =
          "CASE WHEN "
              + table
              + ".tenant_id IS NULL OR "
              + table
              + ".tenant_id = '' THEN COALESCE("
              + table
              + ".dept_id, (SELECT u.dept_id FROM iot_user u WHERE u.union_id = "
              + table
              + ".creator)) ELSE "
              + table
              + ".dept_id END";
      visible.andCondition(department + " in", scope.getScopeDeptIds());
    }
    if (!scope.getScopeUnionIds().isEmpty()) visible.orIn("creator", scope.getScopeUnionIds());
    if (scope.getScopeDeptIds().isEmpty() && scope.getScopeUnionIds().isEmpty())
      visible.andCondition("1 = 0");
  }

  public static <T> T unscoped(Supplier<T> action) {
    TenantIgnoreContext.enter();
    try {
      return action.get();
    } finally {
      TenantIgnoreContext.exit();
    }
  }

  private static ResponseStatusException denied() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限访问该通知资源");
  }
}
