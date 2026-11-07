package cn.universal.security.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

@Component
public class TenantLoginResolver {

  @Resource private IoTUserMapper ioTUserMapper;
  @Resource private IoTTenantMapper ioTTenantMapper;
  @Resource private TenantDomainResolver tenantDomainResolver;

  public IoTUser resolveLoginUser(String username, String tenantCode) {
    List<IoTUser> candidates = selectCandidates(username);
    if (CollectionUtils.isEmpty(candidates)) {
      return null;
    }
    if (!StringUtils.hasText(tenantCode)) {
      return resolveWithoutTenant(candidates);
    }
    IoTTenant tenant = resolveTenant(tenantCode);
    for (IoTUser candidate : candidates) {
      if (StrUtil.equals(resolveTenantId(candidate), tenant.getTenantId())) {
        return candidate;
      }
    }
    throw loginError("账号不属于所选租户");
  }

  private IoTUser resolveWithoutTenant(List<IoTUser> candidates) {
    IoTTenant currentDomainTenant = resolveCurrentDomainTenant();
    IoTUser domainMatchedUser = resolveDomainMatchedUser(candidates, currentDomainTenant);
    if (domainMatchedUser != null) {
      return domainMatchedUser;
    }
    List<IoTUser> platformAdmins = new ArrayList<>();
    for (IoTUser candidate : candidates) {
      if (candidate != null && candidate.isAdmin()) {
        platformAdmins.add(candidate);
      }
    }
    if (platformAdmins.size() == 1) {
      return platformAdmins.get(0);
    }
    if (candidates.size() == 1) {
      IoTUser user = candidates.get(0);
      if (user != null
          && !user.isAdmin()
          && StringUtils.hasText(user.getTenantId())) {
        if (currentDomainTenant != null) {
          throw loginError("当前账号不属于当前域名对应租户");
        }
        throw loginError("租户账号登录请联系管理员");
      }
      return user;
    }
    throw loginError("该账号存在多个租户，请先选择租户");
  }

  private IoTTenant resolveCurrentDomainTenant() {
    try {
      return tenantDomainResolver.resolveCurrentTenant();
    } catch (IoTException ex) {
      throw loginError(ex.getMessage());
    }
  }

  private IoTUser resolveDomainMatchedUser(List<IoTUser> candidates, IoTTenant currentDomainTenant) {
    if (currentDomainTenant == null || CollectionUtils.isEmpty(candidates)) {
      return null;
    }
    List<IoTUser> matchedUsers = new ArrayList<>();
    for (IoTUser candidate : candidates) {
      if (candidate != null
          && StrUtil.equals(resolveTenantId(candidate), currentDomainTenant.getTenantId())) {
        matchedUsers.add(candidate);
      }
    }
    if (matchedUsers.isEmpty()) {
      return null;
    }
    if (matchedUsers.size() > 1) {
      throw loginError("当前域名下存在多个同名账号，请联系管理员处理");
    }
    return matchedUsers.get(0);
  }

  private List<IoTUser> selectCandidates(String username) {
    if (!StringUtils.hasText(username)) {
      return List.of();
    }
    Example usernameExample = new Example(IoTUser.class);
    usernameExample
        .createCriteria()
        .andEqualTo("username", username)
        .andEqualTo("deleted", 0);
    List<IoTUser> candidates = ioTUserMapper.selectByExample(usernameExample);
    if (!CollectionUtils.isEmpty(candidates)) {
      return candidates;
    }
    Example unionIdExample = new Example(IoTUser.class);
    unionIdExample
        .createCriteria()
        .andEqualTo("unionId", username)
        .andEqualTo("deleted", 0);
    return ioTUserMapper.selectByExample(unionIdExample);
  }

  private IoTTenant resolveTenant(String tenantCode) {
    Example example = new Example(IoTTenant.class);
    example
        .createCriteria()
        .andEqualTo("tenantCode", tenantCode)
        .andEqualTo("deleted", 0);
    IoTTenant tenant = ioTTenantMapper.selectOneByExample(example);
    if (tenant == null) {
      throw loginError("所选租户不存在");
    }
    if (Integer.valueOf(1).equals(tenant.getStatus())) {
      throw loginError("当前租户已停用");
    }
    if (tenant.getExpireTime() != null && tenant.getExpireTime().before(new Date())) {
      throw loginError("当前租户已过期");
    }
    return tenant;
  }

  private String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  private OAuth2AuthenticationException loginError(String message) {
    return new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", message, null));
  }
}
