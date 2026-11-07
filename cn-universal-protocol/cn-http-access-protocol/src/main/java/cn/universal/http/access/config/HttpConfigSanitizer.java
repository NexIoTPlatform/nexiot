package cn.universal.http.access.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HttpConfigSanitizer {

  public static final String MASK = "******";
  private static final Set<String> SECRET_FIELDS = Set.of("value", "secret");
  private final HttpConfigParser parser;

  public HttpConfigSanitizer(HttpConfigParser parser) {
    this.parser = parser;
  }

  public String sanitize(String configuration) {
    if (configuration == null || configuration.isBlank()) {
      return configuration;
    }
    JsonNode root = parser.readTree(configuration);
    mask(root);
    return parser.write(root);
  }

  public String mergeMasked(String existingConfiguration, String incomingConfiguration) {
    if (incomingConfiguration == null || incomingConfiguration.isBlank()
        || existingConfiguration == null || existingConfiguration.isBlank()) {
      return incomingConfiguration;
    }
    JsonNode existing = parser.readTree(existingConfiguration);
    JsonNode incoming = parser.readTree(incomingConfiguration);
    merge(existing, incoming);
    return parser.write(incoming);
  }

  private void mask(JsonNode node) {
    if (node == null || !node.isObject()) {
      return;
    }
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (isSecret(field.getKey()) && field.getValue().isValueNode()) {
        ((ObjectNode) node).put(field.getKey(), MASK);
      } else {
        mask(field.getValue());
      }
    }
  }

  private void merge(JsonNode existing, JsonNode incoming) {
    if (existing == null || incoming == null || !existing.isObject() || !incoming.isObject()) {
      return;
    }
    Iterator<Map.Entry<String, JsonNode>> fields = incoming.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      JsonNode oldValue = existing.get(field.getKey());
      if (isSecret(field.getKey()) && field.getValue().isTextual()
          && MASK.equals(field.getValue().asText()) && oldValue != null) {
        ((ObjectNode) incoming).set(field.getKey(), oldValue.deepCopy());
      } else {
        merge(oldValue, field.getValue());
      }
    }
  }

  private boolean isSecret(String field) {
    return field != null && SECRET_FIELDS.contains(field.toLowerCase(Locale.ROOT));
  }
}
