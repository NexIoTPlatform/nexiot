package cn.universal.security.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TenantJwtValidatorTest {

  @Mock private IoTUserService ioTUserService;
  @Mock private IoTTenantMapper ioTTenantMapper;
  @Mock private OauthClientDetailsMapper oauthClientDetailsMapper;

  private TenantJwtValidator validator;

  @BeforeEach
  void setUp() {
    validator = new TenantJwtValidator(ioTUserService, ioTTenantMapper, oauthClientDetailsMapper);
  }

  @Test
  void validateShouldRejectExpiredTenant() {
    when(ioTUserService.selectUserByUnionId("user-1"))
        .thenReturn(
            IoTUser.builder()
                .identity(1)
                .unionId("user-1")
                .tenantId("tenant-a")
                .status(IoTConstant.NORMAL)
                .build());
    when(ioTTenantMapper.selectByPrimaryKey("tenant-a"))
        .thenReturn(
            IoTTenant.builder()
                .tenantId("tenant-a")
                .status(0)
                .deleted(0)
                .expireTime(new Date(System.currentTimeMillis() - 1000))
                .build());

    OAuth2TokenValidatorResult result = validator.validate(jwt("user-1", "tenant-a"));

    assertTrue(result.hasErrors());
  }

  @Test
  void validateShouldRejectTenantClaimMismatch() {
    when(ioTUserService.selectUserByUnionId("user-1"))
        .thenReturn(
            IoTUser.builder()
                .identity(1)
                .unionId("user-1")
                .tenantId("tenant-a")
                .status(IoTConstant.NORMAL)
                .build());

    OAuth2TokenValidatorResult result = validator.validate(jwt("user-1", "tenant-b"));

    assertTrue(result.hasErrors());
    verify(ioTTenantMapper, never()).selectByPrimaryKey("tenant-a");
  }

  @Test
  void validateShouldAcceptActiveTenant() {
    when(ioTUserService.selectUserByUnionId("user-1"))
        .thenReturn(
            IoTUser.builder()
                .identity(1)
                .unionId("user-1")
                .tenantId("tenant-a")
                .status(IoTConstant.NORMAL)
                .build());
    when(ioTTenantMapper.selectByPrimaryKey("tenant-a"))
        .thenReturn(
            IoTTenant.builder().tenantId("tenant-a").status(0).deleted(0).expireTime(null).build());

    OAuth2TokenValidatorResult result = validator.validate(jwt("user-1", "tenant-a"));

    assertFalse(result.hasErrors());
  }

  @Test
  void validateShouldAcceptClientCredentialsTokenWithActiveTenant() {
    when(ioTUserService.selectUserByUnionId("app-1")).thenReturn(null);
    when(oauthClientDetailsMapper.findByClientId("app-1"))
        .thenReturn(
            OAuth2ClientDetails.builder()
                .clientId("app-1")
                .iotUnionId("user-1")
                .tenantId("tenant-a")
                .AuthorizedGrantTypes("client_credentials")
                .build());
    when(ioTUserService.selectUserByUnionId("user-1"))
        .thenReturn(
            IoTUser.builder()
                .identity(1)
                .unionId("user-1")
                .tenantId("tenant-a")
                .status(IoTConstant.NORMAL)
                .build());
    when(ioTTenantMapper.selectByPrimaryKey("tenant-a"))
        .thenReturn(
            IoTTenant.builder().tenantId("tenant-a").status(0).deleted(0).expireTime(null).build());

    OAuth2TokenValidatorResult result = validator.validate(jwt("app-1", "tenant-a", "user-1"));

    assertFalse(result.hasErrors());
  }

  @Test
  void validateShouldRejectDisabledClientCredentialsToken() {
    when(ioTUserService.selectUserByUnionId("app-1")).thenReturn(null);
    when(oauthClientDetailsMapper.findByClientId("app-1"))
        .thenReturn(
            OAuth2ClientDetails.builder()
                .clientId("app-1")
                .iotUnionId("user-1")
                .tenantId("tenant-a")
                .AuthorizedGrantTypes("")
                .build());

    OAuth2TokenValidatorResult result = validator.validate(jwt("app-1", "tenant-a", "user-1"));

    assertTrue(result.hasErrors());
    verify(ioTTenantMapper, never()).selectByPrimaryKey("tenant-a");
  }

  private Jwt jwt(String unionId, String tenantId) {
    return jwt(unionId, tenantId, null);
  }

  private Jwt jwt(String unionId, String tenantId, String ownerUnionId) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(unionId)
            .claim("tenantId", tenantId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600));
    if (ownerUnionId != null) {
      builder.claim("unionId", ownerUnionId);
    }
    return builder.build();
  }
}
