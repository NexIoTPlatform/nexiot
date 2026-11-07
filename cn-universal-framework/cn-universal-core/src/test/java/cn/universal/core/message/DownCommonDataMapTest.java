package cn.universal.core.message;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class DownCommonDataMapTest {

  @Test
  public void shouldKeepDynamicFieldsWhenBuildFromMap() {
    Map<String, Object> source = new HashMap<>();
    source.put("deviceName", "sasa");
    source.put("imei", "sasa");
    source.put("latitude", "42.527911");
    source.put("longitude", "87.636442");
    source.put("secret", "assa");

    DownCommonData data = DownCommonData.from(source);

    Assert.assertEquals("sasa", data.getDeviceName());
    Assert.assertEquals("sasa", data.getImei());
    Assert.assertEquals("42.527911", data.getLatitude());
    Assert.assertEquals("87.636442", data.getLongitude());
    Assert.assertEquals("assa", data.get("secret"));
    Assert.assertTrue(data.containsKey("secret"));
    Assert.assertTrue(data.containsKey("deviceName"));
  }
}
