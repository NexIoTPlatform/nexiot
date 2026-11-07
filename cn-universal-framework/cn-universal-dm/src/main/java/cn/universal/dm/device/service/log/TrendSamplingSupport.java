package cn.universal.dm.device.service.log;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** 安全归约有限样本；每个窗口的额外一行仅用于判断是否截断。 */
public final class TrendSamplingSupport {
  private static final Pattern NUMBER =
      Pattern.compile("^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$");
  private static final MathContext PRECISION = MathContext.DECIMAL128;

  private TrendSamplingSupport() {}

  public static PropertyTrendVO aggregate(
      PropertyTrendQuery query, List<PropertyTrendSampleVO> samples) {
    query.validateRange();
    int windows = query.getSampleWindowCount();
    int limit = query.getSamplesPerWindow();
    long[] seen = new long[windows];
    PropertyTrendVO.Bucket[] buckets = new PropertyTrendVO.Bucket[windows];
    BigDecimal[] sums = new BigDecimal[windows];
    boolean truncated = false;
    for (PropertyTrendSampleVO sample : samples) {
      int index = sample.getWindowIndex();
      if (index < 0 || index >= windows || ++seen[index] > query.getSampleFetchSize()) {
        throw new IoTException("历史趋势返回无效或超量样本", 503);
      }
      if (seen[index] > limit) {
        truncated = true;
        continue;
      }
      PropertyTrendVO.Bucket bucket = buckets[index];
      if (bucket == null) {
        bucket = new PropertyTrendVO.Bucket();
        bucket.setTime((query.getBeginCreateTime() + index * query.getIntervalSeconds()) * 1000);
        buckets[index] = bucket;
        sums[index] = BigDecimal.ZERO;
      }
      bucket.setCount(bucket.getCount() + 1);
      BigDecimal value = number(sample.getContent());
      if (value == null) continue;
      bucket.setNumericCount(bucket.getNumericCount() + 1);
      bucket.setMin(bucket.getMin() == null ? value : bucket.getMin().min(value));
      bucket.setMax(bucket.getMax() == null ? value : bucket.getMax().max(value));
      sums[index] = sums[index].add(value, PRECISION);
    }
    List<PropertyTrendVO.Bucket> result = new ArrayList<>();
    for (int index = 0; index < windows; index++) {
      PropertyTrendVO.Bucket bucket = buckets[index];
      if (bucket == null) continue;
      if (bucket.getNumericCount() > 0) {
        bucket.setAvg(sums[index].divide(BigDecimal.valueOf(bucket.getNumericCount()), PRECISION));
      }
      result.add(bucket);
    }
    PropertyTrendVO trend = PropertyTrendVO.sampled(query, result, truncated);
    trend.setScannedCount(samples.size());
    return trend;
  }

  static BigDecimal number(String content) {
    if (content == null || content.length() > 1024) return null;
    String text = content.trim();
    if (text.isEmpty() || !NUMBER.matcher(text).matches()) return null;
    try {
      // Parse into a finite double before BigDecimal, bounding exponent and scale work.
      double value = Double.parseDouble(text);
      return Double.isFinite(value) ? BigDecimal.valueOf(value) : null;
    } catch (NumberFormatException invalid) {
      return null;
    }
  }
}
