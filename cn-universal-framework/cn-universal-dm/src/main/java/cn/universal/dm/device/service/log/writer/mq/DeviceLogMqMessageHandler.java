package cn.universal.dm.device.service.log.writer.mq;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.log.DefaultIoTDeviceLogService;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRecord;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeviceLogMqMessageHandler {

  private static final String PROCESSING = "processing";
  private static final String DONE = "done";
  private static final DefaultRedisScript<Long> RELEASE_PROCESSING_SCRIPT =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('del', KEYS[1]) else return 0 end",
          Long.class);
  private static final DefaultRedisScript<Long> MARK_DONE_SCRIPT =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); return 1 else return 0 end",
          Long.class);

  private final ObjectProvider<DefaultIoTDeviceLogService> logServiceProvider;
  private final StringRedisTemplate redisTemplate;
  private final boolean dedupEnabled;
  private final String dedupKeyPrefix;
  private final Duration processingTtl;
  private final Duration doneTtl;

  public DeviceLogMqMessageHandler(
      ObjectProvider<DefaultIoTDeviceLogService> logServiceProvider,
      StringRedisTemplate redisTemplate,
      @Value("${nexiot.device-log.mq.dedup-enabled:false}") boolean dedupEnabled,
      @Value("${nexiot.device-log.mq.dedup-key-prefix:nexiot:device-log:event:}")
          String dedupKeyPrefix,
      @Value("${nexiot.device-log.mq.processing-ttl-seconds:300}") long processingTtlSeconds,
      @Value("${nexiot.device-log.mq.dedup-ttl-days:7}") long doneTtlDays) {
    this.logServiceProvider = logServiceProvider;
    this.redisTemplate = redisTemplate;
    this.dedupEnabled = dedupEnabled;
    this.dedupKeyPrefix = dedupKeyPrefix;
    this.processingTtl = Duration.ofSeconds(Math.max(30L, processingTtlSeconds));
    this.doneTtl = Duration.ofDays(Math.max(1L, doneTtlDays));
  }

  public void handle(DeviceLogWriteRecord record) {
    handleBatch(List.of(record));
  }

  public void handleBatch(List<DeviceLogWriteRecord> records) {
    if (records == null || records.isEmpty()) {
      return;
    }
    records.forEach(this::validate);
    List<LockedRecord> lockedRecords = new ArrayList<>(records.size());
    try {
      for (DeviceLogWriteRecord record : records) {
        String key = dedupKeyPrefix + record.eventId();
        if (acquire(key)) {
          lockedRecords.add(new LockedRecord(key, record));
          continue;
        }
        String state = dedupState(key);
        if (!DONE.equals(state)) {
          throw new IllegalStateException(
              "Device-log event is already being processed: " + record.eventId());
        }
      }
      if (lockedRecords.isEmpty()) {
        return;
      }
      logServiceProvider.getObject().persistMqRecords(
          lockedRecords.stream().map(LockedRecord::record).toList());
    } catch (RuntimeException ex) {
      lockedRecords.forEach(locked -> releaseProcessingKey(locked.key()));
      throw ex;
    }
    lockedRecords.forEach(locked -> markDone(locked.key()));
  }

  private void validate(DeviceLogWriteRecord record) {
    if (record == null
        || record.schemaVersion() != DeviceLogWriteRecord.CURRENT_SCHEMA_VERSION
        || StrUtil.isBlank(record.eventId())) {
      throw new IllegalArgumentException("Invalid device-log MQ event");
    }
  }

  private boolean acquire(String key) {
    if (!dedupEnabled) {
      return true;
    }
    try {
      return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, PROCESSING, processingTtl));
    } catch (Exception ex) {
      log.warn("Device-log dedup unavailable; continuing eventId={}, err={}", key, ex.getMessage());
      return true;
    }
  }

  private String dedupState(String key) {
    if (!dedupEnabled) {
      return null;
    }
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception ex) {
      return null;
    }
  }

  private void releaseProcessingKey(String key) {
    if (!dedupEnabled) {
      return;
    }
    try {
      redisTemplate.execute(RELEASE_PROCESSING_SCRIPT, List.of(key), PROCESSING);
    } catch (Exception ex) {
      log.warn("Device-log dedup cleanup failed key={}, err={}", key, ex.getMessage());
    }
  }

  private void markDone(String key) {
    if (!dedupEnabled) {
      return;
    }
    try {
      Long updated =
          redisTemplate.execute(
              MARK_DONE_SCRIPT,
              List.of(key),
              PROCESSING,
              DONE,
              String.valueOf(doneTtl.toMillis()));
      if (!Long.valueOf(1L).equals(updated)) {
        log.warn("Device-log dedup completion marker lost ownership key={}", key);
      }
    } catch (Exception ex) {
      log.error(
          "Device-log persisted but dedup completion marker failed key={}, err={}",
          key,
          ex.getMessage());
    }
  }

  private record LockedRecord(String key, DeviceLogWriteRecord record) {}
}
