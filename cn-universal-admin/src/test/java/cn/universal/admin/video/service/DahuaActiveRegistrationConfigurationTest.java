package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class DahuaActiveRegistrationConfigurationTest {
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final ObjectMapper json = new ObjectMapper();
  private final DahuaActiveRegistrationConfiguration configuration = new DahuaActiveRegistrationConfiguration(instances, json);
  private VideoPlatformInstance instance(String options) {
    return VideoPlatformInstance.builder().id(1L).instanceKey("dh-a").platformType("dahua_sdk")
        .endpoint("http://gateway:5102").tenantId("tenant-a")
        .auth("{\"defaultUsername\":\"direct-user\",\"defaultPassword\":\"direct-pass\",\"activeRegistrationPassword\":\"registration-pass\"}")
        .options(options).enabled(1).build();
  }
  @Test void optionalRegistrationPreservesExistingDirectInstance() {
    var instance = instance("{\"streamMode\":\"rtsp_first\",\"subscription\":{\"enabled\":true}}");
    var original = instance.getOptions(); configuration.validate(instance);
    assertEquals(original, instance.getOptions()); verifyNoInteractions(instances);
  }
  @Test void registrationOnlyNormalizesItsOwnOptionsAndPreservesDirectCredentials() throws Exception {
    var instance = instance("{\"activeRegistration\":{\"enabled\":true,\"advertisedHost\":\"register.example.test\"},\"streamMode\":\"rtsp_first\"}");
    when(instances.select(any())).thenReturn(List.of()); String auth = instance.getAuth();
    configuration.validate(instance);
    assertEquals(auth, instance.getAuth());
    assertEquals("rtsp_first", json.readTree(instance.getOptions()).path("streamMode").asText());
    assertEquals(9500, json.readTree(instance.getOptions()).path("activeRegistration").path("listenPort").asInt());
    assertEquals("0.0.0.0", json.readTree(instance.getOptions()).path("activeRegistration").path("listenIp").asText());
  }
  @Test void otherTenantCannotUseSameRegistrationPortOnSameGateway() {
    var instance = instance("{\"activeRegistration\":{\"enabled\":true,\"listenPort\":9500,\"advertisedHost\":\"register.example.test\"}}");
    var other = instance(instance.getOptions()); other.setId(2L); other.setTenantId("tenant-b");
    when(instances.select(any())).thenReturn(List.of(other));
    assertThrows(BaseException.class, () -> configuration.validate(instance));
    other.setEndpoint("http://different-gateway:5102");
    assertDoesNotThrow(() -> configuration.validate(instance));
  }
  @Test void rejectsInvalidListenerOrAddressWithoutTouchingCredentials() {
    for (String config : List.of(
        "{\"enabled\":true,\"listenPort\":1,\"advertisedHost\":\"192.0.2.1\"}",
        "{\"enabled\":true,\"listenIp\":\"256.0.0.1\",\"advertisedHost\":\"192.0.2.1\"}",
        "{\"enabled\":true,\"advertisedHost\":\"0.0.0.0\"}",
        "{\"enabled\":true,\"advertisedHost\":\"http://192.0.2.1:9500\"}")) {
      var instance = instance("{\"activeRegistration\":" + config + "}"); String original = instance.getAuth();
      assertThrows(BaseException.class, () -> configuration.validate(instance)); assertEquals(original, instance.getAuth());
    }
  }
  @Test void blankDedicatedRegistrationPasswordRetainsSavedValueAndDirectPassword() throws Exception {
    var previous = instance("{\"activeRegistration\":{\"enabled\":false}}");
    var next = instance(previous.getOptions());
    next.setAuth("{\"defaultPassword\":\"direct-pass\",\"activeRegistrationPassword\":\"\"}");
    when(instances.selectByPrimaryKey(1L)).thenReturn(previous); configuration.validate(next);
    assertEquals("registration-pass", json.readTree(next.getAuth()).path("activeRegistrationPassword").asText());
    assertEquals("direct-pass", json.readTree(next.getAuth()).path("defaultPassword").asText());
  }
  @Test void dedicatedRegistrationPasswordIsMaskedAndOmittedOrMaskedSavesRetainIt() throws Exception {
    var saved = instance("{}");
    String masked = VideoPlatformSecretMask.mask(saved.getAuth());
    assertEquals("***", json.readTree(masked).path("activeRegistrationPassword").asText());
    assertFalse(masked.contains("registration-pass")); assertFalse(masked.contains("direct-pass"));
    for (String submitted : List.of("{\"activeRegistrationUsername\":\"registration-user\"}",
        "{\"activeRegistrationPassword\":\"***\"}")) {
      String merged = cn.universal.common.utils.JsonDesensitizationUtil.mergeAuthConfig(submitted, saved.getAuth());
      assertEquals("registration-pass", json.readTree(merged).path("activeRegistrationPassword").asText());
      assertEquals("direct-pass", json.readTree(merged).path("defaultPassword").asText());
    }
  }
}
