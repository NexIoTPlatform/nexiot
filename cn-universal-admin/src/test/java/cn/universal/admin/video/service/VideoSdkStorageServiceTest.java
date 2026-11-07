package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.BaseException;
import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.impl.MinioCloudStorageServiceImpl;
import cn.universal.ossm.service.ISysOssConfigService;
import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

class VideoSdkStorageServiceTest {
  final ObjectMapper mapper = new ObjectMapper();
  final ISysOssConfigService oss = mock(ISysOssConfigService.class);
  final VideoPlatformInstanceService instances = mock(VideoPlatformInstanceService.class);
  final VideoSdkStorageService service = new VideoSdkStorageService(oss, instances, mapper);
  final VideoPlatformInstance instance = new VideoPlatformInstance();
  HttpServer gateway;
  MockedStatic<OssFactory> factory;
  Map<String, Object> sent;
  int deletes;
  Map<String, Object> defaults = new LinkedHashMap<>(Map.of("mode", "s3", "provider", "qiniu",
      "s3PublicBaseUrl", "http://startup-only", "ownerTenantId", "", "alarmPreSeconds", 17));
  Map<String, Object> current = new LinkedHashMap<>(Map.of("mode", "local", "provider", "s3",
      "localDirectory", "data/images", "localBaseUrl", "http://gateway/sdk/images", "ownerTenantId", "tenant"));
  SysOssConfig config;

  @BeforeEach void setup() throws Exception {
    gateway = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    gateway.createContext("/sdk/storage/config", exchange -> {
      Map<String, Object> data = current;
      if (exchange.getRequestMethod().equals("DELETE")) deletes++;
      if (exchange.getRequestMethod().equals("PUT")) {
        sent = mapper.readValue(exchange.getRequestBody(), new TypeReference<>() {});
        data = new LinkedHashMap<>(sent);
      }
      data = new LinkedHashMap<>(data);
      data.put("s3AccessKey", ""); data.put("s3SecretKey", "");
      byte[] bytes = mapper.writeValueAsBytes(Map.of("success", true, "data", data));
      exchange.sendResponseHeaders(200, bytes.length);
      try (var out = exchange.getResponseBody()) { out.write(bytes); }
    });
    gateway.createContext("/sdk/storage/defaults", exchange -> {
      byte[] bytes = mapper.writeValueAsBytes(Map.of("success", true, "data", defaults));
      exchange.sendResponseHeaders(200, bytes.length);
      try (var out = exchange.getResponseBody()) { out.write(bytes); }
    });
    gateway.start();
    instance.setPlatformType("hik_sdk"); instance.setTenantId("tenant");
    instance.setEndpoint("http://127.0.0.1:" + gateway.getAddress().getPort());
    when(instances.listAll()).thenReturn(List.of(instance));
    ReflectionTestUtils.setField(service, "managementToken", "test-token");
    config = new SysOssConfig().setProvider("minio").setEndpoint("http://storage:9000")
        .setPublicEndpoint("https://files.example.com/evidence").setBucketName("evidence")
        .setAccessKey("platform-ak").setSecretKey("platform-secret").setRegion("us-east-1").setPrefix("tenant-assets");
    when(oss.getActiveProvider("tenant")).thenReturn("minio");
    when(oss.getRuntimeConfig("minio", "tenant")).thenReturn(config);
    when(oss.getRuntimeConfig("minio")).thenAnswer(invocation -> {
      assertEquals("tenant", OssTenantContext.getTenantId()); return config;
    });
    factory = mockStatic(OssFactory.class);
    factory.when(() -> OssFactory.instance("minio")).thenReturn(new MinioCloudStorageServiceImpl(oss));
  }

  @AfterEach void close() { factory.close(); gateway.stop(0); }

  @Test void isupCanReadButCannotOverwriteSharedGatewayStorage() {
    instance.setPlatformType("hik_isup");
    var registry=mock(cn.universal.dm.video.VideoPlatformAdapterRegistry.class);
    var adapter=mock(cn.universal.dm.video.VideoPlatformAdapter.class);
    var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getObject()).thenReturn(registry);
    when(registry.getAdapter("hik_isup")).thenReturn(adapter);
    when(adapter.executeOperation(eq(instance),eq("instanceStatus"),eq(Map.of()))).thenReturn(Map.of("tenantId","tenant","services",Map.of("storage",Map.of("listening",true,"lastError","","privateStorageSecret","hidden")),"storage","private-path"));
    ReflectionTestUtils.setField(service,"adapters",provider);ReflectionTestUtils.setField(service,"managementToken","");
    var result=service.read(instance);
    assertEquals(true,result.get("readOnly"));assertEquals("ready",result.get("status"));assertEquals("gateway_runtime_status",result.get("configSource"));
    assertFalse(result.toString().contains("hidden"));assertFalse(result.containsKey("storage"));
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("mode", "s3")));
    assertNull(sent);
    when(adapter.executeOperation(eq(instance),eq("instanceStatus"),eq(Map.of()))).thenReturn(Map.of("tenantId","another-tenant","services",Map.of()));
    assertThrows(BaseException.class,()->service.read(instance));
  }

  @Test void cloudUsesOnlyTenantPlatformSettingsAndPublicUrlRules() {
    var result = service.configure(instance, Map.of("mode", "s3", "provider", "qiniu", "s3Endpoint", "http://untrusted",
        "s3SecretKey", "browser-secret", "s3PublicBaseUrl", "http://wrong", "s3Prefix", "wrong"));
    assertEquals("minio", sent.get("provider"));
    assertEquals("http://storage:9000", sent.get("s3Endpoint"));
    assertEquals("https://files.example.com/evidence", sent.get("s3PublicBaseUrl"));
    assertEquals("tenant-assets/video-sdk", sent.get("s3Prefix"));
    assertEquals("platform-secret", sent.get("s3SecretKey"));
    assertFalse(sent.containsKey("platformCloud"));
    assertFalse(result.toString().contains("platform-secret"));
    assertEquals(true, ((Map<?, ?>) result.get("platformCloud")).get("configured"));
    assertNull(OssTenantContext.getTenantId());
  }

  @Test void minioBarePublicEndpointIncludesBucketExactlyOnce() {
    config.setPublicEndpoint("https://files.example.com/");
    service.configure(instance, Map.of("mode", "s3"));
    assertEquals("https://files.example.com/evidence", sent.get("s3PublicBaseUrl"));
  }

  @Test void localWorksWithoutCloudCredentialsAndValidatesItsOwnFields() {
    config.setSecretKey("");
    var result = service.configure(instance, Map.of("mode", "local", "localDirectory", " data/local ",
        "localBaseUrl", " https://gateway.example.com/sdk/images/ "));
    assertEquals("data/local", sent.get("localDirectory"));
    assertEquals("https://gateway.example.com/sdk/images", sent.get("localBaseUrl"));
    assertEquals(false, ((Map<?, ?>) result.get("platformCloud")).get("configured"));
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("mode", "s3")));
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("mode", "local", "localBaseUrl", "/relative")));
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("mode", "local", "localDirectory", " ")));
  }

  @Test void cloudDoesNotRequireLocalFieldsAndRejectsManualConfig() {
    current.put("localBaseUrl", ""); current.put("localDirectory", "");
    service.configure(instance, Map.of("mode", "s3"));
    assertEquals("", sent.get("localBaseUrl"));
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("mode", "s3", "source", "manual")));
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("mode", "both")));
  }

  @Test void crossTenantGatewayOwnerCannotBeOverwritten() {
    current.put("ownerTenantId", "another-tenant");
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("mode", "s3")));
    assertNull(sent);
  }

  @Test void resetReusesPlatformCloudAndKeepsTenantBindingWithoutIntermediateDelete() {
    service.configure(instance, Map.of("resetToStartup", true));
    assertEquals("minio", sent.get("provider"));
    assertEquals("https://files.example.com/evidence", sent.get("s3PublicBaseUrl"));
    assertEquals("platform-secret", sent.get("s3SecretKey"));
    assertEquals("tenant", sent.get("ownerTenantId"));
    assertEquals(17, sent.get("alarmPreSeconds"));
    assertEquals(0, deletes);
    sent = null;
    config.setSecretKey("");
    assertThrows(BaseException.class, () -> service.configure(instance, Map.of("resetToStartup", true)));
    assertNull(sent);
    assertEquals(0, deletes);
  }
}
