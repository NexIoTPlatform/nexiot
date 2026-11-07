package cn.universal.http.access.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.http.access.config.HttpIntegrationConfig;
import org.junit.jupiter.api.Test;

class HttpOperationRegistryTest {
  @Test
  void replacingSameNetworkMovesItsAccessCodeAtomically() {
    HttpOperationRegistry registry = new HttpOperationRegistry();
    registry.replace(snapshot(10L, "factory-old"));
    HttpRouteSnapshot replacement = snapshot(10L, "factory-new");
    registry.replace(replacement);
    assertTrue(registry.getByAccessCode("factory-old").isEmpty());
    assertEquals(replacement, registry.getByAccessCode("factory-new").orElseThrow());
    assertEquals(replacement, registry.getByNetworkId(10L).orElseThrow());
    assertEquals(1, registry.all().size());
  }

  @Test
  void duplicateAccessCodeCannotReplaceAnotherNetwork() {
    HttpOperationRegistry registry = new HttpOperationRegistry();
    HttpRouteSnapshot original = snapshot(10L, "factory-east");
    registry.replace(original);
    assertThrows(IllegalArgumentException.class,
        () -> registry.replace(snapshot(11L, "factory-east")));
    assertEquals(original, registry.getByAccessCode("factory-east").orElseThrow());
    assertTrue(registry.getByNetworkId(11L).isEmpty());
  }

  @Test
  void removingNetworkPreservesOtherEndpoints() {
    HttpOperationRegistry registry = new HttpOperationRegistry();
    registry.replace(snapshot(10L, "factory-east"));
    registry.replace(snapshot(11L, "factory-west"));
    registry.remove(10L);
    assertTrue(registry.getByAccessCode("factory-east").isEmpty());
    assertTrue(registry.getByAccessCode("factory-west").isPresent());
    assertEquals(1, registry.all().size());
  }

  private HttpRouteSnapshot snapshot(Long id, String accessCode) {
    HttpIntegrationConfig config = new HttpIntegrationConfig();
    config.setAccessCode(accessCode);
    NetworkRuntimeDefinition definition =
        new NetworkRuntimeDefinition(id, "HTTP_SERVER", null, "union-" + id, "test", "{}", 1);
    return new HttpRouteSnapshot(definition, config, null);
  }
}
