package cn.universal.dm.video;

import cn.universal.common.exception.BaseException;
import java.math.BigDecimal;
import java.util.*;

/** Versioned business operations shared by REST, IoT functions and automation. */
public final class VideoOperationRegistry {
  public static final String VERSION = "20261022.1";
  public enum Scope { INSTANCE, DEVICE, CHANNEL, SESSION }
  public enum Category { READ, VIEW, CONTROL, CONFIG, MAINTENANCE, SUBSCRIPTION, INTERNAL }
  public enum Type { TEXT, INTEGER, NUMBER, BOOLEAN, TIME, OBJECT }
  public record Parameter(Type type, boolean required, int limit) {}
  public record Operation(String id, Scope scope, Category category, boolean offlineAllowed,
                          Map<String, Parameter> parameters) {
    public String permission() {
      return switch (category) {
        case READ -> "video:platform:query";
        case VIEW -> "video:device:play";
        case CONTROL -> "video:device:control";
        case CONFIG -> "video:device:config";
        case MAINTENANCE -> "video:device:maintain";
        case SUBSCRIPTION -> "video:platform:subscribe";
        case INTERNAL -> "";
      };
    }
    public boolean allowed(Set<String> grants) {
      if (category == Category.INTERNAL) return false;
      if (grants.contains("*:*:*") || grants.contains(permission())) return true;
      if (category == Category.SUBSCRIPTION) return false;
      // Existing editors retain their established rights during permission migration.
      return grants.contains("video:platform:edit")
          || category == Category.READ && grants.contains("video:platform:view");
    }
    public Map<String, Object> descriptor(Set<String> grants) {
      Map<String, Object> schema = new LinkedHashMap<>();
      parameters.forEach((key, p) -> schema.put(key, Map.of("type", p.type().name().toLowerCase(Locale.ROOT),
          "required", p.required(), "maxLength", p.limit())));
      return Map.of("id", id, "scope", scope.name().toLowerCase(Locale.ROOT),
          "category", category.name().toLowerCase(Locale.ROOT), "permission", permission(),
          "allowed", allowed(grants), "offlineAllowed", offlineAllowed,
          "parameters", schema, "additionalProperties", false);
    }
  }

  private static final Map<String, Operation> OPERATIONS = new LinkedHashMap<>();
  private static final Map<String, String> ALIASES = new HashMap<>();
  static {
    add("instanceStatus", Scope.INSTANCE, Category.READ, true, fields());
    add("activeRegistrationStatus", Scope.INSTANCE, Category.READ, true, fields());
    add("activeRegistrationDevices", Scope.INSTANCE, Category.READ, true, fields());
    alias("instanceStatus", "isupStatus", "status", "serviceInfo");
    add("operationCatalog", Scope.INSTANCE, Category.READ, true, fields());
    add("capabilities", Scope.INSTANCE, Category.READ, true, fields("deviceId", Type.TEXT, "channelId", Type.TEXT, "channelNo", Type.INTEGER));
    alias("capabilities", "capability");
    add("devices", Scope.INSTANCE, Category.READ, true, fields()); alias("devices", "listDevices");
    add("deviceInfo", Scope.DEVICE, Category.READ, true, fields());
    add("deviceHealth", Scope.DEVICE, Category.READ, false, fields("channelId", Type.TEXT, "channelNo", Type.INTEGER));
    add("deviceCapabilityRefresh", Scope.DEVICE, Category.READ, false, fields("channelId", Type.TEXT, "channelNo", Type.INTEGER));
    add("recordingSearch", Scope.CHANNEL, Category.READ, true, fields("startTime", Type.TIME, "endTime", Type.TIME,
        "streamType", Type.TEXT, "recordType", Type.TEXT, "page", Type.INTEGER, "pageSize", Type.INTEGER, "limit", Type.INTEGER, "offset", Type.INTEGER));
    add("evidenceTimeline", Scope.DEVICE, Category.READ, true, fields("channelId", Type.TEXT, "startTime", Type.TIME,
        "endTime", Type.TIME, "page", Type.INTEGER, "pageSize", Type.INTEGER, "limit", Type.INTEGER, "offset", Type.INTEGER,
        "alarmType", Type.TEXT, "eventType", Type.TEXT, "evidenceId", Type.TEXT, "sourceEventId", Type.TEXT,
        "kind", Type.TEXT, "onlyEvidence", Type.BOOLEAN, "cursor", Type.TEXT));
    add("preview", Scope.CHANNEL, Category.VIEW, false, fields("streamType", Type.TEXT));
    add("ptz", Scope.CHANNEL, Category.CONTROL, false, fields("command", Type.TEXT, "speed", Type.INTEGER));
    add("previewHeartbeat", Scope.CHANNEL, Category.VIEW, true, fields("stream", Type.TEXT, "zlmStream", Type.TEXT, "streamKey", Type.TEXT, "ownerNode", Type.TEXT,
        "streamType", Type.TEXT, "app", Type.TEXT, "mediaServerId", Type.TEXT));
    add("stopPreview", Scope.CHANNEL, Category.VIEW, true, fields("stream", Type.TEXT, "zlmStream", Type.TEXT, "streamKey", Type.TEXT, "ownerNode", Type.TEXT,
        "streamType", Type.TEXT, "app", Type.TEXT, "mediaServerId", Type.TEXT));
    alias("stopPreview", "previewStop", "stopStream");
    add("playbackStreamStart", Scope.CHANNEL, Category.VIEW, false, fields("startTime", Type.TIME, "endTime", Type.TIME,
        "streamType", Type.TEXT, "playbackRate", Type.NUMBER, "mediaServerId", Type.TEXT, "zlmMediaServerId", Type.TEXT, "previewMediaServerId", Type.TEXT));
    for (String id : List.of("playbackStreamStatus", "playbackStreamStop", "playbackStreamPause", "playbackStreamResume", "playbackStreamSpeed", "playbackStreamSeek"))
      add(id, Scope.SESSION, Category.VIEW, true, fields("playbackId", Type.TEXT, "paused", Type.BOOLEAN,
          "completed", Type.BOOLEAN, "playbackRate", Type.NUMBER, "position", Type.TIME, "seekTime", Type.TIME, "ownerNode", Type.TEXT, "channelId", Type.TEXT));
    add("snapshot", Scope.CHANNEL, Category.CONTROL, false, fields("requestId", Type.TEXT, "streamType", Type.TEXT, "source", Type.TEXT));
    for (String id : List.of("snapshotStatus", "snapshotCancel"))
      add(id, Scope.CHANNEL, id.endsWith("Status") ? Category.READ : Category.CONTROL, true, fields("requestId", Type.TEXT));
    for (String id : List.of("presetList", "presetSet", "presetGoto", "presetDelete", "ptz3D"))
      add(id, Scope.CHANNEL, id.equals("presetList") ? Category.READ : Category.CONTROL, false,
          fields("presetId", Type.INTEGER, "preset", Type.INTEGER, "presetIndex", Type.INTEGER, "name", Type.TEXT, "x", Type.NUMBER, "y", Type.NUMBER, "width", Type.NUMBER, "height", Type.NUMBER));
    alias("presetGoto", "presetCall"); alias("presetDelete", "presetClear");
    add("alarmPictureConfig", Scope.CHANNEL, Category.READ, false, fields());
    add("alarmPictureConfigUpdate", Scope.CHANNEL, Category.CONFIG, false, fields("useCapture", Type.BOOLEAN,
        "captureChannel", Type.BOOLEAN, "sendTo", Type.INTEGER, "expectedCapture", Type.OBJECT, "requestId", Type.TEXT));
    add("deviceConfigRead", Scope.DEVICE, Category.READ, false, fields("resource", Type.TEXT, "channelId", Type.TEXT, "alarmType", Type.INTEGER, "weekday", Type.INTEGER, "alarmInputNo", Type.INTEGER, "alarmOutputNo", Type.INTEGER, "ptzChannelNo", Type.INTEGER, "compressionType", Type.INTEGER));
    add("deviceConfigApply", Scope.DEVICE, Category.CONFIG, false, fields("resource", Type.TEXT, "channelId", Type.TEXT,
        "changes", Type.OBJECT, "expectedRevision", Type.TEXT, "requestId", Type.TEXT, "alarmType", Type.INTEGER, "weekday", Type.INTEGER, "alarmInputNo", Type.INTEGER, "alarmOutputNo", Type.INTEGER, "ptzChannelNo", Type.INTEGER, "compressionType", Type.INTEGER));
    add("wvpConfigCapabilities", Scope.DEVICE, Category.READ, true,
        fields("channelId", Type.TEXT, "refresh", Type.BOOLEAN));
    add("wvpConfigRead", Scope.DEVICE, Category.READ, false,
        fields("channelId", Type.TEXT, "resource", Type.TEXT));
    add("wvpConfigApply", Scope.DEVICE, Category.CONFIG, false,
        fields("channelId", Type.TEXT, "resource", Type.TEXT, "configuration", Type.OBJECT));
    add("wvpConfigStatus", Scope.DEVICE, Category.READ, true,
        fields("channelId", Type.TEXT, "operationId", Type.TEXT));
    add("alarmOutputControl",Scope.DEVICE,Category.CONTROL,false,fields("alarmOutputNo",Type.INTEGER,"enabled",Type.BOOLEAN,"durationSeconds",Type.INTEGER));
    add("deviceReboot",Scope.DEVICE,Category.MAINTENANCE,false,fields("requestId",Type.TEXT));
    add("deviceAlgorithmRestart",Scope.DEVICE,Category.MAINTENANCE,false,fields("requestId",Type.TEXT));
    add("deviceDefenseControl",Scope.DEVICE,Category.CONTROL,false,fields("requestId",Type.TEXT,"enabled",Type.BOOLEAN,"armDelaySeconds",Type.INTEGER));
    for (String id : List.of("recordingStart", "platformRecordingStart", "playbackStart", "recordingStatus", "recordingCancel"))
      add(id, Scope.CHANNEL, id.equals("recordingStatus") ? Category.READ : Category.CONTROL, true,
          fields("requestId", Type.TEXT, "jobId", Type.TEXT, "startTime", Type.TIME, "endTime", Type.TIME,
              "playbackMode", Type.TEXT, "source", Type.TEXT, "durationSeconds", Type.INTEGER));
    for (String id : List.of("talkStart", "talkStop", "talkStatus"))
      add(id, Scope.CHANNEL, id.equals("talkStatus") ? Category.READ : Category.CONTROL, id.equals("talkStop"),
          fields("requestId", Type.TEXT, "talkId", Type.TEXT, "voiceChannel", Type.INTEGER, "codec", Type.TEXT, "mode", Type.TEXT));
    Map<String,Parameter> broadcast=fields("requestId",Type.TEXT,"voiceChannel",Type.INTEGER,"codec",Type.TEXT);
    broadcast.put("pcmBase64",new Parameter(Type.TEXT,true,320000));
    add("voiceBroadcastStart",Scope.CHANNEL,Category.CONTROL,false,broadcast);
    add("voiceBroadcastStatus",Scope.CHANNEL,Category.READ,true,fields("requestId",Type.TEXT));
    add("voiceBroadcastCancel",Scope.CHANNEL,Category.CONTROL,true,fields("requestId",Type.TEXT));
    add("subscriptionStatus", Scope.INSTANCE, Category.READ, true, fields());
    alias("subscriptionStatus", "alarmStatus", "alarmSubscriptionStatus");
    for (String id : List.of("configure", "stopServices", "startServices", "cmsRegister", "subscribe", "unsubscribe", "saveAlarmSubscription"))
      add(id, Scope.INSTANCE, Category.INTERNAL, true, fields());
  }

  private VideoOperationRegistry() {}
  public static Optional<Operation> find(String id) {
    if (id == null) return Optional.empty();
    String key = id.trim().toLowerCase(Locale.ROOT);
    return Optional.ofNullable(OPERATIONS.get(ALIASES.getOrDefault(key, key)));
  }
  public static Operation requirePublic(String id) {
    Operation op = find(id).orElseThrow(() -> new BaseException("未注册的视频操作：" + id));
    if (op.category() == Category.INTERNAL) throw new BaseException("该操作仅供内部服务使用，请使用专用管理接口");
    return op;
  }
  public static List<Map<String, Object>> descriptors(Set<String> grants) {
    return OPERATIONS.values().stream().filter(op -> op.category() != Category.INTERNAL).map(op -> op.descriptor(grants)).toList();
  }
  public static Map<String, Object> validate(Operation op, Map<String, Object> input) {
    Map<String, Object> result = new LinkedHashMap<>(input == null ? Map.of() : input);
    for (var entry : result.entrySet()) {
      Parameter p = op.parameters().get(entry.getKey());
      if (p == null) throw new BaseException("操作参数不允许：" + entry.getKey());
      // A GB record plan contains schedule and time-segment lists (five levels).
      int objectDepth = "wvpConfigApply".equals(op.id()) && "configuration".equals(entry.getKey()) ? 5 : 4;
      if (entry.getValue() != null) check(entry.getKey(), entry.getValue(), p, objectDepth);
    }
    op.parameters().forEach((key,p) -> {
      Object v = result.get(key);
      if (p.required() && (v == null || v instanceof String s && s.isBlank())) throw new BaseException("缺少操作参数：" + key);
    });
    if (result.containsKey("streamType") && !Set.of("main", "sub").contains(result.get("streamType"))) throw new BaseException("码流类型无效");
    if ("snapshot".equals(op.id()) && result.containsKey("source") && !Set.of("device", "manual_snapshot", "platform_frame").contains(result.get("source")))
      throw new BaseException("抓图来源无效");
    if (Set.of("recordingStatus", "recordingCancel").contains(op.id())) requireAny(result, "requestId", "jobId");
    if (Set.of("presetSet", "presetGoto", "presetDelete").contains(op.id())) requireAny(result, "presetId", "preset", "presetIndex");
    if ("playbackStreamSeek".equals(op.id())) requireAny(result, "position", "seekTime");
    if("alarmOutputControl".equals(op.id())) {
      long seconds=((Number)result.get("durationSeconds")).longValue();
      if(seconds<1||seconds>600)throw new BaseException("告警输出联动时长需为1到600秒");
    }
    if("deviceDefenseControl".equals(op.id())) {
      long seconds=((Number)result.get("armDelaySeconds")).longValue();
      if(seconds<0||seconds>300)throw new BaseException("布防延迟需为0到300秒");
    }
    if(Set.of("deviceConfigRead","deviceConfigApply").contains(op.id()) && "encodingNative".equals(result.get("resource"))) {
      requireAny(result,"compressionType");
      long type=((Number)result.get("compressionType")).longValue();
      if(type<1||type>3)throw new BaseException("原生编码类型需为1主码流、2辅码流或3事件码流");
    }
    if(Set.of("deviceConfigRead","deviceConfigApply").contains(op.id()) && "alarmInput".equals(result.get("resource"))) {
      requireAny(result,"ptzChannelNo");
      if(((Number)result.get("ptzChannelNo")).longValue()<1)throw new BaseException("告警输入联动必须指定有效视频通道");
    }
    return result;
  }
  private static void requireAny(Map<String,Object> input, String... fields) {
    for (String field : fields) if (input.get(field) != null && !input.get(field).toString().isBlank()) return;
    throw new BaseException("缺少操作参数：" + String.join(" 或 ", fields));
  }
  private static void check(String key, Object value, Parameter p, int objectDepth) {
    boolean valid = switch (p.type()) {
      case TEXT -> value instanceof String s && s.length() <= p.limit() && s.indexOf(0) < 0;
      case BOOLEAN -> value instanceof Boolean;
      case TIME -> value instanceof Number n && Double.isFinite(n.doubleValue()) || value instanceof String s && s.length() <= 40;
      case NUMBER -> value instanceof Number n && Double.isFinite(n.doubleValue());
      case INTEGER -> value instanceof Number n && Double.isFinite(n.doubleValue()) && new BigDecimal(n.toString()).stripTrailingZeros().scale() <= 0;
      case OBJECT -> value instanceof Map<?,?> m && m.size() <= 128 && bounded(m, 0, objectDepth);
    };
    if (!valid) throw new BaseException("操作参数类型或大小无效：" + key);
  }
  private static boolean bounded(Object value, int depth, int maxDepth) {
    if (depth > maxDepth) return false;
    if (value instanceof Map<?,?> m) return m.size() <= 128 && m.entrySet().stream().allMatch(e -> e.getKey() instanceof String s && s.length() <= 100 && bounded(e.getValue(), depth+1, maxDepth));
    if (value instanceof List<?> l) return l.size() <= 128 && l.stream().allMatch(v -> bounded(v,depth+1,maxDepth));
    return value == null || value instanceof Boolean || value instanceof Number || value instanceof String s && s.length() <= 4096;
  }
  private static Map<String, Parameter> fields(Object... pairs) {
    Map<String, Parameter> result = new LinkedHashMap<>();
    for (int i=0; i<pairs.length; i+=2) result.put((String)pairs[i], new Parameter((Type)pairs[i+1],false,512));
    return result;
  }
  private static void add(String id, Scope scope, Category category, boolean offline, Map<String, Parameter> params) {
    if (scope != Scope.INSTANCE) params.put("deviceId", new Parameter(Type.TEXT,scope != Scope.SESSION,100));
    if (scope == Scope.CHANNEL) params.put("channelId", new Parameter(Type.TEXT,true,100));
    if (scope == Scope.SESSION) params.put("playbackId", new Parameter(Type.TEXT,true,100));
    for (String field : switch (id) {
      case "playbackStreamStart", "recordingSearch", "recordingStart", "playbackStart" -> List.of("startTime","endTime");
      case "platformRecordingStart" -> List.of("durationSeconds");
      case "snapshotStatus", "snapshotCancel" -> List.of("requestId");
      case "voiceBroadcastStart", "voiceBroadcastStatus", "voiceBroadcastCancel" -> List.of("requestId");
      case "alarmOutputControl" -> List.of("alarmOutputNo","enabled","durationSeconds");
      case "deviceReboot", "deviceAlgorithmRestart" -> List.of("requestId");
      case "deviceDefenseControl" -> List.of("requestId","enabled","armDelaySeconds");
      case "deviceConfigRead", "wvpConfigRead" -> List.of("resource");
      case "wvpConfigApply" -> List.of("resource", "configuration");
      case "wvpConfigStatus" -> List.of("operationId");
      case "deviceConfigApply" -> List.of("resource","changes","expectedRevision","requestId");
      default -> List.<String>of();
    }) { Parameter old=params.get(field); params.put(field,new Parameter(old.type(),true,old.limit())); }
    OPERATIONS.put(id.toLowerCase(Locale.ROOT), new Operation(id,scope,category,offline,Collections.unmodifiableMap(params)));
  }
  private static void alias(String canonical, String... aliases) {
    for (String alias: aliases) ALIASES.put(alias.toLowerCase(Locale.ROOT),canonical.toLowerCase(Locale.ROOT));
  }
}
