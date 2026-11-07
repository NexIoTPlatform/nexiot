package cn.universal.http.access.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.universal.http.access.runtime.HttpOperationRegistry;
import cn.universal.http.access.security.HttpPathSecurity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;

class HttpIngressControllerTest {

  @Test
  void unknownAccessCodeReturnsStableModuleError() {
    HttpIngressController controller =
        new HttpIngressController(
            new HttpOperationRegistry(),
            mock(HttpPathSecurity.class),
            mock(HttpInboundProcessor.class));
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/iot/http/v2/missing/telemetry");
    when(request.getMethod()).thenReturn("POST");

    HttpAccessException error =
        assertThrows(
            HttpAccessException.class,
            () -> controller.ingress("missing", "/telemetry", new byte[0], request));

    assertEquals(404, error.getStatus());
    assertEquals("ACCESS_CODE_NOT_FOUND", error.getCode());
  }

  @Test
  void ingressExceptionHandlerHasPriorityAndPreservesHttpStatus() {
    Order order = HttpIngressExceptionHandler.class.getAnnotation(Order.class);
    assertEquals(Ordered.HIGHEST_PRECEDENCE, order.value());

    ResponseEntity<Map<String, Object>> response =
        new HttpIngressExceptionHandler()
            .handle(new HttpAccessException(404, "ROUTE_NOT_FOUND", "HTTP operation not found"));

    assertEquals(404, response.getStatusCode().value());
    assertFalse((Boolean) response.getBody().get("success"));
    assertEquals("ROUTE_NOT_FOUND", response.getBody().get("code"));
  }
}
