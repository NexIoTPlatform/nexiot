package cn.universal.http.access.runtime;

import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.core.network.NetworkRuntimeProvider;
import cn.universal.http.access.config.HttpConfigParser;
import cn.universal.http.access.config.HttpConfigSanitizer;
import cn.universal.http.access.config.HttpConfigValidator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HttpNetworkRuntimeProvider implements NetworkRuntimeProvider {

  private final HttpIntegrationRuntimeManager runtimeManager;
  private final HttpConfigParser parser;
  private final HttpConfigValidator validator;
  private final HttpConfigSanitizer sanitizer;

  public HttpNetworkRuntimeProvider(
      HttpIntegrationRuntimeManager runtimeManager,
      HttpConfigParser parser,
      HttpConfigValidator validator,
      HttpConfigSanitizer sanitizer) {
    this.runtimeManager = runtimeManager;
    this.parser = parser;
    this.validator = validator;
    this.sanitizer = sanitizer;
  }

  @Override public Set<String> supportedTypes() { return HttpIntegrationRuntimeManager.TYPES; }
  @Override public void validate(NetworkRuntimeDefinition definition) {
    validator.validate(parser.parse(definition.configuration()));
  }
  @Override public boolean start(NetworkRuntimeDefinition definition) { return runtimeManager.start(definition); }
  @Override public boolean stop(NetworkRuntimeDefinition definition) { return runtimeManager.stop(definition); }
  @Override public boolean restart(NetworkRuntimeDefinition definition) { return runtimeManager.start(definition); }
  @Override public boolean isRunning(NetworkRuntimeDefinition definition) {
    return runtimeManager.isRunning(definition.id());
  }
  @Override public String sanitizeConfiguration(String configuration) { return sanitizer.sanitize(configuration); }
  @Override public String prepareConfiguration(String existing, String incoming) {
    return sanitizer.mergeMasked(existing, incoming);
  }
}
