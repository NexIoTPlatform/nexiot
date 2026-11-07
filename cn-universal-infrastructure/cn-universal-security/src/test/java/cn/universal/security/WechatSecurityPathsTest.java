package cn.universal.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class WechatSecurityPathsTest {

  @Test
  void publicSessionsAreReachableAndBindingAndConfigurationRequireJwtInBothModes()
      throws Exception {
    for (boolean production : new boolean[] {false, true}) {
      try (AnnotationConfigWebApplicationContext context =
          new AnnotationConfigWebApplicationContext()) {
        context.setServletContext(new MockServletContext());
        context
            .getEnvironment()
            .getPropertySources()
            .addFirst(
                new MapPropertySource("test", Map.of("security.production.enabled", production)));
        context.register(TestConfiguration.class);
        context.refresh();
        MockMvc mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();

        for (String[] endpoint :
            new String[][] {
              {"GET", "/auth/wechat/options"},
              {"POST", "/auth/wechat/sessions"},
              {"GET", "/auth/wechat/sessions/session-one"},
              {"DELETE", "/auth/wechat/sessions/session-one"},
              {"POST", "/auth/wechat/sessions/session-one/ws-ticket"},
              {"GET", "/wechat/callback/test"},
              {"POST", "/wechat/callback/test"},
              {"GET", "/ws/wechat-auth"}
            }) {
          mvc.perform(request(HttpMethod.valueOf(endpoint[0]), endpoint[1]))
              .andExpect(status().isOk());
        }

        for (String[] endpoint :
            new String[][] {
              {"GET", "/admin/system/profile/wechat"},
              {"DELETE", "/admin/system/profile/wechat"},
              {"POST", "/admin/system/profile/wechat/sessions"},
              {"POST", "/admin/system/profile/wechat/sessions/session-one/confirm"},
              {"GET", "/admin/system/wechat/config"},
              {"PUT", "/admin/system/wechat/config"},
              {"GET", "/admin/system/wechat/users"}
            }) {
          HttpMethod method = HttpMethod.valueOf(endpoint[0]);
          mvc.perform(request(method, endpoint[1])).andExpect(status().isUnauthorized());
          mvc.perform(request(method, endpoint[1]).header("Authorization", "Bearer valid"))
              .andExpect(status().isOk());
        }
        // New subpaths and unsupported HTTP verbs do not inherit anonymous access.
        mvc.perform(request(HttpMethod.POST, "/auth/wechat/options"))
            .andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.GET, "/auth/wechat/sessions/session-one/ws-ticket"))
            .andExpect(status().isUnauthorized());
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  @Import({DefaultSecurityConfig.class, TestEndpoints.class})
  static class TestConfiguration {
    @Bean
    JwtDecoder jwtDecoder() {
      JwtDecoder decoder = mock(JwtDecoder.class);
      Instant now = Instant.now();
      when(decoder.decode("valid"))
          .thenReturn(
              Jwt.withTokenValue("valid")
                  .header("alg", "RS256")
                  .subject("user-1")
                  .claim("roles", List.of("WEB"))
                  .issuedAt(now)
                  .expiresAt(now.plusSeconds(300))
                  .build());
      return decoder;
    }
  }

  @RestController
  static class TestEndpoints {
    @RequestMapping("/**")
    String ok() {
      return "ok";
    }
  }
}
