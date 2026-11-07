package cn.universal.admin.video.service;

import cn.universal.core.video.CameraThingModel;
import cn.universal.dm.video.VideoSdkEventCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

/** WVP 国标物模型：每种告警独立声明，字典决定启用的事件标识及名称。 */
public final class WvpThingModelTemplate {
  private final ObjectMapper objectMapper;
  private final VideoSdkEventCatalog catalog;
  private final ObjectNode template;
  private final Map<String, ObjectNode> eventTemplates = new LinkedHashMap<>();
  // key 使用不可变字典快照；字典内容变化自动重新编译，不按实例重复构建相同字段。
  private final Cache<List<VideoSdkEventCatalog.Event>, String> declarations =
      Caffeine.newBuilder().maximumSize(32).expireAfterAccess(Duration.ofMinutes(5)).build();

  public WvpThingModelTemplate(ObjectMapper objectMapper, VideoSdkEventCatalog catalog) {
    this.objectMapper = objectMapper;
    this.catalog = catalog;
    try (var input = new ClassPathResource("video/wvp-thing-model.json").getInputStream()) {
      template = (ObjectNode) objectMapper.readTree(input);
      template.path("events").forEach(event ->
          eventTemplates.put(event.path("id").asText(), (ObjectNode) event));
    } catch (IOException e) {
      throw new IllegalStateException("WVP 国标物模型模板加载失败", e);
    }
  }

  public String merge(String metadata, boolean creating) throws IOException {
    String source = metadata == null || metadata.isBlank() ? "{}" : metadata;
    if (creating) {
      JsonNode parsed = objectMapper.readTree(source);
      if (!(parsed instanceof ObjectNode root)) throw new IllegalArgumentException("物模型必须是 JSON 对象");
      ArrayNode events = root.withArray("events");
      boolean removed = false;
      for (int i = events.size() - 1; i >= 0; i--) {
        if ("cameraAlarm".equals(events.get(i).path("id").asText())) {
          events.remove(i);
          removed = true;
        }
      }
      if (removed) source = root.toString();
    }
    String compiled = declarations.get(catalog.modelEvents("wvp"), this::compile);
    return CameraThingModel.compactVideoMetadata(CameraThingModel.mergeMetadata(source, compiled));
  }

  private String compile(List<VideoSdkEventCatalog.Event> snapshot) {
    ObjectNode declarations = template.deepCopy();
    ArrayNode events = declarations.putArray("events");
    events.add(eventTemplates.get("online").deepCopy());
    events.add(eventTemplates.get("offline").deepCopy());
    for (VideoSdkEventCatalog.Event event : snapshot) {
      // 自定义字典事件复用国标载荷结构，仍然生成独立的 events[] 项。
      ObjectNode model = eventTemplates.getOrDefault(event.id(), eventTemplates.get("wvpGb28181Event")).deepCopy();
      model.put("id", event.id()).put("name", event.name()).put("description", event.sdkCode());
      events.add(model);
    }
    return declarations.toString();
  }
}
