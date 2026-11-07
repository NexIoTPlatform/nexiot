package cn.universal.web.config.xss;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONUtil;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

class XssHttpServletRequestWrapperTest {

  @Test
  void transformedBodyMustNotBeLimitedByOriginalContentLength() throws Exception {
    String payload =
        "{\"picUrl\":\"https://example.test/image.jpg?a=1&b=2&c=3\","
            + "\"utcTime\":\"20260905T102846\"}";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContentType(MediaType.APPLICATION_JSON_VALUE);
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    request.setContent(payload.getBytes(StandardCharsets.UTF_8));

    XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);
    String actual =
        new StringHttpMessageConverter(StandardCharsets.UTF_8)
            .read(String.class, new ServletServerHttpRequest(wrapper));

    assertEquals(-1L, wrapper.getContentLengthLong());
    assertTrue(actual.contains("&amp;"));
    assertTrue(actual.endsWith("\"20260905T102846\"}"));
    assertDoesNotThrow(() -> JSONUtil.parseObj(actual));
  }
}
