package cn.universal.web.config.intercepter;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONUtil;
import cn.universal.web.config.log.RequestWrapper;
import cn.universal.web.config.log.WebLog;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class APILogPrintIntercepterTest {
  private final APILogPrintIntercepter interceptor = new APILogPrintIntercepter();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @AfterEach
  void cleanUp() {
    scope().remove();
  }

  @Test
  void wechatParametersAndMalformedBodiesNeverEnterRequestLogs() throws Exception {
    for (String path :
        new String[] {
          "/api/auth/wechat/sessions/session-one",
          "/api/" + "wechat/callback/test",
          "/api/oauth/token"
        }) {
      var request = new MockHttpServletRequest("POST", path);
      request.addParameter("grant_type", "wechat_qr");
      request.addParameter("browser_proof", "never-log-browser-proof");
      request.addParameter("code", "never-log-message-code");
      String body = "{\"browserProof\":\"never-log-browser-proof\",broken";
      request.setContentType("application/json");
      request.setContent(body.getBytes(StandardCharsets.UTF_8));
      var wrapper = new RequestWrapper(request);

      interceptor.preHandle(wrapper, response, this);

      assertFalse(scope().get().getReqParams().contains("never-log"));
      assertTrue(scope().get().getReqParams().contains("微信认证内容已省略"));
      assertEquals(body, wrapper.getBodyString());
    }
  }

  @Test
  void ordinaryQueryParametersRemainVisibleWhileCredentialsAndAuthorizationAreMasked() {
    var request = new MockHttpServletRequest("GET", "/api/openapi/v1/iot/devices");
    request.addParameter("keyword", "phone");
    request.addParameter("page", "2");
    request.addParameter("sipPassword", "never-log-this");
    request.addParameter("raw", "not-json sipPassword=another-secret");
    request.addHeader("Authorization", "Bearer private-jwt");
    interceptor.preHandle(request, response, this);
    WebLog captured = scope().get();
    var params = JSONUtil.parseObj(captured.getReqParams());
    assertEquals("phone", params.getStr("keyword"));
    assertEquals("2", params.getStr("page"));
    assertEquals("***", params.getStr("sipPassword"));
    assertFalse(captured.getReqParams().contains("never-log-this"));
    assertFalse(captured.getReqParams().contains("another-secret"));
    assertFalse(captured.getHeaderInfo().contains("private-jwt"));
  }

  @Test
  void wechatManagementAndBindingBodiesAreOmittedWithContextAndApiPrefixes() throws Exception {
    for (String suffix :
        new String[] {
          "/admin/system/wechat/config",
          "/admin/system/profile/wechat",
          "/admin/system/profile/wechat/sessions",
          "/admin/system/profile/wechat/sessions/session-one/confirm"
        }) {
      for (String prefix : new String[] {"", "/nexiot/api"}) {
        for (String method : new String[] {"POST", "PUT"}) {
          for (String body :
              new String[] {
                "{\"appSecret\":\"never-log-app-secret\",\"encodingAESKey\":\"never-log-aes-key\"}",
                "{\"callbackToken\":\"never-log-callback-token\",broken"
              }) {
            var request = new MockHttpServletRequest(method, prefix + suffix);
            if (!prefix.isEmpty()) request.setContextPath("/nexiot");
            request.addParameter("browser_proof", "never-log-query-proof");
            request.addHeader("X-Wechat-Proof", "never-log-header-proof");
            request.setContentType("application/json");
            request.setContent(body.getBytes(StandardCharsets.UTF_8));
            var wrapper = new RequestWrapper(request);

            interceptor.preHandle(wrapper, response, this);

            assertTrue(scope().get().getReqParams().contains("微信认证内容已省略"));
            assertFalse(scope().get().getReqParams().contains("never-log"));
            assertFalse(scope().get().getHeaderInfo().contains("never-log"));
            assertEquals(body, wrapper.getBodyString());
          }
        }
      }
    }
  }

  @Test
  void adjacentManagementRoutesKeepTheirOrdinaryQueryLogs() {
    for (String path :
        new String[] {
          "/admin/system/wechat/config-preview",
          "/admin/system/profile/wechat-settings",
          "/admin/system/wechat/users"
        }) {
      var request = new MockHttpServletRequest("GET", path);
      request.addParameter("keyword", "visible-query");
      interceptor.preHandle(request, response, this);
      assertEquals(
          "visible-query", JSONUtil.parseObj(scope().get().getReqParams()).getStr("keyword"));
    }
  }

  @Test
  void jsonAndMalformedSipBodiesDoNotExposePasswordsOrMutateTheRequest() throws Exception {
    for (String body :
        new String[] {
          "{\"deviceId\":\"phone\",\"configuration\":\"{\\\"sipPassword\\\":\\\"secret-one\\\"}\"}",
          "{\"sipEslPassword\":\"secret-two\",broken"
        }) {
      var request = new MockHttpServletRequest("POST", "/api/openapi/v1/iot/device/p/add");
      request.setContentType("application/json");
      request.setContent(body.getBytes(StandardCharsets.UTF_8));
      var wrapper = new RequestWrapper(request);
      interceptor.preHandle(wrapper, response, this);
      assertFalse(scope().get().getReqParams().contains("secret-one"));
      assertFalse(scope().get().getReqParams().contains("secret-two"));
      assertEquals(body, wrapper.getBodyString());
    }
  }

  @SuppressWarnings("unchecked")
  private ThreadLocal<WebLog> scope() {
    return (ThreadLocal<WebLog>)
        ReflectionTestUtils.getField(APILogPrintIntercepter.class, "WEB_LOG_SCOPE");
  }
}
