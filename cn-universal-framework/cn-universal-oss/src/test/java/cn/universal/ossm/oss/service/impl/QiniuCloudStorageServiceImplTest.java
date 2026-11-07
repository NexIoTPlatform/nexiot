package cn.universal.ossm.oss.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.ossm.service.ISysOssConfigService;
import java.lang.reflect.Proxy;
import java.net.URI;
import org.junit.Test;

public class QiniuCloudStorageServiceImplTest {

  @Test
  public void normalizesBareDomainUsingHttpsSetting() throws Exception {
    var storage = storage(new SysOssConfig().setDomain("files.example.com").setIsHttps(true));
    assertEquals("https://files.example.com", storage.getEndpointLink());
    assertTrue(storage.signedReadUrl("ota/fw.bin", 600)
        .startsWith(storage.getEndpointLink() + "/ota/fw.bin?"));
  }

  @Test
  public void normalizesBareDomainUsingHttpSetting() throws Exception {
    var storage = storage(new SysOssConfig().setDomain("files.example.com").setIsHttps(false));
    assertEquals("http://files.example.com", storage.getEndpointLink());
  }

  @Test
  public void publicEndpointKeepsPathAndUsesSameOriginAsSignedReadUrl() throws Exception {
    var storage = storage(new SysOssConfig().setDomain("old.example.com")
        .setPublicEndpoint(" http://cdn.example.com/public/ " ).setIsHttps(true));
    assertEquals("https://cdn.example.com/public", storage.getEndpointLink());
    URI signed = URI.create(storage.signedReadUrl("ota/fw.bin", 600));
    assertEquals("https", signed.getScheme());
    assertEquals("cdn.example.com", signed.getHost());
    assertEquals("/public/ota/fw.bin", signed.getPath());
  }

  private QiniuCloudStorageServiceImpl storage(SysOssConfig config) throws Exception {
    config.setAccessKey("test-access-key").setSecretKey("test-secret-key");
    ISysOssConfigService configs = (ISysOssConfigService) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] {ISysOssConfigService.class},
        (proxy, method, args) -> config);
    var storage = new QiniuCloudStorageServiceImpl();
    var field = QiniuCloudStorageServiceImpl.class.getDeclaredField("ossConfigService");
    field.setAccessible(true);
    field.set(storage, configs);
    return storage;
  }
}
