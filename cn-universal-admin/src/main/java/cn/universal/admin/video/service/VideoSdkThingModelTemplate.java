package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.core.video.CameraThingModel;
import cn.universal.dm.video.VideoSdkEventCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 固定 JSON 定义告警结构，字典只提供事件标识、名称和厂商说明。 */
@Component
public class VideoSdkThingModelTemplate {
  private static final String EVENT_TEMPLATE =
      """
      {
        "id": "", "name": "", "description": "",
        "valueType": {
          "type": "object",
          "properties": [
            {"id":"instanceKey","name":"平台实例","valueType":{"type":"string"}},
            {"id":"deviceId","name":"设备ID","valueType":{"type":"string"}},
            {"id":"channelId","name":"通道号","valueType":{"type":"string"}},
            {"id":"channelName","name":"通道名称","valueType":{"type":"string"}},
            {"id":"vendorEventCode","name":"厂商事件码","valueType":{"type":"string"}},
            {"id":"vendorAlarmType","name":"厂商报警子类型","valueType":{"type":"string"}},
            {"id":"eventId","name":"事件追踪ID","valueType":{"type":"string"}},
            {"id":"description","name":"告警描述","valueType":{"type":"string"}},
            {"id":"alarmTime","name":"告警时间","valueType":{"type":"long"}},
            {"id":"alarmState","name":"告警状态","valueType":{"type":"enum","elements":[
              {"value":"start","text":"触发"},{"value":"stop","text":"恢复"},
              {"value":"pulse","text":"瞬时事件"},{"value":"unknown","text":"未知"}
            ]}},
            {"id":"diskNo","name":"磁盘编号","valueType":{"type":"int"}},
            {"id":"ruleId","name":"规则编号","valueType":{"type":"int"}},
            {"id":"images","name":"告警图片","valueType":{"type":"array","elementType":{
              "type":"object","properties":[
                {"id":"url","name":"图片地址","valueType":{"type":"string"}},
                {"id":"status","name":"存储状态","valueType":{"type":"string"}},
                {"id":"imageSource","name":"图片来源","valueType":{"type":"string"}},
                {"id":"capturedAt","name":"抓拍时间","valueType":{"type":"string"}},
                {"id":"contentType","name":"媒体类型","valueType":{"type":"string"}},
                {"id":"size","name":"文件大小","valueType":{"type":"long"}},
                {"id":"width","name":"图片宽度","valueType":{"type":"int"}},
                {"id":"height","name":"图片高度","valueType":{"type":"int"}},
                {"id":"sha256","name":"文件摘要","valueType":{"type":"string"}},
                {"id":"ossId","name":"归档文件ID","valueType":{"type":"string"}},
                {"id":"objectKey","name":"归档对象键","valueType":{"type":"string"}}
              ]}}}
          ]
        }
      }
      """;

  private final ObjectMapper objectMapper;
  private final VideoSdkEventCatalog catalog;
  private final ObjectNode eventTemplate;
  private final WvpThingModelTemplate wvpTemplate;

  public VideoSdkThingModelTemplate(ObjectMapper objectMapper, VideoSdkEventCatalog catalog) {
    this.objectMapper = objectMapper;
    this.catalog = catalog;
    this.wvpTemplate = new WvpThingModelTemplate(objectMapper, catalog);
    try {
      eventTemplate = (ObjectNode) objectMapper.readTree(EVENT_TEMPLATE);
    } catch (Exception e) {
      throw new IllegalStateException("SDK 事件模板格式错误", e);
    }
  }

  public String merge(String metadata, String platformType, boolean creating) {
    try {
      if ("wvp".equals(platformType)) return wvpTemplate.merge(metadata, creating);
      JsonNode parsed =
          objectMapper.readTree(metadata == null || metadata.isBlank() ? "{}" : metadata);
      if (!(parsed instanceof ObjectNode root))
        throw new IllegalArgumentException("物模型必须是 JSON 对象");
      ArrayNode events = root.withArray("events");
      boolean changed = false;
      // 新产品按具体事件建模；已有泛化事件留存以兼容历史日志。
      if (creating) {
        for (int i = events.size() - 1; i >= 0; i--) {
          if ("cameraAlarm".equals(events.get(i).path("id").asText())) {
            events.remove(i);
            changed = true;
          }
        }
      }
      Set<String> existingIds = new HashSet<>();
      events.forEach(event -> existingIds.add(event.path("id").asText()));
      for (VideoSdkEventCatalog.Event event : catalog.modelEvents(platformType)) {
        ObjectNode model = eventTemplate.deepCopy();
        model.put("id", event.id()).put("name", event.name()).put("description", event.sdkCode());
        ArrayNode fields = (ArrayNode) model.path("valueType").path("properties");
        for (var field : CameraThingModel.eventFields().jsonIter()) {
          String id = field.getStr("id");
          if (CameraThingModel.COMPACT_VIDEO_FIELDS.contains(id)
              || "data".equals(id) || "outputData".equals(id)) continue;
          boolean existing = false;
          for (JsonNode defined : fields) if (id.equals(defined.path("id").asText())) existing = true;
          if (!existing) fields.add(objectMapper.readTree(field.toString()));
        }
        if (existingIds.add(event.id())) {
          // 每次深拷贝固定模板，防止多个产品共用可变节点；只补缺失事件，保留用户字段。
          events.add(model);
          changed = true;
        } else {
          for (int i = 0; i < events.size(); i++) {
            if (!event.id().equals(events.get(i).path("id").asText())) continue;
            String existing = events.get(i).toString();
            cn.hutool.json.JSONObject upgraded = cn.hutool.json.JSONUtil.parseObj(existing);
            cn.hutool.json.JSONArray additions = cn.hutool.json.JSONUtil.parseObj(model.toString())
                .getJSONObject("valueType").getJSONArray("properties");
            if (CameraThingModel.mergeEventFields(upgraded, additions)) {
              events.set(i, objectMapper.readTree(upgraded.toString())); changed = true;
            }
          }
        }
      }
      // 无变化直接返回原文，避免格式差异引发无意义 UPDATE 和缓存失效。
      return CameraThingModel.compactVideoMetadata(changed ? objectMapper.writeValueAsString(root) : metadata);
    } catch (Exception e) {
      throw new BaseException("SDK告警物模型更新失败：" + e.getMessage());
    }
  }
}
