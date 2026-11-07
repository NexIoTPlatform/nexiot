package cn.universal.dm.video;

import cn.universal.core.product.metadata.ProductMetadataProvider;
import cn.universal.core.video.CameraThingModel;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTProductMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/** Persist bounded schemas for real vendor messages, including previously unknown event codes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoThingModelSynchronizer {
  private static final Set<String> CLOUDS = Set.of("imoulife", "ezviz", "dahua_cloud");
  private static final Set<String> VIDEO_PLATFORMS = Set.of(
      "imoulife", "ezviz", "dahua_cloud", "hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup", "wvp");
  private static final Set<String> COMPACT_PLATFORMS = Set.of(
      "hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup", "wvp");
  private final IoTProductMapper productMapper;
  private final IoTCacheInvalidationPublisher invalidation;
  @Autowired(required = false) private List<ProductMetadataProvider> providers = List.of();
  private final Cache<String, String> metadata = Caffeine.newBuilder().maximumSize(2000)
      .expireAfterWrite(Duration.ofSeconds(30)).build();
  private record Observation(String platform, String metadata, String eventId, String eventName, String schema) {}
  // 完整物模型及观测结构共同决定结果；命中后不再遍历、解析整个产品模型。
  // 以实际字符串占用估算权重，避免不同产品和新字段持续增长占满堆。
  private final Cache<Observation, String> observations = Caffeine.newBuilder()
      .maximumWeight(32L * 1024 * 1024)
      .weigher((Observation key, String value) -> (int) Math.min(Integer.MAX_VALUE,
          2L * (key.metadata().length() + key.schema().length() + value.length()) + 256))
      .expireAfterAccess(Duration.ofMinutes(2)).build();

  /** Called before logging/shadow/rules so the same uplink can use its newly declared fields. */
  public void observe(IoTProduct product, String eventId, String eventName,
      Map<String, Object> data, Map<String, Object> properties) {
    if (product == null || !VIDEO_PLATFORMS.contains(Objects.toString(product.getThirdPlatform(), ""))) return;
    synchronize(product, current -> observations.get(new Observation(product.getThirdPlatform(), current, eventId, eventName,
        CameraThingModel.observationSignature(data, properties)),
        ignored -> compact(product, CameraThingModel.observe(CameraThingModel.enrichEvents(current),
            eventId, eventName, data, properties))));
  }

  @EventListener
  public void invalidate(IoTCacheInvalidationEvent event) {
    if (!"video-event-metadata-update".equals(event.getReason()) && event.getProductKey() != null)
      metadata.invalidate(event.getProductKey());
  }

  public void ensureMetadata(IoTProduct product, String template) {
    if (product == null || product.getId() == null) return;
    synchronize(product, current -> compact(product, CameraThingModel.mergeMetadata(current, template)));
  }

  private static String compact(IoTProduct product, String metadata) {
    if ("imoulife".equals(Objects.toString(product.getThirdPlatform(), "")))
      return CameraThingModel.compactImoulifeMetadata(metadata);
    return COMPACT_PLATFORMS.contains(Objects.toString(product.getThirdPlatform(), ""))
        ? CameraThingModel.compactVideoMetadata(metadata) : metadata;
  }

  public void ensureProviderMetadata(IoTProduct product, String template, String source) {
    if (product == null || product.getId() == null) return;
    synchronize(product, current -> compact(product, CameraThingModel.mergeProviderMetadata(current, template, source)));
  }

  private void synchronize(IoTProduct product, UnaryOperator<String> merge) {
    if (product.getId() == null) return;
    String key = product.getProductKey();
    try {
      String cached = metadata.getIfPresent(key);
      String current = cached == null ? Objects.toString(product.getMetadata(), "{}") : cached;
      if (current.isBlank()) current = "{}";
      String merged = merge.apply(current);
      if (Objects.equals(current, merged)) { product.setMetadata(current); return; }
      // Always reread before writing: cached DTOs and parallel callbacks can carry old metadata.
      for (int attempt = 0; attempt < 3; attempt++) {
        IoTProduct latest = productMapper.selectByPrimaryKey(product.getId());
        if (latest == null || Integer.valueOf(1).equals(latest.getIsDeleted())
            || !Objects.equals(latest.getProductKey(), key)
            || !Objects.equals(latest.getThirdPlatform(), product.getThirdPlatform())
            || !Objects.equals(latest.getTenantId(), product.getTenantId())) return;
        String old = latest.getMetadata();
        merged = merge.apply(old == null || old.isBlank() ? "{}" : old);
        if (Objects.equals(old, merged)) {
          metadata.put(key, merged); product.setMetadata(merged); return;
        }
        Example expected = new Example(IoTProduct.class);
        Example.Criteria criteria = expected.createCriteria().andEqualTo("id", latest.getId());
        if (old == null) criteria.andIsNull("metadata"); else criteria.andEqualTo("metadata", old);
        IoTProduct patch = new IoTProduct();
        patch.setMetadata(merged); patch.setUpdateTime(new Date());
        if (productMapper.updateByExampleSelective(patch, expected) == 1) {
          metadata.put(key, merged); product.setMetadata(merged);
          invalidation.publishProductChange(key, "video-event-metadata-update");
          return;
        }
      }
      log.warn("[视频物模型] 并发更新稍后重试 productKey={}", key);
    } catch (Exception e) {
      // A malformed administrator definition must not drop the actual alarm message.
      log.warn("[视频物模型] 补齐失败 productKey={}, reason={}", key, e.getClass().getSimpleName());
    }
  }

  /** Existing installations get provider events before opening a rule editor or receiving alarms. */
  @EventListener(ApplicationReadyEvent.class)
  public void prepareCloudProducts() {
    for (ProductMetadataProvider provider : providers) {
      if (!CLOUDS.contains(provider.getProtocolCode())) continue;
      try {
        IoTProduct probe = new IoTProduct(); probe.setThirdPlatform(provider.getProtocolCode());
        String template = provider.getMetadata();
        for (IoTProduct product : productMapper.select(probe))
          if (!Integer.valueOf(1).equals(product.getIsDeleted())) ensureMetadata(product, template);
      } catch (Exception e) {
        log.warn("[视频物模型] 厂商模板初始化失败 platform={}, reason={}",
            provider.getProtocolCode(), e.getClass().getSimpleName());
      }
    }
  }
}
