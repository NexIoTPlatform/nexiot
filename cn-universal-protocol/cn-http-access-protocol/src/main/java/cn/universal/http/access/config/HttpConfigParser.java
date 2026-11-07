package cn.universal.http.access.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

@Component
public class HttpConfigParser {

  private final ObjectMapper mapper =
      JsonMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  public HttpIntegrationConfig parse(String configuration) {
    if (configuration == null || configuration.isBlank()) {
      throw new IllegalArgumentException("HTTP configuration cannot be empty");
    }
    try {
      return mapper.readValue(configuration, HttpIntegrationConfig.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid HTTP configuration: " + e.getOriginalMessage(), e);
    }
  }

  public String write(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize HTTP configuration", e);
    }
  }

  public JsonNode readTree(String value) {
    try {
      return mapper.readTree(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid HTTP configuration", e);
    }
  }

  public ObjectMapper mapper() {
    return mapper;
  }
}
