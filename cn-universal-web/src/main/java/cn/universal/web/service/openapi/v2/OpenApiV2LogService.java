package cn.universal.web.service.openapi.v2;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.log.HistoryPageSupport;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.CursorQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.HistoryPage;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogCursorQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogEntry;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogPageQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.PageQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.PropertyEntry;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.TimeRange;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * V2 日志门面。权限校验先于查询和游标解码，所有存储查询仅使用服务端查出的设备标识。
 *
 * <p>内部历史服务负责 time/id keyset 分页与条件指纹；外层 HMAC 再绑定应用、租户和接口类型。
 * 不修改旧日志游标格式，不绕过现有存储路由、限流、超时或留存策略。
 */
@Service
public class OpenApiV2LogService {
  private static final long MAX_TIME = 253402300799L;
  private static final long MAX_WINDOW = 31L * 86400;
  private final OpenApiV2Access access;
  private final IIoTDeviceDataService history;

  public OpenApiV2LogService(OpenApiV2Access access, IIoTDeviceDataService history) {
    this.access = access;
    this.history = history;
  }

  public HistoryPage<LogEntry> logs(String productKey, String deviceId, LogPageQuery request) {
    IoTDeviceDTO device = access.device(productKey, deviceId);
    LogQuery query = pagedQuery(device, request);
    filters(query, request.getMessageType(), request.getEvent());
    return result(history.pageList(query), query, false, null, "logs", OpenApiV2LogService::entry);
  }

  public HistoryPage<LogEntry> logCursor(String productKey, String deviceId, LogCursorQuery request) {
    IoTDeviceDTO device = access.device(productKey, deviceId);
    IoTUserApplication application = access.application();
    LogQuery query = cursorQuery(device, application, request, "logs");
    filters(query, request.getMessageType(), request.getEvent());
    HistoryPageSupport.prepare(query, false);
    return result(history.pageList(query), query, true, application, "logs", OpenApiV2LogService::entry);
  }

  public HistoryPage<PropertyEntry> properties(
      String productKey, String deviceId, String property, PageQuery request) {
    IoTDeviceDTO device = access.device(productKey, deviceId);
    validateProperty(property);
    LogQuery query = pagedQuery(device, request);
    query.setMessageType(MessageType.PROPERTIES.name());
    query.setProperty(property);
    return result(history.queryLogMeta(query), query, false, null, "properties", OpenApiV2LogService::property);
  }

  public HistoryPage<PropertyEntry> propertyCursor(
      String productKey, String deviceId, String property, CursorQuery request) {
    IoTDeviceDTO device = access.device(productKey, deviceId);
    IoTUserApplication application = access.application();
    validateProperty(property);
    LogQuery query = cursorQuery(device, application, request, "properties");
    query.setMessageType(MessageType.PROPERTIES.name());
    query.setProperty(property);
    HistoryPageSupport.prepare(query, true);
    return result(history.queryLogMeta(query), query, true, application, "properties", OpenApiV2LogService::property);
  }

  private LogQuery base(IoTDeviceDTO device, TimeRange range) {
    if (range == null
        || range.getStartTime() == null
        || range.getEndTime() == null
        || range.getStartTime() < 0
        || range.getEndTime() > MAX_TIME
        || range.getEndTime() < range.getStartTime()
        || range.getEndTime() - range.getStartTime() > MAX_WINDOW) {
      throw new IoTException("startTime/endTime 必须为有效 Unix 秒，且时间窗口不得超过31天", 400);
    }
    return LogQuery.builder()
        .productKey(device.getProductKey())
        .deviceId(device.getDeviceId())
        .iotId(device.getIotId())
        .beginCreateTime(range.getStartTime())
        .endCreateTime(range.getEndTime())
        .build();
  }

  private LogQuery pagedQuery(IoTDeviceDTO device, PageQuery request) {
    LogQuery query = base(device, request);
    Integer page = request.getPage();
    Integer size = request.getPageSize();
    validateSize(size);
    if (page == null || page < 1 || ((long) page - 1) * size > 10000) {
      throw new IoTException("page 必须大于0，普通分页最多跳过10000条，请使用游标继续查询", 400);
    }
    query.setPageNum(page);
    query.setPageSize(size);
    return query;
  }

  private LogQuery cursorQuery(
      IoTDeviceDTO device, IoTUserApplication application, CursorQuery request, String kind) {
    LogQuery query = base(device, request);
    validateSize(request.getLimit());
    query.setCursorMode(true);
    query.setPageNum(1);
    query.setPageSize(request.getLimit());
    query.setCursor(unwrap(application, kind, request.getCursor()));
    return query;
  }

  private void filters(LogQuery query, String messageType, String event) {
    if (messageType != null && !messageType.isBlank()) {
      try {
        MessageType type = MessageType.valueOf(messageType.trim().toUpperCase(Locale.ROOT));
        if (type != MessageType.ALL) query.setMessageType(type.name());
      } catch (IllegalArgumentException exception) {
        throw new IoTException("messageType 仅支持 ALL/PROPERTIES/EVENT/FUNCTIONS/REPLY/ACK", 400);
      }
    }
    if (event != null && !event.isBlank()) {
      if (event.length() > 128
          || (query.getMessageType() != null
              && !MessageType.EVENT.name().equals(query.getMessageType()))) {
        throw new IoTException("event 最长128字符，且只可与 EVENT 消息类型一起使用", 400);
      }
      query.setMessageType(MessageType.EVENT.name());
      query.setEvent(event);
    }
  }

  private void validateSize(Integer size) {
    if (size == null || size < 1 || size > 200) {
      throw new IoTException("pageSize/limit 必须在1到200之间", 400);
    }
  }

  private void validateProperty(String property) {
    if (property == null || property.isBlank() || property.length() > 128) {
      throw new IoTException("property 为必填的物模型属性标识，最长128字符", 400);
    }
  }

  private <S, T> HistoryPage<T> result(
      PageBean<S> source,
      LogQuery query,
      boolean cursor,
      IoTUserApplication application,
      String kind,
      Function<S, T> mapping) {
    List<S> rows = source == null || source.getList() == null ? List.of() : source.getList();
    long total = source == null || source.getTotalCount() == null ? 0 : source.getTotalCount();
    boolean more = cursor
        ? source != null && source.isHasMore()
        : (long) query.getPageNum() * query.getPageSize() < total;
    String next = source == null ? null : source.getNextCursor();
    if (cursor && more && (next == null || next.isBlank())) {
      throw new IoTException("历史存储未返回有效续页位置，请联系管理员核对存储能力", 503);
    }
    HistoryPage<T> page = new HistoryPage<>();
    page.setItems(rows.stream().map(mapping).toList());
    page.setTotal(cursor ? null : total);
    page.setPage(cursor ? null : query.getPageNum());
    page.setPageSize(query.getPageSize());
    page.setHasMore(more);
    page.setNextCursor(cursor && more ? wrap(application, kind, next) : null);
    page.setStartTime(query.getBeginCreateTime());
    page.setEndTime(query.getEndCreateTime());
    return page;
  }

  /** 外层只签名内部游标；内部 time/id、设备与条件指纹由既有 HistoryPageSupport 校验。 */
  private String wrap(IoTUserApplication application, String kind, String nativeCursor) {
    String payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(nativeCursor.getBytes(StandardCharsets.UTF_8));
    return "2." + payload + "." + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(signature(application, kind, payload));
  }

  private String unwrap(IoTUserApplication application, String kind, String cursor) {
    if (cursor == null) return null;
    try {
      if (cursor.isBlank() || cursor.length() > 2048) throw new IllegalArgumentException();
      String[] parts = cursor.split("\\.", -1);
      if (parts.length != 3 || !"2".equals(parts[0])) throw new IllegalArgumentException();
      byte[] supplied = Base64.getUrlDecoder().decode(parts[2]);
      if (!MessageDigest.isEqual(signature(application, kind, parts[1]), supplied)) {
        throw new IllegalArgumentException();
      }
      String nativeCursor = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      if (nativeCursor.isBlank() || nativeCursor.length() > 1024) throw new IllegalArgumentException();
      return nativeCursor;
    } catch (IllegalArgumentException exception) {
      throw new IoTException("cursor 无效、应用密钥已轮换或应用/租户上下文不匹配，请从第一页重新查询", 400);
    }
  }

  private byte[] signature(IoTUserApplication application, String kind, String payload) {
    String secret = application.getAppSecret();
    if (secret == null || secret.length() < 16) {
      throw new IoTException("当前应用密钥强度不足以生成安全游标，请联系管理员更新应用密钥", 503);
    }
    // 长度前缀避免拼接歧义；内容不进入日志、响应或 OpenAPI 示例。
    String context = component("openapi-v2-history")
        + component(application.getAppId()) + component(application.getAppUniqueId())
        + component(access.tenantId(application)) + component(kind) + component(payload);
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(context.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("HMAC-SHA256 unavailable", exception);
    }
  }

  private String component(String value) {
    return value == null ? "-1:" : value.length() + ":" + value;
  }

  private static LogEntry entry(IoTDeviceLogVO source) {
    LogEntry entry = new LogEntry();
    entry.setId(source.getId() == null ? null : source.getId().toString());
    entry.setIotId(source.getIotId());
    entry.setProductKey(source.getProductKey());
    entry.setDeviceId(source.getDeviceId());
    entry.setDeviceName(source.getDeviceName());
    entry.setMessageType(source.getMessageType());
    entry.setCommandId(source.getCommandId());
    entry.setCommandStatus(source.getCommandStatus());
    entry.setEvent(source.getEvent());
    entry.setContent(source.getContent());
    entry.setCreateTime(source.getCreateTime());
    return entry;
  }

  private static PropertyEntry property(IoTDeviceLogMetadataVO source) {
    PropertyEntry entry = new PropertyEntry();
    entry.setId(source.getId() == null ? null : source.getId().toString());
    entry.setIotId(source.getIotId());
    entry.setProductKey(source.getProductKey());
    entry.setDeviceId(source.getDeviceId());
    entry.setProperty(source.getProperty());
    entry.setName(source.getExt1());
    entry.setValue(source.getContent());
    entry.setUnit(source.getExt3());
    entry.setCreateTime(source.getCreateTime());
    return entry;
  }
}
