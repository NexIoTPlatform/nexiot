package cn.universal.dm.device.service;

import cn.universal.persistence.entity.IoTDashboardStatistics;
import cn.universal.persistence.mapper.IoTDashboardStatisticsMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/** 读取已落库的统计快照，不将产品明细与平台汇总重复相加。 */
@Service
@RequiredArgsConstructor
public class DashboardOperationsService {
  private final IoTDashboardStatisticsMapper mapper;
  private static final List<String> METRICS = List.of(
      "message_total", "message_success", "message_failed", "message_retry", "message_push");

  public Map<String, Object> getStatistics(int days) {
    int range = days == 14 || days == 30 ? days : 7;
    LocalDate today = LocalDate.now();
    Example example = new Example(IoTDashboardStatistics.class);
    example.createCriteria().andIsNull("productKey")
        .andBetween("statDate", today.minusDays(range - 1), today)
        .andIn("metricType", METRICS);
    return summarize(mapper.selectByExample(example), today, range);
  }

  static Map<String, Object> summarize(List<IoTDashboardStatistics> rows, LocalDate today, int days) {
    // NULL 维度在不同数据库可能产生重复记录，优先使用最近更新的快照。
    Map<LocalDate, Map<String, Map<String, IoTDashboardStatistics>>> dates = new LinkedHashMap<>();
    for (var row : rows) {
      if (row.getProductKey() != null || !METRICS.contains(row.getMetricType())
          || row.getStatDate() == null || row.getMetricValue() == null) continue;
      String channel = row.getChannel() == null || row.getChannel().isBlank() ? "" : row.getChannel();
      dates.computeIfAbsent(row.getStatDate(), key -> new LinkedHashMap<>())
          .computeIfAbsent(row.getMetricType(), key -> new LinkedHashMap<>())
          .merge(channel, row, (left, right) -> updated(left).isAfter(updated(right)) ? left : right);
    }
    List<Map<String, Object>> trend = new ArrayList<>();
    Map<String, Object> todayStats = new LinkedHashMap<>();
    for (int i = days - 1; i >= 0; i--) {
      LocalDate date = today.minusDays(i);
      Map<String, Object> point = new LinkedHashMap<>();
      point.put("date", date.toString());
      var metrics = dates.getOrDefault(date, Map.of());
      for (String metric : METRICS) {
        var channels = metrics.getOrDefault(metric, Map.of());
        Long value = channels.isEmpty() ? null : channels.containsKey("")
            ? channels.get("").getMetricValue()
            : channels.values().stream().mapToLong(IoTDashboardStatistics::getMetricValue).sum();
        point.put(metric, value);
      }
      trend.add(point);
      if (date.equals(today)) {
        todayStats.putAll(point);
        todayStats.put("updatedAt", metrics.values().stream().flatMap(m -> m.values().stream())
            .map(DashboardOperationsService::updated).max(Comparator.naturalOrder())
            .map(LocalDateTime::toString).orElse(null));
      }
    }
    List<Map<String, Object>> channels = new ArrayList<>();
    dates.getOrDefault(today, Map.of()).getOrDefault("message_total", Map.of())
        .forEach((name, row) -> {
          if (!name.isBlank()) channels.add(Map.of("name", name, "value", row.getMetricValue()));
        });
    channels.sort(Comparator.comparingLong(item -> -((Number) item.get("value")).longValue()));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("today", todayStats);
    result.put("trend", trend);
    result.put("channels", channels);
    result.put("days", days);
    result.put("scope", "platform");
    return result;
  }

  private static LocalDateTime updated(IoTDashboardStatistics row) {
    return row.getUpdateTime() != null ? row.getUpdateTime()
        : row.getCreateTime() != null ? row.getCreateTime() : LocalDateTime.MIN;
  }
}
