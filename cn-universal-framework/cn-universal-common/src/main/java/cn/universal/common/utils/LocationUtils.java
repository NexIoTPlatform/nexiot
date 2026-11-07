package cn.universal.common.utils;

import cn.hutool.core.util.StrUtil;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.lionsoul.ip2region.xdb.Searcher;

/**
 * IP地址地理位置解析工具类（基于IP2Region增强版） 优化点：解决文件损坏问题、增强容错性、完善缓存机制
 *
 * @author gitee.com/NexIoT
 */
@Slf4j
public class LocationUtils {

  // IP2Region 数据库文件名（建议使用官网最新版）
  private static final String IP2REGION_FILE_NAME = "ip2region_v4.xdb";
  // 缓存默认过期时间（1小时）
  private static final long CACHE_EXPIRE_MILLIS = TimeUnit.HOURS.toMillis(1);
  // 最大缓存数量（防止内存溢出）
  private static final int MAX_CACHE_SIZE = 10_000;

  // IP解析结果缓存（value为数组：[地理位置, 过期时间戳]）
  private static final ConcurrentHashMap<String, Object[]> IP_LOCATION_CACHE =
      new ConcurrentHashMap<>();
  // IP2Region搜索器（volatile保证多线程可见性）
  private static volatile Searcher searcher;
  // 初始化锁（防止并发初始化导致的资源竞争）
  private static final Object INIT_LOCK = new Object();

  static {
    // 类加载时初始化一次
    initialize();
  }

  /** 初始化IP2Region（带双重检查锁，避免并发问题） */
  private static void initialize() {
    // 已初始化则直接返回
    if (searcher != null) {
      return;
    }

    synchronized (INIT_LOCK) {
      if (searcher != null) {
        return;
      }

      log.info("开始初始化IP2Region，加载数据库文件: {}", IP2REGION_FILE_NAME);
      try {
        // 1. 尝试从资源文件加载
        byte[] dbBuffer = loadDbFile();
        if (dbBuffer == null || dbBuffer.length == 0) {
          log.error("IP2Region数据库文件为空或不存在，将使用简单解析模式");
          return;
        }

        // 2. 验证文件有效性（简单校验：文件大小至少1MB，避免明显损坏）
        if (dbBuffer.length < 1024 * 1024) {
          log.error("IP2Region数据库文件过小（{} bytes），疑似损坏", dbBuffer.length);
          return;
        }

        // 3. 初始化搜索器
        searcher = Searcher.newWithBuffer(dbBuffer);
        log.info("IP2Region初始化成功，数据库文件大小: {} bytes", dbBuffer.length);

      } catch (Exception e) {
        log.error("IP2Region初始化失败，将使用简单解析模式", e);
        searcher = null;
      }
    }
  }

  /** 加载IP2Region数据库文件（支持资源文件和外部路径） */
  private static byte[] loadDbFile() throws IOException {
    // 优先从项目资源目录加载
    try (InputStream is =
        LocationUtils.class.getClassLoader().getResourceAsStream(IP2REGION_FILE_NAME)) {
      if (is != null) {
        return readInputStreamToBytes(is);
      }
    }

    // 尝试从外部路径加载（方便容器部署时挂载文件）
    String externalPath = System.getProperty("ip2region.path", IP2REGION_FILE_NAME);
    if (Files.exists(Paths.get(externalPath))) {
      log.info("从外部路径加载IP2Region数据库: {}", externalPath);
      return Files.readAllBytes(Paths.get(externalPath));
    }

    log.warn("未找到IP2Region数据库文件: {}（资源目录和外部路径均不存在）", IP2REGION_FILE_NAME);
    return null;
  }

  /** 分块读取输入流（避免大文件一次性加载导致OOM） */
  private static byte[] readInputStreamToBytes(InputStream is) throws IOException {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(is)) {

      byte[] buffer = new byte[8192];
      int len;
      while ((len = bis.read(buffer)) != -1) {
        os.write(buffer, 0, len);
      }
      return os.toByteArray();
    }
  }

  /**
   * 根据IP地址获取地理位置信息
   *
   * @param ip IP地址（如183.219.12.163）
   * @return 地理位置（格式：国家 省份 城市，如"中国 广东省 广州市"）
   */
  public static String getLocationByIp(String ip) {
    // 无效IP直接返回
    if (isInvalidIp(ip)) {
      return "未知";
    }

    // 检查缓存（过期自动清理）
    Object[] cacheValue = IP_LOCATION_CACHE.get(ip);
    if (cacheValue != null && cacheValue.length == 2) {
      String location = (String) cacheValue[0];
      long expireTime = (long) cacheValue[1];
      if (System.currentTimeMillis() < expireTime) {
        return location; // 缓存未过期
      } else {
        IP_LOCATION_CACHE.remove(ip); // 缓存过期，移除
      }
    }

    // 尝试使用IP2Region查询（失败则降级）
    String location;
    try {
      // 确保初始化完成（若之前失败，此处会重试）
      initialize();
      location = searcher != null ? queryByIp2Region(ip) : queryBySimpleMode(ip);
    } catch (Exception e) {
      log.warn("IP[{}]解析失败，自动降级到简单模式", ip, e);
      location = queryBySimpleMode(ip);
    }

    // 缓存结果（控制缓存大小）
    if (IP_LOCATION_CACHE.size() < MAX_CACHE_SIZE) {
      IP_LOCATION_CACHE.put(
          ip, new Object[] {location, System.currentTimeMillis() + CACHE_EXPIRE_MILLIS});
    } else {
      log.debug("IP缓存已达最大值({})，暂不缓存新结果", MAX_CACHE_SIZE);
    }

    return location;
  }

  /** 使用IP2Region查询地理位置 */
  private static String queryByIp2Region(String ip) {
    try {
      Validate.notNull(searcher, "IP2Region未初始化");

      String region = searcher.search(ip);
      if (StrUtil.isBlank(region)) {
        return "未知";
      }

      // 解析格式：国家|省份|城市|...（过滤无效值"0"）
      String[] parts = region.split("\\|");
      StringBuilder location = new StringBuilder();

      for (int i = 0; i < Math.min(3, parts.length); i++) { // 只取前3段（国家、省份、城市）
        String part = parts[i].trim();
        if (StrUtil.isNotBlank(part) && !"0".equals(part)) {
          if (location.length() > 0) {
            location.append(" ");
          }
          location.append(part);
        }
      }

      return location.length() > 0 ? location.toString() : "未知";

    } catch (Exception e) {
      // 若出现数组越界等文件损坏特征，主动触发重新初始化
      if (e.getMessage() != null && e.getMessage().contains("arraycopy")) {
        log.error("IP2Region数据库文件可能损坏，将尝试重新加载", e);
        reset(); // 重置搜索器，下次查询会重新初始化
      }
      throw new RuntimeException("IP2Region查询异常", e);
    }
  }

  /** 简单解析模式（降级方案） */
  private static String queryBySimpleMode(String ip) {
    if (isPrivateIp(ip)) {
      return "内网IP";
    }
    if (isChineseIp(ip)) {
      return "中国";
    }
    return "未知";
  }

  // -------------------- 辅助方法 --------------------

  /** 判断IP是否无效 */
  private static boolean isInvalidIp(String ip) {
    return StrUtil.isBlank(ip)
        || "unknown".equalsIgnoreCase(ip)
        || "127.0.0.1".equals(ip)
        || "localhost".equalsIgnoreCase(ip);
  }

  /** 判断是否为私有IP（内网） */
  private static boolean isPrivateIp(String ip) {
    return ip.startsWith("192.168.")
        || ip.startsWith("10.")
        || ip.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\.")
        || "127.0.0.1".equals(ip);
  }

  /** 判断是否为中国IP（基于IP段特征） */
  private static boolean isChineseIp(String ip) {
    String[] prefixes = {
      "1.", "14.", "27.", "36.", "39.", "42.", "49.", "58.", "59.", "60.", "61.", "101.", "103.",
      "106.", "110.", "111.", "112.", "113.", "114.", "115.", "116.", "117.", "118.", "119.",
      "120.", "121.", "122.", "123.", "124.", "125.", "126.", "171.", "175.", "180.", "182.",
      "183.", "202.", "203.", "210.", "211.", "218.", "219.", "220.", "221.", "222.", "223."
    };
    for (String prefix : prefixes) {
      if (ip.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  // -------------------- 管理方法 --------------------

  /** 重置IP2Region（强制重新初始化） */
  public static void reset() {
    synchronized (INIT_LOCK) {
      if (searcher != null) {
        try {
          searcher.close();
        } catch (IOException e) {
          log.warn("关闭IP2Region搜索器失败", e);
        }
        searcher = null;
      }
      log.info("IP2Region已重置，下次查询将重新初始化");
    }
  }

  /** 清理过期缓存（可定时调用） */
  public static void cleanExpiredCache() {
    long now = System.currentTimeMillis();
    int removed = 0;
    for (Map.Entry<String, Object[]> entry : IP_LOCATION_CACHE.entrySet()) {
      if ((long) entry.getValue()[1] < now) {
        IP_LOCATION_CACHE.remove(entry.getKey());
        removed++;
      }
    }
    if (removed > 0) {
      log.debug("清理过期IP缓存，共移除{}条记录", removed);
    }
  }

  /** 检查IP2Region是否可用 */
  public static boolean isAvailable() {
    return searcher != null;
  }

  /** 获取当前缓存大小 */
  public static int getCacheSize() {
    return IP_LOCATION_CACHE.size();
  }

  /** 关闭资源（程序退出时调用） */
  public static void shutdown() {
    reset();
    IP_LOCATION_CACHE.clear();
    log.info("IP2Region工具已关闭，缓存已清理");
  }
}
