package cn.universal.admin.platform.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Redacts SIP credentials at the HTTP boundary without changing entities or cached values. */
@RestControllerAdvice(basePackages = "cn.universal")
public class SipConfigurationResponseAdvice implements ResponseBodyAdvice<Object> {
  private final ObjectMapper mapper;

  public SipConfigurationResponseAdvice(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean supports(
      MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
    return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converter);
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter method,
      MediaType contentType,
      Class<? extends HttpMessageConverter<?>> converter,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body == null) return null;
    JsonNode result = mapper.valueToTree(body);
    return redact(result) ? result : body;
  }

  private boolean redact(JsonNode node) {
    boolean changed = false;
    if (node.isArray()) {
      for (JsonNode child : node) changed |= redact(child);
    } else if (node instanceof ObjectNode object) {
      Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String name = entry.getKey();
        JsonNode value = entry.getValue();
        if ("sipEslPassword".equals(name) || "sipPassword".equals(name)) {
          if (!value.isNull() && !value.asText().isEmpty()) {
            object.put(name, "***");
            changed = true;
          }
        } else if (("configuration".equals(name) || "productConfiguration".equals(name))
            && value.isTextual()
            && (value.asText().contains("sipEslPassword")
                || value.asText().contains("sipPassword"))) {
          try {
            JsonNode config = mapper.readTree(value.asText());
            redact(config);
            object.put(name, mapper.writeValueAsString(config));
          } catch (java.io.IOException invalidConfiguration) {
            // A malformed credential-bearing configuration must not leak through an error response.
            object.put(name, "{}");
          }
          changed = true;
        } else {
          changed |= redact(value);
        }
      }
    }
    return changed;
  }
}
