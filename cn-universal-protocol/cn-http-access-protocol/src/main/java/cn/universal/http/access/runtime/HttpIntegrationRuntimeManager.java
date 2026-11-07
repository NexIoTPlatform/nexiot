package cn.universal.http.access.runtime;

import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.http.access.client.HttpClientManager;
import cn.universal.http.access.config.HttpConfigParser;
import cn.universal.http.access.config.HttpConfigValidator;
import cn.universal.http.access.config.HttpIntegrationConfig;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpIntegrationRuntimeManager {

  public static final Set<String> TYPES = Set.of("HTTP_SERVER");

  private final NetworkMapper networkMapper;
  private final IoTProductMapper productMapper;
  private final HttpConfigParser parser;
  private final HttpConfigValidator validator;
  private final HttpClientManager clientManager;
  private final HttpOperationRegistry registry;
  private final HttpDriverPollingScheduler pollingScheduler;

  public HttpIntegrationRuntimeManager(
      NetworkMapper networkMapper,
      IoTProductMapper productMapper,
      HttpConfigParser parser,
      HttpConfigValidator validator,
      HttpClientManager clientManager,
      HttpOperationRegistry registry,
      HttpDriverPollingScheduler pollingScheduler) {
    this.networkMapper = networkMapper;
    this.productMapper = productMapper;
    this.parser = parser;
    this.validator = validator;
    this.clientManager = clientManager;
    this.registry = registry;
    this.pollingScheduler = pollingScheduler;
  }

  public synchronized boolean start(NetworkRuntimeDefinition definition) {
    if (!TYPES.contains(definition.type())) {
      throw new IllegalArgumentException("HTTP access only supports HTTP_SERVER");
    }
    HttpIntegrationConfig config = parser.parse(definition.configuration());
    validator.validate(config);
    IoTProduct product = validateProduct(definition.productKey());
    HttpRouteSnapshot snapshot = new HttpRouteSnapshot(
        definition, config, clientManager.build(definition.id(), config.getOutbound()));
    registry.replace(snapshot);
    pollingScheduler.reschedule(snapshot, product);
    log.info("HTTP access network started: id={}, productKey={}, accessCode={}, revision={}",
        definition.id(), definition.productKey(), config.getAccessCode(), config.getRevision());
    return true;
  }

  public synchronized boolean stop(NetworkRuntimeDefinition definition) {
    pollingScheduler.cancel(definition.id());
    registry.remove(definition.id());
    clientManager.remove(definition.id());
    log.info("HTTP access network stopped: id={}", definition.id());
    return true;
  }

  public boolean isRunning(Long networkId) {
    return registry.getByNetworkId(networkId).isPresent();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void restoreEnabledNetworks() {
    reconcile();
  }

  @Scheduled(fixedDelayString = "${nexiot.http-access.reconciliation-interval-ms:30000}")
  public void reconcile() {
    Set<Long> enabledIds = new HashSet<>();
    List<Network> networks = networkMapper.select(
        Network.builder().type("HTTP_SERVER").state(Network.STATE_ENABLED).build());
    for (Network network : networks) {
      enabledIds.add(network.getId());
      try {
        HttpIntegrationConfig incoming = parser.parse(network.getConfiguration());
        boolean needsReload = registry.getByNetworkId(network.getId())
            .map(current -> current.config().getRevision() != incoming.getRevision())
            .orElse(true);
        if (needsReload) {
          start(toDefinition(network));
        }
      } catch (Exception e) {
        log.error("Unable to reconcile HTTP access network: id={}", network.getId(), e);
      }
    }
    for (HttpRouteSnapshot snapshot : registry.all()) {
      if (!enabledIds.contains(snapshot.definition().id())) {
        stop(snapshot.definition());
      }
    }
  }

  public NetworkRuntimeDefinition toDefinition(Network network) {
    return new NetworkRuntimeDefinition(network.getId(), network.getType(), network.getProductKey(),
        network.getUnionId(), network.getName(), network.getConfiguration(), network.getState());
  }

  private IoTProduct validateProduct(String productKey) {
    if (blank(productKey)) {
      throw new IllegalArgumentException("HTTP access network must bind one product");
    }
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null) {
      throw new IllegalArgumentException("HTTP product does not exist: " + productKey);
    }
    if (!"http".equalsIgnoreCase(product.getThirdPlatform())) {
      throw new IllegalArgumentException("HTTP access network can only bind an HTTP product");
    }
    return product;
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
