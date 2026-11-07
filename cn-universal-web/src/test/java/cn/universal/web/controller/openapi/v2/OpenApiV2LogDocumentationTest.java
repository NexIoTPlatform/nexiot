package cn.universal.web.controller.openapi.v2;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

/** Guards the shared success annotation and concrete generic history response schemas. */
class OpenApiV2LogDocumentationTest {
  @Test
  void sharedSuccessResponseUsesEachMethodsDeclaredReturnType() {
    ApiResponses responses = OpenApiV2LogController.class.getAnnotation(ApiResponses.class);
    assertNotNull(responses);
    ApiResponse success =
        Arrays.stream(responses.value())
            .filter(response -> "200".equals(response.responseCode()))
            .findFirst()
            .orElseThrow();
    assertTrue(
        success.useReturnTypeSchema(), "Success must preserve each method's generic page type");
    assertFalse(success.description().isBlank());
  }

  @Test
  void everyHistoryMethodRetainsItsConcretePageAndItemSchema() {
    int methods = 0;
    for (var method : OpenApiV2LogController.class.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(GetMapping.class)) continue;
      methods++;
      var result =
          ModelConverters.getInstance()
              .resolveAsResolvedSchema(new AnnotatedType(method.getGenericReturnType()));
      Schema<?> wrapper = resolve(result.schema, result.referencedSchemas);
      assertTrue(wrapper.getProperties().containsKey("code"), method.getName());
      Schema<?> page = resolve(wrapper.getProperties().get("data"), result.referencedSchemas);
      for (String field : new String[] {"items", "total", "hasMore", "nextCursor"}) {
        assertTrue(page.getProperties().containsKey(field), method.getName() + " " + field);
      }
      Schema<?> item =
          resolve(page.getProperties().get("items").getItems(), result.referencedSchemas);
      assertEquals("string", item.getProperties().get("id").getType(), method.getName());
      if (method.getName().equals("properties") || method.getName().equals("propertyCursor")) {
        assertTrue(item.getProperties().containsKey("property"), method.getName());
        assertTrue(item.getProperties().containsKey("value"), method.getName());
      } else {
        assertTrue(item.getProperties().containsKey("messageType"), method.getName());
        assertTrue(item.getProperties().containsKey("content"), method.getName());
      }
    }
    assertEquals(4, methods);
  }

  private static Schema<?> resolve(Schema<?> schema, Map<String, Schema> schemas) {
    assertNotNull(schema);
    if (schema.get$ref() == null) return schema;
    String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
    Schema<?> resolved = schemas.get(name);
    assertNotNull(resolved, schema.get$ref());
    return resolved;
  }
}
