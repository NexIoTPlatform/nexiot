package cn.universal.web.service.openapi.v2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.security.service.IoTUserService;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class OpenApiV2AccessTest {
  private final IoTUserApplicationMapper applications = mock(IoTUserApplicationMapper.class);
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final IoTDeviceService deviceService = mock(IoTDeviceService.class);
  private final IoTProductDeviceService products = mock(IoTProductDeviceService.class);
  private final IoTUserService users = mock(IoTUserService.class);
  private final OpenApiV2Access access =
      new OpenApiV2Access(applications, devices, deviceService, products, users);
  private IoTUserApplication app;
  private IoTDevice row;
  private IoTDeviceDTO dto;
  private IoTProduct product;

  @BeforeEach
  void setup() {
    app =
        IoTUserApplication.builder()
            .appId("client")
            .appUniqueId("app")
            .unionId("owner")
            .tenantId("tenant")
            .appStatus(0)
            .deleted(0)
            .build();
    row =
        IoTDevice.builder()
            .iotId("iot")
            .productKey("product")
            .deviceId("device")
            .creatorId("owner")
            .tenantId("tenant")
            .application("app")
            .build();
    dto =
        IoTDeviceDTO.builder()
            .iotId("iot")
            .productKey("product")
            .deviceId("device")
            .userUnionId("owner")
            .tenantId("tenant")
            .applicationId("app")
            .build();
    product =
        IoTProduct.builder()
            .productKey("product")
            .tenantId("tenant")
            .creatorId("owner")
            .isDeleted(0)
            .build();
    when(applications.selectIotUserApplicationByAppId("client")).thenReturn(app);
    when(products.getProduct("product")).thenReturn(product);
    when(deviceService.selectDevInstanceBONoCache(
            Map.of("productKey", "product", "deviceId", "device")))
        .thenReturn(dto);
    when(devices.selectOne(any()))
        .thenAnswer(
            call -> {
              IoTDevice query = call.getArgument(0);
              return Objects.equals(query.getProductKey(), row.getProductKey())
                      && Objects.equals(query.getDeviceId(), row.getDeviceId())
                      && Objects.equals(query.getCreatorId(), row.getCreatorId())
                      && Objects.equals(query.getTenantId(), row.getTenantId())
                      && Objects.equals(query.getApplication(), row.getApplication())
                  ? row
                  : null;
            });
    authenticate("client", "client", "tenant");
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void acceptsAuthenticatedClientJwtAndReadsCurrentApplicationEachTime() {
    assertSame(app, access.application());
    assertSame(app, access.application());
    verify(applications, times(2)).selectIotUserApplicationByAppId("client");
  }

  @Test
  void rejectsMissingAuthenticationAndPasswordPrincipalsBeforeAnyLookup() {
    SecurityContextHolder.clearContext();
    assertThrows(AccessDeniedException.class, access::application);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("owner", "unused", List.of()));
    assertThrows(AccessDeniedException.class, access::application);
    verifyNoInteractions(applications);
  }

  @Test
  void rejectsUserJwtAndJwtWithoutClientClaimBeforeAnyApplicationLookup() {
    authenticate("owner", "web-client", "tenant");
    assertThrows(AccessDeniedException.class, access::application);
    authenticate("client", null, "tenant");
    assertThrows(AccessDeniedException.class, access::application);
    verifyNoInteractions(applications);
  }

  @Test
  void rejectsUnauthenticatedJwtEvenWhenClaimsLookLikeAClient() {
    var authentication = new JwtAuthenticationToken(jwt("client", "client", "tenant"));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    assertThrows(AccessDeniedException.class, access::application);
    verifyNoInteractions(applications);
  }

  @Test
  void rejectsMissingDisabledDeletedExpiredAndOwnerlessApplications() {
    when(applications.selectIotUserApplicationByAppId("client")).thenReturn(null);
    assertThrows(AccessDeniedException.class, access::application);
    when(applications.selectIotUserApplicationByAppId("client")).thenReturn(app);
    app.setAppStatus(1);
    assertThrows(AccessDeniedException.class, access::application);
    app.setAppStatus(0);
    app.setDeleted(1);
    assertThrows(AccessDeniedException.class, access::application);
    app.setDeleted(0);
    app.setValidEndDate(new Date(System.currentTimeMillis() - 1000));
    assertThrows(AccessDeniedException.class, access::application);
    app.setValidEndDate(new Date(System.currentTimeMillis() + 60000));
    app.setUnionId(null);
    assertThrows(AccessDeniedException.class, access::application);
  }

  @Test
  void rejectsTenantMismatchAndDerivesLegacyTenantOnlyFromDatabaseOwner() {
    authenticate("client", "client", "other");
    assertThrows(AccessDeniedException.class, access::application);
    app.setTenantId(null);
    when(users.selectUserByUnionId("owner"))
        .thenReturn(IoTUser.builder().unionId("owner").tenantId("tenant").build());
    authenticate("client", "client", "tenant");
    assertSame(app, access.application());
    assertEquals("tenant", access.tenantId(app));
    when(users.selectUserByUnionId("owner")).thenReturn(null);
    assertThrows(AccessDeniedException.class, access::application);
  }

  @Test
  void accessRequiresFreshDeviceBindingOwnerAndTenantTogether() {
    row.setApplication(null);
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    row.setApplication("sibling-app");
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    row.setApplication("app");
    row.setCreatorId("other-owner");
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    row.setCreatorId("owner");
    row.setTenantId("other-tenant");
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    verifyNoInteractions(deviceService);
  }

  @Test
  void authorizedDeviceUsesExactProductDeviceAndApplicationScope() {
    assertSame(dto, access.device("product", "device"));
    verify(devices)
        .selectOne(
            argThat(
                (IoTDevice q) ->
                    "product".equals(q.getProductKey())
                        && "device".equals(q.getDeviceId())
                        && "owner".equals(q.getCreatorId())
                        && "tenant".equals(q.getTenantId())
                        && "app".equals(q.getApplication())));
    verify(deviceService)
        .selectDevInstanceBONoCache(Map.of("productKey", "product", "deviceId", "device"));
  }

  @Test
  void rejectsCrossTenantDeletedOrMissingProductsBeforeDeviceDataRead() {
    product.setTenantId("other");
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    product.setTenantId("tenant");
    product.setIsDeleted(1);
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    when(products.getProduct("product")).thenReturn(null);
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    verifyNoInteractions(deviceService);
  }

  @Test
  void rejectsChangedDeviceIdentityTenantOrApplicationOnFreshDtoRead() {
    dto.setIotId("other-iot");
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    dto.setIotId("iot");
    dto.setTenantId("other");
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    dto.setTenantId("tenant");
    dto.setApplicationId("sibling-app");
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
    when(deviceService.selectDevInstanceBONoCache(anyMap())).thenReturn(null);
    assertThrows(AccessDeniedException.class, () -> access.device("product", "device"));
  }

  @Test
  void creationRequiresTheProductOwnerAndMatchingTenant() {
    assertSame(product, access.product("product"));
    product.setCreatorId("other-owner");
    assertThrows(AccessDeniedException.class, () -> access.product("product"));
    product.setCreatorId("owner");
    product.setTenantId("other");
    assertThrows(AccessDeniedException.class, () -> access.product("product"));
    product.setTenantId("tenant");
    product.setIsDeleted(1);
    assertThrows(AccessDeniedException.class, () -> access.product("product"));
  }

  @Test
  void identifiersRejectBlanksTrimmingControlsAndOversizedValuesBeforeLookup() {
    for (String value :
        new String[] {
          null, "", " ", " device", "device ", "de\tvice", "de\nvice", "x".repeat(101)
        }) {
      ResponseStatusException error =
          assertThrows(ResponseStatusException.class, () -> access.device(value, "device"));
      assertEquals(400, error.getStatusCode().value());
    }
    assertDoesNotThrow(() -> OpenApiV2Access.identifier("x".repeat(100), "deviceId"));
    assertDoesNotThrow(() -> OpenApiV2Access.identifier("设备-001", "deviceId"));
    verifyNoInteractions(applications, devices, products, deviceService);
  }

  private static void authenticate(String subject, String client, String tenant) {
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt(subject, client, tenant), List.of()));
  }

  private static Jwt jwt(String subject, String client, String tenant) {
    var builder =
        Jwt.withTokenValue("test")
            .header("alg", "none")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300));
    if (client != null) builder.claim("clientId", client);
    if (tenant != null) builder.claim("tenantId", tenant);
    return builder.build();
  }
}
