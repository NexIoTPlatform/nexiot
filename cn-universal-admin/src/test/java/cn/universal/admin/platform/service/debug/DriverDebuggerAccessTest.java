package cn.universal.admin.platform.service.debug;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.platform.web.DriverDebuggerController;
import cn.universal.admin.system.service.IoTDeviceProtocolService;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceProtocolBO;
import cn.universal.security.service.IoTUserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class DriverDebuggerAccessTest {
  private AnnotationConfigApplicationContext context;
  private DriverDebuggerController controller;
  private IoTDeviceProtocolService protocols;

  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean("ss")
    Permission permission() {
      return new Permission();
    }

    @Bean
    IoTDeviceProtocolService protocols() {
      return mock(IoTDeviceProtocolService.class);
    }

    @Bean
    DriverDebugService debugger() {
      return new DriverDebugService();
    }

    @Bean
    DriverDebuggerController controller(
        IoTDeviceProtocolService protocols, DriverDebugService debug) {
      DriverDebuggerController controller =
          new DriverDebuggerController(protocols, debug, mock(DriverRevisionService.class));
      return controller;
    }

    @Bean
    IoTUserService users() {
      IoTUserService users = mock(IoTUserService.class);
      when(users.selectUserByUnionId("tenant-user"))
          .thenReturn(
              IoTUser.builder().identity(1).unionId("tenant-user").tenantId("tenant-a").build());
      return users;
    }
  }

  public static class Permission {
    boolean codec = true;
    boolean edit = false;

    public boolean hasPermi(String permission) {
      return permission.endsWith(":edit") ? edit : codec;
    }
  }

  @BeforeEach
  void setup() {
    context = new AnnotationConfigApplicationContext(Config.class);
    controller = context.getBean(DriverDebuggerController.class);
    protocols = context.getBean(IoTDeviceProtocolService.class);
    Jwt token =
        Jwt.withTokenValue("test")
            .header("alg", "none")
            .subject("tenant-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
  }

  @AfterEach
  void cleanup() {
    if (context != null) context.close();
    SecurityContextHolder.clearContext();
  }

  @Test
  void codecPermissionDoesNotGrantApplyAndMissingCodecBlocksRead() {
    assertThrows(
        AccessDeniedException.class,
        () ->
            controller.apply("driver", new DriverDebuggerController.ApplyRequest("code", "hash")));
    context.getBean(Permission.class).codec = false;
    assertThrows(AccessDeniedException.class, () -> controller.detail("driver"));
    verifyNoInteractions(protocols);
  }

  @Test
  void allOperationsCheckCurrentDriverAccess() {
    assertThrows(IoTException.class, () -> controller.detail("other-tenant"));
    assertThrows(IoTException.class, () -> controller.history("other-tenant"));
    assertThrows(IoTException.class, () -> controller.poll("other-tenant", "run"));
    assertThrows(IoTException.class, () -> controller.cancel("other-tenant", "run"));
    assertThrows(
        IoTException.class,
        () ->
            controller.run(
                "other-tenant",
                new DriverDebugService.RunRequest("code", "", null, "decode", false, List.of())));
    verify(protocols, times(5)).selectDevProtocolById("other-tenant", "tenant-user");
  }

  @Test
  void applyUsesExistingSaveServiceWithoutOverwritingMetadata() {
    context.getBean(Permission.class).edit = true;
    when(protocols.selectDevProtocolById("driver", "tenant-user"))
        .thenReturn(
            IoTDeviceProtocol.builder()
                .id("driver")
                .type("magic")
                .name("name")
                .state((byte) 0)
                .configuration("{}")
                .build());
    controller.apply("driver", new DriverDebuggerController.ApplyRequest("new-code", "hash"));
    ArgumentCaptor<IoTDeviceProtocolBO> update = ArgumentCaptor.forClass(IoTDeviceProtocolBO.class);
    verify(protocols).updateDevProtocol(update.capture(), any());
    assertEquals("new-code", update.getValue().getJscript());
    assertEquals("hash", update.getValue().getExpectedSourceHash());
    assertNull(update.getValue().getName());
    assertNull(update.getValue().getState());
    assertNull(update.getValue().getConfiguration());
  }
}
