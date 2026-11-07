package cn.universal.ossm.util;

import cn.universal.core.alarm.VideoAlarmPayload;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.service.ISysOssService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Archive signed cloud alarm pictures before the event log is written and the URL expires. */
@Service
public class VideoCloudEvidenceService {
  private static final int MAX_BYTES = 10 * 1024 * 1024;
  private static final Set<String> FILE_FIELDS = Set.of("ossId", "objectKey", "provider", "url", "mimeType", "size", "sha256");
  static final String DEFAULT_ALLOWED_HOSTS = "*.aliyuncs.com,*.myhuaweicloud.com,*.volces.com,*.myqcloud.com,*.ys7.com,*.ezvizlife.com,*.lechange.com,*.imoulife.com,lc-zj-uc-online-paas-private-picture.cn-zj.ufileos.com";
  private final ISysOssService oss;
  private final List<String> allowedHosts;

  /** Preserve a safe response code for diagnosis without retaining or logging source URLs. */
  public static final class DownloadFailure extends IllegalStateException {
    private final int httpStatus;
    public DownloadFailure(int httpStatus, String message, Throwable cause) {
      super(message, cause); this.httpStatus = httpStatus;
    }
    public int getHttpStatus() { return httpStatus; }
  }
  private final Cache<String, Map<String, Object>> archived = Caffeine.newBuilder()
      .maximumSize(5000).expireAfterWrite(Duration.ofHours(24)).build();
  private final Cache<String, Map<String, Object>> failedArchives = Caffeine.newBuilder()
      .maximumSize(5000).expireAfterWrite(Duration.ofSeconds(5)).build();

  private final Cache<String, Map<String, Object>> originalArchives = Caffeine.newBuilder()
      .maximumSize(5000).expireAfterWrite(Duration.ofHours(24)).build();

  /** Password changes clear preview outcomes; the already saved vendor bytes remain reusable. */
  public void retryAfterSecretChange() { archived.invalidateAll(); failedArchives.invalidateAll(); }

  public VideoCloudEvidenceService(ISysOssService oss,
      @Value("${video.cloud.evidence.allowed-hosts:" + DEFAULT_ALLOWED_HOSTS + "}") String hosts) {
    this.oss = oss;
    this.allowedHosts = java.util.Arrays.stream(hosts.split(","))
        .map(String::trim).map(s -> s.toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty()).toList();
  }

  public void archive(String tenantId, String instanceKey, String deviceId, Map<String, Object> data) {
    archive(tenantId, instanceKey, deviceId, data, null);
  }

  public void archive(String tenantId, String instanceKey, String deviceId, Map<String, Object> data,
      BiFunction<String, byte[], byte[]> decoder) {
    if (data == null || data.containsKey("evidence")) return;
    var refs = VideoAlarmPayload.evidence(data);
    if (refs.isEmpty()) return;
    List<Map<String, Object>> result = new ArrayList<>();
    for (var ref : refs) {
      String key = hash(Objects.toString(tenantId, "") + "|" + ref.url);
      Map<String, Object> entry = failedArchives.getIfPresent(key);
      if (entry == null) {
        entry = archived.get(key, ignored -> collect(tenantId, instanceKey, deviceId, key, ref, decoder));
        if ("failed".equals(entry.get("status"))) {
          failedArchives.put(key, entry);
          archived.invalidate(key);
        }
      }
      result.add(new LinkedHashMap<>(entry));
    }
    applyResults(data, result);
  }

  /** Bounded explicit retry; sources come from the authorized log, never from client URLs. */
  public int retry(String tenantId, String instanceKey, String deviceId, Map<String, Object> data,
      BiFunction<String, byte[], byte[]> decoder, int budget) {
    Map<String, Object> vendor = new LinkedHashMap<>(data);
    vendor.remove("evidence");
    // snapshotUrl may have been populated with our archived preview on the first attempt.
    if (data.containsKey("evidence")) vendor.remove("snapshotUrl");
    var refs = VideoAlarmPayload.evidence(vendor);
    List<Map<String, Object>> existing = new ArrayList<>();
    if (data.get("evidence") instanceof List<?> entries)
      for (Object value : entries) if (value instanceof Map<?, ?> map) existing.add(new LinkedHashMap<>(VideoAlarmPayload.safeData(map)));
    int attempted = 0;
    List<Map<String, Object>> result = new ArrayList<>();
    int count = Math.max(existing.size(), refs.size());
    for (int i = 0; i < count; i++) {
      Map<String, Object> previous = i < existing.size() ? existing.get(i) : null;
      if (previous != null && ("ready".equals(previous.get("status")) || attempted >= budget)) {
        result.add(previous); continue;
      }
      if (attempted >= budget) break;
      if (previous != null && (previous.get("original") instanceof Map<?, ?> || previous.containsKey("ossId"))) {
        result.add(retryStored(tenantId, instanceKey, deviceId, previous, decoder));
        attempted++;
      } else if (i < refs.size()) {
        var ref = refs.get(i);
        String key = hash(Objects.toString(tenantId, "") + "|" + ref.url);
        archived.invalidate(key);
        result.add(collect(tenantId, instanceKey, deviceId, key, ref, decoder));
        attempted++;
      } else if (previous != null) result.add(previous);
    }
    if (!result.isEmpty()) applyResults(data, result);
    return attempted;
  }

  private Map<String, Object> retryStored(String tenant, String instance, String device,
      Map<String, Object> previous, BiFunction<String, byte[], byte[]> decoder) {
    Map<String, Object> entry = new LinkedHashMap<>(previous);
    entry.remove("decodeError"); entry.remove("message");
    Map<?, ?> original = previous.get("original") instanceof Map<?, ?> file ? file : previous;
    // Compact encrypted entries keep the source file at the top level. Capture its file
    // reference before a successful decode replaces those fields with a different preview.
    if (!previous.containsKey("original")) {
      Map<String, Object> source = new LinkedHashMap<>();
      FILE_FIELDS.forEach(field -> { if (original.containsKey(field)) source.put(field, original.get(field)); });
      entry.put("original", source);
    }
    try {
      SysOss file = requireFile(tenant, Objects.toString(original.get("ossId"), ""));
      String url = signedUrl(tenant, file);
      byte[] bytes = readTrusted(url);
      String type = mime(bytes);
      if (decoder != null) {
        // LeChange's official SDK requires the device serial before the URL query string.
        // Rebind legacy hashed object names from already authorized local bytes, never from
        // the expired vendor URL. Keep the original object and persist the new reference.
        if (safeSerial(device) && !URI.create(url).getPath().contains(device)) {
          Map<String, Object> rebound = save(tenant, instance, device, "device-bound-" + file.getOssId(), bytes,
              type.startsWith("image/") ? suffix(type) : ".bin");
          rebound.put("mimeType", type); rebound.put("size", bytes.length); rebound.put("sha256", hash(bytes));
          entry.put("original", rebound);
          file = requireFile(tenant, Objects.toString(rebound.get("ossId"), ""));
          url = signedUrl(tenant, file);
        }
        byte[] decoded = decoder.apply(url, bytes);
        if (decoded != null && decoded.length > 0 && decoded.length <= MAX_BYTES && mime(decoded).startsWith("image/")) {
          bytes = decoded; type = mime(bytes); entry.put("decoded", true);
        }
      }
      boolean image = type.startsWith("image/") && (!"ENCRYPTED_IMAGE".equals(entry.get("kind")) || Boolean.TRUE.equals(entry.get("decoded")));
      if (image) entry.putAll(save(tenant, instance, device, "retry-" + file.getOssId(), bytes, suffix(type)));
      entry.put("mimeType", type); entry.put("size", bytes.length); entry.put("sha256", hash(bytes));
      entry.put("status", image ? "ready" : "encrypted");
      if (!image) entry.put("message", pendingMessage(decoder));
    } catch (RuntimeException e) {
      entry.put("status", previous.containsKey("ossId") ? "encrypted" : "failed");
      entry.put("decodeError", e.getClass().getSimpleName());
      entry.put("message", decodeMessage(e, "已归档图片重试失败：" + e.getClass().getSimpleName()));
    }
    return entry;
  }

  private void applyResults(Map<String, Object> data, List<Map<String, Object>> result) {
    result.forEach(VideoCloudEvidenceService::compactOriginal);
    data.put("evidence", result);
    data.put("imageStatus", result.stream().anyMatch(e -> "ready".equals(e.get("status"))) ? "ready"
        : result.stream().anyMatch(e -> "encrypted".equals(e.get("status"))) ? "encrypted" : "failed");
    result.stream().filter(e -> "ready".equals(e.get("status"))).findFirst()
        .ifPresent(e -> data.put("snapshotUrl", e.get("url")));
  }

  public byte[] readArchivedFile(String tenant, String ossId) {
    return readTrusted(signedUrl(tenant, requireFile(tenant, ossId)));
  }

  private SysOss requireFile(String tenant, String ossId) {
    var file = oss.getById(Long.valueOf(ossId));
    if (file == null || !Objects.equals(tenant, file.getTenantId())) throw new IllegalArgumentException("取证原文件不存在");
    return file;
  }

  public static String objectKey(SysOss file) {
    // Older uploadStream rows saved a bare filename although the object lives in snapshot/.
    String key = file.getFileName();
    if (key != null && key.startsWith("cloud-alarm-") && !key.contains("/")) key = "snapshot/" + key;
    return key;
  }

  protected String signedUrl(String tenant, SysOss file) {
    return OssTenantContext.run(tenant, () -> OssFactory.instance(file.getService()).signedReadUrl(objectKey(file), 300));
  }

  private Map<String, Object> collect(String tenantId, String instanceKey, String deviceId, String key,
      cn.universal.core.alarm.AlarmSignal.EvidenceRef ref, BiFunction<String, byte[], byte[]> decoder) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("id", ref.id.isBlank() ? key : ref.id);
    entry.put("kind", ref.kind);
    entry.put("name", ref.name);
    entry.put("status", "failed");
    var stage = new java.util.concurrent.atomic.AtomicReference<>("download");
    try {
      var fresh = new java.util.concurrent.atomic.AtomicReference<byte[]>();
      Map<String, Object> original = originalArchives.get(key, ignored -> {
        byte[] value = download(ref.url);
        fresh.set(value);
        String type = mime(value);
        stage.set("original-upload");
        Map<String, Object> saved = save(tenantId, instanceKey, deviceId, key + "-original", value,
            type.startsWith("image/") ? suffix(type) : ".bin");
        saved.put("mimeType", type); saved.put("size", value.length); saved.put("sha256", hash(value));
        return saved;
      });
      entry.put("original", original);
      byte[] bytes = fresh.get();
      String decodeUrl = ref.url;
      if (bytes == null) {
        stage.set("original-read");
        decodeUrl = storedUrl(tenantId, original);
        bytes = readTrusted(decodeUrl);
      }
      String type = mime(bytes);
      if (decoder != null && (!type.startsWith("image/") || "ENCRYPTED_IMAGE".equals(ref.kind))) {
        try {
          byte[] decoded = decoder.apply(decodeUrl, bytes);
          if (decoded != null && decoded.length > 0 && decoded.length <= MAX_BYTES
              && mime(decoded).startsWith("image/")) {
            bytes = decoded; type = mime(bytes); entry.put("decoded", true);
          }
        } catch (RuntimeException e) {
          entry.put("decodeError", e.getClass().getSimpleName());
          entry.put("message", decodeMessage(e, pendingMessage(decoder)));
        }
      }
      boolean image = type.startsWith("image/") && (!"ENCRYPTED_IMAGE".equals(ref.kind) || Boolean.TRUE.equals(entry.get("decoded")));
      stage.set("preview-upload");
      entry.putAll(Boolean.TRUE.equals(entry.get("decoded")) ? save(tenantId, instanceKey, deviceId, key, bytes, suffix(type)) : original);
      entry.put("mimeType", type); entry.put("size", bytes.length); entry.put("sha256", hash(bytes));
      entry.put("status", image ? "ready" : "encrypted");
      if (!image && !entry.containsKey("message")) entry.put("message", pendingMessage(decoder));
    } catch (RuntimeException e) {
      var cause = e.getCause();
      org.slf4j.LoggerFactory.getLogger(VideoCloudEvidenceService.class).warn(
          "[云告警图片] device={} stage={} error={} cause={} httpStatus={}", deviceId, stage.get(),
          e.getClass().getSimpleName(), cause == null ? "none" : cause.getClass().getSimpleName(), downloadStatus(e));
      entry.put("message", "云图片转存失败：" + e.getClass().getSimpleName());
    }
    return entry;
  }

  private String storedUrl(String tenant, Map<String, Object> file) {
    return signedUrl(tenant, requireFile(tenant, Objects.toString(file.get("ossId"), "")));
  }

  private String pendingMessage(BiFunction<String, byte[], byte[]> decoder) {
    return decoder == null ? "厂家加密原图已转存，尚未接入该厂家的图片解密服务"
        : "厂家封装或加密图片已转存，请配置正确的相机图片解密密码后重试";
  }

  private static String decodeMessage(RuntimeException error, String fallback) {
    // Only expose messages owned by our decoder adapters; vendor exception text can
    // contain signed URLs or credentials and must never become an event data field.
    return switch (Objects.toString(error.getMessage(), "")) {
      case "相机图片解密密码错误" -> "相机图片解密密码错误，厂家原图已转存，请配置正确密码后重试";
      case "SDK网关乐橙图片解密依赖未就绪", "SDK图片解密网关管理凭证无效", "SDK图片解密网关不可达",
          "图片解密超时，请稍后重试", "厂家图片鉴权或下载失败", "TCM相机需配置图片解密密码（设备密码/SC码）" -> error.getMessage();
      default -> fallback;
    };
  }

  private static void compactOriginal(Map<String, Object> entry) {
    if (!(entry.get("original") instanceof Map<?, ?> original)) return;
    String sourceId = Objects.toString(original.get("ossId"), "");
    if (!sourceId.isBlank() && sourceId.equals(Objects.toString(entry.get("ossId"), ""))
        && original.entrySet().stream().allMatch(field -> "ossId".equals(field.getKey())
            || Objects.equals(field.getValue(), entry.get(field.getKey())))) entry.remove("original");
  }

  private static String suffix(String mime) {
    return mime.equals("image/png") ? ".png" : mime.equals("image/webp") ? ".webp" : mime.equals("image/gif") ? ".gif" : ".jpg";
  }

  private Map<String, Object> save(String tenantId, String instanceKey, String deviceId, String key, byte[] bytes, String suffix) {
    String prefix = "cloud-alarm-" + (safeSerial(deviceId) ? deviceId + "-" : "");
    // sys_oss.file_name stores snapshot/<name> in an existing VARCHAR(64).
    // Keep at least 64 hash bits and the exact serial without changing the database schema.
    int digestLength = 64 - "snapshot/".length() - prefix.length() - suffix.length();
    if (digestLength < 16 && safeSerial(deviceId)) {
      prefix = deviceId + "-";
      digestLength = 64 - "snapshot/".length() - prefix.length() - suffix.length();
    }
    String name = prefix + hash(instanceKey + "|" + deviceId + "|" + key)
        .substring(0, Math.min(32, digestLength)) + suffix;
    SysOss file = OssTenantContext.run(tenantId, () -> oss.uploadStream(new ByteArrayInputStream(bytes), name));
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("ossId", Objects.toString(file.getOssId(), ""));
    value.put("objectKey", file.getFileName());
    value.put("provider", file.getService());
    value.put("url", file.getUrl());
    return value;
  }

  private static boolean safeSerial(String value) {
    return value != null && value.matches("[A-Za-z0-9_-]{1,34}");
  }

  public byte[] downloadDecodedImage(String url) { return download(url); }

  private static int downloadStatus(Throwable error) {
    for (int i = 0; error != null && i < 10; i++, error = error.getCause())
      if (error instanceof DownloadFailure failure) return failure.getHttpStatus();
    return 0;
  }

  protected byte[] download(String url) {
    validateSource(url);
    int[] pauses = {300, 1000, 2000};
    for (int attempt = 0; ; attempt++) {
      try { return readTrusted(url); }
      catch (DownloadFailure e) {
        int status = e.getHttpStatus();
        boolean transientResponse = status == 404 || status == 429 || (status >= 500 && status < 600);
        if (!transientResponse || attempt >= pauses.length) throw e;
        pauseBeforeRetry(pauses[attempt]);
      }
    }
  }

  protected void pauseBeforeRetry(int milliseconds) {
    try { Thread.sleep(milliseconds); }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt(); throw new IllegalStateException("图片下载重试被中断", e);
    }
  }

  protected void validateSource(String url) {
    try {
      URI uri = URI.create(url);
      String host = Objects.toString(uri.getHost(), "").toLowerCase(Locale.ROOT);
      if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getUserInfo() != null
          || host.isBlank() || allowedHosts.stream().noneMatch(pattern -> pattern.startsWith("*.")
              ? host.endsWith(pattern.substring(1)) : host.equals(pattern))) {
        throw new IllegalArgumentException("云图片来源主机不在允许列表");
      }
      for (InetAddress address : resolveSourceAddresses(host)) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()
            || address.isLinkLocalAddress() || address.isMulticastAddress())
          throw new IllegalArgumentException("云图片来源不能访问本地网络");
      }
    } catch (java.io.IOException e) {
      throw new IllegalStateException("云图片下载失败", e);
    }
  }

  protected InetAddress[] resolveSourceAddresses(String host) throws java.net.UnknownHostException {
    return InetAddress.getAllByName(host);
  }

  /** This path accepts only URLs generated by our own tenant storage implementation. */
  protected byte[] readTrusted(String url) {
    int status = 0;
    try {
      HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(8000);
      connection.setReadTimeout(15000);
      try {
        status = connection.getResponseCode();
        if (status != 200 || connection.getContentLengthLong() > MAX_BYTES)
          throw new DownloadFailure(status, "图片下载失败或超过10MiB", null);
        try (InputStream input = connection.getInputStream()) {
          byte[] bytes = input.readNBytes(MAX_BYTES + 1);
          if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new DownloadFailure(status, "图片为空或超过10MiB", null);
          return bytes;
        }
      } finally { connection.disconnect(); }
    } catch (java.io.IOException e) { throw new DownloadFailure(status, "图片下载失败", e); }
  }

  static String mime(byte[] bytes) {
    if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff) return "image/jpeg";
    try {
      String type = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
      if (type != null && type.startsWith("image/")) return type;
    } catch (java.io.IOException ignored) { }
    if (bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
        && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
    return "application/octet-stream";
  }

  private static String hash(String value) { return hash(value.getBytes(StandardCharsets.UTF_8)); }
  private static String hash(byte[] value) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
    catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
