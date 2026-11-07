package cn.universal.dm.device.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.IoTDashboardStatistics;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardOperationsServiceTest {
  @Test
  @SuppressWarnings("unchecked")
  void usesNewestChannelSnapshotsWithoutAddingProductOrGlobalTotalsTwice() {
    LocalDate day = LocalDate.of(2026, 9, 23);
    var stale = row(day, null, "TCP", "message_total", 9);
    stale.setUpdateTime(day.atTime(1, 0));
    var result = DashboardOperationsService.summarize(List.of(stale,
        row(day, null, "TCP", "message_total", 10),
        row(day, null, "MQTT", "message_total", 20),
        row(day, "product-a", null, "message_total", 999),
        row(day, null, "TCP", "message_success", 8)), day, 7);
    var today = (Map<String, Object>) result.get("today");
    assertEquals(30L, today.get("message_total"));
    assertEquals(8L, today.get("message_success"));
    assertNull(today.get("message_failed"));
    var trend = (List<Map<String, Object>>) result.get("trend");
    assertEquals(7, trend.size());
    assertNull(trend.get(0).get("message_total"));
    assertEquals("2026-09-17", trend.get(0).get("date"));
    result = DashboardOperationsService.summarize(List.of(
        row(day, null, null, "message_total", 25),
        row(day, null, "TCP", "message_total", 10),
        row(day, null, "MQTT", "message_total", 20)), day, 7);
    assertEquals(25L, ((Map<String, Object>) result.get("today")).get("message_total"));
  }

  private static IoTDashboardStatistics row(LocalDate day, String product, String channel, String metric, long value) {
    return IoTDashboardStatistics.builder().statDate(day).productKey(product).channel(channel)
        .metricType(metric).metricValue(value).updateTime(day.atTime(12, 0)).build();
  }
}
