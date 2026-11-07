package cn.universal.security.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import java.util.Arrays;
import java.util.Date;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Validates request-time user and tenant state for already issued JWTs. */
public class TenantJwtValidator implements OAuth2TokenValidator<Jwt> {

  private final IoTUserService ioTUserService;
  private final IoTTenantMapper ioTTenantMapper;
  private final OauthClientDetailsMapper oauthClientDetailsMapper;

  public TenantJwtValidator(
      IoTUserService ioTUserService,
      IoTTenantMapper ioTTenantMapper,
      OauthClientDetailsMapper oauthClientDetailsMapper) {
    this.ioTUserService = ioTUserService;
    this.ioTTenantMapper = ioTTenantMapper;
    this.oauthClientDetailsMapper = oauthClientDetailsMapper;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    String subject = resolveSubject(jwt);
    if (StrUtil.isBlank(subject)) {
      return failure("无法获取用户信息");
    }
    IoTUser user = ioTUserService.selectUserByUnionId(subject);
    if (user == null) {
      return validateClientToken(jwt, subject);
    }
    if (IoTConstant.UN_NORMAL.equals(user.getStatus())) {
      return failure("用户已冻结");
    }
    if (user.isAdmin()) {
      return OAuth2TokenValidatorResult.success();
    }

    String tenantId = StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
    String tokenTenantId = jwt.getClaimAsString("tenantId");
    if (StrUtil.isNotBlank(tokenTenantId) && !StrUtil.equals(tenantId, tokenTenantId)) {
      return failure("登录租户已变更，请重新登录");
    }
    if (StrUtil.isBlank(tenantId)) {
      return failure("当前租户信息不存在");
    }

    return validateTenant(tenantId);
  }

  private OAuth2TokenValidatorResult validateClientToken(Jwt jwt, String clientId) {
    OAuth2ClientDetails clientDetails = oauthClientDetailsMapper.findByClientId(clientId);
    if (clientDetails == null) {
      return failure("用户或客户端不存在");
    }
    if (!containsGrantType(clientDetails.getAuthorizedGrantTypes(), "client_credentials")) {
      return failure("客户端已停用");
    }

    IoTUser owner = null;
    if (StrUtil.isNotBlank(clientDetails.getIotUnionId())) {
      owner = ioTUserService.selectUserByUnionId(clientDetails.getIotUnionId());
      if (owner == null) {
        return failure("客户端所属用户不存在或已删除");
      }
      if (IoTConstant.UN_NORMAL.equals(owner.getStatus())) {
        return failure("客户端所属用户已冻结");
      }
    }

    String tenantId = StrUtil.blankToDefault(clientDetails.getTenantId(), resolveTenantId(owner));
    String tokenTenantId = jwt.getClaimAsString("tenantId");
    if (StrUtil.isNotBlank(tokenTenantId) && !StrUtil.equals(tenantId, tokenTenantId)) {
      return failure("客户端所属租户已变更，请重新获取令牌");
    }
    String tokenUnionId = jwt.getClaimAsString("unionId");
    if (StrUtil.isNotBlank(tokenUnionId)
        && StrUtil.isNotBlank(clientDetails.getIotUnionId())
        && !StrUtil.equals(clientDetails.getIotUnionId(), tokenUnionId)) {
      return failure("客户端所属用户已变更，请重新获取令牌");
    }
    if (StrUtil.isBlank(tenantId)) {
      return failure("当前租户信息不存在");
    }

    return validateTenant(tenantId);
  }

  private OAuth2TokenValidatorResult validateTenant(String tenantId) {
    IoTTenant tenant = ioTTenantMapper.selectByPrimaryKey(tenantId);
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
      return failure("当前租户不存在或已删除");
    }
    if (Integer.valueOf(1).equals(tenant.getStatus())) {
      return failure("当前租户已停用");
    }
    if (tenant.getExpireTime() != null && tenant.getExpireTime().before(new Date())) {
      return failure("当前租户已过期");
    }
    return OAuth2TokenValidatorResult.success();
  }

  private String resolveSubject(Jwt jwt) {
    if (jwt == null) {
      return null;
    }
    return StrUtil.blankToDefault(jwt.getSubject(), jwt.getClaimAsString("unionId"));
  }

  private String resolveTenantId(IoTUser iotUser) {
    if (iotUser == null) {
      return null;
    }
    return StrUtil.blankToDefault(iotUser.getTenantId(), iotUser.getUnionId());
  }

  private boolean containsGrantType(String authorizedGrantTypes, String grantType) {
    if (StrUtil.isBlank(authorizedGrantTypes)) {
      return false;
    }
    return Arrays.stream(authorizedGrantTypes.split(","))
        .map(String::trim)
        .anyMatch(grantType::equals);
  }

  private OAuth2TokenValidatorResult failure(String description) {
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
  }
}
