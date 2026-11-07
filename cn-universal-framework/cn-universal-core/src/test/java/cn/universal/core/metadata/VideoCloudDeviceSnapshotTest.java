package cn.universal.core.metadata;

import cn.universal.core.video.hosting.VideoCloudDeviceSnapshot;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class VideoCloudDeviceSnapshotTest {
  @Test public void retainsCapabilitiesAndDropsVendorCredentialsRecursively() {
    var raw = Map.<String, Object>of("deviceAbility", "PTZ1,TCM,AudioTalk", "deviceVersion", "v1",
        "devicePasswordV2", "private-password", "playToken", "private-token",
        "channelList", List.of(Map.of("channelId", 0, "movable", true,
            "abilityRefs", List.of("336500"), "shareFunctions", List.of("Capture", "Live"),
            "playInfo", Map.of("token", "private"),
            "resolutions", List.of(Map.of("name", "极清", "imageSize", 136, "password", "private")))));
    var hosted = new VideoCloudHostingProvider.HostedDevice("camera", "0", "name", null, "model",
        "camera", null, null, null, null, 1, raw);
    var snapshot = VideoCloudDeviceSnapshot.device(hosted);
    assertEquals(true, snapshot.get("isTcmDevice"));
    assertEquals("model", snapshot.get("deviceModel"));
    assertFalse(snapshot.containsKey("devicePasswordV2"));
    assertFalse(snapshot.containsKey("playToken"));
    var channel = VideoCloudDeviceSnapshot.channels(raw).get(0);
    assertEquals("0", channel.get("channelId"));
    assertEquals(true, channel.get("movable"));
    assertEquals(List.of("Capture", "Live"), channel.get("shareFunctions"));
    assertFalse(channel.toString().contains("private"));
  }

  @Test public void doesNotGuessTcmFromEncryptModeOrSubstring() {
    var hosted = new VideoCloudHostingProvider.HostedDevice("camera", "0", "name", null, null,
        "camera", null, null, null, null, 1, Map.of("deviceAbility", "NOTTCM,PTZ", "encryptMode", 0));
    assertEquals(false, VideoCloudDeviceSnapshot.device(hosted).get("isTcmDevice"));
    var unknown = new VideoCloudHostingProvider.HostedDevice("camera", "0", "name", null, null,
        "camera", null, null, null, null, 1, Map.of("encryptMode", 0));
    assertFalse(VideoCloudDeviceSnapshot.device(unknown).containsKey("isTcmDevice"));
  }
}
