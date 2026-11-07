package cn.universal.core.message;

import cn.hutool.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class UnifiedDownlinkCommandDataMappingTest {

  @Test
  public void shouldExtractRootLevelCustomFieldsIntoExtensions() {
    JSONObject json = new JSONObject();
    json.set("productKey", "pk-1");
    json.set("deviceId", "dev-1");
    json.set("cmd", "DEV_ADD");
    json.set("tenantId", "tenant-001");
    json.set("deviceName", "root-device-name");
    json.set("imei", "123456789012345");
    json.set("customFlag", true);
    json.set("slaveAddress", "01");
    json.set("data", new JSONObject().set("projectName", "demo-project"));

    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(json);

    Assert.assertEquals("tenant-001", command.getTenantId());
    Assert.assertNull(command.getExtension("tenantId"));
    Assert.assertEquals("root-device-name", command.getExtension("deviceName"));
    Assert.assertEquals("123456789012345", command.getExtension("imei"));
    Assert.assertEquals(Boolean.TRUE, command.getExtension("customFlag"));
    Assert.assertEquals("demo-project", command.getExtension("projectName"));
    Assert.assertEquals("01", command.getSlaveAddress());
    Assert.assertNull(command.getExtension("slaveAddress"));
    Assert.assertEquals("tenant-001", command.toJson().getStr("tenantId"));
  }

  @Test
  public void explicitDataShouldOverrideRootLevelCustomField() {
    JSONObject json = new JSONObject();
    json.set("productKey", "pk-1");
    json.set("deviceId", "dev-1");
    json.set("cmd", "DEV_ADD");
    json.set("deviceName", "root-device-name");
    json.set("data", new JSONObject().set("deviceName", "data-device-name"));

    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(json);

    Assert.assertEquals("data-device-name", command.getExtension("deviceName"));
  }
}
