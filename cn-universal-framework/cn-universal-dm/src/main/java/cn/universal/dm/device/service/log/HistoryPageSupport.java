package cn.universal.dm.device.service.log;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/** Fixed-window keyset pagination; tokens identify a position, never grant device access. */
public final class HistoryPageSupport {
  private HistoryPageSupport() {}

  public static void prepare(LogQuery query, boolean metadata) {
    if (!query.isCursorMode()) {
      prepareLegacy(query);
      return;
    }
    if (blank(query.getProductKey()) || blank(query.getIotId())) {
      throw new IoTException("请选择产品和设备", 400);
    }
    if (!blank(query.getId())) {
      try {
        Long.parseLong(query.getId());
      } catch (NumberFormatException e) {
        throw new IoTException("消息ID格式无效", 400);
      }
    }
    Long begin = query.getBeginCreateTime();
    Long end = query.getEndCreateTime();
    if (begin == null
        || end == null
        || begin < 0
        || end > 253402300799L
        || end < begin
        || end - begin > 31L * 86400) {
      throw new IoTException("请选择不超过31天的有效时间范围", 400);
    }
    if (query.getPageSize() == null || query.getPageSize() < 1 || query.getPageSize() > 200) {
      throw new IoTException("每页记录数必须在1到200之间", 400);
    }
    if (metadata && (blank(query.getProperty()) == blank(query.getEvent()))) {
      throw new IoTException("请选择单个属性或事件", 400);
    }
    // Do not accept independently supplied seek keys or legacy nested filters.
    query.setParams(null);
    query.setBeforeTime(null);
    query.setBeforeId(null);
    query.setPageNum(1);
    if (blank(query.getCursor())) return;
    try {
      if (query.getCursor().length() > 1024) throw new IllegalArgumentException();
      String decoded =
          new String(Base64.getUrlDecoder().decode(query.getCursor()), StandardCharsets.UTF_8);
      String[] parts = decoded.split("\\|", -1);
      if (parts.length != 4 || !"1".equals(parts[0]) || !fingerprint(query).equals(parts[1])) {
        throw new IllegalArgumentException();
      }
      query.setBeforeTime(LocalDateTime.parse(parts[2]));
      query.setBeforeId(Long.valueOf(parts[3]));
    } catch (RuntimeException e) {
      throw new IoTException("翻页位置无效或查询条件已变化，请重新查询", 400);
    }
  }

  /** Keep legacy response formats but disallow unbounded scans or deep offsets. */
  private static void prepareLegacy(LogQuery query) {
    if (query.getPageNum() == null
        || query.getPageNum() < 1
        || query.getPageSize() == null
        || query.getPageSize() < 1
        || query.getPageSize() > 200
        || ((long) query.getPageNum() - 1) * query.getPageSize() > 10000) {
      throw new IoTException("旧分页接口仅支持浅分页，请使用游标接口继续查询", 400);
    }
    int page = query.getPageNum();
    java.util.Map<String, Object> legacy = query.getParams();
    if (legacy != null) {
      try {
        if (query.getBeginCreateTime() == null && legacy.get("beginCreateTime") != null)
          query.setBeginCreateTime(Long.valueOf(legacy.get("beginCreateTime").toString()));
        if (query.getEndCreateTime() == null && legacy.get("endCreateTime") != null)
          query.setEndCreateTime(Long.valueOf(legacy.get("endCreateTime").toString()));
        if (blank(query.getEvent()) && legacy.get("event") != null)
          query.setEvent(legacy.get("event").toString());
        if (blank(query.getProperty()) && legacy.get("property") != null)
          query.setProperty(legacy.get("property").toString());
      } catch (NumberFormatException e) {
        throw new IoTException("时间范围格式无效", 400);
      }
    }
    if (query.getEndCreateTime() == null)
      query.setEndCreateTime(java.time.Instant.now().getEpochSecond());
    if (query.getBeginCreateTime() == null)
      query.setBeginCreateTime(Math.max(0, query.getEndCreateTime() - 86400));
    query.setCursor(null);
    query.setCursorMode(true);
    try {
      prepare(query, false);
    } finally {
      query.setCursorMode(false);
      query.setPageNum(page);
    }
    if (legacy != null && legacy.get("properties") != null) {
      query.setParams(java.util.Map.of("properties", legacy.get("properties")));
    }
  }

  public static <T> PageBean<T> page(
      List<T> fetched, LogQuery query, Function<T, LocalDateTime> time, Function<T, Long> id) {
    boolean more = fetched.size() > query.getPageSize();
    List<T> rows =
        new ArrayList<>(fetched.subList(0, Math.min(fetched.size(), query.getPageSize())));
    PageBean<T> page = new PageBean<>(rows, -1L, query.getPageSize(), 1);
    page.setTotalPage(0);
    page.setHasMore(more);
    if (more && !rows.isEmpty()) {
      T last = rows.getLast();
      LocalDateTime lastTime = time.apply(last);
      Long lastId = id.apply(last);
      if (lastTime == null) throw new IoTException("历史记录缺少时间，无法继续翻页");
      if (lastId == null) throw new IoTException("历史记录缺少ID，无法继续翻页");
      page.setNextCursor(cursor(query, lastTime, lastId));
    }
    return page;
  }

  /** 筛选后的一页可能在原始日志批次中间结束，继续位置必须是最后实际消费的记录。 */
  public static String cursor(LogQuery query, LocalDateTime time, Long id) {
    if (time == null || id == null) throw new IoTException("历史记录缺少时间或ID，无法继续翻页");
    String token = "1|" + fingerprint(query) + "|" + time + "|" + id;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
  }

  private static String fingerprint(LogQuery query) {
    return DigestUtil.sha256Hex(
        JSONUtil.toJsonStr(
            Arrays.asList(
                query.getProductKey(),
                query.getIotId(),
                query.getDeviceId(),
                query.getProperty(),
                query.getEvent(),
                query.getMessageType(),
                query.getId(),
                query.getDeviceName(),
                query.getBeginCreateTime(),
                query.getEndCreateTime(),
                query.getPageSize())));
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }
}
