package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.BaseException;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class HikIsupPlatformConfigurationTest {
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final ObjectMapper json = new ObjectMapper();
  private final HikIsupPlatformConfiguration config = new HikIsupPlatformConfiguration(instances, json);
  private VideoPlatformInstance instance() {
    return VideoPlatformInstance.builder().instanceKey("isup-a").tenantId("tenant-a")
        .platformType("hik_isup").endpoint("http://gateway:5102").auth(authWithKey("Ab12Cd34Ef56"))
        .options("{\"cmsPort\":7660}").build();
  }

  private String authWithKey(String key) {
    return json.createObjectNode().put("isupKey", key).toString();
  }

  private VideoPlatformInstance savedInstanceWithKey(String key) {
    var existing = instance();
    existing.setId(9L);
    existing.setAuth(authWithKey(key));
    when(instances.selectByPrimaryKey(9L)).thenReturn(existing);
    when(instances.select(any())).thenReturn(List.of(existing));
    return existing;
  }

  @Test void rejectsMissingTenantMissingKeyAndMaskedKey() {
    var value = instance(); value.setTenantId(null);
    assertThrows(BaseException.class, () -> config.validate(value));
    value.setTenantId("tenant-a"); value.setAuth("{}");
    assertThrows(BaseException.class, () -> config.validate(value));
    value.setAuth("{\"isupKey\":\"******\"}");
    assertThrows(BaseException.class, () -> config.validate(value));
  }

  @Test void migratesLegacyOptionKeyAndMasksResponses() throws Exception {
    when(instances.select(any())).thenReturn(List.of());
    var value = instance(); value.setAuth("{}"); value.setOptions("{\"isupKey\":\"Ab12Cd34Ef56\",\"cmsPort\":7660}");
    config.validate(value);
    assertEquals("Ab12Cd34Ef56", json.readTree(value.getAuth()).path("isupKey").asText());
    assertFalse(json.readTree(value.getOptions()).has("isupKey"));
    assertFalse(VideoPlatformSecretMask.mask(value.getAuth()).contains("Ab12Cd34Ef56"));
    assertFalse(VideoPlatformSecretMask.mask("{\"subscription\":{\"token\":\"private-token\"}}").contains("private-token"));
  }

  @Test void rejectsPortCollisionOnSameNodeAndAllowsDifferentNode() {
    var existing = instance(); existing.setId(9L);
    when(instances.select(any())).thenReturn(List.of(existing));
    var candidate = instance(); candidate.setEndpoint("http://GATEWAY:5102/api/isup/");
    assertThrows(BaseException.class, () -> config.validate(candidate));
    candidate.setEndpoint("http://other-gateway:5102");
    assertDoesNotThrow(() -> config.validate(candidate));
  }

  @Test void permitsOwnPortButRejectsInvalidOrOversizedKey() {
    var value = instance(); value.setId(9L);
    when(instances.select(any())).thenReturn(List.of(value));
    assertDoesNotThrow(() -> config.validate(value));
    value.setOptions("{\"cmsPort\":65536}");
    assertThrows(BaseException.class, () -> config.validate(value));
    value.setOptions("{\"cmsPort\":7660}"); value.setAuth("{\"isupKey\":\"" + "汉".repeat(11) + "\"}");
    assertThrows(BaseException.class, () -> config.validate(value));
  }

  @Test void independentPortStillRequiresIndependentPlatformKeyAndListenerPort() {
    var existing = instance(); existing.setId(9L);
    when(instances.select(any())).thenReturn(List.of(existing));
    var candidate = instance(); candidate.setOptions("{\"cmsPort\":7661}");
    assertThrows(BaseException.class, () -> config.validate(candidate));
    candidate.setAuth(authWithKey("Zy98Xw76Vu54"));
    candidate.setOptions("{\"cmsPort\":7661,\"cmsListenPort\":7660}");
    assertThrows(BaseException.class, () -> config.validate(candidate));
    candidate.setOptions("{\"cmsPort\":7661}");
    assertDoesNotThrow(() -> config.validate(candidate));
  }

  @Test void acceptsNewKeysAtEightAndTwelveAsciiCharacterBoundaries() throws Exception {
    when(instances.select(any())).thenReturn(List.of());
    for (String key : List.of("Ab12Cd34", "Ab12Cd34Ef56")) {
      var value = instance();
      value.setAuth(authWithKey(key));
      assertDoesNotThrow(() -> config.validate(value));
      assertEquals(key, json.readTree(value.getAuth()).path("isupKey").asText());
    }
  }

  @Test void rejectsNewKeysOutsideAsciiAlphanumericEightToTwelveCharacters() {
    when(instances.select(any())).thenReturn(List.of());
    for (String key : List.of("Ab12Cd3", "Ab12Cd34Ef567", "Ab12Cd34Ef567890",
        "汉".repeat(8), "Abcd 1234", "Abcd\0" + "1234", "Abcd-1234")) {
      var value = instance();
      value.setAuth(authWithKey(key));
      assertThrows(BaseException.class, () -> config.validate(value));
    }
  }

  @Test void retainsExistingValidNativeKeysWithoutChangingTheirBytes() throws Exception {
    for (String key : List.of("X", "a".repeat(16), "b".repeat(32), "legacy-key!", "旧密钥abc")) {
      savedInstanceWithKey(key);
      var value = instance();
      value.setId(9L);
      value.setAuth(authWithKey(key));
      assertDoesNotThrow(() -> config.validate(value));
      assertEquals(key, json.readTree(value.getAuth()).path("isupKey").asText());
    }
  }

  @Test void migratesUnchangedLegacyOptionKeyToAuth() throws Exception {
    String key = "legacy-option-key!";
    var existing = savedInstanceWithKey(key);
    existing.setAuth("{}");
    existing.setOptions(json.createObjectNode().put("cmsPort", 7660).put("isupKey", key).toString());
    var value = instance();
    value.setId(9L);
    value.setAuth("{}");
    value.setOptions(existing.getOptions());
    assertDoesNotThrow(() -> config.validate(value));
    assertEquals(key, json.readTree(value.getAuth()).path("isupKey").asText());
    assertFalse(json.readTree(value.getOptions()).has("isupKey"));
  }

  @Test void replacesExistingLongKeyWithTwelveAsciiCharacters() throws Exception {
    savedInstanceWithKey("a".repeat(16));
    var value = instance();
    value.setId(9L);
    value.setAuth(authWithKey("Zy98Xw76Vu54"));
    assertDoesNotThrow(() -> config.validate(value));
    assertEquals("Zy98Xw76Vu54", json.readTree(value.getAuth()).path("isupKey").asText());
  }

  @Test void rejectsReplacingExistingKeyWithDifferentSixteenCharacterKey() {
    savedInstanceWithKey("a".repeat(16));
    var value = instance();
    value.setId(9L);
    value.setAuth(authWithKey("b".repeat(16)));
    assertThrows(BaseException.class, () -> config.validate(value));
  }

  @Test void preservesLegacyKeyAfterMaskedBlankOrOmittedAuthIsMerged() throws Exception {
    String key = "legacy-key-123456";
    var existing = savedInstanceWithKey(key);
    for (String submitted : List.of("", "{}", authWithKey("***"), authWithKey("******"))) {
      var value = instance();
      value.setId(9L);
      value.setAuth(JsonDesensitizationUtil.mergeAuthConfig(submitted, existing.getAuth()));
      assertDoesNotThrow(() -> config.validate(value));
      assertEquals(key, json.readTree(value.getAuth()).path("isupKey").asText());
    }
  }

  @Test void legacyCompatibilityStillRejectsOversizedOrNulContainingKey() {
    for (String key : List.of("a".repeat(33), "汉".repeat(11), "Abcd\0" + "1234")) {
      savedInstanceWithKey(key);
      var value = instance();
      value.setId(9L);
      value.setAuth(authWithKey(key));
      assertThrows(BaseException.class, () -> config.validate(value));
    }
  }
}
