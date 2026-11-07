package cn.universal.admin.network.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.dm.device.service.protocol.ProtocolClusterService;
import cn.universal.admin.config.ConnectionConfig;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.mapper.NetworkMapper;
import cn.universal.security.support.DeptDataPermissionSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NetworkServiceImplTest {
  private final NetworkMapper mapper = mock(NetworkMapper.class);
  private final ProtocolClusterService cluster = mock(ProtocolClusterService.class);
  private NetworkServiceImpl service;
  private Network network;

  @BeforeEach
  void setUp() {
    service = new NetworkServiceImpl();
    DeptDataPermissionSupport permissions = mock(DeptDataPermissionSupport.class);
    when(permissions.canAccessResource(any(), any(), any(), any())).thenReturn(true);
    ReflectionTestUtils.setField(service, "networkMapper", mapper);
    ReflectionTestUtils.setField(service, "deptDataPermissionSupport", permissions);
    ReflectionTestUtils.setField(service, "tcpClusterService", cluster);
    ConnectionConfig config = new ConnectionConfig();
    config.setTcpPortMin(20000);
    config.setTcpPortMax(30000);
    ReflectionTestUtils.setField(service, "connectionConfig", config);
    network = Network.builder().id(1L).name("测试组件").productKey("product-1")
        .type("TCP_SERVER").state(1).configuration("{\"port\":29000}").build();
    when(mapper.selectNetworkById(1L)).thenReturn(network);
  }

  @Test
  void tcpPortPoolIncludesBothBoundariesAndRejectsOutsidePorts() {
    for (int port : new int[] {20000, 30000}) {
      network.setConfiguration("{\"port\":" + port + "}");
      assertTrue(service.validateNetworkConfig(network));
    }
    for (int port : new int[] {19999, 30001}) {
      network.setConfiguration("{\"port\":" + port + "}");
      RuntimeException error = assertThrows(RuntimeException.class,
          () -> service.validateNetworkConfig(network));
      assertTrue(error.getMessage().contains("20000–30000"));
    }
  }

  @Test
  void cannotCreateClientComponent() {
    network.setType("TCP_CLIENT");
    assertThrows(IllegalArgumentException.class, () -> service.insertNetwork(network));
    verifyNoInteractions(cluster);
  }

  @Test
  void startAndRestartReturnRuntimeReasonAndPreserveCause() {
    RuntimeException failure = new IllegalStateException("监听 0.0.0.0:29000 失败: Address already in use");
    when(cluster.start("product-1")).thenThrow(failure);
    when(cluster.restart("product-1")).thenThrow(failure);
    RuntimeException start = assertThrows(RuntimeException.class, () -> service.startNetwork(1L));
    RuntimeException restart = assertThrows(RuntimeException.class, () -> service.restartNetwork(1L));
    assertTrue(start.getMessage().contains(failure.getMessage()));
    assertTrue(restart.getMessage().contains(failure.getMessage()));
    assertTrue(hasCause(start, failure));
    assertTrue(hasCause(restart, failure));
  }

  @Test
  void clientComponentIsRejectedBeforeServerLookupOrBroadcast() {
    network.setType("TCP_CLIENT");
    RuntimeException error = assertThrows(RuntimeException.class, () -> service.startNetwork(1L));
    assertTrue(error.getMessage().contains("TCP_CLIENT"));
    assertTrue(error.getMessage().contains("TCP_SERVER"));
    assertThrows(RuntimeException.class, () -> service.restartNetwork(1L));
    verifyNoInteractions(cluster);
  }

  private boolean hasCause(Throwable error, Throwable expected) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause == expected) return true;
    }
    return false;
  }
}
