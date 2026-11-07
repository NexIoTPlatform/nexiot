package cn.universal.dm.device.service.ota;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.ota.support.OtaFirmwareStore;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.mapper.SysOssMapper;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.ICloudStorageService;
import cn.universal.persistence.entity.IoTOtaPackage;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OtaFirmwareStoreTest {

  @TempDir Path directory;
  private HttpServer server;
  private SysOssMapper mapper;
  private OtaFirmwareStore store;
  private IoTOtaPackage firmware;
  private SysOss source;
  private byte[] content;
  private final AtomicInteger requests = new AtomicInteger();

  @BeforeEach
  void setUp() throws Exception {
    content = new byte[300];
    for (int i = 0; i < content.length; i++) content[i] = (byte) i;
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/firmware/fw.bin", exchange -> {
      requests.incrementAndGet();
      exchange.sendResponseHeaders(200, content.length);
      try (var output = exchange.getResponseBody()) { output.write(content); }
    });
    server.createContext("/firmware/redirect.bin", exchange -> {
      exchange.getResponseHeaders().add("Location", "/firmware/fw.bin");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });
    server.start();
    String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/firmware";
    String provider = "ota-test-" + UUID.randomUUID();
    ICloudStorageService storage = mock(ICloudStorageService.class);
    when(storage.getEndpointLink()).thenReturn(endpoint);
    when(storage.signedReadUrl(any(), anyInt())).thenThrow(new UnsupportedOperationException());
    OssFactory.register(provider, storage);
    source = SysOss.builder().url(endpoint + "/fw.bin").fileName("fw.bin")
        .service(provider).tenantId("tenant-a").createBy("uploader-a").build();
    firmware = IoTOtaPackage.builder().id(100L).tenantId("tenant-a").creatorId("uploader-a")
        .fileUrl(source.getUrl()).fileSize((long) content.length).checksumAlgorithm("SHA256")
        .checksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)))
        .build();
    mapper = mock(SysOssMapper.class);
    when(mapper.listByUrls(any())).thenReturn(List.of(source));
    store = new OtaFirmwareStore(mapper, directory.toString(), 1024);
  }

  @AfterEach void tearDown() { server.stop(0); }

  @Test void readsExactChunksAndShortFinalChunkWithOneDownload() throws Exception {
    assertArrayEquals(java.util.Arrays.copyOfRange(content, 0, 128), store.readChunk(firmware, 128, 0));
    assertArrayEquals(java.util.Arrays.copyOfRange(content, 128, 256), store.readChunk(firmware, 128, 1));
    assertArrayEquals(java.util.Arrays.copyOfRange(content, 256, 300), store.readChunk(firmware, 128, 2));
    assertEquals(1, requests.get());
    assertThrows(IOException.class, () -> store.readChunk(firmware, 128, 3));
    assertThrows(IOException.class, () -> store.readChunk(firmware, 0, 0));
    assertThrows(IOException.class, () -> store.readChunk(firmware, 128, -1));
  }

  @Test void checksumMismatchNeverLeavesAnAcceptedCacheFile() throws Exception {
    firmware.setChecksum("0".repeat(64));
    assertThrows(IOException.class, () -> store.prepare(firmware));
    try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
  }

  @Test void rejectsUnregisteredAndCrossTenantSourcesBeforeConnecting() {
    when(mapper.listByUrls(any())).thenReturn(List.of());
    assertThrows(IOException.class, () -> store.prepare(firmware));
    when(mapper.listByUrls(any())).thenReturn(List.of(source));
    source.setTenantId("tenant-b");
    source.setCreateBy("uploader-b");
    assertThrows(IOException.class, () -> store.prepare(firmware));
    assertEquals(0, requests.get());
  }

  @Test void rejectsSpoofedOriginsAndRedirects() {
    source.setUrl(source.getUrl().replace("127.0.0.1", "localhost"));
    firmware.setFileUrl(source.getUrl());
    assertThrows(IOException.class, () -> store.prepare(firmware));
    source.setUrl(source.getUrl().replace("localhost", "127.0.0.1").replace("fw.bin", "redirect.bin"));
    firmware.setFileUrl(source.getUrl());
    assertThrows(IOException.class, () -> store.prepare(firmware));
    assertEquals(0, requests.get());
  }

  @Test void restartRevalidatesPersistentCacheBeforeResume() throws Exception {
    Path cached = store.prepare(firmware);
    byte[] corrupt = content.clone();
    corrupt[0] ^= 1;
    Files.write(cached, corrupt);
    OtaFirmwareStore restarted = new OtaFirmwareStore(mapper, directory.toString(), 1024);
    assertThrows(IOException.class, () -> restarted.readChunk(firmware, 128, 0));
    assertEquals(1, requests.get());
  }

  @Test void rejectsOversizedFirmwareAndWeakHashBeforeConnecting() {
    firmware.setFileSize(1025L);
    assertThrows(IOException.class, () -> store.prepare(firmware));
    firmware.setFileSize(300L);
    firmware.setChecksumAlgorithm("MD5");
    assertThrows(IOException.class, () -> store.prepare(firmware));
    assertEquals(0, requests.get());
  }
}
