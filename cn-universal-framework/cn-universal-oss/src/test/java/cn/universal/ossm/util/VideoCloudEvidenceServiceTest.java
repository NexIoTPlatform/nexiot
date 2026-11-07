package cn.universal.ossm.util;

import static org.junit.Assert.*;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.service.ISysOssService;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class VideoCloudEvidenceServiceTest {
  private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0,
      0, 16, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1, 0, 1};

  @Test public void storesDecodedImageAndOriginalInCorrectTenantAndReusesSuccessfulArchive() {
    List<String> uploads = new ArrayList<>();
    ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> {
          if (!method.getName().equals("uploadStream")) throw new UnsupportedOperationException();
          uploads.add(OssTenantContext.getTenantId() + ":" + args[1]);
          return new SysOss().setOssId((long) uploads.size()).setFileName("stored/" + args[1])
              .setUrl("https://ours.example/" + uploads.size()).setService("minio").setTenantId("tenant-a");
        });
    var service = new VideoCloudEvidenceService(oss, "camera.example") {
      @Override protected byte[] download(String url) { return new byte[]{'D', 'H', 'A', 'V', 1, 2}; }
    };
    Map<String, Object> data = new LinkedHashMap<>(Map.of("picUrlArr", List.of("https://camera.example/signed")));
    service.archive("tenant-a", "p", "cam", data, (url, bytes) -> JPEG);
    assertEquals("ready", data.get("imageStatus"));
    assertEquals(2, uploads.size());
    assertTrue(uploads.getFirst().startsWith("tenant-a:"));
    assertTrue(uploads.getFirst().endsWith(".bin"));
    assertTrue(uploads.getFirst().contains("cloud-alarm-cam-"));
    assertTrue(uploads.getLast().endsWith(".jpg"));
    assertNotNull(data.get("snapshotUrl"));
    var decoded = (Map<?, ?>) ((List<?>) data.get("evidence")).getFirst();
    var original = (Map<?, ?>) decoded.get("original");
    assertNotEquals("decoded preview must preserve the separate encrypted source", decoded.get("ossId"), original.get("ossId"));
    Map<String, Object> duplicate = new LinkedHashMap<>(Map.of("picUrlArr", List.of("https://camera.example/signed")));
    service.archive("tenant-a", "p", "cam", duplicate, (url, bytes) -> { throw new AssertionError(); });
    assertEquals(2, uploads.size());
    assertEquals("ready", duplicate.get("imageStatus"));
    assertNull(OssTenantContext.getTenantId());
  }

  @Test public void failedOrEncryptedImagesNeverProduceBrokenPreviewUrls() {
    ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> new SysOss().setOssId(1L)
            .setFileName("stored/one.bin").setUrl("https://ours.example/one.bin").setService("minio"));
    var service = new VideoCloudEvidenceService(oss, "camera.example") {
      @Override protected byte[] download(String url) { return new byte[]{'D', 'H', 'A', 'V', 1, 2}; }
    };
    Map<String, Object> encrypted = new LinkedHashMap<>(Map.of("picUrlArr", List.of("https://camera.example/signed")));
    service.archive("tenant-a", "p", "cam", encrypted);
    assertEquals("encrypted", encrypted.get("imageStatus"));
    assertFalse(encrypted.containsKey("snapshotUrl"));
    var encryptedEntry = (Map<?, ?>) ((List<?>) encrypted.get("evidence")).getFirst();
    assertEquals("1", encryptedEntry.get("ossId"));
    assertFalse("same file metadata must appear only once", encryptedEntry.containsKey("original"));
    assertEquals("厂家加密原图已转存，尚未接入该厂家的图片解密服务", encryptedEntry.get("message"));
    var denied = new VideoCloudEvidenceService(oss, "camera.example");
    Map<String, Object> invalid = new LinkedHashMap<>(Map.of("pictureUrl", "http://localhost/private"));
    denied.archive("tenant-a", "p", "cam", invalid);
    assertEquals("failed", invalid.get("imageStatus"));
    assertFalse(invalid.containsKey("snapshotUrl"));
  }

  @Test public void deviceBoundObjectPathsFitTheExistingOssColumnIncludingItsDirectory() {
    for (String serial : List.of("42EB6BBPSF7C77D", "A".repeat(34), "B".repeat(35))) {
      ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
          new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> {
            String path = "snapshot/" + args[1];
            assertTrue(path.length() <= 64);
            if (serial.length() <= 34) assertTrue(path.contains(serial));
            return new SysOss().setOssId(1L).setFileName(path).setTenantId("t").setService("minio");
          });
      var service = new VideoCloudEvidenceService(oss, "camera.example") {
        @Override protected byte[] download(String url) { return new byte[]{'D', 'H', 'A', 'V'}; }
      };
      var data = new LinkedHashMap<String, Object>(Map.of("pictureUrl", "https://camera.example/signed"));
      service.archive("t", "p", serial, data);
      assertEquals("encrypted", data.get("imageStatus"));
    }
  }

  @Test public void retriesSavedCiphertextAfterRestartWithoutRequestingExpiredVendorUrl() {
    List<String> uploads = new ArrayList<>();
    Map<Long, SysOss> stored = new LinkedHashMap<>();
    stored.put(41L, new SysOss().setOssId(41L).setFileName("cloud-alarm-old.bin").setTenantId("t").setService("minio"));
    ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> {
          if (method.getName().equals("getById")) return stored.get(Long.valueOf(args[0].toString()));
          uploads.add((String) args[1]);
          var file = new SysOss().setOssId(41L + uploads.size()).setFileName("snapshot/" + args[1]).setTenantId("t")
              .setUrl("https://ours.example/decoded").setService("minio");
          stored.put(file.getOssId(), file);
          return file;
        });
    var service = new VideoCloudEvidenceService(oss, "camera.example") {
      @Override protected byte[] download(String url) { throw new AssertionError("must reuse archived bytes"); }
      @Override protected String signedUrl(String tenant, SysOss file) {
        return "https://ours.example/" + objectKey(file) + "?signed=private";
      }
      @Override protected byte[] readTrusted(String url) { return new byte[]{'D', 'H', 'A', 'V'}; }
    };
    Map<String, Object> data = new LinkedHashMap<>(Map.of("picUrlArr", List.of("https://camera.example/expired"),
        "evidence", List.of(Map.of("status", "encrypted", "ossId", "41", "kind", "ORIGINAL"))));
    assertEquals(1, service.retry("t", "p", "cam", data, (url, bytes) -> {
      assertTrue(URIPath(url).contains("cloud-alarm-cam-"));
      assertArrayEquals(new byte[]{'D', 'H', 'A', 'V'}, bytes);
      return JPEG;
    }, 20));
    assertEquals("ready", data.get("imageStatus")); assertEquals(2, uploads.size());
    assertEquals("https://ours.example/decoded", data.get("snapshotUrl"));
    var retried = (Map<?, ?>) ((List<?>) data.get("evidence")).getFirst();
    assertEquals("42", ((Map<?, ?>) retried.get("original")).get("ossId"));
    assertEquals("43", retried.get("ossId"));
    assertEquals(0, service.retry("t", "p", "cam", data, (url, bytes) -> { throw new AssertionError(); }, 20));
    Map<String, Object> foreign = new LinkedHashMap<>(Map.of("evidence", List.of(Map.of("status", "encrypted", "ossId", "41"))));
    service.retry("other", "p", "cam", foreign, (url, bytes) -> { throw new AssertionError(); }, 20);
    assertEquals("encrypted", foreign.get("imageStatus")); assertEquals(2, uploads.size());
  }

  private static String URIPath(String url) { return java.net.URI.create(url).getPath(); }

  @Test public void compactSourceReferenceIsRestoredWhenRetryCreatesADistinctDecodedPreview() {
    List<String> uploads = new ArrayList<>();
    SysOss original = new SysOss().setOssId(41L).setFileName("snapshot/cloud-alarm-cam-original.bin")
        .setTenantId("t").setService("minio");
    ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> {
          if (method.getName().equals("getById")) return original;
          uploads.add((String) args[1]);
          return new SysOss().setOssId(42L).setFileName("snapshot/" + args[1]).setTenantId("t")
              .setUrl("https://ours.example/decoded").setService("minio");
        });
    var service = new VideoCloudEvidenceService(oss, "camera.example") {
      @Override protected String signedUrl(String tenant, SysOss file) { return "https://ours.example/" + file.getFileName(); }
      @Override protected byte[] readTrusted(String url) { return new byte[]{'D', 'H', 'A', 'V'}; }
    };
    Map<String, Object> source = new LinkedHashMap<>(Map.of("status", "encrypted", "ossId", "41", "kind", "ENCRYPTED_IMAGE",
        "objectKey", original.getFileName(), "provider", "minio", "url", "https://ours.example/original", "size", 4));
    Map<String, Object> data = new LinkedHashMap<>(Map.of("evidence", List.of(source)));
    service.retry("t", "p", "cam", data, (url, bytes) -> JPEG, 1);
    var preview = (Map<?, ?>) ((List<?>) data.get("evidence")).getFirst();
    var savedOriginal = (Map<?, ?>) preview.get("original");
    assertEquals("ready", preview.get("status")); assertEquals(1, uploads.size());
    assertEquals("42", preview.get("ossId")); assertEquals("41", savedOriginal.get("ossId"));
    assertEquals(original.getFileName(), savedOriginal.get("objectKey"));
    assertFalse(savedOriginal.containsKey("status")); assertFalse(savedOriginal.containsKey("kind"));
    assertFalse("retry must not mutate the caller's source reference", source.containsKey("original"));
  }

  @Test public void archiveAndRetryReportMissingNativeDependenciesWithoutBlamingPasswordOrLeakingVendorErrors() {
    SysOss original = new SysOss().setOssId(41L).setFileName("snapshot/cloud-alarm-cam-original.bin")
        .setTenantId("t").setService("minio").setUrl("https://ours.example/original");
    ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> original);
    var service = new VideoCloudEvidenceService(oss, "camera.example") {
      @Override protected byte[] download(String url) { return new byte[]{'D', 'H', 'A', 'V'}; }
      @Override protected String signedUrl(String tenant, SysOss file) { return "https://ours.example/" + file.getFileName(); }
      @Override protected byte[] readTrusted(String url) { return new byte[]{'D', 'H', 'A', 'V'}; }
    };
    Map<String, Object> data = new LinkedHashMap<>(Map.of("picUrlArr", List.of("https://camera.example/signed")));
    service.archive("t", "p", "cam", data, (url, bytes) -> {
      throw new IllegalStateException("SDK网关乐橙图片解密依赖未就绪");
    });
    var archived = (Map<?, ?>) ((List<?>) data.get("evidence")).getFirst();
    assertEquals("encrypted", archived.get("status"));
    assertEquals("SDK网关乐橙图片解密依赖未就绪", archived.get("message"));
    assertFalse(archived.containsKey("original"));
    service.retry("t", "p", "cam", data, (url, bytes) -> {
      throw new IllegalStateException("SDK网关乐橙图片解密依赖未就绪");
    }, 1);
    var retried = (Map<?, ?>) ((List<?>) data.get("evidence")).getFirst();
    assertEquals("SDK网关乐橙图片解密依赖未就绪", retried.get("message"));
    assertFalse(retried.containsKey("original"));
    service.retry("t", "p", "cam", data, (url, bytes) -> {
      throw new IllegalStateException("vendor secret https://camera.example/private?token=private");
    }, 1);
    var rejected = (Map<?, ?>) ((List<?>) data.get("evidence")).getFirst();
    assertEquals("已归档图片重试失败：IllegalStateException", rejected.get("message"));
  }

  @Test public void acceptsObservedDahuaPictureHostWithoutAllowingOtherUfileosHosts() throws Exception {
    String host = "lc-zj-uc-online-paas-private-picture.cn-zj.ufileos.com";
    var lookups = new java.util.concurrent.atomic.AtomicInteger();
    var service = new VideoCloudEvidenceService(null, VideoCloudEvidenceService.DEFAULT_ALLOWED_HOSTS) {
      @Override protected java.net.InetAddress[] resolveSourceAddresses(String value) throws java.net.UnknownHostException {
        assertEquals(host, value); lookups.incrementAndGet();
        return new java.net.InetAddress[]{java.net.InetAddress.getByAddress(new byte[]{8, 8, 8, 8})};
      }
    };
    service.validateSource("https://" + host + "/alarm.jpg?signature=private");
    assertEquals(1, lookups.get());
    for (String url : List.of("https://other.cn-zj.ufileos.com/alarm.jpg",
        "https://sub." + host + "/alarm.jpg", "https://" + host + ".evil.example/alarm.jpg",
        "https://private@" + host + "/alarm.jpg", "file://" + host + "/alarm.jpg",
        "https:///alarm.jpg", "https://local_host/alarm.jpg")) {
      try { service.validateSource(url); fail("unsafe source must be rejected"); }
      catch (IllegalArgumentException expected) { }
    }
    assertEquals("rejected URL must not reach DNS resolution", 1, lookups.get());
  }

  @Test public void allowedHostStillRejectsPrivateOrLocalDnsResults() throws Exception {
    for (String blocked : List.of("0.0.0.0", "127.0.0.1", "10.0.0.1", "192.168.1.1",
        "169.254.1.1", "224.0.0.1", "::1", "fe80::1")) {
      var service = new VideoCloudEvidenceService(null, "camera.example") {
        @Override protected java.net.InetAddress[] resolveSourceAddresses(String host) throws java.net.UnknownHostException {
          return new java.net.InetAddress[]{java.net.InetAddress.getByAddress(new byte[]{8, 8, 8, 8}),
              java.net.InetAddress.getByName(blocked)};
        }
      };
      try { service.validateSource("https://camera.example/alarm.jpg"); fail("local address must be rejected: " + blocked); }
      catch (IllegalArgumentException expected) { assertEquals("云图片来源不能访问本地网络", expected.getMessage()); }
    }
  }

  @Test public void downloadFailuresExposeHttpStatusAndNeverFollowRedirects() throws Exception {
    var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/expired", exchange -> { exchange.sendResponseHeaders(404, -1); exchange.close(); });
    server.createContext("/redirect", exchange -> {
      exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + server.getAddress().getPort() + "/ready");
      exchange.sendResponseHeaders(302, -1); exchange.close();
    });
    var readyReads = new java.util.concurrent.atomic.AtomicInteger();
    server.createContext("/ready", exchange -> {
      readyReads.incrementAndGet(); exchange.sendResponseHeaders(200, JPEG.length);
      exchange.getResponseBody().write(JPEG); exchange.close();
    });
    server.start();
    try {
      var service = new VideoCloudEvidenceService(null, "camera.example");
      for (var expected : Map.of("expired", 404, "redirect", 302).entrySet()) {
        try {
          service.readTrusted("http://127.0.0.1:" + server.getAddress().getPort() + "/" + expected.getKey());
          fail("download should reject unsuccessful responses");
        } catch (VideoCloudEvidenceService.DownloadFailure failure) {
          assertEquals(expected.getValue().intValue(), failure.getHttpStatus());
          assertFalse(failure.getMessage().contains("http"));
        }
      }
      assertEquals(0, readyReads.get());
    } finally { server.stop(0); }
  }

  @Test public void retriesTransientHttpResponsesWithBoundedBackoffButRejectsPermanentFailures() {
    for (int status : new int[]{404, 429, 503, 403, 302}) {
      var reads = new java.util.concurrent.atomic.AtomicInteger();
      List<Integer> pauses = new ArrayList<>();
      var service = new VideoCloudEvidenceService(null, "camera.example") {
        @Override protected void validateSource(String url) { }
        @Override protected void pauseBeforeRetry(int milliseconds) { pauses.add(milliseconds); }
        @Override protected byte[] readTrusted(String url) {
          if (reads.incrementAndGet() <= 3) throw new DownloadFailure(status, "来源未成功返回图片", null);
          return JPEG;
        }
      };
      if (status == 403 || status == 302) {
        try { service.download("https://camera.example/private"); fail(); }
        catch (VideoCloudEvidenceService.DownloadFailure e) { assertEquals(status, e.getHttpStatus()); }
        assertEquals(1, reads.get()); assertTrue(pauses.isEmpty());
      } else {
        assertArrayEquals(JPEG, service.download("https://camera.example/private"));
        assertEquals(4, reads.get()); assertEquals(List.of(300, 1000, 2000), pauses);
      }
    }
  }

  @Test public void failedArchiveCacheExpiresSoAFormerlyUnavailableSourceCanBeSaved() throws Exception {
    var reads = new java.util.concurrent.atomic.AtomicInteger();
    ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> new SysOss().setOssId(1L)
            .setFileName("snapshot/one.jpg").setUrl("https://ours.example/one.jpg").setService("qiniu").setTenantId("t"));
    var service = new VideoCloudEvidenceService(oss, "camera.example") {
      @Override protected byte[] download(String url) {
        if (reads.incrementAndGet() == 1) throw new DownloadFailure(404, "来源尚未返回图片", null);
        return JPEG;
      }
    };
    for (int i = 0; i < 2; i++) {
      Map<String, Object> data = new LinkedHashMap<>(Map.of("pictureUrl", "https://camera.example/signed"));
      service.archive("t", "p", "cam", data); assertEquals("failed", data.get("imageStatus"));
    }
    assertEquals(1, reads.get());
    Thread.sleep(5100);
    Map<String, Object> later = new LinkedHashMap<>(Map.of("pictureUrl", "https://camera.example/signed"));
    service.archive("t", "p", "cam", later);
    assertEquals("ready", later.get("imageStatus")); assertEquals(2, reads.get());
  }
}
