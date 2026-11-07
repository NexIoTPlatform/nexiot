/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.dm.device.service.log;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.UPRequest;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.dm.device.constant.DeviceManagerConstant;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTDeviceLogMetadata;
import cn.universal.persistence.entity.IoTDeviceLogMetadata.IoTDeviceLogMetadataBuilder;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTProductMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/23
 */
public abstract class AbstractIoTDeviceLogService implements IIoTDeviceLogService {
  private static final int SQL_DEBUG_BATCH_PREVIEW_LIMIT = 20;
  private static final int SQL_DEBUG_VALUE_MAX_LENGTH = 512;
  private static final int LOCAL_CACHE_MAX_SIZE = 8192;
  private static final int METADATA_TEXT_MAX_LENGTH = 255;
  private static final int METADATA_EXT2_MAX_LENGTH = 1024;

  @Resource private IoTProductMapper ioTProductMapper;
  private final ConcurrentMap<String, LogStorePolicyDTO> localLogStorePolicyCache =
      new ConcurrentHashMap<>();
  private final ConcurrentMap<String, DeviceMetadata> localDeviceMetadataCache =
      new ConcurrentHashMap<>();

  DeviceMetadata getDeviceMetadata(String metadata) {
    String key = localCacheKey("metadata", null, metadata);
    return localCached(
        localDeviceMetadataCache,
        key,
        () -> {
          if (StrUtil.isBlank(metadata)) {
            return new DeviceMetadata(new JSONObject());
          }
          return new DeviceMetadata(new JSONObject(metadata));
        });
  }

  protected LogStorePolicyDTO resolveLogStorePolicy(IoTProduct product) {
    if (product == null || StrUtil.isBlank(product.getStorePolicyConfiguration())) {
      return LogStorePolicyDTO.builder().build();
    }
    String key =
        localCacheKey("policy", product.getProductKey(), product.getStorePolicyConfiguration());
    return localCached(localLogStorePolicyCache, key, () -> parseLogStorePolicy(product));
  }

  protected IoTDeviceDTO resolveDevice(
      BaseUPRequest upRequest, IoTDeviceDTO providedDevice, IoTDeviceService deviceService) {
    if (providedDevice != null) {
      return providedDevice;
    }
    if (upRequest != null && upRequest.getIoTDeviceDTO() != null) {
      return upRequest.getIoTDeviceDTO();
    }
    if (upRequest == null || deviceService == null) {
      return null;
    }
    return deviceService.selectDevInstanceBO(upRequest.getProductKey(), upRequest.getDeviceId());
  }

  private LogStorePolicyDTO parseLogStorePolicy(IoTProduct product) {
    try {
      JSONObject storePolicyObj = JSONUtil.parseObj(product.getStorePolicyConfiguration());
      return LogStorePolicyDTO.builder()
          .properties(parseStorePolicies(storePolicyObj, "properties"))
          .event(parseStorePolicies(storePolicyObj, "event"))
          .build();
    } catch (Exception ex) {
      LoggerFactory.getLogger(getClass())
          .warn(
              "解析日志存储策略失败，policy={}, productKey={}, err={}",
              getPolicy(),
              product == null ? null : product.getProductKey(),
              ex.getMessage());
      return LogStorePolicyDTO.builder().build();
    }
  }

  private Map<String, LogStorePolicyDTO.StorePolicy> parseStorePolicies(
      JSONObject storePolicyObj, String key) {
    Map<String, LogStorePolicyDTO.StorePolicy> result = new HashMap<>();
    if (storePolicyObj == null) {
      return result;
    }
    JSONArray array = storePolicyObj.getJSONArray(key);
    if (array == null || array.isEmpty()) {
      return result;
    }
    for (Object item : array) {
      JSONObject policy = item instanceof JSONObject ? (JSONObject) item : JSONUtil.parseObj(item);
      String id = policy.getStr("id");
      if (StrUtil.isBlank(id)) {
        continue;
      }
      result.put(
          id,
          new LogStorePolicyDTO.StorePolicy(
              id, policy.getInt("maxStorage", IoTConstant.maxStorage)));
    }
    return result;
  }

  private String localCacheKey(String prefix, String productKey, String value) {
    String text = StrUtil.nullToEmpty(value);
    return prefix
        + ':'
        + StrUtil.nullToEmpty(productKey)
        + ':'
        + text.length()
        + ':'
        + DigestUtil.md5Hex(text);
  }

  private <T> T localCached(ConcurrentMap<String, T> cache, String key, Supplier<T> loader) {
    if (cache.size() >= LOCAL_CACHE_MAX_SIZE && !cache.containsKey(key)) {
      cache.keySet().stream().findAny().ifPresent(cache::remove);
    }
    return cache.computeIfAbsent(key, ignored -> loader.get());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onLogCacheInvalidation(IoTCacheInvalidationEvent event) {
    if (!shouldEvictLocalLogPolicy(event)) {
      return;
    }
    int evicted = evictLocalLogPolicy(event.getProductKey());
    LoggerFactory.getLogger(getClass())
        .info(
            "日志本地策略缓存已精细失效，policy={}, reason={}, productKey={}, evicted={}",
            getPolicy(),
            event.getReason(),
            event.getProductKey(),
            evicted);
  }

  private boolean shouldEvictLocalLogPolicy(IoTCacheInvalidationEvent event) {
    if (event == null) {
      return false;
    }
    return event.getCacheNames().contains("iot_product_log_store_policy")
        || event.getCacheNames().contains("iot_product_device")
        || event.getCacheNames().contains("getProductConfiguration")
        || event.getCacheNames().contains(IoTConstant.ENABLE_MYSQL_WRITE)
        || event.getCacheKeys().containsKey("iot_product_log_store_policy")
        || event.getCacheKeys().containsKey("iot_product_device")
        || event.getCacheKeys().containsKey("getProductConfiguration")
        || event.getCacheKeys().containsKey(IoTConstant.ENABLE_MYSQL_WRITE);
  }

  private int evictLocalLogPolicy(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      int size = localLogStorePolicyCache.size();
      localLogStorePolicyCache.clear();
      return size;
    }
    String prefix = "policy:" + productKey + ':';
    int before = localLogStorePolicyCache.size();
    localLogStorePolicyCache.keySet().removeIf(key -> key.startsWith(prefix));
    return before - localLogStorePolicyCache.size();
  }

  IoTDeviceLogMetadataBuilder builder(UPRequest up) {
    return builder(up, JSONUtil.toJsonStr(up.getData()));
  }

  IoTDeviceLogMetadataBuilder builder(UPRequest up, String dataJson) {
    final IoTDeviceLogMetadataBuilder ioTDeviceLogMetadataBuilder =
        IoTDeviceLogMetadata.builder()
            .createTime(resolveCreateTime(up.getTime()))
            .deviceId(up.getDeviceId())
            .deviceName(StrUtil.sub(up.getDeviceName(), 0, 30))
            .ext1(dataJson)
            .iotId(up.getIotId())
            .productKey(up.getProductKey())
            .messageType(up.getMessageType().name());
    return ioTDeviceLogMetadataBuilder;
  }

  protected IoTDeviceLogMetadata normalizeMetadataForRelationalStore(IoTDeviceLogMetadata metadata) {
    if (metadata == null) {
      return null;
    }
    metadata.setIotId(limitText(metadata.getIotId(), 128));
    metadata.setProductKey(limitText(metadata.getProductKey(), 64));
    metadata.setDeviceId(limitText(metadata.getDeviceId(), 64));
    metadata.setDeviceName(limitText(metadata.getDeviceName(), 64));
    metadata.setMessageType(limitText(metadata.getMessageType(), 32));
    metadata.setEvent(limitText(metadata.getEvent(), 128));
    metadata.setProperty(limitText(metadata.getProperty(), 128));
    metadata.setExt1(limitText(metadata.getExt1(), METADATA_TEXT_MAX_LENGTH));
    metadata.setExt2(limitText(metadata.getExt2(), METADATA_EXT2_MAX_LENGTH));
    metadata.setExt3(limitText(metadata.getExt3(), METADATA_TEXT_MAX_LENGTH));
    return metadata;
  }

  protected String metadataValueText(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal.toPlainString();
    }
    return StrUtil.str(value, CharsetUtil.charset("UTF-8"));
  }

  private String limitText(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return StrUtil.sub(value, 0, maxLength);
  }

  IoTDeviceLog build(BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    IoTDeviceLog log =
        IoTDeviceLog.builder()
            .content(buildLogContent(upRequest))
            .deviceId(ioTDeviceDTO.getDeviceId())
            .iotId(ioTDeviceDTO.getIotId())
            .productKey(ioTDeviceDTO.getProductKey())
            .messageType(upRequest.getMessageType().name())
            .event(functionOrEvent(upRequest))
            .commandId(upRequest.getCommandId())
            .commandStatus(upRequest.getCommandStatus())
            .createTime(resolveCreateTime(upRequest.getTime()))
            .deviceName(StrUtil.sub(ioTDeviceDTO.getDeviceName(), 0, 30))
            .build();
    if (upRequest.getProperties() != null
        && upRequest.getProperties().containsKey(DeviceManagerConstant.COORDINATES)) {
      log.setPoint(ioTDeviceDTO.getCoordinate());
    }

    return ensureLogIdentity(log);
  }

  protected IoTDeviceLog ensureLogIdentity(IoTDeviceLog log) {
    if (log == null) {
      return null;
    }
    if (log.getCreateTime() == null) {
      log.setCreateTime(LocalDateTime.now());
    }
    if (log.getId() == null || log.getId() <= 0) {
      Long generatedId = SQenGenId.nextId();
      if (generatedId == null || generatedId <= 0) {
        generatedId = log.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      }
      log.setId(generatedId);
    }
    return log;
  }

  protected long resolveEpochMillis(Long timestamp) {
    if (timestamp == null || timestamp <= 0) {
      return System.currentTimeMillis();
    }
    return timestamp > 10000000000L ? timestamp : timestamp * 1000;
  }

  protected LocalDateTime resolveCreateTime(Long timestamp) {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(resolveEpochMillis(timestamp)), ZoneId.systemDefault());
  }

  protected void debugRawInsertSql(String storePolicy, String sql) {
    Logger sqlLogger = sqlDebugLogger();
    if (!sqlLogger.isDebugEnabled() || StrUtil.isBlank(sql)) {
      return;
    }
    sqlLogger.debug("[{}] insert sql={}", storePolicy, abbreviateDebugText(sql));
  }

  protected void debugPreparedInsertSql(String storePolicy, String sql, Object... params) {
    Logger sqlLogger = sqlDebugLogger();
    if (!sqlLogger.isDebugEnabled() || StrUtil.isBlank(sql)) {
      return;
    }
    sqlLogger.debug("[{}] insert sql={}", storePolicy, renderPreparedSql(sql, params));
  }

  protected void debugPreparedBatchInsertSql(
      String storePolicy, String sql, List<Object[]> batchArgs) {
    Logger sqlLogger = sqlDebugLogger();
    if (!sqlLogger.isDebugEnabled() || StrUtil.isBlank(sql) || batchArgs == null || batchArgs.isEmpty()) {
      return;
    }
    int previewSize = Math.min(batchArgs.size(), SQL_DEBUG_BATCH_PREVIEW_LIMIT);
    sqlLogger.debug("[{}] batch insert size={}, preview={}", storePolicy, batchArgs.size(), previewSize);
    for (int index = 0; index < previewSize; index++) {
      sqlLogger.debug(
          "[{}] batch insert sql[{}/{}]={}",
          storePolicy,
          index + 1,
          batchArgs.size(),
          renderPreparedSql(sql, batchArgs.get(index)));
    }
    if (batchArgs.size() > previewSize) {
      sqlLogger.debug(
          "[{}] batch insert truncated, omittedRows={}", storePolicy, batchArgs.size() - previewSize);
    }
  }

  protected void debugSdkInsert(
      String storePolicy,
      String operation,
      String path,
      long timestamp,
      List<String> measurements,
      List<?> values) {
    Logger sqlLogger = sqlDebugLogger();
    if (!sqlLogger.isDebugEnabled()) {
      return;
    }
    sqlLogger.debug(
        "[{}] {} path={}, timestamp={}, measurements={}, values={}",
        storePolicy,
        operation,
        path,
        timestamp,
        abbreviateDebugJson(measurements),
        abbreviateDebugJson(values));
  }

  protected void debugSdkBatchInsert(
      String storePolicy,
      String operation,
      List<String> paths,
      List<Long> timestamps,
      List<List<String>> measurementsList,
      List<List<Object>> valuesList) {
    Logger sqlLogger = sqlDebugLogger();
    if (!sqlLogger.isDebugEnabled()
        || paths == null
        || paths.isEmpty()
        || timestamps == null
        || measurementsList == null
        || valuesList == null) {
      return;
    }
    int size = Math.min(Math.min(paths.size(), timestamps.size()), Math.min(measurementsList.size(), valuesList.size()));
    int previewSize = Math.min(size, SQL_DEBUG_BATCH_PREVIEW_LIMIT);
    sqlLogger.debug("[{}] {} size={}, preview={}", storePolicy, operation, size, previewSize);
    for (int index = 0; index < previewSize; index++) {
      sqlLogger.debug(
          "[{}] {}[{}/{}] path={}, timestamp={}, measurements={}, values={}",
          storePolicy,
          operation,
          index + 1,
          size,
          paths.get(index),
          timestamps.get(index),
          abbreviateDebugJson(measurementsList.get(index)),
          abbreviateDebugJson(valuesList.get(index)));
    }
    if (size > previewSize) {
      sqlLogger.debug("[{}] {} truncated, omittedRows={}", storePolicy, operation, size - previewSize);
    }
  }

  private String renderPreparedSql(String sql, Object[] params) {
    if (StrUtil.isBlank(sql) || params == null || params.length == 0) {
      return sql;
    }
    StringBuilder builder = new StringBuilder(sql.length() + params.length * 16);
    int paramIndex = 0;
    for (int index = 0; index < sql.length(); index++) {
      char current = sql.charAt(index);
      if (current == '?' && paramIndex < params.length) {
        builder.append(formatSqlLiteral(params[paramIndex++]));
      } else {
        builder.append(current);
      }
    }
    if (paramIndex < params.length) {
      StringJoiner joiner = new StringJoiner(", ", "[", "]");
      for (int index = paramIndex; index < params.length; index++) {
        joiner.add(formatSqlLiteral(params[index]));
      }
      builder.append(" /* extraParams=").append(joiner).append(" */");
    }
    return abbreviateDebugText(builder.toString());
  }

  private String formatSqlLiteral(Object value) {
    if (value == null) {
      return "NULL";
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    String text = String.valueOf(value);
    if (!(value instanceof CharSequence)
        && !(value instanceof TemporalAccessor)
        && !(value instanceof Date)) {
      text = JSONUtil.toJsonStr(value);
    }
    return "'" + escapeDebugSqlText(text) + "'";
  }

  private String escapeDebugSqlText(String text) {
    return abbreviateDebugText(StrUtil.nullToEmpty(text)).replace("'", "''");
  }

  private String abbreviateDebugJson(Object value) {
    return abbreviateDebugText(JSONUtil.toJsonStr(value));
  }

  private Logger sqlDebugLogger() {
    return LoggerFactory.getLogger(getClass());
  }

  private String abbreviateDebugText(String text) {
    if (text == null) {
      return null;
    }
    if (text.length() <= SQL_DEBUG_VALUE_MAX_LENGTH) {
      return text;
    }
    return text.substring(0, SQL_DEBUG_VALUE_MAX_LENGTH) + "...";
  }

  private String buildLogContent(BaseUPRequest upRequest) {
    if (MessageType.PROPERTIES.equals(upRequest.getMessageType())) {
      Map<String, Object> content = new LinkedHashMap<>();
      content.put("messageType", MessageType.PROPERTIES.name());
      content.put("properties", upRequest.getProperties() == null ? Map.of() : upRequest.getProperties());
      return JSONUtil.toJsonStr(content);
    } else if (MessageType.EVENT.equals(upRequest.getMessageType())) {
      // Keep the same envelope consumed by Scene and Flow. Event parameters must not overwrite
      // the model identifier or be flattened into an indistinguishable property report.
      Map<String, Object> content = new LinkedHashMap<>();
      content.put("messageType", MessageType.EVENT.name());
      content.put("event", upRequest.getEvent());
      if (StrUtil.isNotBlank(upRequest.getEventName())) content.put("eventName", upRequest.getEventName());
      content.put("data", upRequest.getData() == null ? Map.of() : upRequest.getData());
      if (MapUtil.isNotEmpty(upRequest.getProperties())) content.put("properties", upRequest.getProperties());
      return JSONUtil.toJsonStr(content);
    } else if (MessageType.REPLY.equals(upRequest.getMessageType())
        || MessageType.ACK.equals(upRequest.getMessageType())) {
      return JSONUtil.toJsonStr(upRequest.getReplyPayload());
    } else if (MessageType.FUNCTIONS.equals(upRequest.getMessageType())) {
      Map<String, Object> data = upRequest.getData();
      return MapUtil.isNotEmpty(data) ? JSONUtil.toJsonStr(data) : StrUtil.EMPTY;
    }
    return StrUtil.EMPTY;
  }

  private String functionOrEvent(BaseUPRequest upRequest) {
    if (MessageType.FUNCTIONS.equals(upRequest.getMessageType())
        || MessageType.REPLY.equals(upRequest.getMessageType())
        || MessageType.ACK.equals(upRequest.getMessageType())) {
      // online-上线
      return upRequest.getFunction();
    } else if (MessageType.EVENT.equals(upRequest.getMessageType())) {
      return upRequest.getEvent();
    }
    return StrUtil.EMPTY;
  }

  List<IoTDeviceEvents> selectDevEvents(String productKey) {
    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(productKey);
    JSONObject metadata = JSONUtil.parseObj(ioTProduct.getMetadata());
    List<IoTDeviceEvents> ioTDeviceEventsList = new ArrayList<>();
    JSONArray properties = metadata.getJSONArray("events");
    if (properties != null) {
      for (Object object : properties) {
        JSONObject jsonObject = JSONUtil.parseObj(object);
        JSONObject expands = JSONUtil.parseObj(jsonObject.getStr("expands"));
        IoTDeviceEvents ioTDeviceEvents =
            IoTDeviceEvents.builder()
                .id(jsonObject.getStr("id"))
                .name(jsonObject.getStr("name"))
                .description(jsonObject.getStr("description"))
                .level(expands.getStr("level"))
                .build();
        ioTDeviceEventsList.add(ioTDeviceEvents);
      }
    }
    return ioTDeviceEventsList;
  }
}
