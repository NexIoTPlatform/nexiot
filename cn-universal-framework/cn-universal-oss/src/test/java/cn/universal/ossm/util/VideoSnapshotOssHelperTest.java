package cn.universal.ossm.util;

import static org.junit.Assert.*;

import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.VideoPlatformInstance;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class VideoSnapshotOssHelperTest {
  @Test
  public void generatedAndRequestedNamesFitPersistedSnapshotObjectKey() {
    var uploaded = new AtomicReference<String>();
    ISysOssService service =
        (ISysOssService)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {ISysOssService.class},
                (proxy, method, args) -> {
                  if ("uploadStream".equals(method.getName())) {
                    String name = (String) args[1];
                    uploaded.set(name);
                    String objectKey = "snapshot/" + name;
                    assertTrue("persisted sys_oss.file_name", objectKey.length() <= 64);
                    assertEquals("tenant-video", OssTenantContext.getTenantId());
                    assertTrue(name.endsWith(".jpg"));
                    return new SysOss()
                        .setFileName(objectKey)
                        .setUrl("https://oss.example/" + objectKey)
                        .setTenantId("tenant-video");
                  }
                  return null;
                });
    var helper = new VideoSnapshotOssHelper(service);
    var instance =
        VideoPlatformInstance.builder()
            .instanceKey("a-long-video-platform-instance-key")
            .tenantId("tenant-video")
            .build();
    Map<String, Object> image = Map.of("snapshotBytes", new byte[] {1, 2, 3});
    var params =
        Map.<String, Object>of(
            "deviceId", "device-with-a-long-unique-identifier",
            "channelId", "channel-with-a-long-identifier", "failOnOssUpload", true);
    var result = helper.uploadSnapshotIfPresent(instance, params, image, "hik_isup");
    assertEquals(true, result.get("ossUploadSuccess"));
    assertEquals("tenant-video", result.get("ossTenantId"));
    assertFalse(result.containsKey("snapshotBytes"));
    String first = uploaded.get();
    helper.uploadSnapshotIfPresent(instance, params, image, "hik_isup");
    assertNotEquals("generated snapshots remain distinct", first, uploaded.get());
    helper.uploadSnapshotIfPresent(
        instance,
        Map.of("fileName", "requested-" + "x".repeat(100) + ".jpg", "failOnOssUpload", true),
        image,
        "hik_isup");
    assertTrue(uploaded.get().startsWith("requested-"));
    assertNull("OSS tenant context is restored", OssTenantContext.getTenantId());
  }
}
