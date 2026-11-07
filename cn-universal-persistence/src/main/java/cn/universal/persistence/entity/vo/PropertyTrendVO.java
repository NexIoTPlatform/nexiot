package cn.universal.persistence.entity.vo;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 趋势统计。countExact=false 时计数和极值仅描述样本，不代表全量数据。 */
@Data
public class PropertyTrendVO {
  private long beginCreateTime;
  private long endCreateTime;
  private long intervalSeconds;
  private long totalCount;
  private long numericCount;
  private boolean sampled;
  private boolean countExact = true;
  private long scannedCount;
  private int scanLimit;
  private String samplingMode = "exact";
  private List<Bucket> buckets;

  public static PropertyTrendVO of(PropertyTrendQuery query, List<Bucket> buckets) {
    for (Bucket bucket : buckets) {
      if (!finite(bucket.getMin()) || !finite(bucket.getMax()) || !finite(bucket.getAvg())) {
        // 所有适配器（包括直接service调用）都不能把溢出结果当成0或Infinity发送到图表。
        throw new IoTException("历史趋势包含超出数值范围的统计结果，请联系管理员检查存储数据", 503);
      }
    }
    PropertyTrendVO result = new PropertyTrendVO();
    result.setBeginCreateTime(query.getBeginCreateTime());
    result.setEndCreateTime(query.getEndCreateTime());
    result.setIntervalSeconds(query.getIntervalSeconds());
    result.setBuckets(buckets);
    result.setTotalCount(buckets.stream().mapToLong(Bucket::getCount).sum());
    result.setNumericCount(buckets.stream().mapToLong(Bucket::getNumericCount).sum());
    result.setScannedCount(result.getTotalCount());
    return result;
  }

  public static PropertyTrendVO sampled(
      PropertyTrendQuery query, List<Bucket> buckets, boolean truncated) {
    PropertyTrendVO result = of(query, buckets);
    result.setSampled(truncated);
    result.setCountExact(!truncated);
    result.setScanLimit(query.getMaxScanRows());
    result.setSamplingMode("time-window-sample");
    return result;
  }

  private static boolean finite(BigDecimal value) {
    return value == null || Double.isFinite(value.doubleValue());
  }

  @Data
  public static class Bucket {
    /** 时间段起点，毫秒时间戳。 */
    private long time;

    private long count;
    private long numericCount;
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal avg;
  }
}
