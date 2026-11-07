package cn.universal.web.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.universal.admin.system.web.WechatAccountConfigController;
import cn.universal.security.wechat.WechatAccountConfigService;
import cn.universal.security.wechat.WechatAuthService;
import cn.universal.web.controller.common.WechatAuthController;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WechatRequestExceptionAdviceTest {
  @Test
  void malformedAndInvalidCredentialBodiesUseSafeAdviceBeforeGlobalExceptionLogging()
      throws Exception {
    WechatAccountConfigService configs = mock(WechatAccountConfigService.class);
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new WechatAccountConfigController(configs))
            .setControllerAdvice(new WechatRequestExceptionAdvice(), new GlobalExceptionHandler())
            .build();
    for (String body :
        new String[] {
          "{\"encodingAESKey\":\"private-aes-fixture\",broken",
          "{\"enabled\":\"private-enabled-fixture\"}",
          "{\"expireSeconds\":\"private-expiration-fixture\"}"
        }) {
      String response =
          mvc.perform(
                  put("/admin/system/wechat/config")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(body))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.code").value(400))
              .andExpect(jsonPath("$.msg").value("微信请求内容格式不正确"))
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertFalse(response.contains("private-"));
      assertFalse(response.contains("Jackson"));
    }
    verifyNoInteractions(configs);
  }

  @Test
  void invalidLoginSessionBodyDoesNotReachAuthOrEchoSubmittedData() throws Exception {
    WechatAuthService auth = mock(WechatAuthService.class);
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new WechatAuthController(auth))
            .setControllerAdvice(new WechatRequestExceptionAdvice(), new GlobalExceptionHandler())
            .build();
    String response =
        mvc.perform(
                post("/auth/wechat/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"tenantCode\":[\"private-tenant-fixture\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("微信请求内容格式不正确"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(response.contains("private-"));
    verifyNoInteractions(auth);
  }
}
