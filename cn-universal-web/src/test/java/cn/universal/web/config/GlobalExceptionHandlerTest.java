package cn.universal.web.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  @Test
  void missingApiHandledAsStaticResourceReturnsNotFound() throws Exception {
    assertNotFound(
        StaticResourceConfig.class, "/admin/v1/alarm/records", NoResourceFoundException.class);
  }

  @Test
  void missingFaviconReturnsNotFound() throws Exception {
    assertNotFound(StaticResourceConfig.class, "/favicon.ico", NoResourceFoundException.class);
  }

  @Test
  void missingApiWithoutStaticResourceMappingReturnsNotFound() throws Exception {
    assertNotFound(MvcConfig.class, "/admin/v1/alarm/records", NoHandlerFoundException.class);
  }

  private void assertNotFound(
      Class<?> config, String path, Class<? extends Exception> exceptionType) throws Exception {
    try (AnnotationConfigWebApplicationContext context =
        new AnnotationConfigWebApplicationContext()) {
      context.setServletContext(new MockServletContext());
      context.register(config, GlobalExceptionHandler.class);
      context.refresh();
      MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

      mvc.perform(get(path))
          .andExpect(status().isNotFound())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.code").value(404))
          .andExpect(jsonPath("$.msg").value("路径不存在，请检查路径是否正确"))
          .andExpect(result -> assertInstanceOf(exceptionType, result.getResolvedException()));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class MvcConfig {}

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/**").addResourceLocations("classpath:/missing-test-static/");
    }
  }
}
