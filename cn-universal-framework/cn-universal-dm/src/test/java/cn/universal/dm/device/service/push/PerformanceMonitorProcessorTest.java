package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerformanceMonitorProcessorTest {

  @Test
  void shouldPreserveExistingCollectionTime() {
    PerformanceMonitorProcessor processor = new PerformanceMonitorProcessor();
    BaseUPRequest request = BaseUPRequest.builder().time(1710000000123L).build();

    processor.beforePush(List.of(request));

    assertEquals(1710000000123L, request.getTime());
  }

  @Test
  void shouldFillTimeWhenMissing() {
    PerformanceMonitorProcessor processor = new PerformanceMonitorProcessor();
    BaseUPRequest request = BaseUPRequest.builder().build();

    long before = System.currentTimeMillis();
    processor.beforePush(List.of(request));

    assertTrue(request.getTime() >= before);
  }
}
