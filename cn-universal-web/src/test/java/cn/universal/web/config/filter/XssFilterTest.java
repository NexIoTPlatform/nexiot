package cn.universal.web.config.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import cn.universal.web.config.xss.XssHttpServletRequestWrapper;
import cn.universal.web.config.xss.XssProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class XssFilterTest {

  @Test
  void wechatCallbackPreservesXmlAndCdataEvenWithoutConfiguredSkips() throws Exception {
    String payload =
        "<xml>\n<Content><![CDATA[LOGIN 123456 < & ]]></Content>\n"
            + "<Encrypt><![CDATA[opaque+A/B==]]></Encrypt></xml>";
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/wechat/callback/test");
    request.setServletPath("/wechat/callback/test");
    request.setContentType(MediaType.TEXT_XML_VALUE);
    request.setContent(payload.getBytes(StandardCharsets.UTF_8));
    MockFilterChain chain = new MockFilterChain();

    wechatFilter().doFilter(request, new MockHttpServletResponse(), chain);

    assertSame(request, chain.getRequest());
    assertEquals(
        payload,
        new String(chain.getRequest().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
  }

  @Test
  void wechatExemptionDoesNotExpandToManagementOrNestedPathsOrOtherMethods() throws Exception {
    for (String path : List.of("/wechat/configuration/test", "/wechat/callback/test/nested")) {
      var request = new MockHttpServletRequest("POST", path);
      request.setServletPath(path);
      var chain = new MockFilterChain();
      wechatFilter().doFilter(request, new MockHttpServletResponse(), chain);
      assertInstanceOf(XssHttpServletRequestWrapper.class, chain.getRequest());
    }
    var request = new MockHttpServletRequest("PUT", "/wechat/callback/test");
    request.setServletPath("/wechat/callback/test");
    var chain = new MockFilterChain();
    wechatFilter().doFilter(request, new MockHttpServletResponse(), chain);
    assertInstanceOf(XssHttpServletRequestWrapper.class, chain.getRequest());
  }

  private XssFilter wechatFilter() {
    XssProperties properties = new XssProperties();
    properties.setEnabled(true);
    properties.setSkipUrl(List.of());
    XssFilter filter = new XssFilter();
    ReflectionTestUtils.setField(filter, "xssProperties", properties);
    return filter;
  }

  @Test
  void imoulifeWebhookMustKeepSignedUrlsUnchanged() throws Exception {
    String payload =
        "{\"picUrl\":\"https://example.test/image.jpg?X-Amz-Date=20260905&X-Amz-Signature=abc\"}";
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/iot/imoulife/events");
    request.setServletPath("/iot/imoulife/events");
    request.setContentType(MediaType.APPLICATION_JSON_VALUE);
    request.setContent(payload.getBytes(StandardCharsets.UTF_8));
    MockFilterChain chain = new MockFilterChain();
    XssProperties properties = new XssProperties();
    properties.setSkipUrl(List.of("/iot/imoulife/**", "/iot/lechengEvents/**"));
    XssFilter filter = new XssFilter();
    ReflectionTestUtils.setField(filter, "xssProperties", properties);

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertSame(request, chain.getRequest());
    assertEquals(
        payload,
        new String(chain.getRequest().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
  }
}
