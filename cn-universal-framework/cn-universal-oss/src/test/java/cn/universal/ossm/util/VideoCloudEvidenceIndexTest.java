package cn.universal.ossm.util;

import static org.junit.Assert.*;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.mapper.SysOssMapper;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.service.ISysOssService;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class VideoCloudEvidenceIndexTest {
  @Test public void indexSurvivesNewServiceInstanceAndCannotBeReadByAnotherTenantOrDevice() {
    tk.mybatis.mapper.mapperhelper.EntityHelper.initEntityNameMap(SysOss.class, new tk.mybatis.mapper.entity.Config());
    List<SysOss> files = new ArrayList<>();
    Map<String, byte[]> bytes = new LinkedHashMap<>();
    ISysOssService oss = (ISysOssService) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ISysOssService.class}, (proxy, method, args) -> {
          String name = (String) args[1]; long id = files.size() + 1L;
          assertTrue("sys_oss file_name is limited to 64 characters", ("snapshot/" + name).length() <= 64);
          bytes.put(Long.toString(id), ((InputStream) args[0]).readAllBytes());
          SysOss file = new SysOss().setOssId(id).setOriginalName(name).setFileName("snapshot/" + name)
              .setTenantId(OssTenantContext.getTenantId()).setService("minio");
          files.addFirst(file); return file;
        });
    SysOssMapper mapper = (SysOssMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{SysOssMapper.class}, (proxy, method, args) -> files);
    var evidence = new VideoCloudEvidenceService(oss, "camera.example") {
      @Override public byte[] readArchivedFile(String tenant, String id) { return bytes.get(id); }
    };
    var first = new VideoCloudEvidenceIndex(oss, mapper, evidence);
    first.save("t", "p", "cam", "12", Map.of("imageStatus", "ready",
        "evidence", List.of(Map.of("ossId", "9", "status", "ready"))));
    var afterRestart = new VideoCloudEvidenceIndex(oss, mapper, evidence);
    var restored = afterRestart.read("t", "p", "cam", List.of("12"));
    assertEquals("ready", restored.get("12").get("imageStatus"));
    assertTrue(afterRestart.read("other", "p", "cam", List.of("12")).isEmpty());
    assertTrue(afterRestart.read("t", "p", "other-cam", List.of("12")).isEmpty());
    first.save("t", "p", "cam", "12", Map.of("imageStatus", "encrypted", "evidence", List.of()));
    assertEquals("encrypted", afterRestart.read("t", "p", "cam", List.of("12")).get("12").get("imageStatus"));
  }
}
