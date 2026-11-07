package cn.universal.ossm.util;

import cn.hutool.json.JSONUtil;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.mapper.SysOssMapper;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.service.ISysOssService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/** Durable image retry results keyed by an existing device log; no event replay or schema change. */
@Service
@RequiredArgsConstructor
public class VideoCloudEvidenceIndex {
  private final ISysOssService oss;
  private final SysOssMapper files;
  private final VideoCloudEvidenceService evidence;
  private final Cache<String, Map<String, Object>> contents = Caffeine.newBuilder()
      .maximumSize(5000).expireAfterAccess(Duration.ofHours(1)).build();

  public void save(String tenant, String product, String device, String logId, Map<String, Object> data) {
    Map<String, Object> index = new LinkedHashMap<>();
    index.put("logId", logId);
    index.put("evidence", data.getOrDefault("evidence", List.of()));
    index.put("imageStatus", data.getOrDefault("imageStatus", "none"));
    byte[] bytes = JSONUtil.toJsonStr(index).getBytes(StandardCharsets.UTF_8);
    if (bytes.length > 512 * 1024) throw new IllegalArgumentException("取证索引超过上限");
    SysOss file = OssTenantContext.run(tenant,
        () -> oss.uploadStream(new ByteArrayInputStream(bytes), name(tenant, product, device, logId)));
    contents.put(tenant + ":" + file.getOssId(), index);
  }

  /** Resolve all row indexes with one metadata query; missing indexes never cause network calls. */
  public Map<String, Map<String, Object>> read(String tenant, String product, String device, List<String> logIds) {
    if (logIds.isEmpty()) return Map.of();
    Map<String, String> names = new LinkedHashMap<>();
    for (String id : logIds) names.put(name(tenant, product, device, id), id);
    Example query = new Example(SysOss.class);
    var criteria = query.createCriteria().andIn("originalName", names.keySet());
    if (tenant == null) criteria.andIsNull("tenantId"); else criteria.andEqualTo("tenantId", tenant);
    query.setOrderByClause("create_time desc, oss_id desc");
    List<SysOss> matches = files.selectByExample(query);
    Map<String, Map<String, Object>> result = new LinkedHashMap<>();
    if (matches == null) return result;
    for (SysOss file : matches) {
      String logId = names.get(file.getOriginalName());
      if (logId == null || result.containsKey(logId) || !Objects.equals(tenant, file.getTenantId())) continue;
      try {
        Map<String, Object> index = contents.get(tenant + ":" + file.getOssId(), ignored -> {
          byte[] bytes = evidence.readArchivedFile(tenant, Objects.toString(file.getOssId(), ""));
          if (bytes.length > 512 * 1024) throw new IllegalArgumentException("取证索引超过上限");
          return new LinkedHashMap<>(JSONUtil.parseObj(new String(bytes, StandardCharsets.UTF_8)));
        });
        if (logId.equals(Objects.toString(index.get("logId"), ""))) result.put(logId, index);
      } catch (RuntimeException ignored) { /* Keep the original event visible if storage is unavailable. */ }
    }
    return result;
  }

  private String name(String tenant, String product, String device, String logId) {
    try {
      String key = Objects.toString(tenant, "") + "|" + product + "|" + device + "|" + logId;
      return "cloud-alarm-index-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(key.getBytes(StandardCharsets.UTF_8))).substring(0, 32) + ".json";
    } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
