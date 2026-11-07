package cn.universal.admin.common.aspectj;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.annotation.Log;
import cn.universal.persistence.entity.SysOperLog;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LogAspectCredentialTest {

  @Log(title = "tenant")
  void tenantEndpoint() {}

  @Test
  void tenantCreationAuditMasksPasswordAndPreservesOtherRequestFields() throws Exception {
    Map<String, Object> request =
        Map.of(
            "tenantCode",
            "new-tenant",
            "adminUsername",
            "tenant-admin",
            "adminPassword",
            "initial-password-value",
            "remark",
            "bootstrap-marker");
    JoinPoint point = Mockito.mock(JoinPoint.class);
    Mockito.when(point.getArgs()).thenReturn(new Object[] {request});
    SysOperLog log = new SysOperLog();
    log.setRequestMethod("POST");
    new LogAspect()
        .getControllerMethodDescription(
            point, getClass().getDeclaredMethod("tenantEndpoint").getAnnotation(Log.class), log);
    JSONObject result = JSONUtil.parseObj(log.getOperParam());
    assertEquals("***", result.getStr("adminPassword"));
    assertEquals("new-tenant", result.getStr("tenantCode"));
    assertEquals("bootstrap-marker", result.getStr("remark"));
    assertEquals("initial-password-value", request.get("adminPassword"));
  }

  @Test
  void nestedMediaAndClientCredentialsAreMaskedIncludingArrays() {
    Map<String, Object> request =
        Map.of(
            "serverCode",
            "direct-zlm",
            "secret",
            "media-value",
            "clients",
            List.of(
                Map.of(
                    "clientSecret",
                    "client-value",
                    "access_token",
                    "access-value",
                    "PASSWORD",
                    "login-value",
                    "name",
                    "client")));
    JSONArray result = JSONUtil.parseArray(LogAspect.serializeForAudit(List.of(request)));
    JSONObject server = result.getJSONObject(0);
    assertEquals("direct-zlm", server.getStr("serverCode"));
    assertEquals("***", server.getStr("secret"));
    JSONObject client = server.getJSONArray("clients").getJSONObject(0);
    for (String key : List.of("clientSecret", "access_token", "PASSWORD")) {
      assertEquals("***", client.getStr(key));
    }
    assertEquals("client", client.getStr("name"));
    assertEquals("media-value", request.get("secret"));
  }

  @Test
  void jsonEncodedConfigurationIsMaskedWithoutLosingItsStringType() {
    String config =
        "{\"sipHost\":\"127.0.0.1\",\"sipEslPassword\":\"sip-value\","
            + "\"nested\":{\"clientSecret\":\"nested-value\",\"port\":80}}";
    JSONObject result =
        JSONUtil.parseObj(
            LogAspect.serializeForAudit(
                Map.of(
                    "configuration",
                    config,
                    "thirdConfiguration",
                    "{\"password\":\"third-value\"}")));
    JSONObject parsed = JSONUtil.parseObj(result.getStr("configuration"));
    assertEquals("127.0.0.1", parsed.getStr("sipHost"));
    assertEquals("***", parsed.getStr("sipEslPassword"));
    assertEquals(80, parsed.getJSONObject("nested").getInt("port"));
    assertEquals("***", parsed.getJSONObject("nested").getStr("clientSecret"));
    assertFalse(result.toString().contains("third-value"));
    assertTrue(config.contains("sip-value"));
  }

  @Test
  void malformedCredentialJsonNeverFallsBackToPlaintext() {
    String malformed = "{\"password\":\"must-not-appear\"";
    assertFalse(LogAspect.serializeForAudit(malformed).contains("must-not-appear"));
    assertFalse(
        LogAspect.serializeForAudit(Map.of("configuration", malformed))
            .contains("must-not-appear"));
  }

  @Test
  void auditResponseKeepsNormalFieldsAndMasksReturnedCredentials() {
    JSONObject result =
        JSONUtil.parseObj(
            LogAspect.serializeForAudit(
                Map.of(
                    "code",
                    200,
                    "data",
                    Map.of("token", "returned-value", "id", 7),
                    "msg",
                    "password updated")));
    assertEquals(200, result.getInt("code"));
    assertEquals(7, result.getJSONObject("data").getInt("id"));
    assertEquals("***", result.getJSONObject("data").getStr("token"));
    assertEquals("password updated", result.getStr("msg"));
    assertEquals("password updated", LogAspect.serializeForAudit("password updated"));
    assertEquals(JSONUtil.toJsonStr(42), LogAspect.serializeForAudit(42));
    assertEquals(JSONUtil.toJsonStr(null), LogAspect.serializeForAudit(null));
  }

  @Test
  void wechatSessionAndConfigurationCredentialsCannotEnterAuditResponses() {
    Map<String, Object> credentials =
        Map.of(
            "browserProof", "private-browser-proof",
            "browser_proof", "private-form-proof",
            "encodingAESKey", "private-aes-key",
            "encoding_aes_key", "private-snake-aes-key",
            "verificationCode", "private-message-code",
            "verification_code", "private-snake-message-code",
            "ticket", "private-websocket-ticket",
            "sessionId", "session-fixture");
    JSONObject result =
        JSONUtil.parseObj(LogAspect.serializeForAudit(Map.of("code", 200, "data", credentials)));
    JSONObject data = result.getJSONObject("data");
    for (String key : credentials.keySet()) {
      assertEquals("sessionId".equals(key) ? "session-fixture" : "***", data.getStr(key));
    }
    assertFalse(result.toString().contains("private-"));
    assertEquals("private-browser-proof", credentials.get("browserProof"));
  }

  @Test
  void malformedAndJsonEncodedWechatCredentialsNeverFallBackToPlaintext() {
    for (String key : List.of("browserProof", "encodingAESKey", "verificationCode", "ticket")) {
      String malformed = "{\"" + key + "\":\"private-fixture\",";
      assertFalse(LogAspect.serializeForAudit(malformed).contains("private-fixture"));
      assertFalse(
          LogAspect.serializeForAudit(Map.of("configuration", malformed))
              .contains("private-fixture"));
      String valid = "{\"" + key + "\":\"private-fixture\",\"name\":\"retained\"}";
      JSONObject result =
          JSONUtil.parseObj(LogAspect.serializeForAudit(Map.of("configuration", valid)));
      JSONObject configuration = JSONUtil.parseObj(result.getStr("configuration"));
      assertEquals("***", configuration.getStr(key));
      assertEquals("retained", configuration.getStr("name"));
    }
  }
}
