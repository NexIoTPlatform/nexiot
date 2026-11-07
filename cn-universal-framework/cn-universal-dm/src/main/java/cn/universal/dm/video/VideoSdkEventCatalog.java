package cn.universal.dm.video;

import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.persistence.mapper.admin.SysDictTypeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 视频告警语义目录：订阅选项、上行事件和产品物模型均读取同一份字典快照。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSdkEventCatalog {
  public static final String DICT_PREFIX = "video_sdk_event_";
  private static final Set<String> PLATFORMS = Set.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup", "wvp");
  // ISUP dwAlarmType 与 HCNetSDK COMM_* 子类型不同，只使用 ISUP SDK 已解码的类型。
  private static final Map<Long, String> ISUP_ALARM_TYPES = Map.ofEntries(
      Map.entry(0L, "disk_full"), Map.entry(1L, "disk_error"),
      Map.entry(5L, "video_lost"), Map.entry(6L, "alarm_input"),
      Map.entry(7L, "shelter"), Map.entry(8L, "motion"),
      Map.entry(19L, "illegal_access"), Map.entry(99L, "audio_exception"),
      Map.entry(101L, "line_crossing"), Map.entry(102L, "region_enter"),
      Map.entry(103L, "region_exit"), Map.entry(104L, "intrusion"));
  private static final Map<String, String> ISUP_ISAPI_TYPES = Map.of(
      "motion", "VMD", "shelter", "tamperdetection", "video_lost", "videoloss",
      "line_crossing", "linedetection", "intrusion", "fielddetection",
      "region_enter", "regionEntrance", "region_exit", "regionExiting");

  /** type 为业务类型，id 为稳定物模型标识；SDK 编号索引由字典值单独建立。 */
  public record Event(String type, String id, String name, String vendor, String sdkCode) {}

  /** 同一整数在普通告警和 IVS 中可能重号；海康还需要外层命令与子类型共同定位。 */
  private record NativeCode(String family, long code, Long subtype) {}

  private record Catalog(
      List<Event> events, Map<String, Event> lookup, Map<NativeCode, Event> codes,
      List<Event> modelEvents) {}

  private final SysDictDataMapper dataMapper;
  private final SysDictTypeMapper typeMapper;
  private final ObjectMapper objectMapper;
  // get 的原子加载合并同一厂商的并发回源；短 TTL 使直接 SQL 修改也能自动生效。
  private final Cache<String, Catalog> catalogs =
      Caffeine.newBuilder().maximumSize(6).expireAfterWrite(Duration.ofSeconds(30)).build();

  public List<Event> events(String platformType) {
    return catalog(platformType).events();
  }

  public Event resolve(String platformType, String type) {
    return resolve(platformType, type, Map.of());
  }

  public Event resolve(String platformType, String type, Map<String, Object> attributes) {
    if (PLATFORMS.contains(platformType == null ? "" : platformType) && "gateway".equals(attributes.get("sdkEventFamily"))) {
      for (Event event : gatewayEvents(platformType)) if (event.type().equals(type)) return event;
    }
    Catalog catalog = catalog(platformType);
    if ("wvp".equals(platformType)) return resolveGb(catalog, attributes);
    // 可信实例决定事件空间，缺失/大小写不同的 family 不能落入 HCNetSDK 编号索引。
    if ("hik_isup".equals(platformType)) return resolveIsup(catalog, type, attributes);
    Object codeValue =
        first(attributes, "vendorEventCode", "vendorEventCodeHex", "command", "code");
    if (codeValue == null && type != null && type.matches("(?i)(0x[0-9a-f]+|[0-9]+)"))
      codeValue = type;
    if (codeValue != null) {
      try {
        long code = number(codeValue);
        String family = family(first(attributes, "sdkEventFamily"));
        Object subtypeValue = first(attributes, "vendorAlarmType");
        Long subtype = subtypeValue == null ? null : number(subtypeValue);
        Event event = catalog.codes().get(new NativeCode(family, code, subtype));
        if (event == null && subtype != null)
          event = catalog.codes().get(new NativeCode(family, code, null));
        // 有原始编号时它是权威来源：停用/未知编号不能通过旧 alarmType 绕回已知事件。
        return event == null ? rawEvent(platformType) : event;
      } catch (IllegalArgumentException e) {
        return rawEvent(platformType);
      }
    }
    // 仅兼容不提供 SDK 编号的旧网关；新网关一律按编号匹配。
    return catalog.lookup().getOrDefault(type == null ? "" : type, rawEvent(platformType));
  }

  private Event resolveGb(Catalog catalog, Map<String, Object> attributes) {
    Object methodValue = first(attributes, "alarmMethod");
    Object typeValue = first(attributes, "gbAlarmType");
    String methods = methodValue == null ? "" : String.valueOf(methodValue).trim();
    String subtype = typeValue == null ? "" : String.valueOf(typeValue).trim();
    if (!methods.matches("[1-7]{1,7}|[1-7](?:/[1-7]){1,6}")
        || (!subtype.isEmpty() && !subtype.matches("[1-9][0-9]{0,8}")))
      return rawEvent("wvp");
    Event resolved = null;
    Set<String> methodSet = new java.util.HashSet<>();
    for (String method : methods.replace("/", "").split("")) {
      if (!methodSet.add(method)) return rawEvent("wvp");
      // 组合报警方式可能有不同含义；只有每项均能映射到同一个事件时才归一，避免猜测。
      Event event = catalog.codes().get(new NativeCode("gb28181", Long.parseLong(method),
          subtype.isEmpty() ? null : Long.parseLong(subtype)));
      if (event == null || resolved != null && !resolved.id().equals(event.id()))
        return rawEvent("wvp");
      resolved = event;
    }
    return resolved == null ? rawEvent("wvp") : resolved;
  }

  private Event resolveIsup(Catalog catalog, String type, Map<String, Object> attributes) {
    Object codeValue = first(attributes, "vendorEventCode", "vendorEventCodeHex", "command", "code");
    String semantic = null;
    try {
      if (codeValue != null) {
        long code = number(codeValue);
        if (code == 1 || code == 6) {
          Object subtype = first(attributes, "vendorAlarmType");
          if (subtype != null) {
            semantic = ISUP_ALARM_TYPES.get(number(subtype));
            return catalog.lookup().getOrDefault(
                semantic == null ? "" : semantic, rawEvent("hik_isup"));
          }
        } else if (code != 13) {
          // 未实现的外层类型保留原始事件；13 仅是 ISAPI 包装，不能单独当作移动侦测。
          return rawEvent("hik_isup");
        }
      }
      semantic = isupSemantic(type);
      if (semantic == null) semantic = isupSemantic(first(attributes, "cameraEventType"));
      return catalog.lookup().getOrDefault(
          semantic == null ? "" : semantic, rawEvent("hik_isup"));
    } catch (IllegalArgumentException ignored) {
      return rawEvent("hik_isup");
    }
  }

  private static String isupSemantic(Object value) {
    if (value == null) return null;
    return switch (String.valueOf(value).trim().toLowerCase(java.util.Locale.ROOT)) {
      case "motion", "vmd", "videomotion" -> "motion";
      case "shelter", "tamperdetection" -> "shelter";
      case "video_lost", "videoloss", "videolost" -> "video_lost";
      case "line_crossing", "linedetection", "linecrossing" -> "line_crossing";
      case "intrusion", "fielddetection" -> "intrusion";
      case "region_enter", "regionentrance" -> "region_enter";
      case "region_exit", "regionexiting" -> "region_exit";
      case "disk_full", "disk_error", "alarm_input", "illegal_access", "audio_exception" ->
          String.valueOf(value).trim().toLowerCase(java.util.Locale.ROOT);
      default -> null;
    };
  }

  private static List<String> isupCodes(String type) {
    List<String> codes = new ArrayList<>();
    ISUP_ALARM_TYPES.entrySet().stream()
        .filter(entry -> entry.getValue().equals(type))
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> {
          codes.add("EHOME_ALARM(1): dwAlarmType=" + entry.getKey());
          codes.add("EHOME_ALARM_NOTICE_PICURL(6): dwAlarmType=" + entry.getKey());
        });
    if (ISUP_ISAPI_TYPES.containsKey(type))
      codes.add("EHOME_ISAPI_ALARM(13): " + ISUP_ISAPI_TYPES.get(type));
    return List.copyOf(codes);
  }

  public List<String> subscriptionTopics(String platformType, List<String> selected) {
    List<String> topics =
        new ArrayList<>(selected == null ? List.of() : selected);
    boolean all =
        topics.contains("cameraAlarm") || topics.contains("alarm") || topics.contains("vendor_raw");
    if ("dahua_sdk".equals(platformType)
        && (all
            || catalog(platformType).codes().entrySet().stream()
                .anyMatch(
                    entry ->
                        entry.getKey().family().equals("ivs")
                            && topics.contains(entry.getValue().type())))) {
      // 这是网关监听能力标志，不作为面向用户的业务事件保存。
      topics.add(all ? "__sdk_ivs_optional" : "__sdk_ivs");
    }
    return List.copyOf(topics);
  }

  /** 网关过滤使用同一份已启用编号映射，不能用解码器的旧名称绕过动态字典。 */
  public List<Map<String, Object>> subscriptionCatalog(String platformType) {
    Catalog catalog = catalog(platformType);
    List<Map<String, Object>> result = new ArrayList<>();
    for (Event event : modelEvents(platformType)) {
      List<String> sdkCodes;
      if ("hik_isup".equals(platformType)) {
        List<String> codes = new ArrayList<>();
        ISUP_ALARM_TYPES.entrySet().stream()
            .filter(entry -> entry.getValue().equals(event.type()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
              codes.add(String.format("isup:0x%08X:0x%08X", 1, entry.getKey()));
              codes.add(String.format("isup:0x%08X:0x%08X", 6, entry.getKey()));
            });
        if (ISUP_ISAPI_TYPES.containsKey(event.type())) codes.add("isapi:" + ISUP_ISAPI_TYPES.get(event.type()));
        sdkCodes = List.copyOf(codes);
      } else {
        sdkCodes = catalog.codes().entrySet().stream()
            .filter(entry -> entry.getValue().id().equals(event.id()))
            .map(entry -> formatCode(entry.getKey())).sorted().toList();
      }
      List<String> aliases = catalog.lookup().entrySet().stream()
          .filter(entry -> entry.getValue().id().equals(event.id()) && !entry.getKey().equals(event.type()))
          .map(Map.Entry::getKey).sorted().toList();
      result.add(Map.of("type", event.type(), "eventId", event.id(), "aliases", aliases, "sdkCodes", sdkCodes));
    }
    return List.copyOf(result);
  }

  private static Object first(Map<String, Object> values, String... keys) {
    for (String key : keys) {
      Object value = values.get(key);
      if (value != null && !String.valueOf(value).isBlank()) return value;
    }
    return null;
  }

  private static String family(Object value) {
    if (value == null) return "alarm";
    return switch (String.valueOf(value).toLowerCase(java.util.Locale.ROOT)) {
      case "ivs" -> "ivs";
      case "alarm", "net_alarm", "legacy_alarm" -> "alarm";
      default -> throw new IllegalArgumentException("未知 SDK 事件空间");
    };
  }

  private static long number(Object value) {
    String text = String.valueOf(value).trim();
    long parsed;
    if (text.matches("(?i)0x[0-9a-f]{1,8}")) parsed = Long.parseLong(text.substring(2), 16);
    else if (text.matches("-?[0-9]{1,10}")) parsed = Long.parseLong(text);
    else throw new IllegalArgumentException("SDK 编号必须为十六进制或十进制整数");
    if (parsed < Integer.MIN_VALUE || parsed > 0xffffffffL)
      throw new IllegalArgumentException("SDK 编号超出 DWORD 范围");
    return parsed < 0 ? parsed & 0xffffffffL : parsed;
  }

  private static String formatCode(NativeCode key) {
    if ("gb28181".equals(key.family()))
      return key.code() + (key.subtype() == null ? "" : ":" + key.subtype());
    return ("ivs".equals(key.family()) ? "ivs:" : "")
        + String.format("0x%08X", key.code())
        + (key.subtype() == null ? "" : String.format(":0x%02X", key.subtype()));
  }

  private static NativeCode nativeCode(String key) {
    String[] parts = key.split(":", -1);
    int offset = parts[0].equalsIgnoreCase("ivs") || parts[0].equalsIgnoreCase("alarm") ? 1 : 0;
    if (parts.length - offset < 1 || parts.length - offset > 2)
      throw new IllegalArgumentException("无效 SDK 编号键");
    return new NativeCode(
        offset == 0 ? "alarm" : family(parts[0]),
        number(parts[offset]),
        parts.length - offset == 2 ? number(parts[offset + 1]) : null);
  }

  private static NativeCode gbCode(String key) {
    if (key == null || !key.matches("[1-7](?::[1-9][0-9]{0,8})?"))
      throw new IllegalArgumentException("国标事件键必须为 AlarmMethod[:AlarmType] 十进制组合");
    String[] parts = key.split(":");
    if (parts.length == 2 && !Set.of("2", "5", "6").contains(parts[0]))
      throw new IllegalArgumentException("该报警方式没有国标 AlarmType 定义");
    return new NativeCode("gb28181", Long.parseLong(parts[0]),
        parts.length == 2 ? Long.parseLong(parts[1]) : null);
  }

  public List<Map<String, Object>> options(String platformType) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Event event : modelEvents(platformType)) {
      result.add(
          Map.of(
              "value",
              event.type(),
              "eventId",
              event.id(),
              "label",
              event.name(),
              "sdkCode",
              event.sdkCode(),
              "sdkCodes",
              "hik_isup".equals(platformType) ? isupCodes(event.type()) : catalog(platformType).codes().entrySet().stream()
                  .filter(entry -> entry.getValue().id().equals(event.id()))
                  .map(entry -> formatCode(entry.getKey()))
                  .sorted()
                  .toList()));
    }
    return result;
  }

  public List<Event> modelEvents(String platformType) {
    return catalog(platformType).modelEvents();
  }

  private Catalog snapshot(String platformType, List<Event> events,
      Map<String, Event> lookup, Map<NativeCode, Event> codes) {
    List<Event> models = new ArrayList<>(events);
    models.addAll(gatewayEvents(platformType));
    models.add(rawEvent(platformType));
    return new Catalog(events, lookup, codes, List.copyOf(models));
  }

  private static List<Event> gatewayEvents(String platformType) {
    if ("wvp".equals(platformType) || !PLATFORMS.contains(platformType == null ? "" : platformType)) return List.of();
    return List.of(new Event("device_health", "sdkDeviceHealth", "设备健康巡检", platformType, ""),
        new Event("evidence_ready", "sdkEvidenceReady", "告警取证更新", platformType, ""));
  }

  private Catalog catalog(String platformType) {
    if (!PLATFORMS.contains(platformType == null ? "" : platformType)) {
      return snapshot(platformType, List.of(), Map.of(), Map.of());
    }
    return catalogs.get(platformType, this::load);
  }

  private Catalog load(String platformType) {
    // 两种海康接入复用业务事件 ID / 物模型，原始 ISUP 编号仍留在事件载荷中。
    String dictType = DICT_PREFIX + ("hik_isup".equals(platformType) ? "hik_sdk" : platformType);
    SysDictType probe = new SysDictType();
    probe.setDictType(dictType);
    SysDictType type = typeMapper.selectOne(probe);
    if (type == null || !"0".equals(type.getStatus())) {
      return snapshot(platformType, List.of(), Map.of(), Map.of());
    }
    List<SysDictData> rows = new ArrayList<>(dataMapper.selectDictDataByType(dictType));
    // 显式排序保证同一份配置在多节点上生成相同目录，重复配置不会随机覆盖。
    rows.sort(
        Comparator.comparing(SysDictData::getDictSort, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(SysDictData::getDictCode, Comparator.nullsLast(Long::compareTo)));
    Map<String, Event> lookup = new LinkedHashMap<>();
    Map<String, Event> byId = new LinkedHashMap<>();
    Map<NativeCode, Event> codes = new LinkedHashMap<>();
    for (SysDictData row : rows) {
      if (!"0".equals(row.getStatus())) continue;
      try {
        JsonNode config = objectMapper.readTree(row.getRemark());
        // 新字典 dict_value 是 SDK 编号，remark.type 保持已有物模型及订阅业务类型稳定。
        boolean numbered = config.has("type");
        NativeCode nativeCode = "wvp".equals(platformType) ? gbCode(row.getDictValue())
            : numbered && !"hik_isup".equals(platformType) ? nativeCode(row.getDictValue()) : null;
        String eventType = numbered ? config.path("type").asText() : row.getDictValue();
        if ("hik_isup".equals(platformType) && !ISUP_ALARM_TYPES.containsValue(eventType)) continue;
        String id = config.path("eventId").asText();
        if (eventType == null
            || !eventType.matches("[A-Za-z0-9_.:-]{1,100}")
            || "vendor_raw".equals(eventType)
            || !id.matches("[A-Za-z][A-Za-z0-9_]{0,99}")
            || Set.of("hikSdkEvent", "dahuaSdkEvent", "tiandySdkEvent", "univiewSdkEvent").contains(id)
            || ("wvp".equals(platformType) && Set.of("cameraAlarm", "wvpGb28181Event", "online", "offline").contains(id))
            || row.getDictLabel() == null
            || row.getDictLabel().isBlank()) {
          throw new IllegalArgumentException("事件类型、eventId 或标签无效，或使用了保留标识");
        }
        Event event =
            new Event(
                eventType, id, row.getDictLabel(), platformType,
                "hik_isup".equals(platformType) ? String.join("; ", isupCodes(eventType)) : config.path("sdkCode").asText(""));
        List<String> keys = new ArrayList<>(List.of(eventType));
        JsonNode aliases = config.path("aliases");
        if (!aliases.isMissingNode()) {
          if (!aliases.isArray()) throw new IllegalArgumentException("aliases 必须是字符串数组");
          for (JsonNode alias : aliases) {
            if (!alias.isTextual()
                || alias.asText().isBlank()
                || alias.asText().length() > 100
                || "vendor_raw".equals(alias.asText()))
              throw new IllegalArgumentException("alias 无效");
            keys.add(alias.asText());
          }
        }
        Event previous = byId.get(id);
        if ((previous != null
                && (!previous.type().equals(event.type()) || !previous.name().equals(event.name())))
            || keys.stream()
                .anyMatch(key -> lookup.containsKey(key) && !lookup.get(key).id().equals(id))
            || (nativeCode != null && codes.containsKey(nativeCode))) {
          throw new IllegalArgumentException("冲突的编号、eventId、类型或别名");
        }
        // 多个 SDK 版本编号允许指向同一个业务事件，物模型中只生成一项。
        byId.putIfAbsent(id, event);
        keys.forEach(key -> lookup.putIfAbsent(key, event));
        if (nativeCode != null) codes.put(nativeCode, event);
      } catch (Exception e) {
        // 一条坏配置只隔离自身；不要记录 remark 原文，避免误填的敏感信息进入日志。
        log.warn(
            "[视频SDK字典] 跳过无效项 dictType={}, dictCode={}, reason={}",
            dictType,
            row.getDictCode(),
            e.getClass().getSimpleName());
      }
    }
    return snapshot(platformType, List.copyOf(byId.values()), Map.copyOf(lookup), Map.copyOf(codes));
  }

  private Event rawEvent(String platformType) {
    if ("wvp".equals(platformType)) {
      return new Event("vendor_raw", "wvpGb28181Event", "国标未解析事件", "GB28181", "原始国标报警方式及类型");
    }
    // 未配置/停用的类型仍保留厂商原始事件，避免新增 SDK 事件被静默丢弃。
    String id = switch (platformType == null ? "" : platformType) {
      case "dahua_sdk" -> "dahuaSdkEvent";
      case "tiandy_sdk" -> "tiandySdkEvent";
      case "uniview_sdk" -> "univiewSdkEvent";
      default -> "hikSdkEvent";
    };
    String name = switch (platformType == null ? "" : platformType) {
      case "dahua_sdk" -> "大华未解析事件";
      case "tiandy_sdk" -> "天地伟业未解析事件";
      case "uniview_sdk" -> "宇视未解析事件";
      default -> "海康未解析事件";
    };
    return new Event("vendor_raw", id, name, platformType, "原始厂商事件码");
  }

  public static boolean affectsCatalog(IoTCacheInvalidationEvent event) {
    return event.getCacheNames().contains("selectDictDataByType")
        || event.getCacheNames().contains("selectDictTypeAll")
        || event.getCacheKeys().entrySet().stream()
            .filter(
                entry ->
                    Set.of("selectDictDataByType", "selectDictTypeByType").contains(entry.getKey()))
            .flatMap(entry -> entry.getValue().stream())
            .anyMatch(key -> String.valueOf(key).startsWith(DICT_PREFIX));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onCacheInvalidation(IoTCacheInvalidationEvent event) {
    // 提交后再清理，避免回调线程在事务提交前把旧字典重新放进缓存；远端集群事件也走这里。
    if (affectsCatalog(event)) catalogs.invalidateAll();
  }
}
