package cn.universal.http.access.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpDriverContext {
  Long networkId;
  String networkUnionId;
  String accessCode;
  String operationId;
  String operationType;
  String source;
  String method;
  String routePath;
  String remoteAddress;
  String productKey;
  String deviceId;
  String cursor;
  @Builder.Default Map<String, String> pathVariables = new LinkedHashMap<>();
  @Builder.Default Map<String, List<String>> headers = new LinkedHashMap<>();
  @Builder.Default Map<String, List<String>> query = new LinkedHashMap<>();
  @Builder.Default Map<String, Object> attributes = new LinkedHashMap<>();
}
