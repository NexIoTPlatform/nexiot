package cn.universal.persistence.query;

import cn.universal.common.exception.IoTException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 单设备、单属性的有界趋势查询，时间单位为秒。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PropertyTrendQuery extends LogQuery {
  private Integer maxPoints = 100;

  /** 每个请求的样本预算，由页面选择；每个窗口额外读取一行检测截断。 */
  private Integer maxScanRows = 10000;

  /** 默认有界采样；全量统计只能由调用方明确请求。 */
  private String samplingMode = "sample";

  public void validateRange() {
    if (blank(getProductKey()) || blank(getIotId()) || blank(getProperty())) {
      throw new IoTException("请选择设备和属性", 400);
    }
    Long begin = getBeginCreateTime();
    Long end = getEndCreateTime();
    if (begin == null
        || end == null
        || begin < 0
        || end > 253402300799L
        || end < begin
        || end - begin > 31L * 86400) {
      throw new IoTException("请选择不超过31天的有效时间范围", 400);
    }
    if (maxPoints == null || maxPoints < 20 || maxPoints > 1000) {
      throw new IoTException("趋势点数必须在20到1000之间", 400);
    }
    if (maxScanRows == null || maxScanRows < 1000 || maxScanRows > 100000) {
      throw new IoTException("趋势读取上限必须在1000到100000之间", 400);
    }
    if (!"sample".equals(samplingMode) && !"exact".equals(samplingMode)) {
      throw new IoTException("趋势查询模式无效", 400);
    }
    setMessageType("PROPERTIES");
  }

  public boolean isSampledQuery() {
    return "sample".equals(samplingMode);
  }

  public int getSampleWindowCount() {
    return Math.toIntExact((getEndCreateTime() - getBeginCreateTime()) / getIntervalSeconds() + 1);
  }

  public int getSamplesPerWindow() {
    return maxScanRows / getSampleWindowCount();
  }

  public int getSampleFetchSize() {
    return getSamplesPerWindow() + 1;
  }

  public List<SampleWindow> getSampleWindows() {
    List<SampleWindow> windows = new ArrayList<>(getSampleWindowCount());
    long begin = Math.multiplyExact(getBeginCreateTime(), 1000L);
    // Existing filters include exactly the end timestamp, not the entire following second.
    long end = Math.addExact(Math.multiplyExact(getEndCreateTime(), 1000L), 1L);
    long step = Math.multiplyExact(getIntervalSeconds(), 1000L);
    for (int index = 0; index < getSampleWindowCount(); index++) {
      long start = begin + index * step;
      windows.add(new SampleWindow(index, start, Math.min(end, start + step)));
    }
    return windows;
  }

  @Data
  public static class SampleWindow {
    private final int index;
    private final long beginMillis;
    private final long endExclusiveMillis;

    public LocalDateTime getBeginTime() {
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(beginMillis), ZoneId.systemDefault());
    }

    public LocalDateTime getEndExclusiveTime() {
      return LocalDateTime.ofInstant(
          Instant.ofEpochMilli(endExclusiveMillis), ZoneId.systemDefault());
    }
  }

  public long getIntervalSeconds() {
    long interval =
        Math.max(1, (getEndCreateTime() - getBeginCreateTime() + maxPoints) / maxPoints);
    // Minute-aligned windows allow native pre-aggregates to be reused without losing extrema.
    return interval < 60 ? interval : ((interval + 59) / 60) * 60;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
