package cn.universal.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.common.exception.BaseException;
import cn.universal.core.config.InstanceDiagnosticsRegistry;
import cn.universal.dm.device.service.DashboardOperationsService;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

public class DashboardOperationsAccessTest {
  private final DashboardController tenantController = new DashboardController() {
    @Override
    public boolean isAdmin() {
      return false;
    }
  };
  private final InstanceDiagnosticsRegistry registry = mock(InstanceDiagnosticsRegistry.class);
  private final DashboardOperationsService operations = mock(DashboardOperationsService.class);

  @Before
  public void setUp() {
    SecurityContextHolder.clearContext();
    ReflectionTestUtils.setField(tenantController, "instanceDiagnosticsRegistry", registry);
    ReflectionTestUtils.setField(tenantController, "operationsService", operations);
  }

  @After
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginTenant() {
    Jwt jwt = Jwt.withTokenValue("tenant-test").header("alg", "RS256").subject("tenant-user").build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @Test
  public void tenantCanReadClusterDiagnostics() {
    loginTenant();
    var nodes = List.of(new InstanceDiagnosticsRegistry.NodeSnapshot());
    when(registry.listNodes()).thenReturn(nodes);

    Map<?, ?> response = (Map<?, ?>) tenantController.getOperationsNodes();

    assertSame(nodes, response.get("nodes"));
    assertTrue((Long) response.get("timestamp") > 0);
  }

  @Test
  public void tenantCanReadPlatformMessageTotals() {
    loginTenant();
    Map<String, Object> statistics = Map.of("scope", "platform", "days", 7);
    when(operations.getStatistics(7)).thenReturn(statistics);

    assertEquals(statistics, tenantController.getOperationsStatistics(7));
  }

  @Test
  public void anonymousCannotReadNodesOrStatistics() {
    assertThrows(BaseException.class, () -> tenantController.getOperationsNodes());
    assertThrows(BaseException.class, () -> tenantController.getOperationsStatistics(7));
    verifyNoInteractions(registry, operations);
  }
}
