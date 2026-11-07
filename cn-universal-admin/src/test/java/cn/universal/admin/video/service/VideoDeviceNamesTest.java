package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.BaseException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VideoDeviceNamesTest {
  @Test
  void remoteRenameRefreshesSourcesWhileManualAliasesSurviveForBothStreams() {
    String stored =
        """
        {"deviceNameOverride":"大华球机","channelNameOverrides":{"1":"平台门口"},
         "sourceChannelNames":{"1":"旧原名","1_sub":"旧原名 Sub"}}
        """;
    Map<String, Object> incoming =
        Map.of(
            "deviceName",
            "camera",
            "channelList",
            List.of(
                Map.of("channelId", "1", "channelName", "露台铁架子", "streamType", "main"),
                Map.of("channelId", "1_sub", "channelName", "露台铁架子 Sub", "streamType", "sub")));
    var result = VideoDeviceNames.apply(stored, incoming);
    assertEquals("大华球机", result.get("deviceName"));
    var list = (List<?>) result.get("channelList");
    assertEquals("平台门口", VideoDeviceNames.object(list.get(0)).get("channelName"));
    assertEquals("平台门口 Sub", VideoDeviceNames.object(list.get(1)).get("channelName"));
    assertEquals(
        "露台铁架子", VideoDeviceNames.object(result.get(VideoDeviceNames.SOURCE_CHANNELS)).get("1"));
    assertEquals(
        "露台铁架子",
        VideoDeviceNames.object(((List<?>) incoming.get("channelList")).getFirst())
            .get("channelName"));
  }

  @Test
  void transientConfigFailureKeepsLastObservedTitleAndSource() {
    var result =
        VideoDeviceNames.apply(
            "{\"sourceChannelNames\":{\"1\":\"实际通道\"}}",
            Map.of(
                "channelList",
                List.of(
                    Map.of(
                        "channelId",
                        "1",
                        "channelName",
                        "CH1",
                        "attributes",
                        Map.of("channelNameSource", "fallback")))));
    assertEquals(
        "实际通道",
        VideoDeviceNames.object(((List<?>) result.get("channelList")).getFirst())
            .get("channelName"));
    assertEquals(
        "实际通道", VideoDeviceNames.object(result.get(VideoDeviceNames.SOURCE_CHANNELS)).get("1"));
    var initial =
        VideoDeviceNames.apply(
            null,
            Map.of(
                "channelList",
                List.of(
                    Map.of(
                        "channelId", "1", "channelName", "CH1", "channelNameSource", "fallback"))));
    assertFalse(initial.containsKey(VideoDeviceNames.SOURCE_CHANNELS));
  }

  @Test
  void malformedConfigurationAndInvalidNamesFailWithoutSilentlyDiscardingData() {
    for (String config : List.of("{bad", "null", "[]"))
      assertThrows(BaseException.class, () -> VideoDeviceNames.configuration(config));
    for (Object name : List.of(" ", "bad\nname", "x".repeat(201), 123))
      assertThrows(BaseException.class, () -> VideoDeviceNames.validateName(name));
    assertEquals("露台", VideoDeviceNames.validateName(" 露台 "));
  }
}
