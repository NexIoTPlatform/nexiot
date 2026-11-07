package cn.universal.dm.device.service.push.transform.executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScriptPushTransformExecutorTest {

  private ScriptPushTransformExecutor executor;

  @BeforeEach
  void setUp() {
    executor = new ScriptPushTransformExecutor();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldExecuteDirectReturnScript() {
    Object result = executor.execute(1L, 1, buildDirectReturnScript(), buildBindings());

    Map<String, Object> resultMap = assertInstanceOf(Map.class, result);
    assertEquals("device-001", resultMap.get("deviceId"));
    assertEquals("pk-001", resultMap.get("productKey"));
    assertEquals("PROPERTIES", resultMap.get("messageType"));
    assertEquals("temp-1", ((Map<String, Object>) resultMap.get("body")).get("temperature"));
  }

  @Test
  void shouldExecuteUiGeneratedFunctionScript() {
    String script =
        """
        function transform(ctx, request, source) {
          return {
            deviceId: ctx.deviceId,
            productKey: ctx.productKey,
            messageType: ctx.messageType,
            timestamp: ctx.time,
            body: ctx.properties || ctx.data || {},
            event: ctx.event || null,
            eventName: ctx.eventName || null
          }
        }
        return transform(ctx, request, source);
        """;

    Object result = executor.execute(2L, 1, script, buildBindings());

    Map<String, Object> resultMap = assertInstanceOf(Map.class, result);
    assertEquals("device-001", resultMap.get("deviceId"));
    assertEquals("pk-001", resultMap.get("productKey"));
    assertEquals("PROPERTIES", resultMap.get("messageType"));
    assertEquals(1710000000000L, ((Number) resultMap.get("timestamp")).longValue());
  }

  @Test
  void shouldAutoInvokeTransformLambdaWhenNoExplicitInvocation() {
    String script =
        """
        var transform = (ctx, request, source) => {
          return {
            deviceId: ctx.deviceId,
            sourceDevice: source.deviceId,
            requestDevice: request.deviceId
          }
        }
        """;

    Object result = executor.execute(3L, 1, script, buildBindings());

    Map<String, Object> resultMap = assertInstanceOf(Map.class, result);
    assertEquals("device-001", resultMap.get("deviceId"));
    assertEquals("device-001", resultMap.get("sourceDevice"));
    assertEquals("device-001", resultMap.get("requestDevice"));
  }

  @Test
  void shouldDecodeHtmlEncodedScript() {
    String script =
        "return {\n"
            + "  deviceId: ctx.deviceId,\n"
            + "  content: \"&lt;payload&gt;&amp;&lt;/payload&gt;\"\n"
            + "}";

    Object result = executor.execute(4L, 1, script, buildBindings());

    Map<String, Object> resultMap = assertInstanceOf(Map.class, result);
    assertEquals("device-001", resultMap.get("deviceId"));
    assertEquals("<payload>&</payload>", resultMap.get("content"));
  }

  @Test
  void shouldValidateFunctionDeclarationScript() {
    String script =
        """
        function transform(ctx, request, source) {
          return {
            deviceId: ctx.deviceId,
            productKey: ctx.productKey
          }
        }
        """;

    assertDoesNotThrow(() -> executor.validate(script));
  }

  @Test
  void shouldRejectInvalidScript() {
    assertThrows(RuntimeException.class, () -> executor.validate("return {"));
  }

  private String buildDirectReturnScript() {
    return """
        return {
          deviceId: ctx.deviceId,
          productKey: ctx.productKey,
          messageType: ctx.messageType,
          timestamp: ctx.time,
          body: ctx.properties || ctx.data || {},
          event: ctx.event || null,
          eventName: ctx.eventName || null
        }
        """;
  }

  private Map<String, Object> buildBindings() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("deviceId", "device-001");
    payload.put("productKey", "pk-001");
    payload.put("messageType", "PROPERTIES");
    payload.put("time", 1710000000000L);
    payload.put("event", null);
    payload.put("eventName", null);

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("temperature", "temp-1");
    properties.put("humidity", 66);
    payload.put("properties", properties);
    payload.put("data", null);

    Map<String, Object> bindings = new LinkedHashMap<>();
    bindings.put("ctx", payload);
    bindings.put("source", payload);
    bindings.put("request", payload);
    bindings.put("requestMap", payload);
    bindings.putAll(payload);
    assertNotNull(bindings.get("ctx"));
    return bindings;
  }
}
