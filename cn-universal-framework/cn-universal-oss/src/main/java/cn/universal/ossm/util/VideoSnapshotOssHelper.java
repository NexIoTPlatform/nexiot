package cn.universal.ossm.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.VideoPlatformInstance;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSnapshotOssHelper {

  private static final int DEFAULT_DOWNLOAD_TIMEOUT_MS = 15_000;
  private static final List<String> URL_KEYS =
      List.of("ossUrl", "snapshotUrl", "imageUrl", "pictureUrl", "picUrl", "url");
  private static final List<String> BASE64_KEYS =
      List.of(
          "snapshotBase64",
          "snapshotData",
          "imageBase64",
          "imageData",
          "pictureBase64",
          "pictureData",
          "picBase64",
          "picData",
          "jpegBase64",
          "jpgBase64",
          "base64");
  private static final List<String> BYTES_KEYS =
      List.of("snapshotBytes", "imageBytes", "pictureBytes", "picBytes", "bytes", "data");
  private static final List<String> RAW_KEYS;

  static {
    List<String> keys = new ArrayList<>();
    keys.addAll(BASE64_KEYS);
    keys.addAll(BYTES_KEYS);
    RAW_KEYS = List.copyOf(keys);
  }

  private final ISysOssService ossService;

  public Map<String, Object> uploadSnapshotIfPresent(
      VideoPlatformInstance instance,
      Map<String, Object> params,
      Map<String, Object> result,
      String platformType) {
    Map<String, Object> normalized =
        result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    // SDK gateway owns the image bytes and storage. Never download its URL for another upload.
    if (Boolean.TRUE.equals(normalized.get("gatewayStored"))) {
      stripRawSnapshotData(normalized);
      return normalized;
    }
    if (!parseBoolean(firstNonBlank(params == null ? null : params.get("uploadToOss")), true)) {
      return normalized;
    }

    SnapshotSource source = resolveSnapshotSource(normalized);
    if (source == null) {
      return normalized;
    }

    try {
      SysOss oss = upload(instance, params, normalized, source, platformType);
      normalized.put("ossUploadSuccess", true);
      normalized.put("ossId", oss.getOssId());
      normalized.put("snapshotOssId", oss.getOssId());
      normalized.put("ossUrl", oss.getUrl());
      normalized.put("snapshotOssUrl", oss.getUrl());
      normalized.put("snapshotUrl", oss.getUrl());
      normalized.put("ossFileName", oss.getFileName());
      normalized.put("ossTenantId", oss.getTenantId());
      normalized.put("snapshotSource", source.kind());
      if (StrUtil.isNotBlank(source.originalUrl())) {
        normalized.putIfAbsent("originSnapshotUrl", source.originalUrl());
      }
      if (!parseBoolean(firstNonBlank(params == null ? null : params.get("keepRawSnapshotData")), false)) {
        stripRawSnapshotData(normalized);
      }
    } catch (Exception e) {
      log.warn(
          "[video-snapshot-oss] upload failed: platform={}, instanceKey={}, error={}",
          platformType,
          instance == null ? null : instance.getInstanceKey(),
          e.getMessage());
      if (parseBoolean(firstNonBlank(params == null ? null : params.get("failOnOssUpload")), false)) {
        if (e instanceof RuntimeException runtimeException) {
          throw runtimeException;
        }
        throw new IllegalStateException(e);
      }
      normalized.put("ossUploadSuccess", false);
      normalized.put("ossUploadError", e.getMessage());
      if (!parseBoolean(firstNonBlank(params == null ? null : params.get("keepRawSnapshotData")), false)) {
        stripRawSnapshotData(normalized);
      }
    }
    return normalized;
  }

  private SysOss upload(
      VideoPlatformInstance instance,
      Map<String, Object> params,
      Map<String, Object> result,
      SnapshotSource source,
      String platformType) {
    String tenantId =
        firstNonBlank(OssTenantContext.getTenantId(), instance == null ? null : instance.getTenantId());
    String fileName = buildFileName(instance, params, result, source, platformType);
    if ("url".equals(source.kind())) {
      return uploadUrl(tenantId, source.originalUrl(), fileName, params);
    }
    return OssTenantContext.run(
        tenantId, () -> ossService.uploadStream(new ByteArrayInputStream(source.bytes()), fileName));
  }

  private SysOss uploadUrl(
      String tenantId, String url, String fileName, Map<String, Object> params) {
    int timeoutMs =
        parseInt(
            firstNonBlank(params == null ? null : params.get("snapshotDownloadTimeoutMs")),
            DEFAULT_DOWNLOAD_TIMEOUT_MS);
    try (HttpResponse response = HttpRequest.get(url).timeout(timeoutMs).execute()) {
      int status = response.getStatus();
      if (status < 200 || status >= 300) {
        throw new IllegalStateException("snapshot url download failed, HTTP " + status);
      }
      InputStream inputStream = response.bodyStream();
      return OssTenantContext.run(tenantId, () -> ossService.uploadStream(inputStream, fileName));
    }
  }

  private SnapshotSource resolveSnapshotSource(Map<String, Object> result) {
    Map<String, Object> data = flattenSnapshotData(result);
    String url = firstUrl(data);
    byte[] bytes = firstBytes(data);
    if (bytes != null && bytes.length > 0) {
      return new SnapshotSource("bytes", bytes, null, firstContentType(data));
    }
    Base64Value base64 = firstBase64(data);
    if (base64 != null && base64.bytes().length > 0) {
      return new SnapshotSource(
          "base64", base64.bytes(), null, firstNonBlank(base64.contentType(), firstContentType(data)));
    }
    if (StrUtil.isNotBlank(url)) {
      return new SnapshotSource("url", null, url, firstContentType(data));
    }
    return null;
  }

  private Map<String, Object> flattenSnapshotData(Map<String, Object> source) {
    Map<String, Object> data = new LinkedHashMap<>();
    if (source == null) {
      return data;
    }
    data.putAll(source);
    for (String key : List.of("snapshot", "image", "picture", "pic", "result", "payload", "data")) {
      Object nested = source.get(key);
      if (nested instanceof Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          if (entry.getKey() != null && entry.getValue() != null) {
            data.putIfAbsent(String.valueOf(entry.getKey()), entry.getValue());
          }
        }
      }
    }
    return data;
  }

  private String firstUrl(Map<String, Object> data) {
    for (String key : URL_KEYS) {
      Object value = data.get(key);
      if (value != null && isHttpUrl(String.valueOf(value))) {
        return String.valueOf(value);
      }
    }
    Object fallback = data.get("data");
    if (fallback instanceof CharSequence text && isHttpUrl(text.toString())) {
      return text.toString();
    }
    return null;
  }

  private byte[] firstBytes(Map<String, Object> data) {
    for (String key : BYTES_KEYS) {
      byte[] bytes = bytesFromObject(data.get(key));
      if (bytes != null && bytes.length > 0) {
        return bytes;
      }
    }
    return null;
  }

  private byte[] bytesFromObject(Object value) {
    if (value instanceof byte[] bytes) {
      return bytes;
    }
    if (value instanceof Byte[] boxed) {
      byte[] bytes = new byte[boxed.length];
      for (int i = 0; i < boxed.length; i++) {
        bytes[i] = boxed[i] == null ? 0 : boxed[i];
      }
      return bytes;
    }
    if (value instanceof Iterable<?> iterable) {
      List<Byte> list = new ArrayList<>();
      for (Object item : iterable) {
        if (!(item instanceof Number number)) {
          return null;
        }
        list.add(number.byteValue());
      }
      byte[] bytes = new byte[list.size()];
      for (int i = 0; i < list.size(); i++) {
        bytes[i] = list.get(i);
      }
      return bytes;
    }
    return null;
  }

  private Base64Value firstBase64(Map<String, Object> data) {
    for (String key : BASE64_KEYS) {
      Base64Value value = decodeBase64(data.get(key));
      if (value != null) {
        return value;
      }
    }
    Base64Value fallback = decodeBase64(data.get("data"));
    return fallback == null ? decodeBase64(data.get("body")) : fallback;
  }

  private Base64Value decodeBase64(Object value) {
    if (!(value instanceof CharSequence text)) {
      return null;
    }
    String raw = text.toString().trim();
    if (StrUtil.isBlank(raw) || isHttpUrl(raw)) {
      return null;
    }
    String contentType = null;
    int commaIndex = raw.indexOf(',');
    if (raw.startsWith("data:") && commaIndex > 0) {
      String meta = raw.substring(5, commaIndex);
      int semicolon = meta.indexOf(';');
      contentType = semicolon > 0 ? meta.substring(0, semicolon) : meta;
      raw = raw.substring(commaIndex + 1);
    }
    String compact = raw.replaceAll("\\s+", "");
    if (compact.length() < 32) {
      return null;
    }
    try {
      return new Base64Value(Base64.getDecoder().decode(compact), contentType);
    } catch (IllegalArgumentException e) {
      try {
        return new Base64Value(Base64.getUrlDecoder().decode(compact), contentType);
      } catch (IllegalArgumentException ignore) {
        // Try MIME decoder below.
      }
      try {
        return new Base64Value(Base64.getMimeDecoder().decode(raw), contentType);
      } catch (IllegalArgumentException ignore) {
        return null;
      }
    }
  }

  private String buildFileName(
      VideoPlatformInstance instance,
      Map<String, Object> params,
      Map<String, Object> result,
      SnapshotSource source,
      String platformType) {
    String requested =
        firstNonBlank(
            params == null ? null : params.get("fileName"),
            params == null ? null : params.get("snapshotFileName"),
            result == null ? null : result.get("fileName"),
            result == null ? null : result.get("snapshotFileName"));
    String suffix =
        normalizeSuffix(
            firstNonBlank(
                params == null ? null : params.get("fileSuffix"),
                result == null ? null : result.get("fileSuffix"),
                extensionFromContentType(source.contentType()),
                extensionFromUrl(source.originalUrl())));
    if (StrUtil.isBlank(suffix)) {
      suffix = ".jpg";
    }
    if (StrUtil.isNotBlank(requested)) {
      String clean = sanitizeFileName(requested);
      return limitFileName(clean.contains(".") ? clean : clean + suffix, suffix);
    }
    String instanceKey = sanitizeFileName(instance == null ? null : instance.getInstanceKey());
    String deviceId =
        sanitizeFileName(
            firstNonBlank(
                params == null ? null : params.get("deviceId"),
                params == null ? null : params.get("extDeviceId")));
    String channelId =
        sanitizeFileName(
            firstNonBlank(
                params == null ? null : params.get("channelId"),
                params == null ? null : params.get("cameraIndexCode"),
                params == null ? null : params.get("channelCode")));
    return limitFileName(
        String.join(
            "-",
            List.of(
                "video-snapshot",
                sanitizeFileName(platformType),
                StrUtil.blankToDefault(instanceKey, "instance"),
                StrUtil.blankToDefault(deviceId, "device"),
                StrUtil.blankToDefault(channelId, "channel"),
                String.valueOf(System.currentTimeMillis()),
                UUID.randomUUID().toString().replace("-", "").substring(0, 8)))
            + suffix,
        suffix);
  }

  private String limitFileName(String fileName, String suffix) {
    String value = sanitizeFileName(fileName);
    String normalizedSuffix = normalizeSuffix(suffix);
    if (StrUtil.isBlank(normalizedSuffix)) {
      normalizedSuffix = "";
    }
    // uploadStream persists the storage object key ("snapshot/" + fileName) in
    // sys_oss.file_name, whose supported database schemas allow 64 characters.
    int maxLength = 64 - "snapshot/".length();
    if (value.length() <= maxLength) {
      return value;
    }
    String name = StrUtil.removeSuffix(value, normalizedSuffix);
    int maxNameLength = Math.max(8, maxLength - normalizedSuffix.length());
    if (name.length() <= maxNameLength) {
      return name + normalizedSuffix;
    }
    int tailLength = Math.min(16, Math.max(6, maxNameLength / 3));
    int headLength = Math.max(1, maxNameLength - tailLength - 1);
    return name.substring(0, headLength)
        + "-"
        + name.substring(name.length() - tailLength)
        + normalizedSuffix;
  }

  private void stripRawSnapshotData(Map<String, Object> result) {
    for (String key : RAW_KEYS) {
      result.remove(key);
    }
    Object data = result.get("data");
    if (data instanceof CharSequence text
        && (isLikelyBase64(text.toString()) || text.toString().startsWith("data:"))) {
      result.remove("data");
    }
  }

  private boolean isLikelyBase64(String value) {
    if (StrUtil.isBlank(value) || isHttpUrl(value)) {
      return false;
    }
    String compact = value.trim().replaceAll("\\s+", "");
    return compact.length() >= 128 && compact.matches("^[A-Za-z0-9+/=_-]+$");
  }

  private String firstContentType(Map<String, Object> data) {
    return firstNonBlank(
        data.get("contentType"),
        data.get("mimeType"),
        data.get("mediaType"),
        data.get("snapshotContentType"),
        data.get("imageContentType"));
  }

  private String extensionFromContentType(String contentType) {
    if (StrUtil.isBlank(contentType)) {
      return null;
    }
    String value = contentType.toLowerCase(Locale.ROOT);
    if (value.contains("png")) {
      return ".png";
    }
    if (value.contains("webp")) {
      return ".webp";
    }
    if (value.contains("bmp")) {
      return ".bmp";
    }
    if (value.contains("gif")) {
      return ".gif";
    }
    return value.contains("jpeg") || value.contains("jpg") ? ".jpg" : null;
  }

  private String extensionFromUrl(String url) {
    if (StrUtil.isBlank(url)) {
      return null;
    }
    String path = url;
    int queryIndex = path.indexOf('?');
    if (queryIndex >= 0) {
      path = path.substring(0, queryIndex);
    }
    int slashIndex = path.lastIndexOf('/');
    int dotIndex = path.lastIndexOf('.');
    if (dotIndex <= slashIndex || dotIndex < 0) {
      return null;
    }
    return normalizeSuffix(path.substring(dotIndex));
  }

  private String normalizeSuffix(String suffix) {
    if (StrUtil.isBlank(suffix)) {
      return null;
    }
    String value = suffix.trim().toLowerCase(Locale.ROOT);
    if (!value.startsWith(".")) {
      value = "." + value;
    }
    return value.matches("\\.[a-z0-9]{1,8}") ? value : null;
  }

  private String sanitizeFileName(Object value) {
    String text = value == null ? "" : String.valueOf(value);
    String clean = text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_").replaceAll("_+", "_");
    clean = clean.replaceAll("^[_.]+|[_.]+$", "");
    // Limit only the completed name so the generated timestamp/UUID suffix survives.
    return clean;
  }

  private boolean isHttpUrl(String value) {
    return StrUtil.startWithIgnoreCase(value, "http://")
        || StrUtil.startWithIgnoreCase(value, "https://");
  }

  private boolean parseBoolean(String value, boolean defaultValue) {
    if (StrUtil.isBlank(value)) {
      return defaultValue;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (List.of("1", "true", "yes", "y", "on").contains(normalized)) {
      return true;
    }
    if (List.of("0", "false", "no", "n", "off").contains(normalized)) {
      return false;
    }
    return defaultValue;
  }

  private int parseInt(String value, int defaultValue) {
    if (StrUtil.isBlank(value)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private String firstNonBlank(Object... values) {
    if (values == null) {
      return null;
    }
    for (Object value : values) {
      if (value != null && StrUtil.isNotBlank(String.valueOf(value))) {
        return String.valueOf(value);
      }
    }
    return null;
  }

  private record SnapshotSource(String kind, byte[] bytes, String originalUrl, String contentType) {}

  private record Base64Value(byte[] bytes, String contentType) {}
}
