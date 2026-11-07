package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.vo.VideoCatalogConfigurationChannel;
import cn.universal.persistence.entity.vo.VideoCatalogDeviceRow;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import com.github.pagehelper.PageHelper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VideoCatalogServiceTest {
  private VideoCatalogService service(IoTDeviceMapper mapper) {
    return new VideoCatalogService(mapper) {
      @Override protected IoTUser currentTenantUser() { return IoTUser.builder().tenantId("tenant-a").build(); }
      @Override protected void applyDataPermissionScope(Object query, IoTUser user) {
        ((IoTDevice) query).setTenantId(user.getTenantId());
      }
    };
  }

  private VideoCatalogDeviceRow device(String configuration) {
    var row = new VideoCatalogDeviceRow();
    row.setIotId("iot-a"); row.setProductKey("product-a"); row.setDeviceId("serial-a");
    row.setTenantId("tenant-a"); row.setPlatformType("imoulife"); row.setConfiguration(configuration);
    return row;
  }

  @AfterEach void clearPagination() { PageHelper.clearPage(); }

  @Test void devicePageIsBoundedAndDoesNotSerializeConfigurationOrInlineChannels() {
    var mapper = mock(IoTDeviceMapper.class);
    when(mapper.selectVideoCatalogDevices(any(), any(), any(), isNull())).thenAnswer(call -> {
      var page = PageHelper.getLocalPage();
      assertEquals(100, page.getPageSize()); assertEquals(2, page.getPageNum());
      page.setTotal(220);
      return List.of(device("{\"channelList\":[{\"channelId\":0,\"movable\":true}],\"pictureSecret\":\"private\",\"token\":\"private\"}"));
    });
    when(mapper.selectVideoCatalogChannelCounts(anyList(), any())).thenReturn(List.of());
    var result = service(mapper).list(2, 1000, null, null);
    assertEquals(220, result.getTotal()); assertTrue(result.isHasMore());
    assertEquals(1, result.getRows().get(0).getChannelCount());
    assertEquals("imou", result.getRows().get(0).getSdkVendor());
    String json = JSONUtil.toJsonStr(result);
    assertFalse(json.contains("private")); assertFalse(json.contains("configuration"));
    assertNull(PageHelper.getLocalPage());
  }

  @Test void configurationChannelsUsePageHelperAndExcludeNonVideoAndDisabledUnits() {
    var mapper = mock(IoTDeviceMapper.class);
    var row = device("{\"preview\":false,\"channelList\":[{\"channelId\":0,\"movable\":true},{\"channelId\":7,\"unitType\":7},{\"channelId\":8,\"enabled\":false}]}");
    when(mapper.selectVideoCatalogDevices(any(), isNull(), isNull(), eq("iot-a"))).thenReturn(List.of(row));
    when(mapper.selectVideoCatalogChannelCounts(anyList(), any())).thenReturn(List.of());
    when(mapper.selectVideoCatalogConfigurationChannels(anyString())).thenAnswer(call -> {
      var page = PageHelper.getLocalPage();
      assertEquals(3, page.getPageNum()); assertEquals(50, page.getPageSize());
      page.setTotal(1);
      return List.<VideoCatalogConfigurationChannel>of();
    });
    var service = service(mapper);
    var result = service.channels("iot-a", 3, 50);
    assertEquals(1, result.getTotal()); assertTrue(result.getRows().isEmpty()); assertFalse(result.isHasMore());
    var captured = ArgumentCaptor.forClass(String.class);
    verify(mapper).selectVideoCatalogConfigurationChannels(captured.capture());
    var normalized = JSONUtil.parseArray(captured.getValue());
    assertEquals(1, normalized.size());
    assertEquals("0", normalized.getJSONObject(0).getStr("channelId"));
    var channel = service.requireVisibleChannel("iot-a", "0");
    assertEquals(0, channel.getChannelNo());
    assertEquals("supported", channel.getCapabilities().get("ptz"));
    assertEquals("unsupported", channel.getCapabilities().get("preview"));
    assertThrows(BaseException.class, () -> service.requireVisibleChannel("iot-a", "7"));
  }

  @Test void missingOrOutOfScopeDeviceNeverQueriesChannels() {
    var mapper = mock(IoTDeviceMapper.class);
    when(mapper.selectVideoCatalogDevices(any(), any(), any(), any())).thenReturn(List.of());
    assertThrows(BaseException.class, () -> service(mapper).channels("foreign", 1, 50));
    verify(mapper, never()).selectVideoCatalogNativeChannels(any(), any(), any(), any());
    verify(mapper, never()).selectVideoCatalogConfigurationChannels(any());
  }
}
