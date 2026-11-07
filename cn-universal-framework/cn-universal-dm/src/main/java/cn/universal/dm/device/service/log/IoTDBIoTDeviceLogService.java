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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.log.writer.DeviceLogEntityWrite;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.tsfile.file.metadata.enums.CompressionType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSEncoding;
import org.apache.iotdb.tsfile.read.common.Field;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * IoTDB 存储日志
 *
 * <p>通过配置文件动态加载
 *
 * <p><b>重连机制说明：</b> IoTDB Session 客户端没有内置自动重连机制（官方文档明确说明）， 因此本类实现了统一的重连管理机制，符合官方推荐的最佳实践。 所有业务代码通过
 * getSession() 方法获取 session，无需关心重连细节。
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15 16:10
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "iotdb", name = "enable", havingValue = "true")
public class IoTDBIoTDeviceLogService extends AbstractIoTDeviceLogService {

  private String storePolicy = "iotdb";

  private static final String NUMERIC_PATTERN =
      "^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$";
  private static final Pattern NUMERIC_VALUE = Pattern.compile(NUMERIC_PATTERN);

  @Value("${iotdb.history.query-timeout-ms:8000}")
  private long historyQueryTimeoutMs = 8000;

  @Value("${iotdb.host:127.0.0.1}")
  private String host;

  @Value("${iotdb.port:6667}")
  private Integer port;

  @Value("${iotdb.connection-timeout-ms:5000}")
  private int connectionTimeoutMs = 5000;

  @Value("${iotdb.username:root}")
  private String username;

  @Value("${iotdb.password:root}")
  private String password;

  @Value("${iotdb.storage.group:root.device}")
  private String storageGroup;

  @Value("${iotdb.compressor:SNAPPY}")
  private String compressorConfig;

  @Resource private IoTDeviceService iotDeviceService;

  private volatile Session session;

  private final Object sessionLock = new Object();

  @Value("${iotdb.reconnect.maxRetries:3}")
  private int maxRetries;

  @Value("${iotdb.reconnect.backoffMillis:}")
  private String backoffConfig;

  private long[] backoffMillis = new long[] {200, 500, 1000};

  // 重连冷却时间（毫秒），防止频繁重连
  @Value("${iotdb.reconnect.cooldownMillis:5000}")
  private long reconnectCooldownMillis;

  // 最后一次重连尝试时间
  private volatile long lastReconnectAttemptTime = 0;

  // 重连状态：false=未重连中, true=重连中
  private volatile boolean reconnecting = false;

  private final ConcurrentHashMap<String, Boolean> timeseriesCache = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, ReentrantLock> timeseriesLocks =
      new ConcurrentHashMap<>();

  @Value("${iotdb.alert.enabled:false}")
  private boolean alertEnabled;

  @Value("${iotdb.alert.templateId:0}")
  private Long alertTemplateId;

  @Value("${iotdb.alert.receivers:}")
  private String alertReceivers;

  @PostConstruct
  public void init() {
    try {
      createAndOpenSession();
      log.info("初始化IoTDB连接成功，host={}, port={}", host, port);
    } catch (Exception e) {
      log.error("初始化IoTDB连接失败，host={}, port={}", host, port, e);
      // 初始化失败时确保 session 为 null
      session = null;
    }
  }

  /**
   * 将时间戳转换为IoTDB需要的毫秒格式
   *
   * <p>判断逺辑：
   *
   * <ul>
   *   <li>如果时间戳 > 10000000000L，说明已经是毫秒，直接返回
   *   <li>否则说明是秒，乘以1000转换为毫秒
   * </ul>
   *
   * @param timestamp 时间戳（秒或毫秒）
   * @return 毫秒时间戳
   */
  private long toMillisTimestamp(Long timestamp) {
    if (timestamp == null) {
      return 0L;
    }
    // 如果时间戳 > 10000000000 (2286-11-20 17:46:40)，则认为已经是毫秒
    // 否则认为是秒，需要乘以1000
    return timestamp > 10000000000L ? timestamp : timestamp * 1000;
  }

  /** 创建并打开 IoTDB Session 注意：此方法会抛出异常，调用方需要处理 */
  private void createAndOpenSession() throws Exception {
    Session newSession = null;
    try {
      // 1. 创建新 Session
      newSession =
          new Session.Builder().host(host).port(port).username(username).password(password).build();

      // 2. 打开连接（这一步会初始化内部的 deviceIdToEndpoint 等 Map）
      newSession.open(false, Math.max(1, connectionTimeoutMs));

      // 3. 创建存储组
      try {
        newSession.setStorageGroup(storageGroup);
        log.info("创建存储组成功: {}", storageGroup);
      } catch (Exception e) {
        // 存储组可能已存在，忽略错误
        log.debug("存储组可能已存在: {}", storageGroup);
      }

      // 4. 创建时间序列（类似MySQL的表结构）
      createTimeseries();

      // 5. 只有完全成功后才赋值给成员变量
      session = newSession;

    } catch (Exception e) {
      // 失败时关闭新创建的 Session
      if (newSession != null) {
        try {
          newSession.close();
        } catch (Exception ignore) {
        }
      }
      throw e; // 重新抛出异常，让调用方知道初始化失败
    }
  }

  /**
   * 创建时间序列（动态创建，类似自动建表） 预定义常用字段的数据类型，防止 IoTDB 自动推断错误
   *
   * <p>MySQL 表结构映射： - iot_device_log: device_name(varchar32), message_type(varchar20),
   * command_id(varchar32), command_status(tinyint), event(varchar80), point(varchar128),
   * content(text) - iot_device_log_metadata: property(varchar32), ext1(varchar255),
   * ext2(varchar655), ext3(varchar255)
   */
  private void createTimeseries() {
    try {
      timeseriesCache.clear();
      log.debug("IoTDB 启用应用侧自动建时序（类型定义与 MySQL 表结构对齐）");
      log.debug("=== iot_device_log 字段类型映射 ===");
      log.debug("  device_name    : varchar(32)  -> TEXT");
      log.debug("  message_type   : varchar(20)  -> TEXT");
      log.debug("  command_id     : varchar(32)  -> TEXT");
      log.debug("  command_status : tinyint      -> INT32");
      log.debug("  event          : varchar(80)  -> TEXT");
      log.debug("  point          : varchar(128) -> TEXT");
      log.debug("  content        : text         -> TEXT");
      log.debug("=== iot_device_log_metadata 字段类型映射 ===");
      log.debug("  property       : varchar(32)  -> TEXT");
      log.debug("  ext1           : varchar(255) -> TEXT");
      log.debug("  ext2           : varchar(655) -> TEXT");
      log.debug("  ext3           : varchar(255) -> TEXT");
      log.debug("  create_time    : datetime     -> INT64(timestamp)");
      log.debug("  max_storage    : int          -> INT32");
    } catch (Exception e) {
      log.warn("预创建时间序列失败（不影响使用）: {}", e.getMessage());
    }
  }

  // === IoTDB重试与重连机制 ===
  private long[] getBackoffMillis() {
    try {
      if (backoffConfig != null && !backoffConfig.isEmpty()) {
        String[] arr = backoffConfig.split(",");
        long[] res = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
          res[i] = Long.parseLong(arr[i].trim());
        }
        return res;
      }
    } catch (Exception e) {
      log.warn("解析重试退避配置失败: {}", backoffConfig);
    }
    return backoffMillis;
  }

  /**
   * 统一获取 Session，自动处理重连逻辑
   *
   * <p>这是所有业务代码获取 session 的唯一入口。 IoTDB Session 本身没有内置自动重连机制（官方文档明确说明）， 因此需要在应用层实现重连逻辑，这是官方推荐的做法。
   *
   * <p>重连策略： 1. 冷却时间机制：避免频繁重连（默认5秒） 2. 错开重连：使用锁确保同一时间只有一个线程执行重连 3. 等待机制：其他线程等待重连完成，避免重复重连
   *
   * @return Session 实例，如果重连失败返回 null
   */
  private Session getSession() {
    // 快速路径：如果 session 已存在，直接返回
    Session currentSession = session;
    if (currentSession != null) {
      return currentSession;
    }

    // 检查冷却时间，避免频繁重连
    long now = System.currentTimeMillis();
    long timeSinceLastAttempt = now - lastReconnectAttemptTime;

    if (timeSinceLastAttempt < reconnectCooldownMillis) {
      // 还在冷却期内，等待其他线程的重连结果
      long waitTime = reconnectCooldownMillis - timeSinceLastAttempt;
      log.debug("IoTDB重连冷却中，等待 {}ms", waitTime);

      synchronized (sessionLock) {
        // 等待期间可能其他线程已经重连成功
        if (session != null) {
          return session;
        }

        // 等待一段时间，让其他线程完成重连
        try {
          sessionLock.wait(Math.min(waitTime, 1000)); // 最多等待1秒
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // 再次检查
        if (session != null) {
          return session;
        }
      }
    }

    // 执行重连（带锁，确保只有一个线程执行重连）
    synchronized (sessionLock) {
      // 双重检查：可能其他线程已经重连成功
      if (session != null) {
        return session;
      }

      // 检查是否正在重连中
      if (reconnecting) {
        log.debug("其他线程正在重连IoTDB，等待重连完成");
        // 等待重连完成（最多等待5秒）
        long waitStart = System.currentTimeMillis();
        while (reconnecting && (System.currentTimeMillis() - waitStart) < 5000) {
          try {
            sessionLock.wait(500); // 每500ms检查一次
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
          if (session != null) {
            return session; // 其他线程重连成功
          }
        }
        // 如果等待超时，尝试自己重连
        if (reconnecting) {
          log.warn("等待其他线程重连超时，尝试自己重连");
        }
      }

      // 更新重连尝试时间
      lastReconnectAttemptTime = System.currentTimeMillis();

      // 标记正在重连
      reconnecting = true;

      try {
        // 1. 先关闭旧连接
        if (session != null) {
          try {
            session.close();
            log.debug("已关闭旧的 IoTDB Session");
          } catch (Exception ignore) {
            // 关闭失败不影响重连
          }
        }

        // 2. 清空 session
        session = null;

        // 3. 尝试创建新连接
        createAndOpenSession();
        log.info("✓ IoTDB重连成功");
        return session;

      } catch (Exception e) {
        log.error("✗ IoTDB重连失败，错误: {}", e.getMessage());
        // 重连失败时确保 session 为 null
        session = null;
        return null;
      } finally {
        // 重置重连标志
        reconnecting = false;
        // 通知等待的线程
        sessionLock.notifyAll();
      }
    }
  }

  /** 标记 session 无效，触发下次重连 当检测到连接错误时调用此方法 */
  private void invalidateSession() {
    synchronized (sessionLock) {
      if (session != null) {
        try {
          session.close();
        } catch (Exception ignore) {
          // 忽略关闭异常
        }
        session = null;
      }
    }
  }

  /** 判断异常是否需要重连 */
  private boolean isReconnectNeeded(Exception e) {
    if (e == null) {
      return false;
    }
    String errMsg = e.getMessage();
    if (errMsg == null) {
      return false;
    }
    return errMsg.contains("deviceIdToEndpoint")
        || errMsg.contains("Connection refused")
        || errMsg.contains("Broken pipe")
        || errMsg.contains("Connection reset")
        || errMsg.contains("Session is closed")
        || errMsg.contains("session未初始化");
  }

  /**
   * 带重试机制的执行器
   *
   * @param action 要执行的操作
   * @param operation 操作名称（用于日志）
   * @param path IoTDB路径
   * @param iotId 设备ID
   * @return 操作结果
   * @throws Exception 所有重试失败后抛出最后一次异常
   */
  private <T> T executeWithRetry(
      java.util.concurrent.Callable<T> action, String operation, String path, String iotId)
      throws Exception {
    Exception last = null;
    long[] delays = getBackoffMillis();
    int retries = Math.max(maxRetries, 1);

    for (int i = 0; i < retries; i++) {
      try {
        // 统一获取 session（自动处理重连）
        Session currentSession = getSession();
        if (currentSession == null) {
          throw new IllegalStateException("IoTDB Session 不可用且重连失败");
        }

        // 执行操作
        return action.call();

      } catch (Exception e) {
        last = e;
        String errMsg = e.getMessage();

        // 判断是否需要重连
        boolean needReconnect = isReconnectNeeded(e);

        log.warn(
            "IoTDB {} 失败，path={}, iotId={}, attempt={}/{}, needReconnect={}, error={}",
            operation,
            path,
            iotId,
            (i + 1),
            retries,
            needReconnect,
            errMsg);

        // 需要重连时，标记 session 无效
        if (needReconnect) {
          invalidateSession();
        }

        // 最后一次重试失败，不再等待
        if (i < retries - 1 && i < delays.length) {
          try {
            Thread.sleep(delays[i]);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }

    // 所有重试都失败
    triggerIoTDBAlert("IoTDB操作失败: " + operation, "path=" + path + ", iotId=" + iotId, last);
    throw last;
  }

  private void triggerIoTDBAlert(String title, String content, Throwable e) {
    log.error("{} - {}, err={}", title, content, e == null ? "" : e.toString());
    // 如需推送到外部通知渠道，请在 cn-universal-notice 模块配置模板，并在此处接入对应服务
  }

  @PreDestroy
  public void closeSession() {
    if (session != null) {
      try {
        session.close();
        log.info("IoTDB连接已关闭");
      } catch (Exception e) {
        log.error("关闭IoTDB连接失败", e);
      }
    }
  }

  @Override
  @Async("taskExecutor")
  public void saveDeviceLog(BaseUPRequest upRequest, IoTDeviceDTO noUse, IoTProduct ioTProduct) {
    saveDeviceLogDirect(upRequest, noUse, ioTProduct);
  }

  @Override
  public void saveDeviceLogDirect(
      BaseUPRequest upRequest, IoTDeviceDTO noUse, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      IoTDeviceDTO ioTDeviceDTO = resolveDevice(upRequest, noUse, iotDeviceService);
      try {
        if (ioTDeviceDTO == null) {
          log.warn(
              "IoTDB保存设备日志跳过，设备不存在 productKey={}, deviceId={}",
              upRequest.getProductKey(),
              upRequest.getDeviceId());
          return;
        }
        IoTDeviceLog ioTDeviceLog = build(upRequest, ioTDeviceDTO);
        saveDeviceLogToIoTDB(ioTDeviceLog, upRequest);
        log.debug("IoTDB插入设备日志成功，iotId={}", upRequest.getIotId());
      } catch (Exception e) {
        log.error("保存设备日志到IoTDB报错={}", e);
      }

      // 处理存储策略配置
      String storePolicyConfiguration = ioTProduct.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)
            || MessageType.EVENT.equals(upRequest.getMessageType())) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(ioTProduct);
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
        }
      } catch (Exception e) {
        log.error("IoTDB保存设备属性扩展日志报错={}", e);
      }
    }
  }

  @Override
  public void flushDeviceLogRequests(List<DeviceLogWriteRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    for (DeviceLogWriteRequest request : requests) {
      if (request != null) {
        saveDeviceLogDirect(request.upRequest(), request.ioTDeviceDTO(), request.ioTProduct());
      }
    }
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    /** 产品数据存储策略，不为空则保存日志 */
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        saveDeviceLogToIoTDB(ensureLogIdentity(ioTDeviceLog), null);
        log.info(
            "IoTDB插入设备日志成功，deviceId={}", ioTDeviceDTO != null ? ioTDeviceDTO.getDeviceId() : null);
      } catch (Exception e) {
        log.error("保存设备日志到IoTDB报错={}", e);
      }
    }
  }

  @Override
  public void flushDeviceLogEntities(List<DeviceLogEntityWrite> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    for (DeviceLogEntityWrite entity : entities) {
      if (entity != null) {
        saveDeviceLog(entity.ioTDeviceLog(), entity.ioTDeviceDTO(), entity.ioTProduct());
      }
    }
  }

  /** 根据存储策略保存元数据日志 支持属性和事件的详细信息存储 */
  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, BaseUPRequest up, IoTProduct ioTProduct) {
    LogStorePolicyDTO effectivePolicy =
        logStorePolicyDTO == null ? LogStorePolicyDTO.builder().build() : logStorePolicyDTO;
    List<IoTDBSeriesWrite> seriesWrites = new ArrayList<>();
    DeviceMetadata deviceMetadata = getDeviceMetadata(ioTProduct.getMetadata());

    // 处理属性消息
    if (MessageType.PROPERTIES.equals(up.getMessageType())
        && up.getProperties() != null
        && CollectionUtil.isNotEmpty(effectivePolicy.getProperties())) {
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (effectivePolicy.getProperties().containsKey(key)) {
                  try {
                    AbstractPropertyMetadata propertyOrNull = deviceMetadata.getPropertyOrNull(key);
                    IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                    ioTDevicePropertiesBO.withValue(propertyOrNull, value);
                    // 构建元数据并保存
                    String property = key;
                    String propertyName = ioTDevicePropertiesBO.getPropertyName();
                    String formatValue = ioTDevicePropertiesBO.getFormatValue();
                    String symbol = ioTDevicePropertiesBO.getSymbol();
                    String content = metadataValueText(value);
                    long ts = up.getTime() != null ? up.getTime() : System.currentTimeMillis();
                    seriesWrites.add(
                        buildPropertySeriesWrite(
                            up, property, content, propertyName, formatValue, symbol, ts));
                  } catch (Exception e) {
                    log.error("IoTDB保存属性元数据失败: iotId={}, property={}", up.getIotId(), key, e);
                  }
                }
              });
    }

    // 处理事件消息
    if (MessageType.EVENT.equals(up.getMessageType())) {
      try {
        int maxStorage =
            CollectionUtil.isNotEmpty(effectivePolicy.getEvent())
                    && effectivePolicy.getEvent().containsKey(up.getEvent())
                ? effectivePolicy.getEvent().get(up.getEvent()).getMaxStorage()
                : cn.universal.common.constant.IoTConstant.maxStorage;
        long ts = up.getTime() != null ? up.getTime() : System.currentTimeMillis();
        seriesWrites.add(buildEventSeriesWrite(up, maxStorage, ts));
      } catch (Exception e) {
        log.error("IoTDB保存事件元数据失败: iotId={}, event={}", up.getIotId(), up.getEvent(), e);
      }
    }

    if (seriesWrites.isEmpty()) {
      return;
    }

    try {
      batchInsertSeriesToIoTDB(seriesWrites, up);
      log.debug("IoTDB批量插入元数据成功: iotId={}, 数量={}", up.getIotId(), seriesWrites.size());
    } catch (Exception e) {
      log.warn("IoTDB批量插入元数据失败，回退逐条写入: iotId={}", up.getIotId(), e);
      fallbackInsertSeriesToIoTDB(seriesWrites, up);
    }
  }

  private IoTDBSeriesWrite buildPropertySeriesWrite(
      BaseUPRequest up,
      String property,
      String content,
      String propertyName,
      String formatValue,
      String symbol,
      long timestamp) {
    String seriesPath =
        buildDevicePath(up.getProductKey(), up.getDeviceId())
            + ".property."
            + sanitizePathNode(property);

    List<String> measurements =
        List.of(
            "message_type",
            "property",
            "content",
            "create_time",
            "device_name",
            "device_id",
            "iot_id",
            "product_key",
            "ext1",
            "ext2",
            "ext3");

    List<org.apache.iotdb.tsfile.file.metadata.enums.TSDataType> types =
        List.of(
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT64,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT);

    List<Object> values =
        List.of(
            escapeValue(MessageType.PROPERTIES.name()),
            escapeValue(property),
            escapeValue(content),
            timestamp,
            escapeValue(up.getDeviceName()),
            escapeValue(up.getDeviceId()),
            escapeValue(up.getIotId()),
            escapeValue(up.getProductKey()),
            escapeValue(propertyName),
            escapeValue(formatValue),
            escapeValue(symbol));

    // Append-only schema evolution: new writes carry a native numeric measurement and a
    // per-record schema marker. Existing TEXT history remains readable without startup backfills.
    measurements = new ArrayList<>(measurements);
    types = new ArrayList<>(types);
    values = new ArrayList<>(values);
    measurements.add("value_schema");
    types.add(TSDataType.INT32);
    Double numeric = finiteNumericValue(content);
    // 0 also prevents a previous numeric cell at this timestamp being treated as current
    // after an overwrite with text; IoTDB retains omitted measurements on partial writes.
    values.add(numeric == null ? 0 : 1);
    if (numeric != null) {
      measurements.add("numeric_value");
      types.add(TSDataType.DOUBLE);
      values.add(numeric);
    }
    return new IoTDBSeriesWrite(seriesPath, timestamp, measurements, types, values, property, null);
  }

  private IoTDBSeriesWrite buildEventSeriesWrite(BaseUPRequest up, int maxStorage, long timestamp) {
    String seriesPath =
        buildDevicePath(up.getProductKey(), up.getDeviceId())
            + ".event."
            + sanitizePathNode(up.getEvent());

    List<String> measurements =
        List.of(
            "message_type",
            "event",
            "content",
            "create_time",
            "device_name",
            "device_id",
            "iot_id",
            "product_key",
            "ext1",
            "max_storage");

    List<org.apache.iotdb.tsfile.file.metadata.enums.TSDataType> types =
        List.of(
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT64,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT32);

    List<Object> values =
        List.of(
            escapeValue(MessageType.EVENT.name()),
            escapeValue(up.getEvent()),
            escapeValue(up.getEventName()),
            timestamp,
            escapeValue(up.getDeviceName()),
            escapeValue(up.getDeviceId()),
            escapeValue(up.getIotId()),
            escapeValue(up.getProductKey()),
            String.valueOf(maxStorage),
            maxStorage);

    return new IoTDBSeriesWrite(
        seriesPath, timestamp, measurements, types, values, null, up.getEvent());
  }

  private void batchInsertSeriesToIoTDB(List<IoTDBSeriesWrite> seriesWrites, BaseUPRequest up)
      throws Exception {
    if (seriesWrites.size() == 1) {
      insertSingleSeriesToIoTDB(seriesWrites.get(0), up.getIotId());
      return;
    }

    Set<String> ensuredPaths = new HashSet<>();
    for (IoTDBSeriesWrite seriesWrite : seriesWrites) {
      if (ensuredPaths.add(seriesWrite.path())) {
        ensureTimeseries(seriesWrite.path(), seriesWrite.measurements(), seriesWrite.types());
      }
    }

    List<String> deviceIds = new ArrayList<>(seriesWrites.size());
    List<Long> times = new ArrayList<>(seriesWrites.size());
    List<List<String>> measurementsList = new ArrayList<>(seriesWrites.size());
    List<List<org.apache.iotdb.tsfile.file.metadata.enums.TSDataType>> typesList =
        new ArrayList<>(seriesWrites.size());
    List<List<Object>> valuesList = new ArrayList<>(seriesWrites.size());

    for (IoTDBSeriesWrite seriesWrite : seriesWrites) {
      deviceIds.add(seriesWrite.path());
      times.add(seriesWrite.timestamp());
      measurementsList.add(seriesWrite.measurements());
      typesList.add(seriesWrite.types());
      valuesList.add(seriesWrite.values());
    }

    String batchPath = buildDevicePath(up.getProductKey(), up.getDeviceId()) + ".metadata_batch";
    debugSdkBatchInsert(
        storePolicy, "insertRecords", deviceIds, times, measurementsList, valuesList);
    executeWithRetry(
        () -> {
          Session currentSession = getSession();
          if (currentSession == null) {
            throw new IllegalStateException("IoTDB Session 不可用");
          }
          currentSession.insertRecords(deviceIds, times, measurementsList, typesList, valuesList);
          return null;
        },
        "insertRecords",
        batchPath,
        up.getIotId());
  }

  private void fallbackInsertSeriesToIoTDB(List<IoTDBSeriesWrite> seriesWrites, BaseUPRequest up) {
    for (IoTDBSeriesWrite seriesWrite : seriesWrites) {
      try {
        insertSingleSeriesToIoTDB(seriesWrite, up.getIotId());
      } catch (Exception ex) {
        log.error(
            "IoTDB逐条插入元数据失败: iotId={}, property={}, event={}, path={}",
            up.getIotId(),
            seriesWrite.property(),
            seriesWrite.event(),
            seriesWrite.path(),
            ex);
      }
    }
  }

  private void insertSingleSeriesToIoTDB(IoTDBSeriesWrite seriesWrite, String iotId)
      throws Exception {
    ensureTimeseries(seriesWrite.path(), seriesWrite.measurements(), seriesWrite.types());
    debugSdkInsert(
        storePolicy,
        "insertRecord",
        seriesWrite.path(),
        seriesWrite.timestamp(),
        seriesWrite.measurements(),
        seriesWrite.values());
    executeWithRetry(
        () -> {
          Session currentSession = getSession();
          if (currentSession == null) {
            throw new IllegalStateException("IoTDB Session 不可用");
          }
          currentSession.insertRecord(
              seriesWrite.path(),
              seriesWrite.timestamp(),
              seriesWrite.measurements(),
              seriesWrite.types(),
              seriesWrite.values());
          return null;
        },
        "insertRecord",
        seriesWrite.path(),
        iotId);
  }

  private record IoTDBSeriesWrite(
      String path,
      long timestamp,
      List<String> measurements,
      List<org.apache.iotdb.tsfile.file.metadata.enums.TSDataType> types,
      List<Object> values,
      String property,
      String event) {}

  /**
   * 保存属性元数据到IoTDB 路径: root.device.{productKey}.{deviceId}.property_metadata
   * 字段与MySQL一致：message_type,property,content,device_name,device_id,iot_id,product_key,create_time,ext1,ext2,ext3
   */
  private void savePropertyMetadataToIoTDB(
      BaseUPRequest up,
      String property,
      String content,
      String propertyName,
      String formatValue,
      String symbol,
      long timestamp)
      throws Exception {
    // 通过 executeWithRetry 统一处理，无需在此检查 session

    String metadataPath =
        buildDevicePath(up.getProductKey(), up.getDeviceId()) + ".property_metadata";

    // 与MySQL一致的字段
    List<String> measurements =
        List.of(
            "message_type",
            "property",
            "content",
            "device_name",
            "device_id",
            "iot_id",
            "product_key",
            "create_time",
            "ext1",
            "ext2",
            "ext3");

    // 显式指定数据类型（对齐 MySQL iot_device_log_metadata 表）
    List<org.apache.iotdb.tsfile.file.metadata.enums.TSDataType> types =
        List.of(
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType
                .TEXT, // message_type: varchar(20)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // property: varchar(32)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // content: text
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // device_name: varchar(64)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // device_id: varchar(64)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // iot_id: varchar(128)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // product_key: varchar(64)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT64, // create_time: datetime
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // ext1: varchar(255)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // ext2: varchar(655)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT); // ext3: varchar(255)

    List<Object> values =
        List.of(
            escapeValue(MessageType.PROPERTIES.name()),
            escapeValue(property),
            escapeValue(content),
            escapeValue(up.getDeviceName()),
            escapeValue(up.getDeviceId()),
            escapeValue(up.getIotId()),
            escapeValue(up.getProductKey()),
            timestamp, // INT64 不需要 String.valueOf
            escapeValue(propertyName),
            escapeValue(formatValue),
            escapeValue(symbol));

    try {
      ensureTimeseries(metadataPath, measurements, types);
      debugSdkInsert(storePolicy, "insertRecord", metadataPath, timestamp, measurements, values);
      executeWithRetry(
          () -> {
            Session currentSession = getSession();
            if (currentSession == null) {
              throw new IllegalStateException("IoTDB Session 不可用");
            }
            currentSession.insertRecord(metadataPath, timestamp, measurements, types, values);
            return null;
          },
          "insertRecord",
          metadataPath,
          up.getIotId());
      log.debug(
          "IoTDB插入属性元数据成功: path={}, property={}, iotId={}", metadataPath, property, up.getIotId());
    } catch (Exception e) {
      log.error(
          "IoTDB插入属性元数据失败: path={}, property={}, iotId={}",
          metadataPath,
          property,
          up.getIotId(),
          e);
      throw e;
    }
  }

  /** 保存事件元数据到IoTDB 路径: root.device.{productKey}.{deviceId}.event_metadata */
  private void saveEventMetadataToIoTDB(BaseUPRequest up, int maxStorage) throws Exception {
    saveEventMetadataToIoTDB(up, maxStorage, System.currentTimeMillis());
  }

  /** 保存事件元数据到IoTDB 路径: root.device.{productKey}.{deviceId}.event_metadata */
  private void saveEventMetadataToIoTDB(BaseUPRequest up, int maxStorage, long timestamp)
      throws Exception {
    // 通过 executeWithRetry 统一处理，无需在此检查 session

    String metadataPath = buildDevicePath(up.getProductKey(), up.getDeviceId()) + ".event_metadata";

    // 与MySQL保持一致的字段
    List<String> measurements =
        List.of(
            "message_type",
            "event",
            "content",
            "device_name",
            "device_id",
            "iot_id",
            "product_key",
            "create_time",
            "ext1",
            "max_storage");

    // 显式指定数据类型（对齐 MySQL iot_device_log_metadata 表）
    List<org.apache.iotdb.tsfile.file.metadata.enums.TSDataType> types =
        List.of(
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType
                .TEXT, // message_type: varchar(20)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // event: varchar(32)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // content: text
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // device_name: varchar(64)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // device_id: varchar(64)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // iot_id: varchar(128)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // product_key: varchar(64)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT64, // create_time: datetime
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // ext1: varchar(255)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT32); // max_storage: int

    List<Object> values =
        List.of(
            escapeValue(MessageType.EVENT.name()),
            escapeValue(up.getEvent()),
            escapeValue(up.getEventName()),
            escapeValue(up.getDeviceName()),
            escapeValue(up.getDeviceId()),
            escapeValue(up.getIotId()),
            escapeValue(up.getProductKey()),
            timestamp, // INT64 不需要 String.valueOf
            escapeValue(JSONUtil.toJsonStr(up.getData())),
            maxStorage); // INT32 不需要 String.valueOf

    try {
      ensureTimeseries(metadataPath, measurements, types);
      debugSdkInsert(storePolicy, "insertRecord", metadataPath, timestamp, measurements, values);
      executeWithRetry(
          () -> {
            Session currentSession = getSession();
            if (currentSession == null) {
              throw new IllegalStateException("IoTDB Session 不可用");
            }
            currentSession.insertRecord(metadataPath, timestamp, measurements, types, values);
            return null;
          },
          "insertRecord",
          metadataPath,
          up.getIotId());
      log.debug(
          "IoTDB插入事件元数据成功: path={}, event={}, iotId={}",
          metadataPath,
          up.getEvent(),
          up.getIotId());
    } catch (Exception e) {
      log.error(
          "IoTDB插入事件元数据失败: path={}, event={}, iotId={}",
          metadataPath,
          up.getEvent(),
          up.getIotId(),
          e);
      throw e;
    }
  }

  /**
   * 保存设备日志到IoTDB 数据路径结构: - 设备日志: root.device.{productKey}.{deviceId}.log - 元数据:
   * root.device.{productKey}.{deviceId}.metadata
   */
  private void saveDeviceLogToIoTDB(IoTDeviceLog ioTDeviceLog, BaseUPRequest upRequest)
      throws Exception {
    IoTDeviceLog target = ensureLogIdentity(ioTDeviceLog);
    if (target == null) {
      log.warn("IoTDB设备日志为空，跳过写入");
      return;
    }

    // 主日志路径 (对应iot_device_log_*)
    String devicePath = buildDevicePath(target.getProductKey(), target.getDeviceId()) + ".log";

    // 时间戳（毫秒）
    long timestamp =
        target.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    long resolvedLogId = target.getId() != null && target.getId() > 0 ? target.getId() : timestamp;
    target.setId(resolvedLogId);

    // 字段定义（对齐 MySQL iot_device_log 表结构）
    List<String> measurements =
        List.of(
            "log_id",
            "message_type",
            "content",
            "event",
            "command_id",
            "command_status",
            "point",
            "device_name",
            "iot_id",
            "device_id",
            "product_key");

    // 显式指定数据类型（重要：防止 IoTDB 自动推断错误）
    List<org.apache.iotdb.tsfile.file.metadata.enums.TSDataType> types =
        List.of(
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT64,
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType
                .TEXT, // message_type: varchar(20)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // content: text
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // event: varchar(80)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // command_id: varchar(32)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.INT32, // command_status: tinyint
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // point: varchar(128)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // device_name: varchar(32)
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // iot_id
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT, // device_id
            org.apache.iotdb.tsfile.file.metadata.enums.TSDataType.TEXT // product_key
            );

    // 字段值（需要根据类型转换）
    List<Object> values = new ArrayList<>(measurements.size());
    values.add(resolvedLogId);
    values.add(escapeValue(target.getMessageType()));
    values.add(escapeValue(target.getContent()));
    values.add(escapeValue(target.getEvent()));
    values.add(escapeValue(target.getCommandId()));
    values.add(target.getCommandStatus() != null ? target.getCommandStatus() : 0);
    values.add(escapeValue(target.getPoint()));
    values.add(escapeValue(target.getDeviceName()));
    values.add(escapeValue(target.getIotId()));
    values.add(escapeValue(target.getDeviceId()));
    values.add(escapeValue(target.getProductKey()));

    try {
      ensureTimeseries(devicePath, measurements, types);
      debugSdkInsert(storePolicy, "insertRecord", devicePath, timestamp, measurements, values);
      executeWithRetry(
          () -> {
            Session currentSession = getSession();
            if (currentSession == null) {
              throw new IllegalStateException("IoTDB Session 不可用");
            }
            currentSession.insertRecord(devicePath, timestamp, measurements, types, values);
            return null;
          },
          "insertRecord",
          devicePath,
          target.getIotId());
      log.debug(
          "IoTDB插入设备日志成功: path={}, timestamp={}, productKey={}, deviceId={}",
          devicePath,
          timestamp,
          target.getProductKey(),
          target.getDeviceId());
    } catch (Exception e) {
      log.error(
          "IoTDB插入设备日志失败: path={}, productKey={}, deviceId={}, error={}",
          devicePath,
          target.getProductKey(),
          target.getDeviceId(),
          e.getMessage());
      throw e;
    }
  }

  /**
   * 按属性名拆分时间序列写入 路径: root.device.{productKey}.{deviceId}.property.{sanitizedProperty}
   * 精简字段：message_type,content,create_time,ext1,ext2,ext3
   */
  private void savePropertySeriesToIoTDB(
      BaseUPRequest up,
      String property,
      String content,
      String propertyName,
      String formatValue,
      String symbol,
      long timestamp)
      throws Exception {
    IoTDBSeriesWrite seriesWrite =
        buildPropertySeriesWrite(
            up, property, content, propertyName, formatValue, symbol, timestamp);
    insertSingleSeriesToIoTDB(seriesWrite, up.getIotId());

    log.debug(
        "IoTDB插入属性拆分序列成功: path={}, property={}, iotId={}",
        seriesWrite.path(),
        property,
        up.getIotId());
  }

  /**
   * 按事件名拆分时间序列写入 路径: root.device.{productKey}.{deviceId}.event.{sanitizedEvent}
   * 精简字段：message_type,content,create_time,ext1(maxStorage)
   */
  private void saveEventSeriesToIoTDB(BaseUPRequest up, int maxStorage, long timestamp)
      throws Exception {
    IoTDBSeriesWrite seriesWrite = buildEventSeriesWrite(up, maxStorage, timestamp);
    insertSingleSeriesToIoTDB(seriesWrite, up.getIotId());

    log.debug(
        "IoTDB插入事件拆分序列成功: path={}, event={}, iotId={}",
        seriesWrite.path(),
        up.getEvent(),
        up.getIotId());
  }

  /** 转义字符串值，防止SQL注入 */
  private String escapeValue(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("'", "\\'");
  }

  /**
   * 从 iotId 中解析出 productKey 和 deviceId iotId 格式: productKey + deviceId (直接拼接，无分隔符) 或
   * productKey:deviceId (使用:分隔符)
   *
   * @param iotId 设备唯一ID
   * @param productKey 产品Key（可选，如果已知）
   * @return 数组 [productKey, deviceId]，解析失败返回 null
   */
  /**
   * 从 iotId 中解析出 productKey 和 deviceId iotId 格式: productKey + deviceId (直接拼接，无分隔符) 或
   * productKey:deviceId (使用:分隔符)
   *
   * @param iotId 设备唯一ID
   * @param productKey 产品Key（可选，如果已知）
   * @return 数组 [productKey, deviceId]，解析失败返回 null
   */
  private String[] parseIotId(String iotId, String productKey) {
    if (StrUtil.isBlank(iotId)) {
      return null;
    }

    // 如果包含冒号，使用分隔符解析 (TCP/UDP协议)
    if (iotId.contains(":")) {
      String[] parts = iotId.split(":", 2);
      if (parts.length == 2) {
        return parts; // [productKey, deviceId]
      }
    }

    // 如果已知 productKey，直接提取 deviceId
    if (StrUtil.isNotBlank(productKey) && iotId.startsWith(productKey)) {
      String deviceId = iotId.substring(productKey.length());
      if (StrUtil.isNotBlank(deviceId)) {
        return new String[] {productKey, deviceId};
      }
    }

    // 无法解析
    log.warn("无法从 iotId 解析出 productKey 和 deviceId: iotId={}, productKey={}", iotId, productKey);
    return null;
  }

  /**
   * 构建设备路径 路径结构: root.device.{productKey}.{deviceId} 这种结构自动实现了MySQL中按iot_id哈希分表的效果
   *
   * <p>IoTDB路径命名规范: 1. 必须以字母或下划线开头 2. 后续可包含字母、数字、下划线 3. 不能包含特殊字符
   */
  private String buildDevicePath(String productKey, String deviceId) {
    String cleanProductKey = encodePathNode(productKey);
    String cleanDeviceId = encodePathNode(deviceId);
    return storageGroup + "." + cleanProductKey + "." + cleanDeviceId;
  }

  private String buildLegacyDevicePath(String productKey, String deviceId) {
    String cleanProductKey = sanitizeLegacyPathNode(productKey);
    String cleanDeviceId = sanitizeLegacyPathNode(deviceId);
    return storageGroup + "." + cleanProductKey + "." + cleanDeviceId;
  }

  private List<String> buildDevicePathCandidates(String productKey, String deviceId) {
    List<String> candidates = new ArrayList<>();
    String primaryPath = buildDevicePath(productKey, deviceId);
    candidates.add(primaryPath);
    String legacyPath = buildLegacyDevicePath(productKey, deviceId);
    if (!primaryPath.equals(legacyPath)) {
      candidates.add(legacyPath);
    }
    return candidates;
  }

  private List<String> buildProductPathCandidates(String productKey) {
    List<String> candidates = new ArrayList<>();
    String primary = storageGroup + "." + encodePathNode(productKey) + ".*";
    candidates.add(primary);
    String legacy = storageGroup + "." + sanitizeLegacyPathNode(productKey) + ".*";
    if (!primary.equals(legacy)) {
      candidates.add(legacy);
    }
    return candidates;
  }

  private String encodePathNode(String node) {
    String legacySanitized = sanitizeLegacyPathNode(node);
    String suffix = stablePathHash(node);
    String base = legacySanitized;
    int maxBaseLength = Math.max(8, 48 - suffix.length());
    if (base.length() > maxBaseLength) {
      base = base.substring(0, maxBaseLength);
    }
    return base + "_" + suffix;
  }

  /**
   * 清理路径节点，确保符合IoTDB命名规范
   *
   * @param node 原始节点名称
   * @return 清理后的合法节点名称
   */
  private String sanitizePathNode(String node) {
    return encodePathNode(node);
  }

  private String sanitizeLegacyPathNode(String node) {
    if (node == null || node.isEmpty()) {
      return "device_unknown";
    }

    // 1. 替换特殊字符为下划线
    String cleaned = node.replaceAll("[^a-zA-Z0-9_]", "_");

    // 2. 如果以数字开头，添加前缀 "d_"
    if (cleaned.matches("^[0-9].*")) {
      cleaned = "d_" + cleaned;
    }

    // 3. 如果为空或只有下划线，使用默认值
    if (cleaned.isEmpty() || cleaned.matches("^_+$")) {
      cleaned = "device_" + Math.abs(node.hashCode());
    }

    return cleaned;
  }

  private String stablePathHash(String node) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes =
          digest.digest(
              StrUtil.blankToDefault(node, "device_unknown").getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(10);
      for (int index = 0; index < bytes.length && builder.length() < 10; index++) {
        builder.append(String.format("%02x", bytes[index]));
      }
      return builder.toString();
    } catch (Exception e) {
      return Integer.toHexString(Math.abs(StrUtil.blankToDefault(node, "").hashCode()));
    }
  }

  private void ensureTimeseries(
      String devicePath, List<String> measurements, List<TSDataType> types) throws Exception {
    if (StrUtil.isBlank(devicePath)
        || measurements == null
        || measurements.isEmpty()
        || types == null
        || types.isEmpty()) {
      return;
    }
    // Include the measurement set: a property's first value may be nonnumeric and a later
    // value numeric, which must still create numeric_value when automatic creation is disabled.
    String schemaKey = devicePath + "|" + String.join(",", measurements);
    if (timeseriesCache.containsKey(schemaKey)) {
      return;
    }

    ReentrantLock lock = timeseriesLocks.computeIfAbsent(devicePath, key -> new ReentrantLock());
    lock.lock();
    try {
      if (timeseriesCache.containsKey(schemaKey)) {
        return;
      }

      executeWithRetry(
          () -> {
            Session currentSession = getSession();
            if (currentSession == null) {
              throw new IllegalStateException("IoTDB Session 不可用");
            }

            for (int index = 0; index < measurements.size() && index < types.size(); index++) {
              createTimeseriesIfMissing(
                  currentSession, devicePath + "." + measurements.get(index), types.get(index));
            }
            return null;
          },
          "createTimeseries",
          devicePath,
          devicePath);

      timeseriesCache.put(schemaKey, Boolean.TRUE);
    } finally {
      lock.unlock();
    }
  }

  private void createTimeseriesIfMissing(
      Session currentSession, String fullPath, TSDataType dataType)
      throws IoTDBConnectionException, StatementExecutionException {
    if (currentSession.checkTimeseriesExists(fullPath)) {
      return;
    }

    try {
      currentSession.createTimeseries(
          fullPath, dataType, resolveEncoding(dataType), resolveCompressionType());
      log.debug("IoTDB自动创建时序成功: path={}, dataType={}", fullPath, dataType);
    } catch (StatementExecutionException e) {
      if (isTimeseriesAlreadyExists(e)) {
        log.debug("IoTDB时序已存在，忽略重复创建: path={}", fullPath);
        return;
      }
      throw e;
    }
  }

  private TSEncoding resolveEncoding(TSDataType dataType) {
    return switch (dataType) {
      case INT32, INT64 -> TSEncoding.RLE;
      default -> TSEncoding.PLAIN;
    };
  }

  private CompressionType resolveCompressionType() {
    try {
      return CompressionType.valueOf(
          StrUtil.blankToDefault(compressorConfig, CompressionType.SNAPPY.name())
              .trim()
              .toUpperCase());
    } catch (Exception e) {
      log.warn("IoTDB压缩算法配置非法，回退为 SNAPPY: {}", compressorConfig);
      return CompressionType.SNAPPY;
    }
  }

  private boolean isTimeseriesAlreadyExists(Exception e) {
    if (e == null || e.getMessage() == null) {
      return false;
    }
    String message = e.getMessage().toLowerCase();
    return message.contains("already exist") || message.contains("path already exist");
  }

  private String resolvePathNode(String path, String tag) {
    if (StrUtil.isBlank(path) || StrUtil.isBlank(tag)) {
      return "";
    }
    int idx = path.indexOf(tag);
    if (idx < 0) {
      return "";
    }
    String sub = path.substring(idx + tag.length());
    int dot = sub.indexOf('.');
    if (dot >= 0) {
      return sub.substring(0, dot);
    }
    return sub;
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery) {
    logQuery.normalizePage();
    long deadline = historyDeadline();
    try {
      Session currentSession = requireHistorySession();
      String queryPath = buildLogQueryPath(logQuery);
      requireHistoryPath(queryPath, logQuery.isCursorMode());
      List<String> conditions = buildLogConditions(logQuery);
      long total = -1;
      if (!logQuery.isCursorMode()) {
        try (SessionDataSet data =
            historyQuery(currentSession, buildCountSql(queryPath, conditions), deadline)) {
          total = sumCountValues(data);
        }
      }
      appendCursorTime(conditions, logQuery);
      String sql =
          "SELECT log_id, message_type, content, event, command_id, command_status, point, "
              + "device_name, iot_id, device_id, product_key FROM "
              + queryPath
              + toWhereClause(conditions)
              + " ORDER BY time DESC LIMIT "
              + (logQuery.isCursorMode() ? logQuery.getFetchSize() : logQuery.getPageSize())
              + (logQuery.isCursorMode()
                  ? ""
                  : " OFFSET " + resolveOffset(logQuery.getPageNum(), logQuery.getPageSize()));
      List<IoTDeviceLogVO> rows = new ArrayList<>();
      try (SessionDataSet data = historyQuery(currentSession, sql, deadline)) {
        while (data.hasNext()) {
          rows.add(
              toLogVO(
                  data.next(),
                  logQuery.getIotId(),
                  logQuery.getDeviceId(),
                  logQuery.getProductKey()));
        }
      }
      if (logQuery.isCursorMode()) {
        return HistoryPageSupport.page(
            rows, logQuery, IoTDeviceLogVO::getCreateTime, IoTDeviceLogVO::getId);
      }
      return new PageBean<>(rows, total, logQuery.getPageSize(), logQuery.getPageNum());
    } catch (Exception e) {
      throw historyFailure("设备日志", e);
    }
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    try {
      Session currentSession = getSession();
      if (currentSession == null) {
        log.warn("IoTDB session不可用且重连失败，返回null");
        return null;
      }

      String queryPath = buildLogQueryPath(logQuery);
      if (StrUtil.isBlank(queryPath) || StrUtil.isBlank(logQuery.getId())) {
        log.warn("IoTDB查询日志详情参数不足，拒绝执行全量扫描");
        return null;
      }

      Long requestedId = parseLongText(logQuery.getId());
      if (requestedId == null) {
        log.warn("IoTDB查询日志详情ID非法: {}", logQuery.getId());
        return null;
      }

      List<String> conditions = buildLogConditions(logQuery);
      conditions.add(
          "(log_id=" + requestedId + " OR time=" + resolveEpochMillis(requestedId) + ")");

      String sql =
          "SELECT log_id, message_type, content, event, command_id, command_status, point,"
              + " device_name, iot_id, device_id, product_key FROM "
              + queryPath
              + toWhereClause(conditions)
              + " ORDER BY time DESC LIMIT 1";

      log.debug("IoTDB查询日志详情SQL: {}", sql);

      try {
        SessionDataSet dataSet = currentSession.executeQueryStatement(sql);
        try {
          if (dataSet.hasNext()) {
            return toLogVO(
                dataSet.next(),
                logQuery.getIotId(),
                logQuery.getDeviceId(),
                logQuery.getProductKey());
          }
        } finally {
          dataSet.closeOperationHandle();
        }
        return null;

      } catch (IoTDBConnectionException | StatementExecutionException e) {
        log.error("IoTDB执行日志详情查询失败: sql={}", sql, e);
        if (isReconnectNeeded(e)) {
          invalidateSession();
        }
        return null;
      }

    } catch (Exception e) {
      log.error("IoTDB查询设备日志详情失败", e);
      return null;
    }
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    List<IoTDeviceEvents> list = selectDevEvents(productKey);
    long deadline = historyDeadline();
    try {
      Session currentSession = requireHistorySession();
      for (IoTDeviceEvents devEvent : list) {
        String queryPath = buildEventQueryPath(productKey, iotId, devEvent.getId());
        requireHistoryPath(queryPath, true);
        List<String> conditions = new ArrayList<>();
        appendTextCondition(conditions, "product_key", productKey);
        appendTextCondition(conditions, "iot_id", iotId);
        EventStats stats = queryEventSeriesStats(currentSession, queryPath, conditions, deadline);
        devEvent.setQty(stats.count() >= 100 ? "99+" : String.valueOf(stats.count()));
        devEvent.setTime(
            stats.count() > 0
                ? java.time.Instant.ofEpochMilli(stats.latestTimestamp()).toString()
                : "");
      }
      return new PageBean<>(list, (long) list.size(), 1, 100);
    } catch (Exception error) {
      throw historyFailure("事件统计", error);
    }
  }

  private EventStats queryEventSeriesStats(
      Session currentSession, String queryPath, List<String> conditions, long deadline)
      throws IoTDBConnectionException, StatementExecutionException {
    // The UI caps counts at 99+. Reading at most 100 latest values gives the exact
    // displayed count and timestamp without scanning all historical event rows.
    String sql =
        "SELECT content FROM "
            + queryPath
            + toWhereClause(conditions)
            + " ORDER BY time DESC LIMIT 100";
    long count = 0;
    long latest = 0;
    try (SessionDataSet data = historyQuery(currentSession, sql, deadline)) {
      while (data.hasNext()) {
        RowRecord row = data.next();
        if (count++ == 0) latest = row.getTimestamp();
      }
    }
    return new EventStats(count, latest);
  }

  private record EventStats(long count, long latestTimestamp) {}

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
    logQuery.normalizePage();
    long deadline = historyDeadline();
    try {
      Session currentSession = requireHistorySession();
      List<String> queryPaths = buildMetadataQueryPaths(logQuery);
      if (queryPaths.isEmpty() || (logQuery.isCursorMode() && queryPaths.size() != 1)) {
        throw new IoTException("请选择单个设备和属性或事件查询历史记录", 400);
      }
      List<IoTDeviceLogMetadataVO> rows = new ArrayList<>();
      long total = logQuery.isCursorMode() ? -1 : 0;
      List<String> conditions = buildMetadataConditions(logQuery);
      appendCursorTime(conditions, logQuery);
      int fetchSize =
          logQuery.isCursorMode()
              ? logQuery.getFetchSize()
              : resolveFetchSize(logQuery.getPageNum(), logQuery.getPageSize());
      for (String queryPath : queryPaths) {
        requireHistoryPath(queryPath, logQuery.isCursorMode());
        boolean propertySeries = queryPath.contains(".property.");
        if (!logQuery.isCursorMode()) {
          try (SessionDataSet data =
              historyQuery(
                  currentSession, buildMetadataCountSql(queryPath, conditions), deadline)) {
            total += sumCountValues(data);
          }
        }
        String columns =
            propertySeries
                ? "message_type, property, content, create_time, device_name, device_id, iot_id,"
                    + " product_key, ext1, ext2, ext3"
                : "message_type, event, content, create_time, device_name, device_id, iot_id,"
                    + " product_key, ext1";
        String sql =
            "SELECT "
                + columns
                + " FROM "
                + queryPath
                + toWhereClause(conditions)
                + " ORDER BY time DESC LIMIT "
                + fetchSize;
        try (SessionDataSet data = historyQuery(currentSession, sql, deadline)) {
          while (data.hasNext()) {
            rows.add(toMetadataVO(data.next(), propertySeries, logQuery));
          }
        }
      }
      rows.sort((left, right) -> right.getCreateTime().compareTo(left.getCreateTime()));
      if (logQuery.isCursorMode()) {
        return HistoryPageSupport.page(
            rows, logQuery, IoTDeviceLogMetadataVO::getCreateTime, IoTDeviceLogMetadataVO::getId);
      }
      return new PageBean<>(
          slicePage(rows, logQuery.getPageNum(), logQuery.getPageSize()),
          total,
          logQuery.getPageSize(),
          logQuery.getPageNum());
    } catch (Exception e) {
      throw historyFailure("属性或事件历史", e);
    }
  }

  /** Bounded returned samples by default; native full-range aggregation is explicitly requested. */
  @Override
  public PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query) {
    query.validateRange();
    long deadline = historyDeadline();
    try {
      Session currentSession = requireHistorySession();
      String path =
          buildMetadataBasePath(query) + ".property." + sanitizePathNode(query.getProperty());
      requireHistoryPath(path, true);
      if (query.isSampledQuery()) {
        return querySampledPropertyTrend(currentSession, path, query, deadline);
      }
      String window =
          " GROUP BY (["
              + Math.multiplyExact(query.getBeginCreateTime(), 1000L)
              + ","
              + Math.addExact(Math.multiplyExact(query.getEndCreateTime(), 1000L), 1L)
              + "),"
              + Math.multiplyExact(query.getIntervalSeconds(), 1000L)
              + "ms)";
      // Device and property are encoded in this exact path. Avoid redundant TEXT identity
      // filters, which would force a value scan instead of IoTDB's page/chunk statistics.
      String sql =
          "SELECT count(content) AS total_count, count(value_schema) AS typed_count, "
              + "sum(value_schema) AS valid_count, "
              + "count(numeric_value) AS numeric_count, min_value(numeric_value) AS numeric_min, "
              + "max_value(numeric_value) AS numeric_max, avg(numeric_value) AS numeric_avg FROM "
              + path
              + window;
      Map<Long, PropertyTrendVO.Bucket> buckets = new LinkedHashMap<>();
      boolean needsLegacyNumeric = false;
      boolean needsNumericFilter = false;
      try (SessionDataSet data = historyQuery(currentSession, sql, deadline)) {
        Map<String, Integer> columns = trendColumns(data);
        int rows = 0;
        while (data.hasNext()) {
          if (++rows > query.getMaxPoints()) {
            throw new IoTException("IoTDB趋势返回点数超过限制");
          }
          RowRecord record = data.next();
          long count = trendCount(record, columns, "total_count");
          if (count == 0) {
            continue;
          }
          PropertyTrendVO.Bucket bucket = new PropertyTrendVO.Bucket();
          bucket.setTime(record.getTimestamp());
          bucket.setCount(count);
          readNumericTrend(record, columns, bucket);
          buckets.put(bucket.getTime(), bucket);
          long typedCount = trendCount(record, columns, "typed_count");
          needsLegacyNumeric |= typedCount < count;
          needsNumericFilter |= trendCount(record, columns, "valid_count") < typedCount;
        }
      }
      if (needsLegacyNumeric || needsNumericFilter) {
        // Compatibility for pre-upgrade TEXT values. This stays inside the database and
        // is time-bounded, but operators should backfill typed values offline for large histories.
        String value = needsLegacyNumeric ? "CAST(content AS DOUBLE)" : "numeric_value";
        String numericFilter =
            needsLegacyNumeric ? "content REGEXP '" + NUMERIC_PATTERN + "'" : "value_schema = 1";
        String legacySql =
            "SELECT count("
                + value
                + ") AS numeric_count, min_value("
                + value
                + ") AS numeric_min, max_value("
                + value
                + ") AS numeric_max, avg("
                + value
                + ") AS numeric_avg FROM "
                + path
                + " WHERE "
                + numericFilter
                + window;
        try (SessionDataSet data = historyQuery(currentSession, legacySql, deadline)) {
          Map<String, Integer> columns = trendColumns(data);
          int rows = 0;
          while (data.hasNext()) {
            if (++rows > query.getMaxPoints()) {
              throw new IoTException("IoTDB趋势返回点数超过限制");
            }
            RowRecord record = data.next();
            PropertyTrendVO.Bucket bucket = buckets.get(record.getTimestamp());
            if (bucket != null) {
              readNumericTrend(record, columns, bucket);
            }
          }
        }
      }
      return PropertyTrendVO.of(query, new ArrayList<>(buckets.values()));
    } catch (Exception e) {
      throw historyFailure("属性趋势", e);
    }
  }

  private PropertyTrendVO querySampledPropertyTrend(
      Session currentSession, String path, PropertyTrendQuery query, long deadline)
      throws IoTDBConnectionException, StatementExecutionException {
    long begin = Math.multiplyExact(query.getBeginCreateTime(), 1000L);
    long endExclusive = Math.addExact(Math.multiplyExact(query.getEndCreateTime(), 1000L), 1L);
    long mainInterval = Math.multiplyExact(query.getIntervalSeconds(), 1000L);
    int samplesPerWindow = query.getSamplesPerWindow();
    long fineStep = (mainInterval + samplesPerWindow - 1L) / samplesPerWindow;
    int fineWindows = Math.toIntExact((endExclusive - begin + fineStep - 1) / fineStep);
    if (fineWindows > query.getMaxScanRows()) {
      throw new IoTException("IoTDB趋势样本数超过限制", 503);
    }
    String sql =
        "SELECT SUBSTRING(last_value(content),1,1025) AS content, "
            + "max_time(content) AS original_time, count(content) AS source_count FROM "
            + path
            + " GROUP BY (["
            + begin
            + ","
            + endExclusive
            + "),"
            + fineStep
            + "ms) ORDER BY time DESC";
    // Aggregate before truncating TEXT so IoTDB can use file/chunk/page statistics. The
    // budget bounds returned candidates; boundary pages may decode more physical rows.
    List<PropertyTrendSampleVO> samples = new ArrayList<>(fineWindows);
    boolean downsampled = false;
    try (SessionDataSet data = historyQuery(currentSession, sql, deadline)) {
      Map<String, Integer> columns = trendColumns(data);
      Integer contentIndex = columns.get("content");
      Integer timeIndex = columns.get("original_time");
      Integer countIndex = columns.get("source_count");
      int rows = 0;
      while (data.hasNext()) {
        if (++rows > fineWindows) {
          throw new IoTException("IoTDB趋势返回样本数超过限制", 503);
        }
        requireHistoryDeadline(deadline);
        // A missing series has no rows and no projected columns in IoTDB. Validate
        // projected fields only when a record exists so that genuine empty history succeeds.
        if (contentIndex == null || timeIndex == null || countIndex == null) {
          throw new IoTException("IoTDB趋势返回无效采样字段", 503);
        }
        RowRecord row = data.next();
        requireHistoryDeadline(deadline);
        List<Field> fields = row.getFields();
        if (fields.size() <= Math.max(contentIndex, Math.max(timeIndex, countIndex))) {
          throw new IoTException("IoTDB趋势返回无效采样字段", 503);
        }
        Long sourceCount = parseLongValue(fields.get(countIndex));
        if (sourceCount == null || sourceCount < 0) {
          throw new IoTException("IoTDB趋势返回无效采样计数", 503);
        }
        if (sourceCount == 0) {
          continue;
        }
        Long originalTime = parseLongValue(fields.get(timeIndex));
        String content = getFieldValue(fields.get(contentIndex));
        long groupBegin = row.getTimestamp();
        if (content == null
            || originalTime == null
            || groupBegin < begin
            || groupBegin >= endExclusive
            || (groupBegin - begin) % fineStep != 0
            || originalTime < groupBegin
            || originalTime >= Math.min(endExclusive, groupBegin + fineStep)) {
          throw new IoTException("IoTDB趋势返回无效采样时间或值", 503);
        }
        // A fine group can straddle a chart window. Its latest value belongs to the
        // original timestamp's chart window, not the GROUP BY timestamp's window.
        int windowIndex = Math.toIntExact((originalTime - begin) / mainInterval);
        if (windowIndex < 0 || windowIndex >= query.getSampleWindowCount()) {
          throw new IoTException("IoTDB趋势返回无效采样窗口", 503);
        }
        samples.add(new PropertyTrendSampleVO(windowIndex, content));
        downsampled |= sourceCount > 1;
      }
    }
    requireHistoryDeadline(deadline);
    PropertyTrendVO trend = TrendSamplingSupport.aggregate(query, samples);
    if (downsampled) {
      trend.setSampled(true);
      trend.setCountExact(false);
    }
    requireHistoryDeadline(deadline);
    return trend;
  }

  private Map<String, Integer> trendColumns(SessionDataSet data) {
    Map<String, Integer> result = new LinkedHashMap<>();
    int fieldIndex = 0;
    for (String column : data.getColumnNames()) {
      if (!"Time".equalsIgnoreCase(column)) {
        result.put(column, fieldIndex++);
      }
    }
    return result;
  }

  private long trendCount(RowRecord record, Map<String, Integer> columns, String name) {
    Integer index = columns.get(name);
    if (index == null) {
      return 0;
    }
    Long count = parseLongValue(record.getFields().get(index));
    return count == null ? 0 : count;
  }

  private BigDecimal trendNumber(RowRecord record, Map<String, Integer> columns, String name) {
    Integer index = columns.get(name);
    String value = index == null ? null : getFieldValue(record.getFields().get(index));
    if (value == null) {
      return null;
    }
    if ("NaN".equalsIgnoreCase(value) || value.toLowerCase().contains("inf")) {
      throw new IoTException("IoTDB旧历史包含超出数值范围的数据，请迁移数值列后重试");
    }
    return new BigDecimal(value);
  }

  private void readNumericTrend(
      RowRecord record, Map<String, Integer> columns, PropertyTrendVO.Bucket bucket) {
    bucket.setNumericCount(trendCount(record, columns, "numeric_count"));
    bucket.setMin(trendNumber(record, columns, "numeric_min"));
    bucket.setMax(trendNumber(record, columns, "numeric_max"));
    bucket.setAvg(trendNumber(record, columns, "numeric_avg"));
  }

  static Double finiteNumericValue(String content) {
    if (content == null || !NUMERIC_VALUE.matcher(content).matches()) {
      return null;
    }
    try {
      double number = Double.parseDouble(content);
      return Double.isFinite(number) ? number : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Session requireHistorySession() {
    Session current = getSession();
    if (current == null) {
      throw new IoTException("IoTDB暂时不可用，请稍后重试");
    }
    return current;
  }

  private void requireHistoryPath(String path, boolean exact) {
    if (StrUtil.isBlank(path) || path.startsWith("null.") || (exact && path.contains("*"))) {
      throw new IoTException("请选择单个设备查询历史记录", 400);
    }
  }

  private long historyDeadline() {
    return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(1, historyQueryTimeoutMs));
  }

  private SessionDataSet historyQuery(Session current, String sql, long deadline)
      throws IoTDBConnectionException, StatementExecutionException {
    long remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
    if (remaining < 1) {
      throw new IoTException("IoTDB历史查询超时，请缩小时间范围后重试", 503);
    }
    return current.executeQueryStatement(sql, remaining);
  }

  private void requireHistoryDeadline(long deadline) {
    if (System.nanoTime() >= deadline) {
      throw new IoTException("IoTDB历史查询超时，请缩小时间范围后重试", 503);
    }
  }

  private void appendCursorTime(List<String> conditions, LogQuery query) {
    if (query.isCursorMode() && query.getBeforeTime() != null) {
      // Within one IoTDB device series the timestamp itself is the unique storage key.
      conditions.add(
          "time < "
              + query.getBeforeTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
  }

  private IoTException historyFailure(String operation, Exception cause) {
    if (cause instanceof IoTException error) {
      return error;
    }
    if (cause instanceof IoTDBConnectionException || isReconnectNeeded(cause)) {
      invalidateSession();
    }
    log.error("IoTDB{}查询失败", operation, cause);
    return new IoTException("IoTDB" + operation + "查询失败或超时，请缩小范围后重试", 503, cause);
  }

  @Override
  public String getPolicy() {
    return storePolicy;
  }

  /**
   * 查询最近的指令记录 使用 productKey 和 deviceId 构建精确的查询路径 路径格式: root.device.{productKey}.{deviceId}.log
   * 注意：IoTDB中未存储iot_id字段，路径已精确定位设备，无需额外过滤
   */
  @Override
  public IoTDeviceLog getRecentCommand(
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    DeviceLogQuery query =
        new DeviceLogQuery.Builder()
            .productKey(productKey)
            .deviceId(deviceId)
            .commandId(commandId)
            .messageType(messageType)
            .startTime(createTime)
            .commandStatus(commandStatus)
            .limit(1)
            .build();
    List<IoTDeviceLog> logs = queryDeviceLogs(query);
    return logs.isEmpty() ? null : logs.get(0);
  }

  /** 通用设备日志查询接口 注意：IoTDB路径格式为 root.device.{productKey}.{deviceId}.log */
  @Override
  public List<IoTDeviceLog> queryDeviceLogs(DeviceLogQuery query) {
    try {
      String queryPath = buildLogQueryPath(query);
      if (StrUtil.isBlank(queryPath)) {
        log.warn(
            "IoTDB无法构建查询路径: productKey={}, deviceId={}, iotId={}",
            query.getProductKey(),
            query.getDeviceId(),
            query.getIotId());
        return Collections.emptyList();
      }

      List<String> conditions = buildLogConditions(query);
      StringBuilder sqlBuilder = new StringBuilder();
      sqlBuilder
          .append(
              "SELECT log_id, message_type, content, event, command_id, command_status, point,"
                  + " device_name, iot_id, device_id, product_key FROM ")
          .append(queryPath)
          .append(toWhereClause(conditions))
          .append(" ORDER BY time DESC");
      if (query.getLimit() > 0) {
        sqlBuilder.append(String.format(" LIMIT %d", query.getLimit()));
      }

      log.debug("IoTDB通用查询SQL: {}", sqlBuilder);

      return queryDeviceLogsFromIoTDB(sqlBuilder.toString());
    } catch (Exception e) {
      log.error("IoTDB通用查询异常: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  /** 从IoTDB查询设备日志 */
  private List<IoTDeviceLog> queryDeviceLogsFromIoTDB(String sql) {
    List<IoTDeviceLog> logs = new ArrayList<>();

    Session session = null;
    try {
      session = getSession(); // 获取IoTDB会话
      if (session == null) {
        log.warn("IoTDB session不可用，返回空结果: sql={}", sql);
        return logs;
      }

      // 执行查询
      SessionDataSet dataSet = session.executeQueryStatement(sql);
      List<String> columnNames = dataSet.getColumnNames();

      // 处理结果集
      while (dataSet.hasNext()) {
        RowRecord record = dataSet.next();
        IoTDeviceLog deviceLog = new IoTDeviceLog();

        // 设置时间戳（从RowRecord直接获取）
        long timestamp = record.getTimestamp();
        deviceLog.setId(timestamp);
        deviceLog.setCreateTime(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));

        // 从记录中提取字段并设置到IoTDeviceLog对象中
        // 注意：columnNames包含"Time"在索引0，但fields不包含时间戳值
        // 所以columnNames和fields的索引有偏移
        List<Field> fields = record.getFields();
        int fieldIndex = 0;
        for (int i = 0; i < columnNames.size() && fieldIndex < fields.size(); i++) {
          String fullColumnName = columnNames.get(i);

          // IoTDB返回的列名是完整路径，如: root.device.xxx.log.command_status
          // 需要提取最后一部分作为字段名
          String columnName = extractFieldName(fullColumnName);

          // 跳过Time列，因为fields中不包含时间戳值
          if ("time".equalsIgnoreCase(columnName)) {
            continue;
          }

          Field field = fields.get(fieldIndex);
          fieldIndex++;
          String value = getFieldValue(field);

          switch (columnName.toLowerCase()) {
            case "log_id":
              Long logId = parseLongText(value);
              if (logId != null) {
                deviceLog.setId(logId);
              }
              break;
            case "iot_id":
              deviceLog.setIotId(value);
              break;
            case "device_id":
              deviceLog.setDeviceId(value);
              break;
            case "product_key":
              deviceLog.setProductKey(value);
              break;
            case "device_name":
              deviceLog.setDeviceName(value);
              break;
            case "message_type":
              deviceLog.setMessageType(value);
              break;
            case "command_id":
              deviceLog.setCommandId(value);
              break;
            case "command_status":
              if (value != null && !value.isEmpty()) {
                try {
                  // 兼容 "0.0" 这种格式
                  Double doubleValue = Double.parseDouble(value);
                  deviceLog.setCommandStatus(doubleValue.intValue());
                } catch (NumberFormatException e) {
                  log.warn("IoTDB解析command_status失败: {}", value);
                }
              }
              break;
            case "event":
              deviceLog.setEvent(value);
              break;
            case "content":
              deviceLog.setContent(value);
              break;
            case "point":
              deviceLog.setPoint(value);
              break;
          }
        }

        if (deviceLog.getId() == null) {
          deviceLog.setId(timestamp);
        }

        logs.add(deviceLog);
      }

      dataSet.close();
    } catch (Exception e) {
      log.error("IoTDB查询设备日志异常: {}", e.getMessage(), e);
    } finally {
      // 注意：这里不要关闭session，因为它是共享的
    }

    return logs;
  }

  @Override
  public JSONObject configMetadata() {
    JSONObject config = new JSONObject();
    config.put("host", host);
    config.put("port", port);
    config.put("username", username);
    config.put("storageGroup", storageGroup);
    return config;
  }

  /** 从 Field 中获取字符串值 */
  private String getFieldValue(Field field) {
    if (field == null || field.getDataType() == null) {
      return null;
    }

    try {
      Object value = field.getObjectValue(field.getDataType());
      return value != null ? value.toString() : null;
    } catch (Exception e) {
      log.warn("IoTDB解析字段值失败", e);
      return null;
    }
  }

  private IoTDeviceLogVO toLogVO(
      RowRecord record, String fallbackIotId, String fallbackDeviceId, String fallbackProductKey) {
    IoTDeviceLogVO vo = new IoTDeviceLogVO();
    vo.setId(record.getTimestamp());
    vo.setCreateTime(
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(record.getTimestamp()), ZoneId.systemDefault()));

    List<Field> fields = record.getFields();
    if (fields.size() >= 11) {
      Long logId = parseLongValue(fields.get(0));
      vo.setId(logId != null ? logId : record.getTimestamp());
      vo.setMessageType(getFieldValue(fields.get(1)));
      vo.setContent(getFieldValue(fields.get(2)));
      vo.setEvent(getFieldValue(fields.get(3)));
      vo.setCommandId(getFieldValue(fields.get(4)));
      vo.setCommandStatus(parseIntValue(fields.get(5)));
      vo.setPoint(getFieldValue(fields.get(6)));
      vo.setDeviceName(getFieldValue(fields.get(7)));
      vo.setIotId(StrUtil.blankToDefault(getFieldValue(fields.get(8)), fallbackIotId));
      vo.setDeviceId(StrUtil.blankToDefault(getFieldValue(fields.get(9)), fallbackDeviceId));
      vo.setProductKey(StrUtil.blankToDefault(getFieldValue(fields.get(10)), fallbackProductKey));
    } else if (fields.size() >= 7) {
      vo.setMessageType(getFieldValue(fields.get(0)));
      vo.setContent(getFieldValue(fields.get(1)));
      vo.setEvent(getFieldValue(fields.get(2)));
      vo.setCommandId(getFieldValue(fields.get(3)));
      vo.setCommandStatus(parseIntValue(fields.get(4)));
      vo.setPoint(getFieldValue(fields.get(5)));
      vo.setDeviceName(getFieldValue(fields.get(6)));
      vo.setIotId(fallbackIotId);
      vo.setDeviceId(fallbackDeviceId);
      vo.setProductKey(fallbackProductKey);
    }

    return vo;
  }

  private IoTDeviceLogMetadataVO toMetadataVO(
      RowRecord record, boolean propertySeries, LogQuery logQuery) {
    IoTDeviceLogMetadataVO vo = new IoTDeviceLogMetadataVO();
    vo.setId(0L);
    vo.setCreateTime(
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(record.getTimestamp()), ZoneId.systemDefault()));

    List<Field> fields = record.getFields();
    if (propertySeries) {
      if (fields.size() >= 11) {
        vo.setMessageType(getFieldValue(fields.get(0)));
        vo.setProperty(
            StrUtil.blankToDefault(getFieldValue(fields.get(1)), logQuery.getProperty()));
        vo.setContent(getFieldValue(fields.get(2)));
        vo.setDeviceName(getFieldValue(fields.get(4)));
        vo.setDeviceId(
            StrUtil.blankToDefault(getFieldValue(fields.get(5)), logQuery.getDeviceId()));
        vo.setIotId(StrUtil.blankToDefault(getFieldValue(fields.get(6)), logQuery.getIotId()));
        vo.setProductKey(
            StrUtil.blankToDefault(getFieldValue(fields.get(7)), logQuery.getProductKey()));
        vo.setExt1(getFieldValue(fields.get(8)));
        vo.setExt2(getFieldValue(fields.get(9)));
        vo.setExt3(getFieldValue(fields.get(10)));
      }
      return vo;
    }

    if (fields.size() >= 9) {
      vo.setMessageType(getFieldValue(fields.get(0)));
      vo.setEvent(StrUtil.blankToDefault(getFieldValue(fields.get(1)), logQuery.getEvent()));
      vo.setContent(getFieldValue(fields.get(2)));
      vo.setDeviceName(getFieldValue(fields.get(4)));
      vo.setDeviceId(StrUtil.blankToDefault(getFieldValue(fields.get(5)), logQuery.getDeviceId()));
      vo.setIotId(StrUtil.blankToDefault(getFieldValue(fields.get(6)), logQuery.getIotId()));
      vo.setProductKey(
          StrUtil.blankToDefault(getFieldValue(fields.get(7)), logQuery.getProductKey()));
      vo.setExt1(getFieldValue(fields.get(8)));
    }
    return vo;
  }

  /** 从IoTDB完整列名中提取字段名 例如: root.device.xxx.log.command_status -> command_status */
  private String extractFieldName(String fullColumnName) {
    if (fullColumnName == null || fullColumnName.isEmpty()) {
      return "";
    }
    int lastDot = fullColumnName.lastIndexOf('.');
    if (lastDot >= 0 && lastDot < fullColumnName.length() - 1) {
      return fullColumnName.substring(lastDot + 1);
    }
    return fullColumnName;
  }

  /** 从 Field 中获取整型值 注意: IoTDB 可能返回浮点数格式（如 "0.0"），需要先转换为 Double 再取整 */
  private Integer parseIntValue(Field field) {
    String value = getFieldValue(field);
    if (value == null || value.isEmpty()) {
      return null;
    }

    try {
      // 先尝试转为 Double（兼容 "0.0" 这种格式）
      Double doubleValue = Double.parseDouble(value);
      return doubleValue.intValue();
    } catch (NumberFormatException e) {
      log.warn("IoTDB解析整型失败: {}", value);
      return null;
    }
  }

  private Long parseLongValue(Field field) {
    return parseLongText(getFieldValue(field));
  }

  private Long parseLongText(String value) {
    if (StrUtil.isBlank(value)) {
      return null;
    }

    try {
      return new BigDecimal(value).longValueExact();
    } catch (NumberFormatException e) {
      log.warn("IoTDB解析长整型失败: {}", value);
      return null;
    }
  }

  private String buildLogQueryPath(LogQuery logQuery) {
    return buildLogQueryPath(logQuery.getProductKey(), logQuery.getDeviceId(), logQuery.getIotId());
  }

  private String buildLogQueryPath(DeviceLogQuery query) {
    return buildLogQueryPath(query.getProductKey(), query.getDeviceId(), query.getIotId());
  }

  private String buildLogQueryPath(String productKey, String deviceId, String iotId) {
    if (StrUtil.isNotBlank(productKey) && StrUtil.isNotBlank(deviceId)) {
      return buildDevicePath(productKey, deviceId) + ".log";
    }

    String[] parsed = parseIotId(iotId, productKey);
    if (parsed != null && parsed.length == 2) {
      return buildDevicePath(parsed[0], parsed[1]) + ".log";
    }

    if (StrUtil.isNotBlank(productKey)) {
      return storageGroup + "." + sanitizePathNode(productKey) + ".*.log";
    }

    if (StrUtil.isNotBlank(iotId) || StrUtil.isNotBlank(deviceId)) {
      return storageGroup + ".*.*.log";
    }

    return null;
  }

  private String buildMetadataBasePath(LogQuery logQuery) {
    if (StrUtil.isNotBlank(logQuery.getProductKey())
        && StrUtil.isNotBlank(logQuery.getDeviceId())) {
      return buildDevicePath(logQuery.getProductKey(), logQuery.getDeviceId());
    }

    String[] parsed = parseIotId(logQuery.getIotId(), logQuery.getProductKey());
    if (parsed != null && parsed.length == 2) {
      return buildDevicePath(parsed[0], parsed[1]);
    }

    if (StrUtil.isNotBlank(logQuery.getProductKey())) {
      return storageGroup + "." + sanitizePathNode(logQuery.getProductKey()) + ".*";
    }

    if (StrUtil.isNotBlank(logQuery.getIotId()) || StrUtil.isNotBlank(logQuery.getDeviceId())) {
      return storageGroup + ".*.*";
    }

    return null;
  }

  private String buildEventQueryPath(String productKey, String iotId, String event) {
    if (StrUtil.isBlank(event)) {
      return null;
    }

    String[] parsed = parseIotId(iotId, productKey);
    if (parsed != null && parsed.length == 2) {
      return buildDevicePath(parsed[0], parsed[1]) + ".event." + sanitizePathNode(event);
    }

    if (StrUtil.isNotBlank(productKey)) {
      return storageGroup
          + "."
          + sanitizePathNode(productKey)
          + ".*.event."
          + sanitizePathNode(event);
    }

    if (StrUtil.isNotBlank(iotId)) {
      return storageGroup + ".*.*.event." + sanitizePathNode(event);
    }

    return null;
  }

  private List<String> buildMetadataQueryPaths(LogQuery logQuery) {
    String basePath = buildMetadataBasePath(logQuery);
    if (StrUtil.isBlank(basePath)) {
      return Collections.emptyList();
    }

    List<String> queryPaths = new ArrayList<>();
    boolean wantProperty =
        MessageType.PROPERTIES.name().equalsIgnoreCase(logQuery.getMessageType())
            || StrUtil.isNotBlank(logQuery.getProperty());
    boolean wantEvent =
        MessageType.EVENT.name().equalsIgnoreCase(logQuery.getMessageType())
            || StrUtil.isNotBlank(logQuery.getEvent());

    if (!wantProperty && !wantEvent) {
      queryPaths.add(basePath + ".property.*");
      queryPaths.add(basePath + ".event.*");
      return queryPaths;
    }

    if (wantProperty) {
      queryPaths.add(
          StrUtil.isNotBlank(logQuery.getProperty())
              ? basePath + ".property." + sanitizePathNode(logQuery.getProperty())
              : basePath + ".property.*");
    }
    if (wantEvent) {
      queryPaths.add(
          StrUtil.isNotBlank(logQuery.getEvent())
              ? basePath + ".event." + sanitizePathNode(logQuery.getEvent())
              : basePath + ".event.*");
    }
    return queryPaths;
  }

  private List<String> buildLogConditions(LogQuery logQuery) {
    List<String> conditions = new ArrayList<>();
    appendTextCondition(conditions, "product_key", logQuery.getProductKey());
    appendTextCondition(conditions, "device_id", logQuery.getDeviceId());
    appendTextCondition(conditions, "iot_id", logQuery.getIotId());
    appendTextCondition(conditions, "message_type", logQuery.getMessageType());
    appendTextCondition(conditions, "event", logQuery.getEvent());
    appendTimeConditions(conditions, logQuery.getBeginCreateTime(), logQuery.getEndCreateTime());
    return conditions;
  }

  private List<String> buildLogConditions(DeviceLogQuery query) {
    List<String> conditions = new ArrayList<>();
    appendTextCondition(conditions, "product_key", query.getProductKey());
    appendTextCondition(conditions, "device_id", query.getDeviceId());
    appendTextCondition(conditions, "iot_id", query.getIotId());
    appendTextCondition(conditions, "message_type", query.getMessageType());
    appendTextCondition(conditions, "command_id", query.getCommandId());
    appendTextCondition(conditions, "event", query.getEventType());
    appendNumericCondition(conditions, "command_status", query.getCommandStatus());
    appendNumericCondition(conditions, "log_id", query.getLogId());
    appendTimeConditions(conditions, query.getStartTime(), query.getEndTime());
    return conditions;
  }

  private List<String> buildMetadataConditions(LogQuery logQuery) {
    List<String> conditions = new ArrayList<>();
    appendTextCondition(conditions, "product_key", logQuery.getProductKey());
    appendTextCondition(conditions, "device_id", logQuery.getDeviceId());
    appendTextCondition(conditions, "iot_id", logQuery.getIotId());
    appendTextCondition(conditions, "message_type", logQuery.getMessageType());
    appendTextCondition(conditions, "property", logQuery.getProperty());
    appendTextCondition(conditions, "event", logQuery.getEvent());
    appendTimeConditions(conditions, logQuery.getBeginCreateTime(), logQuery.getEndCreateTime());
    return conditions;
  }

  private void appendTextCondition(List<String> conditions, String field, String value) {
    if (StrUtil.isNotBlank(value)) {
      conditions.add(field + "='" + escapeValue(value) + "'");
    }
  }

  private void appendNumericCondition(List<String> conditions, String field, Number value) {
    if (value != null) {
      conditions.add(field + "=" + value);
    }
  }

  private void appendTimeConditions(List<String> conditions, Long beginTime, Long endTime) {
    if (beginTime != null) {
      conditions.add("time >= " + toMillisTimestamp(beginTime));
    }
    if (endTime != null) {
      conditions.add("time <= " + toMillisTimestamp(endTime));
    }
  }

  private String toWhereClause(List<String> conditions) {
    if (CollectionUtil.isEmpty(conditions)) {
      return "";
    }
    return " WHERE " + String.join(" AND ", conditions);
  }

  private long sumCountValues(SessionDataSet dataSet)
      throws IoTDBConnectionException, StatementExecutionException {
    long total = 0;
    while (dataSet.hasNext()) {
      RowRecord countRecord = dataSet.next();
      for (Field field : countRecord.getFields()) {
        Long value = parseLongValue(field);
        if (value != null) {
          total += value;
        }
      }
    }
    return total;
  }

  private int resolveOffset(Integer pageNum, Integer pageSize) {
    int normalizedPageNum = pageNum == null || pageNum <= 0 ? 1 : pageNum;
    int normalizedPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
    return (normalizedPageNum - 1) * normalizedPageSize;
  }

  private int resolveFetchSize(Integer pageNum, Integer pageSize) {
    int offset = resolveOffset(pageNum, pageSize);
    int normalizedPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
    return offset + normalizedPageSize;
  }

  private <T> List<T> slicePage(List<T> source, Integer pageNum, Integer pageSize) {
    int offset = resolveOffset(pageNum, pageSize);
    int normalizedPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
    if (offset >= source.size()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(
        source.subList(offset, Math.min(offset + normalizedPageSize, source.size())));
  }

  /** 构建统计SQL */
  private String buildCountSql(String queryPath, List<String> conditions) {
    return "SELECT COUNT(message_type) FROM " + queryPath + toWhereClause(conditions);
  }

  /** 构建元数据统计SQL */
  private String buildMetadataCountSql(String queryPath, List<String> conditions) {
    return "SELECT COUNT(message_type) FROM " + queryPath + toWhereClause(conditions);
  }
}
