package nexiot.web.ide.debug.plugins.auth;

import cn.hutool.core.util.StrUtil;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.config.MagicConfiguration;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.exception.MagicLoginException;
import nexiot.web.ide.debug.plugins.core.interceptor.Authorization;
import nexiot.web.ide.debug.plugins.core.interceptor.AuthorizationInterceptor;
import nexiot.web.ide.debug.plugins.core.model.Group;
import nexiot.web.ide.debug.plugins.core.model.MagicEntity;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** 自定义用户名密码登录 */
@Component // 注入到Spring容器中
@Slf4j
public class CustomAuthorizationInterceptor implements AuthorizationInterceptor {

  private final Map<String, Object> user = new ConcurrentHashMap<>();
  @Autowired private MagicSsoSessionService ssoSessions;
  @Autowired private UserDetailsService userDetailsService;

  /** 配置是否需要登录 */
  @Override
  public boolean requireLogin() {
    return true;
  }

  /** 根据Token获取User */
  @Override
  public MagicUser getUserByToken(String token) throws MagicLoginException {
    if (StrUtil.isBlank(token)) throw new MagicLoginException("token无效");
    if (MagicSsoSessionService.supports(token)) return ssoSessions.getUser(token);
    String username = SymmetricEncryptionUtil.desDecryptWithBuiltinKey(token);
    if (user.containsKey(username)) {
      return new MagicUser(
          username, username, SymmetricEncryptionUtil.desEncryptWithBuiltinKey(username));
    }
    throw new MagicLoginException("token无效");
  }

  @Override
  public MagicUser login(String username, String password) throws MagicLoginException {
    // 根据实际情况进行修改，如查询数据库。。
    if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
      throw new MagicLoginException("用户名或密码不能为空");
    }
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    // 注释密码验证，专注解决类型转换问题
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    if (!passwordEncoder.matches(password, userDetails.getPassword())) {
      throw new MagicLoginException("用户名或密码错误");
    }
    user.put(username, SymmetricEncryptionUtil.desEncryptWithBuiltinKey(username));
    return new MagicUser(
        username, username, SymmetricEncryptionUtil.desEncryptWithBuiltinKey(username));
  }

  @Override
  public void logout(String token) {
    if (MagicSsoSessionService.supports(token)) ssoSessions.revoke(token);
  }

  @Override
  public void refreshToken(MagicUser user) {
    if (MagicSsoSessionService.supports(user.getToken())) {
      try {
        user.setTimeout(ssoSessions.getUser(user.getToken()).getTimeout());
      } catch (MagicLoginException exception) {
        throw new IllegalStateException("平台登录已失效", exception);
      }
    }
  }

  private boolean scoped(MagicUser user) {
    return user != null
        && MagicSsoSessionService.supports(user.getToken())
        && !ssoSessions.isAdmin(user);
  }

  @Override
  public boolean allowRequest(MagicUser user, String controller, String method) {
    if (!scoped(user)) return true;
    // The workspace's datasource, backup, push and reload APIs are global, not tenant resources.
    if (controller.equals("MagicResourceController")) return true;
    return controller.equals("MagicWorkbenchController")
        && Set.of("user", "clazz", "search", "todo", "download").contains(method);
  }

  @Override
  public boolean allowVisit(MagicUser user, MagicHttpServletRequest request, Authorization action) {
    return !scoped(user)
        || (action != Authorization.UPLOAD
            && action != Authorization.PUSH
            && action != Authorization.RELOAD);
  }

  private boolean ownsParent(MagicUser user, String id) {
    if (Constants.ROOT_ID.equals(id)) return true;
    Group parent = MagicConfiguration.getMagicResourceService().getGroup(id);
    return parent != null && Objects.equals(user.getUsername(), parent.getCreateBy());
  }

  @Override
  public boolean allowVisit(
      MagicUser magicUser,
      MagicHttpServletRequest request,
      Authorization authorization,
      MagicEntity entity) {
    if (scoped(magicUser)) {
      if (entity == null) return false;
      MagicEntity persisted =
          entity.getId() == null
              ? null
              : MagicConfiguration.getMagicResourceService().file(entity.getId());
      if (persisted != null && !Objects.equals(magicUser.getUsername(), persisted.getCreateBy()))
        return false;
      if (authorization == Authorization.SAVE) return ownsParent(magicUser, entity.getGroupId());
      return persisted != null;
    }
    if (entity.getCreateBy() == null) {
      return true;
    }
    if (magicUser.getUsername().equalsIgnoreCase(entity.getCreateBy())) {
      return true;
    }
    log.info("magicUser:{}, request:{}, entity:{}", magicUser, request, entity);
    return true;
  }

  @Override
  public boolean allowVisit(
      MagicUser magicUser,
      MagicHttpServletRequest request,
      Authorization authorization,
      Group group) {
    if (scoped(magicUser)) {
      if (group == null) return false;
      if (Constants.ROOT_ID.equals(group.getId())) return authorization == Authorization.VIEW;
      Group persisted =
          group.getId() == null
              ? null
              : MagicConfiguration.getMagicResourceService().getGroup(group.getId());
      if (persisted != null && !Objects.equals(magicUser.getUsername(), persisted.getCreateBy()))
        return false;
      if (persisted != null) {
        group.setCreateBy(persisted.getCreateBy());
        group.setCreateTime(persisted.getCreateTime());
      }
      if (authorization == Authorization.SAVE || authorization == Authorization.DELETE)
        return ownsParent(magicUser, group.getParentId());
      return persisted != null;
    }
    log.info("magicUser:{}, request:{}, group:{}", magicUser, request, group);
    if (group.getCreateBy() == null) {
      return true;
    }
    if (magicUser.getUsername().equalsIgnoreCase(group.getCreateBy())) {
      return true;
    }
    return false;
  }
}
