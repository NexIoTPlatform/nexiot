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

package cn.universal.core.engine.extend;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IoTRedisFunctions {

  @Resource private StringRedisTemplate stringRedisTemplate;
  private static final String DEVICE_PREFIX = "nexiot:device:";

  // 解析时间字符串为毫秒 (支持格式: 10s, 5m, 1h)
  private long parseTimeout(String timeoutStr) {
    if (timeoutStr == null || timeoutStr.isEmpty()) {
      return 0;
    }

    String unit = timeoutStr.replaceAll("\\d", "").toLowerCase();
    long value = Long.parseLong(timeoutStr.replaceAll("[^0-9]", ""));

    switch (unit) {
      case "ms":
        return value;
      case "s":
        return value * 1000;
      case "m":
        return value * 60 * 1000;
      case "h":
        return value * 60 * 60 * 1000;
      case "d":
        return value * 24 * 60 * 60 * 1000;
      default:
        return value * 1000; // 默认秒
    }
  }

  // ==================== 基础数据操作 ====================
  @Function
  @Comment("设置字符串值")
  public void set(String key, String value, String expire) {
    try {
      stringRedisTemplate
          .opsForValue()
          .set(key, value, parseTimeout(expire), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      log.error("设置异常 - Key: {}", key, e);
    }
  }

  @Function
  @Comment("获取字符串值")
  public String get(String key) {
    try {
      return stringRedisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("获取异常 - Key: {}", key, e);
      return null;
    }
  }

  @Function
  @Comment("设置Hash字段")
  public void hset(String key, String field, String value, String expire) {
    try {
      stringRedisTemplate.opsForHash().put(key, field, value);
      stringRedisTemplate.expire(key, parseTimeout(expire), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      log.error("Hash设置异常 - Key: {}", key, e);
    }
  }

  @Function
  @Comment("获取Hash字段")
  public String hget(String key, String field) {
    try {
      Object val = stringRedisTemplate.opsForHash().get(key, field);
      return val != null ? val.toString() : null;
    } catch (Exception e) {
      log.error("Hash获取异常 - Key: {}", key, e);
      return null;
    }
  }

  // ==================== 物联网设备操作 ====================
  @Function
  @Comment("更新设备状态")
  public void updateStatus(String deviceId, String field, String value, String expire) {
    String key = DEVICE_PREFIX + "status:" + deviceId;
    hset(key, field, value, expire);
  }

  @Function
  @Comment("获取设备状态")
  public Map<String, String> getStatus(String deviceId) {
    String key = DEVICE_PREFIX + "status:" + deviceId;
    try {
      Map<Object, Object> raw = stringRedisTemplate.opsForHash().entries(key);
      Map<String, String> result = new HashMap<>();
      raw.forEach((k, v) -> result.put(k.toString(), v.toString()));
      return result;
    } catch (Exception e) {
      log.error("状态获取异常 - Device: {}", deviceId, e);
      return Collections.emptyMap();
    }
  }

  @Function
  @Comment("推送设备指令")
  public void pushCommand(String deviceId, String type, String params, String expire) {
    String key = DEVICE_PREFIX + "cmd:" + deviceId;
    String command =
        String.format(
            "{\"type\":\"%s\",\"params\":%s,\"ts\":%d}", type, params, System.currentTimeMillis());

    stringRedisTemplate.executePipelined(
        (RedisCallback<Object>)
            conn -> {
              conn.rPush(key.getBytes(), command.getBytes());
              conn.expire(key.getBytes(), TimeUnit.MILLISECONDS.toSeconds(parseTimeout(expire)));
              return null;
            });
  }

  @Function
  @Comment("获取并移除指令")
  public String popCommand(String deviceId) {
    return stringRedisTemplate.opsForList().leftPop(DEVICE_PREFIX + "cmd:" + deviceId);
  }

  // ==================== 地理位置操作 ====================
  @Function
  @Comment("记录设备位置")
  public void recordLocation(String deviceId, double lng, double lat) {
    stringRedisTemplate
        .opsForGeo()
        .add(
            DEVICE_PREFIX + "geo:devices",
            new RedisGeoCommands.GeoLocation<>(deviceId, new Point(lng, lat)));
  }

  @Function
  @Comment("获取设备位置")
  public Point getLocation(String deviceId) {
    List<Point> points =
        stringRedisTemplate.opsForGeo().position(DEVICE_PREFIX + "geo:devices", deviceId);
    return (points != null && !points.isEmpty()) ? points.get(0) : null;
  }

  // ==================== 分布式锁实现 ====================
  private static final String LOCK_SCRIPT =
      "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then "
          + "   redis.call('pexpire', KEYS[1], ARGV[2]) "
          + "   return 1 "
          + "else return 0 end";

  private static final String UNLOCK_SCRIPT =
      "if redis.call('get', KEYS[1]) == ARGV[1] then "
          + "   return redis.call('del', KEYS[1]) "
          + "else return 0 end";

  @Function
  @Comment("获取设备锁")
  public boolean acquireLock(String deviceId, String lockId, String expire) {
    String key = DEVICE_PREFIX + "lock:" + deviceId;
    try {
      RedisScript<Boolean> script = new DefaultRedisScript<>(LOCK_SCRIPT, Boolean.class);
      return Boolean.TRUE.equals(
          stringRedisTemplate.execute(
              script,
              Collections.singletonList(key),
              lockId,
              String.valueOf(parseTimeout(expire))));
    } catch (Exception e) {
      log.error("锁获取异常 - Device: {}", deviceId, e);
      return false;
    }
  }

  @Function
  @Comment("释放设备锁")
  public boolean releaseLock(String deviceId, String lockId) {
    String key = DEVICE_PREFIX + "lock:" + deviceId;
    try {
      RedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
      Long result = stringRedisTemplate.execute(script, Collections.singletonList(key), lockId);
      return result != null && result == 1;
    } catch (Exception e) {
      log.error("锁释放异常 - Device: {}", deviceId, e);
      return false;
    }
  }

  @Function
  @Comment("检查锁状态")
  public boolean checkLock(String deviceId, String lockId) {
    String key = DEVICE_PREFIX + "lock:" + deviceId;
    String current = stringRedisTemplate.opsForValue().get(key);
    return lockId.equals(current);
  }

  // ==================== 数据历史记录 ====================
  @Function
  @Comment("缓存设备数据")
  public void cacheData(String deviceId, String dataType, String value, String expire) {
    String currentKey = DEVICE_PREFIX + "data:current:" + deviceId + ":" + dataType;
    String lastValue = stringRedisTemplate.opsForValue().get(currentKey);

    if (lastValue != null) {
      String historyKey = DEVICE_PREFIX + "data:history:" + deviceId + ":" + dataType;
      stringRedisTemplate.executePipelined(
          (RedisCallback<Object>)
              conn -> {
                conn.zAdd(historyKey.getBytes(), System.currentTimeMillis(), lastValue.getBytes());
                conn.zRemRange(historyKey.getBytes(), 0, -101);
                return null;
              });
    }
    set(currentKey, value, expire);
  }

  @Function
  @Comment("获取当前数据")
  public String getCurrentData(String deviceId, String dataType) {
    return get(DEVICE_PREFIX + "data:current:" + deviceId + ":" + dataType);
  }
}
