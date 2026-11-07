package cn.universal.dm.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.query.IoTAPIQuery;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IoTDeviceCacheKeyResolverTest {

  private final IoTDeviceCacheKeyResolver resolver = new IoTDeviceCacheKeyResolver();

  @Test
  void shouldResolveOnlyPureUniqueIdentityQueries() {
    assertEquals("iot:iot-1", resolver.resolveMapKey(Map.of("iotId", "iot-1")));
    assertEquals(
        "pd:pk-1:dev-1",
        resolver.resolveMapKey(Map.of("productKey", "pk-1", "deviceId", "dev-1")));
    assertNull(
        resolver.resolveMapKey(
            Map.of("iotId", "iot-1", "productKey", "wrong-product")));
    assertNull(resolver.resolveMapKey(Map.of("gwProductKey", "gateway-1")));
  }

  @Test
  void shouldBypassApiCacheWhenAuthorizationFiltersArePresent() {
    assertTrue(
        resolver.canCacheApiQuery(
            IoTAPIQuery.builder().productKey("pk-1").deviceId("dev-1").build()));
    assertFalse(
        resolver.canCacheApiQuery(
            IoTAPIQuery.builder()
                .productKey("pk-1")
                .deviceId("dev-1")
                .iotUnionId("user-1")
                .build()));
  }

  @Test
  void shouldBuildAllKnownAliasesForOneDevice() {
    Map<String, ?> keys =
        resolver.buildDeviceCacheKeys(
            IoTDeviceDTO.builder()
                .iotId("iot-1")
                .productKey("pk-1")
                .deviceId("dev-1")
                .thirdPlatform("mqtt")
                .extDeviceId("ext-1")
                .build());

    assertTrue(keys.containsKey("selectDevInstanceBOMap"));
    assertEquals(
        java.util.Set.of("pk-1:dev-1"), keys.get("selectDevInstanceBOPD"));
    assertEquals(java.util.Set.of("iot-1"), keys.get("iot_dev_tag_group_name"));
  }
}
