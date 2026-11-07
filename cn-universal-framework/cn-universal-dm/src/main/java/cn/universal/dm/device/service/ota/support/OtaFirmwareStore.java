package cn.universal.dm.device.service.ota.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.mapper.SysOssMapper;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.ICloudStorageService;
import cn.universal.persistence.entity.IoTOtaPackage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 校验并按需缓存完整固件，小包读取只占用单片大小的内存。 */
@Component
public class OtaFirmwareStore {

  private final SysOssMapper ossMapper;
  private final Path cacheDirectory;
  private final long maxFileBytes;
  private final Object[] locks = new Object[64];
  private final Map<Path, Long> verifiedFiles = new ConcurrentHashMap<>();

  public OtaFirmwareStore(
      SysOssMapper ossMapper,
      @Value("${ota.transfer.cache-dir:data/ota/firmware}") String cacheDirectory,
      @Value("${ota.transfer.max-file-bytes:536870912}") long maxFileBytes) {
    this.ossMapper = ossMapper;
    this.cacheDirectory = Path.of(cacheDirectory).toAbsolutePath().normalize();
    this.maxFileBytes = maxFileBytes;
    for (int i = 0; i < locks.length; i++) {
      locks[i] = new Object();
    }
  }

  public Path prepare(IoTOtaPackage otaPackage) throws IOException {
    validatePackage(otaPackage);
    String checksum = otaPackage.getChecksum().toLowerCase(Locale.ROOT);
    Path target = cacheDirectory.resolve(otaPackage.getId() + "-" + checksum + ".bin");
    synchronized (locks[Math.floorMod(target.hashCode(), locks.length)]) {
      if (Files.isRegularFile(target) && Files.size(target) == otaPackage.getFileSize()) {
        long modified = Files.getLastModifiedTime(target).toMillis();
        if (!Objects.equals(verifiedFiles.get(target), modified)) {
          try (InputStream input = Files.newInputStream(target)) {
            verifyAndCopy(input, OutputStream.nullOutputStream(), otaPackage);
          }
          verifiedFiles.put(target, modified);
        }
        return target;
      }
      Files.createDirectories(cacheDirectory);
      // 仅从平台已登记、属于升级包租户/上传者的 OSS 对象读取，禁止任意 URL 下载。
      String url = resolveReadUrl(otaPackage);
      Path temporary = Files.createTempFile(cacheDirectory, "ota-", ".part");
      try {
        download(url, temporary, otaPackage);
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        verifiedFiles.put(target, Files.getLastModifiedTime(target).toMillis());
      } finally {
        Files.deleteIfExists(temporary);
      }
      return target;
    }
  }

  public byte[] readChunk(IoTOtaPackage otaPackage, int chunkSize, int zeroIndex)
      throws IOException {
    if (chunkSize < 128 || chunkSize > 32768 || zeroIndex < 0) {
      throw new IOException("分包大小或包序号无效");
    }
    long offset = (long) chunkSize * zeroIndex;
    if (otaPackage.getFileSize() == null || offset >= otaPackage.getFileSize()) {
      throw new IOException("包序号超出文件范围");
    }
    Path file = prepare(otaPackage);
    byte[] chunk = new byte[(int) Math.min(chunkSize, otaPackage.getFileSize() - offset)];
    try (RandomAccessFile input = new RandomAccessFile(file.toFile(), "r")) {
      input.seek(offset);
      input.readFully(chunk);
    }
    // 活跃传输保留缓存；重启后第一次使用会重新验证整个文件。
    Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
    verifiedFiles.put(file, Files.getLastModifiedTime(file).toMillis());
    return chunk;
  }

  private void validatePackage(IoTOtaPackage otaPackage) throws IOException {
    if (otaPackage == null || otaPackage.getId() == null || otaPackage.getId() <= 0
        || otaPackage.getFileSize() == null || otaPackage.getFileSize() <= 0
        || otaPackage.getFileSize() > maxFileBytes) {
      throw new IOException("分包固件大小无效或超过服务端限制");
    }
    String algorithm = normalizeAlgorithm(otaPackage.getChecksumAlgorithm());
    int expectedLength = "SHA-256".equals(algorithm) ? 64 : 128;
    if (otaPackage.getChecksum() == null
        || !otaPackage.getChecksum().matches("(?i)[0-9a-f]{" + expectedLength + "}")) {
      throw new IOException("分包固件必须提供正确的 SHA256/SHA512 校验值");
    }
  }

  private String resolveReadUrl(IoTOtaPackage otaPackage) throws IOException {
    List<SysOss> sources = ossMapper.listByUrls(new String[] {otaPackage.getFileUrl()});
    SysOss source = sources == null ? null : sources.stream()
        .filter(item -> Objects.equals(item.getUrl(), otaPackage.getFileUrl()))
        .filter(item -> (StrUtil.isNotBlank(item.getTenantId())
                && Objects.equals(item.getTenantId(), otaPackage.getTenantId()))
            || (StrUtil.isNotBlank(item.getCreateBy())
                && Objects.equals(item.getCreateBy(), otaPackage.getCreatorId())))
        .findFirst().orElse(null);
    if (source == null) {
      throw new IOException("分包传输请使用本平台上传的固件文件");
    }
    try {
      return OssTenantContext.run(source.getTenantId(), () -> {
        ICloudStorageService storage = OssFactory.instance(source.getService());
        validateObjectUrl(source.getUrl(), storage.getEndpointLink());
        try {
          return storage.signedReadUrl(source.getFileName(), 600);
        } catch (UnsupportedOperationException ignored) {
          return source.getUrl();
        }
      });
    } catch (RuntimeException e) {
      throw new IOException("无法读取固件存储，请核对文件与存储配置", e);
    }
  }

  static void validateObjectUrl(String objectUrl, String endpoint) {
    URI object = URI.create(objectUrl).normalize();
    URI base = URI.create(endpoint.endsWith("/") ? endpoint : endpoint + "/").normalize();
    String scheme = object.getScheme();
    if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
        || object.getUserInfo() != null || object.getFragment() != null
        || object.getHost() == null || !object.getHost().equalsIgnoreCase(base.getHost())
        || !object.getScheme().equalsIgnoreCase(base.getScheme())
        || effectivePort(object) != effectivePort(base)
        || !object.getPath().startsWith(base.getPath())
        || object.getRawPath().contains("%")) {
      throw new IllegalArgumentException("固件地址不属于已配置的存储服务");
    }
  }

  private static int effectivePort(URI uri) {
    return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
  }

  private void download(String url, Path temporary, IoTOtaPackage otaPackage) throws IOException {
    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException e) {
      throw new IOException("固件读取地址无效", e);
    }
    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        || uri.getUserInfo() != null) {
      throw new IOException("固件读取地址必须使用 HTTP/HTTPS");
    }
    HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
    connection.setInstanceFollowRedirects(false);
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(30000);
    try {
      if (connection.getResponseCode() != 200) {
        throw new IOException("固件读取失败，HTTP " + connection.getResponseCode());
      }
      long length = connection.getContentLengthLong();
      if (length >= 0 && length != otaPackage.getFileSize()) {
        throw new IOException("固件文件大小与升级包登记信息不一致");
      }
      try (InputStream input = connection.getInputStream(); OutputStream output = Files.newOutputStream(temporary)) {
        verifyAndCopy(input, output, otaPackage);
      }
    } finally {
      connection.disconnect();
    }
  }

  private void verifyAndCopy(InputStream input, OutputStream output, IoTOtaPackage otaPackage)
      throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(normalizeAlgorithm(otaPackage.getChecksumAlgorithm()));
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("系统不支持固件校验算法", e);
    }
    byte[] buffer = new byte[8192];
    long total = 0;
    long deadline = System.nanoTime() + Duration.ofMinutes(5).toNanos();
    int count;
    while ((count = input.read(buffer)) != -1) {
      total += count;
      if (total > otaPackage.getFileSize() || total > maxFileBytes || System.nanoTime() > deadline) {
        throw new IOException("固件大小超出登记值或读取超时");
      }
      digest.update(buffer, 0, count);
      output.write(buffer, 0, count);
    }
    if (total != otaPackage.getFileSize()
        || !HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(otaPackage.getChecksum())) {
      throw new IOException("固件大小或校验值不一致，已停止分包传输");
    }
  }

  private static String normalizeAlgorithm(String algorithm) throws IOException {
    if ("SHA256".equalsIgnoreCase(algorithm) || "SHA-256".equalsIgnoreCase(algorithm)) return "SHA-256";
    if ("SHA512".equalsIgnoreCase(algorithm) || "SHA-512".equalsIgnoreCase(algorithm)) return "SHA-512";
    throw new IOException("分包固件仅支持 SHA256/SHA512 校验");
  }

  @Scheduled(fixedDelayString = "${ota.transfer.cache-cleanup-ms:3600000}")
  public void removeExpiredFiles() {
    if (!Files.isDirectory(cacheDirectory)) return;
    long cutoff = System.currentTimeMillis() - Duration.ofDays(7).toMillis();
    try (var files = Files.list(cacheDirectory)) {
      files.filter(path -> path.toString().endsWith(".bin") || path.toString().endsWith(".part"))
          .forEach(path -> {
            synchronized (locks[Math.floorMod(path.hashCode(), locks.length)]) {
              try {
                if (Files.getLastModifiedTime(path).toMillis() < cutoff) {
                  Files.deleteIfExists(path);
                  verifiedFiles.remove(path);
                }
              } catch (IOException ignored) {
                // 缓存清理失败留待下一轮，不影响升级任务。
              }
            }
          });
    } catch (IOException ignored) {
      // 存储暂时不可用时由实际传输报告错误。
    }
  }
}
