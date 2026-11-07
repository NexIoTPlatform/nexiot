package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeviceLogEnvelopeTest {
  private final MysqlDeviceLogService logs = new MysqlDeviceLogService();
  private final IoTDeviceDTO device = IoTDeviceDTO.builder().iotId("pcam")
      .productKey("p").deviceId("cam").deviceName("Camera").build();

  @Test void eventLogPreservesIdentifierAndDataWithoutConvertingParametersToProperties() {
    var request = BaseUPRequest.builder().messageType(MessageType.EVENT).event("motionDetection")
        .eventName("移动侦测").data(Map.of("channelId", "0", "event", "vendor-value",
            "data", Map.of("confidence", 92))).build();
    var row = logs.build(request, device);
    var content = JSONUtil.parseObj(row.getContent());
    assertEquals("motionDetection", row.getEvent());
    assertEquals("EVENT", content.getStr("messageType"));
    assertEquals("motionDetection", content.getStr("event"));
    assertEquals("移动侦测", content.getStr("eventName"));
    assertEquals("vendor-value", content.getJSONObject("data").getStr("event"));
    assertEquals(92, content.getJSONObject("data").getJSONObject("data").getInt("confidence"));
    assertFalse(content.containsKey("properties"));
    assertFalse(content.containsKey("channelId"));
  }

  @Test void eventPropertiesRemainSeparateAndEmptyEventDataIsAnObject() {
    var request = BaseUPRequest.builder().messageType(MessageType.EVENT).event("offline")
        .properties(Map.of("online", false)).build();
    var content = JSONUtil.parseObj(logs.build(request, device).getContent());
    assertTrue(content.getJSONObject("data").isEmpty());
    assertEquals(false, content.getJSONObject("properties").getBool("online"));
    assertEquals("offline", content.getStr("event"));
  }

  @Test void propertyLogPreservesNumericModelIdentifiersAndNesting() {
    var request = BaseUPRequest.builder().messageType(MessageType.PROPERTIES)
        .properties(Map.of("111100", 1, "14600", Map.of("14603", 1))).build();
    var content = JSONUtil.parseObj(logs.build(request, device).getContent());
    assertEquals("PROPERTIES", content.getStr("messageType"));
    assertEquals(1, content.getJSONObject("properties").getInt("111100"));
    assertEquals(1, content.getJSONObject("properties").getJSONObject("14600").getInt("14603"));
    assertFalse(content.containsKey("event"));
    assertFalse(content.containsKey("data"));
  }
}
