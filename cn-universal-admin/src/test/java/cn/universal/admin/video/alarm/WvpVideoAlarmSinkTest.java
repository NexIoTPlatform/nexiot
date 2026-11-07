package cn.universal.admin.video.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WvpVideoAlarmSinkTest {
  @Test void routesToParentAndPreservesGbFieldsAndTime() {
    SdkVideoAlarmUpService service = mock(SdkVideoAlarmUpService.class);
    new WvpVideoAlarmSink(service).accept("wvp", "instance", Map.of(
        "id", "42", "deviceId", "parent", "channelId", "channel", "alarmMethod", "5",
        "alarmType", "2", "alarmTime", "2026-09-23 16:30:00", "alarmPriority", "1"));
    var request = ArgumentCaptor.forClass(SdkVideoAlarmCallbackRequest.class);
    verify(service).publishFromWvp(request.capture());
    assertEquals("parent", request.getValue().getDeviceId());
    assertEquals("channel", request.getValue().getChannelId());
    assertEquals("vendor_raw", request.getValue().getAlarmType());
    assertEquals("2", request.getValue().getData().get("gbAlarmType"));
    assertEquals("5:2", request.getValue().getData().get("vendorEventCode"));
    assertEquals("GB28181", request.getValue().getData().get("sdkEventFamily"));
    assertEquals("2", request.getValue().getRaw().get("alarmType"));
    assertFalse(request.getValue().getRaw().containsKey("gbAlarmType"));
    assertEquals("unknown", request.getValue().getAlarmState());
    assertEquals("wvp:instance:42", request.getValue().getEventId());
    assertEquals(WvpVideoAlarmSink.parseTime("2026-09-23T08:30:00Z"), request.getValue().getAlarmTime());
  }
}
