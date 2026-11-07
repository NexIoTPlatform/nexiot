package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendSamplingSupportTest {
  private PropertyTrendQuery query() {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey("p");
    query.setIotId("i");
    query.setProperty("speed");
    query.setBeginCreateTime(1700000000L);
    query.setEndCreateTime(1700086400L);
    query.setMaxPoints(100);
    query.setMaxScanRows(1000);
    query.validateRange();
    return query;
  }

  @Test
  void coversTheWholeRangeWithoutOverlappingOrDroppingTheInclusiveEnd() {
    PropertyTrendQuery query = query();
    var windows = query.getSampleWindows();
    assertEquals(query.getBeginCreateTime() * 1000, windows.getFirst().getBeginMillis());
    assertEquals(query.getEndCreateTime() * 1000 + 1, windows.getLast().getEndExclusiveMillis());
    for (int i = 1; i < windows.size(); i++) {
      assertEquals(windows.get(i - 1).getEndExclusiveMillis(), windows.get(i).getBeginMillis());
    }
    assertTrue(windows.size() <= query.getMaxPoints());
    assertTrue(query.getSamplesPerWindow() * windows.size() <= query.getMaxScanRows());
  }

  @Test
  void sentinelDetectsTruncationButDoesNotSkewSampleStatistics() {
    PropertyTrendQuery query = query();
    List<PropertyTrendSampleVO> rows = new ArrayList<>();
    for (int i = 0; i < query.getSamplesPerWindow(); i++)
      rows.add(new PropertyTrendSampleVO(0, "10"));
    rows.add(new PropertyTrendSampleVO(0, "9999"));
    rows.add(new PropertyTrendSampleVO(query.getSampleWindowCount() - 1, "20"));
    var result = TrendSamplingSupport.aggregate(query, rows);
    assertTrue(result.isSampled());
    assertFalse(result.isCountExact());
    assertEquals(rows.size(), result.getScannedCount());
    assertEquals(rows.size() - 1, result.getTotalCount());
    assertEquals(0, BigDecimal.TEN.compareTo(result.getBuckets().getFirst().getMax()));
    assertEquals(2, result.getBuckets().size());
    assertEquals("time-window-sample", result.getSamplingMode());
  }

  @Test
  void finiteNumbersInvalidTextAndOverflowRemainDistinctAndTheMeanDoesNotOverflow() {
    var result =
        TrendSamplingSupport.aggregate(
            query(),
            List.of(
                new PropertyTrendSampleVO(0, "0"), new PropertyTrendSampleVO(0, "bad"),
                new PropertyTrendSampleVO(0, "NaN"), new PropertyTrendSampleVO(0, "1e999"),
                new PropertyTrendSampleVO(0, "1e308"), new PropertyTrendSampleVO(0, "1e308"),
                new PropertyTrendSampleVO(0, "0x123"),
                    new PropertyTrendSampleVO(0, "9".repeat(1025))));
    assertTrue(result.isCountExact());
    assertFalse(result.isSampled());
    assertEquals(8, result.getTotalCount());
    assertEquals(3, result.getNumericCount());
    assertEquals(0, BigDecimal.ZERO.compareTo(result.getBuckets().getFirst().getMin()));
    assertTrue(Double.isFinite(result.getBuckets().getFirst().getAvg().doubleValue()));
  }

  @Test
  void rejectsInvalidBudgetAndUnexpectedRowsInsteadOfCachingAnUnboundedResult() {
    PropertyTrendQuery query = query();
    query.setMaxScanRows(Integer.MAX_VALUE);
    assertThrows(IoTException.class, query::validateRange);
    assertThrows(
        IoTException.class,
        () -> TrendSamplingSupport.aggregate(query(), List.of(new PropertyTrendSampleVO(-1, "1"))));
    query = query();
    List<PropertyTrendSampleVO> rows = new ArrayList<>();
    for (int i = 0; i <= query.getSampleFetchSize(); i++)
      rows.add(new PropertyTrendSampleVO(0, "1"));
    PropertyTrendQuery valid = query;
    assertThrows(IoTException.class, () -> TrendSamplingSupport.aggregate(valid, rows));
  }
}
