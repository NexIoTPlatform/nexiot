package cn.universal.http.access.web;

import cn.universal.core.network.NetworkAccessGuard;
import cn.universal.http.access.config.HttpConfigParser;
import cn.universal.http.access.config.HttpConfigValidator;
import cn.universal.http.access.runtime.HttpDriverPollingScheduler;
import cn.universal.http.access.runtime.HttpIntegrationRuntimeManager;
import cn.universal.http.access.runtime.HttpOperationRegistry;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.mapper.NetworkMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/http-access/networks")
public class HttpManagementController {

  private final HttpOperationRegistry registry;
  private final HttpDriverPollingScheduler pollingScheduler;
  private final HttpConfigParser parser;
  private final HttpConfigValidator validator;
  private final NetworkMapper networkMapper;
  private final HttpIntegrationRuntimeManager runtimeManager;
  private final NetworkAccessGuard accessGuard;

  public HttpManagementController(
      HttpOperationRegistry registry,
      HttpDriverPollingScheduler pollingScheduler,
      HttpConfigParser parser,
      HttpConfigValidator validator,
      NetworkMapper networkMapper,
      HttpIntegrationRuntimeManager runtimeManager,
      ObjectProvider<NetworkAccessGuard> accessGuard) {
    this.registry = registry;
    this.pollingScheduler = pollingScheduler;
    this.parser = parser;
    this.validator = validator;
    this.networkMapper = networkMapper;
    this.runtimeManager = runtimeManager;
    this.accessGuard = accessGuard.getIfAvailable();
  }

  @GetMapping("/{networkId}/status")
  public ResponseEntity<Map<String, Object>> status(@PathVariable Long networkId) {
    assertAccess(networkId);
    HttpRouteSnapshot snapshot = registry.getByNetworkId(networkId).orElse(null);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("running", snapshot != null);
    response.put("polling", pollingScheduler.isConfigured(networkId));
    if (snapshot != null) {
      response.put("accessCode", snapshot.config().getAccessCode());
      response.put("revision", snapshot.config().getRevision());
      response.put("productKey", snapshot.definition().productKey());
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{networkId}/poll")
  public ResponseEntity<Map<String, Object>> poll(@PathVariable Long networkId) {
    assertAccess(networkId);
    pollingScheduler.runNow(networkId);
    return ResponseEntity.accepted().body(Map.of("accepted", true, "networkId", networkId));
  }

  @PostMapping("/validate")
  public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> request) {
    Object value = request.get("configuration");
    String configuration = value instanceof String ? (String) value : parser.write(value);
    validator.validate(parser.parse(configuration));
    return ResponseEntity.ok(Map.of("valid", true));
  }

  @PostMapping("/{networkId}/reload")
  public ResponseEntity<Map<String, Object>> reload(@PathVariable Long networkId) {
    assertAccess(networkId);
    Network network = networkMapper.selectNetworkById(networkId);
    if (network == null || !HttpIntegrationRuntimeManager.TYPES.contains(network.getType())) {
      throw new IllegalArgumentException("HTTP access network does not exist");
    }
    if (!network.isStateEnabled()) {
      throw new IllegalArgumentException("Disabled HTTP network cannot be reloaded");
    }
    runtimeManager.start(runtimeManager.toDefinition(network));
    return ResponseEntity.ok(Map.of("reloaded", true, "networkId", networkId));
  }

  private void assertAccess(Long networkId) {
    if (accessGuard == null) throw new IllegalStateException("Network access guard is unavailable");
    accessGuard.assertAccess(networkId);
  }
}
