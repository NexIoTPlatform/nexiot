package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PropertyTrendTest {
  private PropertyTrendQuery query(long duration) {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey("product");
    query.setIotId("device");
    query.setProperty("speed");
    query.setBeginCreateTime(1700000000L);
    query.setEndCreateTime(query.getBeginCreateTime() + duration);
    return query;
  }

  @Test
  void boundsEvenInclusiveEndpointsToTheRequestedNumberOfBuckets() {
    for (long span : new long[] {0, 599, 600, 86400, 7 * 86400, 31 * 86400}) {
      PropertyTrendQuery query = query(span);
      query.validateRange();
      assertTrue(span / query.getIntervalSeconds() + 1 <= query.getMaxPoints());
    }
  }

  @Test
  void rejectsUnboundedScansAndInvalidWindows() {
    PropertyTrendQuery missingDevice = query(86400);
    missingDevice.setIotId("");
    assertThrows(IoTException.class, missingDevice::validateRange);
    assertThrows(IoTException.class, query(32 * 86400)::validateRange);
    assertThrows(IoTException.class, query(-1)::validateRange);
    PropertyTrendQuery points = query(86400);
    points.setMaxPoints(Integer.MAX_VALUE);
    assertThrows(IoTException.class, points::validateRange);
  }

  @Test
  void countsAllRawRecordsAndKeepsInvalidValuesSeparateFromZero() {
    PropertyTrendVO.Bucket valid = new PropertyTrendVO.Bucket();
    valid.setCount(1000000);
    valid.setNumericCount(1000000);
    valid.setMin(BigDecimal.ZERO);
    valid.setMax(BigDecimal.valueOf(999));
    PropertyTrendVO.Bucket invalid = new PropertyTrendVO.Bucket();
    invalid.setCount(3);
    PropertyTrendVO result = PropertyTrendVO.of(query(86400), List.of(valid, invalid));
    assertEquals(1000003, result.getTotalCount());
    assertEquals(1000000, result.getNumericCount());
    assertEquals(BigDecimal.ZERO, result.getBuckets().get(0).getMin());
    assertNull(result.getBuckets().get(1).getMin());
  }

  @Test
  void doesNotPresentDatabaseTimeoutAsAnEmptyHistory() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Field field = CkDeviceLogService.class.getDeclaredField("jdbcTemplate");
    field.setAccessible(true);
    field.set(service, jdbc);
    when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
        .thenThrow(new QueryTimeoutException("timeout"));
    assertThrows(IoTException.class, () -> service.queryLogMeta(new LogQuery()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void trendUsesBoundParametersAndNeverPagesAwayOlderData() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Field field = CkDeviceLogService.class.getDeclaredField("jdbcTemplate");
    field.setAccessible(true);
    field.set(service, jdbc);
    when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              String sql = invocation.getArgument(0);
              assertTrue(sql.contains("GROUP BY bucket"));
              assertTrue(sql.contains("product_key = ?"));
              assertTrue(sql.contains("iot_id = ?"));
              assertTrue(sql.contains("property = ?"));
              assertTrue(sql.contains("message_type = ?"));
              assertFalse(sql.contains("LIMIT"));
              assertFalse(sql.contains("OFFSET"));
              assertFalse(sql.contains("property'"));
              return List.of();
            });
    PropertyTrendQuery query = query(7 * 86400);
    query.setSamplingMode("exact");
    query.setProperty("property' OR 1=1");
    assertEquals(0, service.queryPropertyTrend(query).getTotalCount());
  }
}
