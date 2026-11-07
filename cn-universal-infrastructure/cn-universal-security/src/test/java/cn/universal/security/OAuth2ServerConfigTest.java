package cn.universal.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTenantMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

class OAuth2ServerConfigTest {

  @Test
  void platformAdminShouldUseReservedPlatformContextWithoutTenantLookup() {
    IoTTenantMapper tenantMapper = mock(IoTTenantMapper.class);
    IoTUser platformAdmin =
        IoTUser.builder()
            .username("nexiot")
            .unionId("nexiot")
            .identity(0)
            .tenantId(null)
            .tenantRole(null)
            .build();
    User userDetails =
        new User(
            "nexiot", "password", List.of(new SimpleGrantedAuthority("WEB")));

    assertNull(OAuth2ServerConfig.resolveCurrentTenant(platformAdmin, tenantMapper));

    JwtClaimsSet.Builder builder = JwtClaimsSet.builder();
    OAuth2ServerConfig.customizeUserClaims(builder, userDetails, platformAdmin, null);
    JwtClaimsSet claims = builder.build();

    assertEquals("nexiot", claims.getSubject());
    assertEquals("nexiot", claims.getClaim("unionId"));
    assertEquals("0", claims.getClaim("tenantId"));
    assertEquals("0", claims.getClaim("tenantCode"));
    assertEquals("PLATFORM_ADMIN", claims.getClaim("tenantRole"));
    assertEquals("平台管理", claims.getClaim("tenantName"));
    assertEquals(List.of("WEB"), claims.getClaim("roles"));
    verifyNoInteractions(tenantMapper);
  }

  @Test
  void missingOptionalTenantClaimsShouldNotBreakTokenCreation() {
    IoTUser user =
        IoTUser.builder().username("legacy").unionId("legacy").identity(2).build();
    User userDetails = new User("legacy", "password", List.of());
    JwtClaimsSet.Builder builder = JwtClaimsSet.builder();

    OAuth2ServerConfig.customizeUserClaims(builder, userDetails, user, null);
    JwtClaimsSet claims = builder.build();

    assertEquals("legacy", claims.getSubject());
    assertEquals("legacy", claims.getClaim("tenantId"));
    assertNull(claims.getClaim("tenantRole"));
    assertNull(claims.getClaim("tenantName"));
  }
}
