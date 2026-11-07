package cn.universal.web.controller.openapi.v2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.universal.common.domain.R;
import cn.universal.web.config.OpenApiV2DocumentationConfig;
import cn.universal.web.config.SwaggerConfig;
import cn.universal.web.service.openapi.v2.OpenApiV2ApplicationService;
import cn.universal.web.service.openapi.v2.OpenApiV2DeviceService;
import cn.universal.web.service.openapi.v2.OpenApiV2LogService;
import cn.universal.web.service.openapi.v2.VideoApiV2Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Generates the real grouped SpringDoc document from production controllers and configuration. Only
 * their business services are mocked; no database, Redis or hardware connection is started. Run
 * with -Dnexiot.openapi.export=/tmp/openapi-v2.json to save the generated document for import.
 */
class OpenApiV2DocumentationTest {
  private static final String ROOT = "/api/openapi/v2";
  private static final String DEVICE = ROOT + "/devices/{productKey}/{deviceId}";
  private static final String VIDEO = ROOT + "/video/devices/{productKey}/{deviceId}/channels";
  private static final String APPLICATION = ROOT + "/applications/current";
  private static AnnotationConfigWebApplicationContext context;
  private static JsonNode document;

  @BeforeAll
  static void generateDocumentFromRealSpringDocEndpoint() throws Exception {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        context,
        "swagger.enable=true",
        "springdoc.api-docs.enabled=true",
        "springdoc.show-actuator=false",
        "springdoc.cache.disabled=true");
    context.register(DocumentationContext.class);
    context.refresh();
    var mvc = MockMvcBuilders.webAppContextSetup(context).build();
    String generated =
        mvc.perform(get("/v3/api-docs/nexiot-openapi-v2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    document = new ObjectMapper().readTree(generated);
    String export = System.getProperty("nexiot.openapi.export");
    if (export != null && !export.isBlank()) {
      Path output = Path.of(export).toAbsolutePath();
      Files.createDirectories(output.getParent());
      Files.writeString(
          output,
          new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(document) + "\n");
    }
  }

  @AfterAll
  static void closeContext() {
    if (context != null) context.close();
  }

  @Test
  void allFourControllersAreIncludedOnceAndV1IsExcluded() {
    Set<String> expected =
        Set.of(
            "GET " + ROOT + "/devices",
            "POST " + ROOT + "/products/{productKey}/devices",
            "GET " + DEVICE,
            "PUT " + DEVICE,
            "DELETE " + DEVICE,
            "GET " + DEVICE + "/properties",
            "GET " + DEVICE + "/model",
            "POST " + DEVICE + "/functions/{function}/invoke",
            "GET " + DEVICE + "/logs",
            "GET " + DEVICE + "/logs/cursor",
            "GET " + DEVICE + "/properties/{property}/logs",
            "GET " + DEVICE + "/properties/{property}/logs/cursor",
            "GET " + APPLICATION,
            "GET " + APPLICATION + "/push/http",
            "PUT " + APPLICATION + "/push/http",
            "PUT " + APPLICATION + "/devices/{productKey}/{deviceId}",
            "DELETE " + APPLICATION + "/devices/{productKey}/{deviceId}",
            "GET " + VIDEO,
            "POST " + VIDEO + "/{channelId}/snapshots",
            "GET " + VIDEO + "/{channelId}/snapshots/{requestId}",
            "DELETE " + VIDEO + "/{channelId}/snapshots/{requestId}");
    Set<String> actual = new HashSet<>();
    Set<String> operations = new HashSet<>();
    document
        .path("paths")
        .fields()
        .forEachRemaining(
            path -> {
              assertTrue(path.getKey().startsWith(ROOT + "/"), path.getKey());
              path.getValue()
                  .fields()
                  .forEachRemaining(
                      operation -> {
                        actual.add(
                            operation.getKey().toUpperCase(java.util.Locale.ROOT)
                                + " "
                                + path.getKey());
                        String id = operation.getValue().path("operationId").asText();
                        assertFalse(id.isBlank(), path.getKey());
                        assertTrue(operations.add(id), "Duplicate operationId: " + id);
                        assertFalse(operation.getValue().path("summary").asText().isBlank(), id);
                        assertFalse(
                            operation.getValue().path("description").asText().isBlank(), id);
                      });
            });
    assertEquals(expected, actual);
    assertFalse(document.path("paths").has("/api/openapi/v1/documentation-probe"));
    assertEquals("2.0", document.path("info").path("version").asText());
  }

  @Test
  void bodyModelsRemainTypedAndDocumentTheirValidation() {
    JsonNode create = request(ROOT + "/products/{productKey}/devices", "post");
    assertTrue(create.path("properties").has("deviceId"));
    assertTrue(create.path("properties").has("deviceName"));
    assertTrue(contains(create.path("required"), "deviceId"));
    assertTrue(contains(create.path("required"), "deviceName"));
    assertFalse(create.path("properties").has("tenantId"));
    assertFalse(create.path("properties").has("applicationId"));
    assertTrue(request(DEVICE, "put").path("properties").has("deviceName"));
    assertTrue(
        request(DEVICE + "/functions/{function}/invoke", "post").path("properties").has("data"));
    JsonNode push = request(APPLICATION + "/push/http", "put");
    assertTrue(push.path("properties").has("url"));
    assertTrue(contains(push.path("required"), "enable"));
    assertTrue(push.path("properties").path("secret").path("writeOnly").asBoolean());
    assertFalse(data(APPLICATION, "get").path("properties").has("appSecret"));
    assertFalse(data(APPLICATION + "/push/http", "get").path("properties").has("secret"));
    JsonNode snapshot = request(VIDEO + "/{channelId}/snapshots", "post");
    assertTrue(contains(snapshot.path("properties").path("source").path("enum"), "device"));
    assertTrue(contains(snapshot.path("properties").path("source").path("enum"), "platform_frame"));
  }

  @Test
  void responseWrappersAndNestedPageItemsKeepTheirGenericTypes() {
    JsonNode creation = data(ROOT + "/products/{productKey}/devices", "post");
    assertTrue(creation.path("properties").has("accepted"));
    assertFalse(creation.path("properties").has("result"));
    JsonNode identity = resolve(creation.path("properties").path("device"));
    assertTrue(identity.path("properties").has("deviceId"));
    assertTrue(identity.path("properties").has("productKey"));
    assertTrue(identity.path("properties").has("iotId"));
    assertFalse(identity.path("properties").has("deviceSecret"));
    JsonNode devicePage = data(ROOT + "/devices", "get");
    assertTrue(devicePage.path("properties").has("total"));
    JsonNode device = resolve(devicePage.path("properties").path("items").path("items"));
    assertTrue(device.path("properties").has("deviceId"));
    assertFalse(device.path("properties").has("deviceSecret"));
    for (String path : List.of(DEVICE + "/logs", DEVICE + "/logs/cursor")) {
      JsonNode page = data(path, "get");
      assertTrue(page.path("properties").has("hasMore"), path);
      assertTrue(page.path("properties").has("nextCursor"), path);
      JsonNode item = resolve(page.path("properties").path("items").path("items"));
      assertTrue(item.path("properties").has("messageType"), path);
      assertEquals("string", item.path("properties").path("id").path("type").asText());
    }
    for (String path :
        List.of(
            DEVICE + "/properties/{property}/logs",
            DEVICE + "/properties/{property}/logs/cursor")) {
      JsonNode item = resolve(data(path, "get").path("properties").path("items").path("items"));
      assertTrue(item.path("properties").has("property"), path);
      assertTrue(item.path("properties").has("value"), path);
    }
    assertTrue(data(APPLICATION, "get").path("properties").has("appId"));
    JsonNode snapshot = data(VIDEO + "/{channelId}/snapshots", "post");
    assertTrue(snapshot.path("properties").has("imageUrl"));
    assertTrue(snapshot.path("properties").has("pending"));
    assertTrue(contains(snapshot.path("properties").path("status").path("enum"), "completed"));
  }

  @Test
  void bearerSecurityAndCommonErrorResponsesArePresentForEveryOperation() {
    JsonNode bearer = document.path("components").path("securitySchemes").path("Authorization");
    assertEquals("http", bearer.path("type").asText());
    assertEquals("bearer", bearer.path("scheme").asText());
    assertEquals("JWT", bearer.path("bearerFormat").asText());
    assertTrue(hasAuthorization(document.path("security")));
    document
        .path("paths")
        .elements()
        .forEachRemaining(
            path ->
                path.elements()
                    .forEachRemaining(
                        operation -> {
                          assertTrue(
                              operation.path("responses").has("200"),
                              operation.path("operationId").asText());
                          assertTrue(
                              operation
                                  .path("responses")
                                  .path("200")
                                  .path("content")
                                  .has("application/json"),
                              operation.path("operationId").asText());
                          assertTrue(
                              hasAuthorization(operation.path("security"))
                                  || hasAuthorization(document.path("security")));
                          for (String status :
                              List.of("400", "401", "403", "409", "429", "500", "502", "503")) {
                            JsonNode response = operation.path("responses").path(status);
                            assertFalse(
                                response.isMissingNode(),
                                operation.path("operationId").asText() + " " + status);
                            if ("401".equals(status)) assertFalse(response.has("content"));
                            if (response.has("content")) {
                              JsonNode wrapper =
                                  resolve(
                                      response.path("content").elements().next().path("schema"));
                              assertTrue(wrapper.path("properties").has("code"));
                              assertTrue(wrapper.path("properties").has("msg"));
                              assertTrue(wrapper.path("properties").has("requestId"));
                              JsonNode errorData = wrapper.path("properties").path("data");
                              assertFalse(
                                  errorData.has("$ref"),
                                  "Error body must not advertise successful data: "
                                      + operation.path("operationId").asText()
                                      + " "
                                      + status);
                              assertFalse(errorData.has("properties"));
                            }
                          }
                        }));
  }

  @Test
  void everyGeneratedComponentReferenceResolves() {
    verifyReferences(document);
  }

  @Test
  void historyQueryParametersExposeCursorAndUnixSecondConstraints() {
    JsonNode parameters = operation(DEVICE + "/logs/cursor", "get").path("parameters");
    Set<String> names = new HashSet<>();
    parameters.forEach(parameter -> names.add(parameter.path("name").asText()));
    assertTrue(
        names.containsAll(
            Set.of("productKey", "deviceId", "startTime", "endTime", "limit", "cursor")));
    for (JsonNode parameter : parameters) {
      if (Set.of("startTime", "endTime").contains(parameter.path("name").asText())) {
        assertTrue(parameter.path("required").asBoolean());
        assertTrue(parameter.path("description").asText().contains("Unix 秒"));
      }
    }
  }

  private static JsonNode operation(String path, String method) {
    JsonNode operation = document.path("paths").path(path).path(method);
    assertFalse(operation.isMissingNode(), method + " " + path);
    return operation;
  }

  private static JsonNode request(String path, String method) {
    return resolve(
        operation(path, method)
            .path("requestBody")
            .path("content")
            .path("application/json")
            .path("schema"));
  }

  private static JsonNode data(String path, String method) {
    JsonNode response = operation(path, method).path("responses").path("200").path("content");
    JsonNode media = response.path("application/json");
    if (media.isMissingNode()) media = response.path("*/*");
    JsonNode wrapper = resolve(media.path("schema"));
    assertFalse(media.isMissingNode(), "Missing successful response schema: " + path);
    assertTrue(wrapper.path("properties").has("code"), path);
    assertTrue(wrapper.path("properties").has("requestId"), path);
    return resolve(wrapper.path("properties").path("data"));
  }

  private static JsonNode resolve(JsonNode schema) {
    if (schema.has("$ref")) {
      String ref = schema.path("$ref").asText();
      assertTrue(ref.startsWith("#/"), ref);
      return resolve(document.at(ref.substring(1)));
    }
    return schema;
  }

  private static void verifyReferences(JsonNode node) {
    if (node.has("$ref")) {
      String ref = node.path("$ref").asText();
      assertTrue(ref.startsWith("#/"), ref);
      assertFalse(document.at(ref.substring(1)).isMissingNode(), "Unresolved reference: " + ref);
    }
    node.elements().forEachRemaining(OpenApiV2DocumentationTest::verifyReferences);
  }

  private static boolean contains(JsonNode array, String value) {
    for (JsonNode item : array) if (value.equals(item.asText())) return true;
    return false;
  }

  private static boolean hasAuthorization(JsonNode security) {
    for (JsonNode item : security) if (item.has("Authorization")) return true;
    return false;
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableWebMvc
  @EnableConfigurationProperties(SpringDocConfigProperties.class)
  @ImportAutoConfiguration({
    SpringDocConfiguration.class,
    SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class
  })
  @Import({
    SwaggerConfig.class,
    OpenApiV2DocumentationConfig.class,
    OpenApiV2DeviceController.class,
    OpenApiV2LogController.class,
    OpenApiV2ApplicationController.class,
    VideoApiControllerV2.class,
    V1Probe.class
  })
  static class DocumentationContext {
    @Bean
    OpenApiV2DeviceService devices() {
      return mock(OpenApiV2DeviceService.class);
    }

    @Bean
    OpenApiV2LogService logs() {
      return mock(OpenApiV2LogService.class);
    }

    @Bean
    OpenApiV2ApplicationService applications() {
      return mock(OpenApiV2ApplicationService.class);
    }

    @Bean
    VideoApiV2Service videos() {
      return mock(VideoApiV2Service.class);
    }
  }

  @RestController
  @TestComponent
  static class V1Probe {
    @GetMapping("/api/openapi/v1/documentation-probe")
    public R<Void> probe() {
      return R.ok();
    }
  }
}
