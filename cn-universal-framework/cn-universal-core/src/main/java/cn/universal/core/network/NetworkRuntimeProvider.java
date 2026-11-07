package cn.universal.core.network;

import java.util.Set;

/** Protocol runtime extension point used by network component management. */
public interface NetworkRuntimeProvider {

  Set<String> supportedTypes();

  default boolean supports(String type) {
    return type != null && supportedTypes().contains(type);
  }

  default void validate(NetworkRuntimeDefinition definition) {}

  boolean start(NetworkRuntimeDefinition definition);

  boolean stop(NetworkRuntimeDefinition definition);

  default boolean restart(NetworkRuntimeDefinition definition) {
    return stop(definition) && start(definition);
  }

  boolean isRunning(NetworkRuntimeDefinition definition);

  default String sanitizeConfiguration(String configuration) {
    return configuration;
  }

  default String prepareConfiguration(String existingConfiguration, String incomingConfiguration) {
    return incomingConfiguration;
  }
}
