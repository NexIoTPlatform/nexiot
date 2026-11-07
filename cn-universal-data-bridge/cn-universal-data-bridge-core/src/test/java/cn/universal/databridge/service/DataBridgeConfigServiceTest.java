package cn.universal.databridge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.mapper.DataBridgeConfigMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DataBridgeConfigServiceTest {

  @Test
  void shouldCacheEmptyOrPresentProductConfigurationAndInvalidateAfterUpdate() {
    DataBridgeConfigService service = new DataBridgeConfigService();
    DataBridgeConfigMapper mapper = mock(DataBridgeConfigMapper.class);
    ReflectionTestUtils.setField(service, "dataBridgeConfigMapper", mapper);
    DataBridgeConfig config = new DataBridgeConfig();
    config.setId(1L);
    when(mapper.selectActiveConfigsByProductKey("pk-1")).thenReturn(List.of(config));
    when(mapper.updateByPrimaryKeySelective(org.mockito.ArgumentMatchers.any())).thenReturn(1);

    assertEquals(1, service.getActiveConfigsByProductKey("pk-1").size());
    assertEquals(1, service.getActiveConfigsByProductKey("pk-1").size());
    verify(mapper).selectActiveConfigsByProductKey("pk-1");

    service.updateConfigStatus(1L, 0, "tester");
    assertEquals(1, service.getActiveConfigsByProductKey("pk-1").size());
    verify(mapper, times(2)).selectActiveConfigsByProductKey("pk-1");
  }
}
