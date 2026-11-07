package cn.universal.web.controller.openapi.v2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.universal.common.constant.IoTConstant;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.common.exception.IoTException;
import cn.universal.web.config.openapi.v2.OpenApiV2ExceptionHandler;
import cn.universal.web.service.openapi.v2.OpenApiV2DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

class OpenApiV2ExceptionHandlerTest {
  private static final String PRIVATE = "jdbc:postgresql://private-host/db?password=secret";
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    MDC.put(IoTConstant.TRACE_ID, "safe-request-id");
    mvc =
        MockMvcBuilders.standaloneSetup(new FailureController())
            .setControllerAdvice(new OpenApiV2ExceptionHandler())
            .build();
  }

  @AfterEach
  void cleanup() {
    MDC.remove(IoTConstant.TRACE_ID);
  }

  @Test
  void authorizationErrorsReturn403WithoutResourceOrCredentialDetails() throws Exception {
    assertSafe("denied", 403, 403);
  }

  @Test
  void businessConflictRetainsBusinessCodeButHidesInternalMessage() throws Exception {
    assertSafe("conflict", 409, 701);
    assertSafe("unsupported", 409, 409);
  }

  @Test
  void queryInputUnavailableAndRateLimitMapToTheirHttpStatuses() throws Exception {
    assertSafe("query-invalid", 400, 400);
    assertSafe("unavailable", 503, 503);
    assertSafe("rate-limit", 429, 429);
  }

  @Test
  void unexpectedFailuresReturn500WithTraceAndNoExceptionText() throws Exception {
    assertSafe("unexpected", 500, 500);
  }

  @Test
  void explicitPublicStatusReasonIsPreserved() throws Exception {
    mvc.perform(get("/test/v2/status"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.msg").value("公开参数说明"))
        .andExpect(jsonPath("$.requestId").value("safe-request-id"));
  }

  @Test
  void malformedAndInvalidBodiesReturn400WithoutEchoingInput() throws Exception {
    for (String body : new String[] {"{\"value\":\"" + PRIVATE + "\"", "{\"value\":\"\"}", "{}"}) {
      String json =
          mvc.perform(post("/test/v2/body").contentType(MediaType.APPLICATION_JSON).content(body))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.code").value(400))
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertFalse(json.contains("private-host"));
      assertFalse(json.contains("password"));
    }
  }

  @Test
  void missingAndMalformedQueryParametersHaveTheSameSafe400Envelope() throws Exception {
    mvc.perform(get("/test/v2/query"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
    String json =
        mvc.perform(get("/test/v2/query").param("number", PRIVATE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(json.contains(PRIVATE));
  }

  @Test
  void realDeviceControllerReturnsJsonForParameterErrorsWithoutAcceptHeader() throws Exception {
    OpenApiV2DeviceService service = mock(OpenApiV2DeviceService.class);
    MockMvc deviceMvc =
        MockMvcBuilders.standaloneSetup(new OpenApiV2DeviceController(service))
            .setControllerAdvice(new OpenApiV2ExceptionHandler())
            .build();
    String json =
        deviceMvc
            .perform(get("/api/openapi/v2/devices").param("page", PRIVATE))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.requestId").value("safe-request-id"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(json.contains("private-host"));
    verifyNoInteractions(service);
  }

  private void assertSafe(String kind, int status, int code) throws Exception {
    String json =
        mvc.perform(get("/test/v2/failure/{kind}", kind))
            .andExpect(status().is(status))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.requestId").value("safe-request-id"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(json.contains("private-host"));
    assertFalse(json.contains("password"));
    assertFalse(json.contains("RuntimeException"));
    assertFalse(json.contains("stackTrace"));
  }

  @RestController
  @RequestMapping(value = "/test/v2", produces = MediaType.APPLICATION_JSON_VALUE)
  static class FailureController {
    @GetMapping("/failure/{kind}")
    R<Void> fail(@PathVariable String kind) {
      throw switch (kind) {
        case "denied" -> new AccessDeniedException(PRIVATE);
        case "conflict" -> new IoTException(PRIVATE, 701);
        case "unsupported" -> new BaseException(PRIVATE);
        case "query-invalid" -> new IoTException(PRIVATE, 400);
        case "unavailable" -> new IoTException(PRIVATE, 503);
        case "rate-limit" -> new IoTException(PRIVATE, 429);
        default -> new IllegalStateException(PRIVATE);
      };
    }

    @GetMapping("/status")
    R<Void> status() {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "公开参数说明");
    }

    @PostMapping("/body")
    R<Void> body(@Valid @RequestBody Input body) {
      return R.ok();
    }

    @GetMapping("/query")
    R<Void> query(@RequestParam int number) {
      return R.ok();
    }
  }

  record Input(@NotBlank String value) {}
}
