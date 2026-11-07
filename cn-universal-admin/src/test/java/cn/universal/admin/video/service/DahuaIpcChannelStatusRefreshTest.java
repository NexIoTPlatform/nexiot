package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class DahuaIpcChannelStatusRefreshTest {
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final cn.universal.admin.video.alarm.SdkVideoAlarmContextCache cache =
      mock(cn.universal.admin.video.alarm.SdkVideoAlarmContextCache.class);
  private final DahuaIpcChannelStatusRefresh service = new DahuaIpcChannelStatusRefresh(devices, channels, cache);
  private final VideoPlatformDevice device = VideoPlatformDevice.builder().id(11L)
      .instanceKey("dh").deviceId("ipc").tenantId("tenant").configuration("""
          {"fusion":{"enabled":true},"channelNameOverrides":{"1":"平台别名"},
           "channelList":[{"channelId":"1","sdkChannel":0,"streamType":"main", "channelName":"平台别名"},
                          {"channelId":"1_sub","sdkChannel":0,"streamType":"sub"},
                          {"channelId":"5","sdkChannel":4,"streamType":"main","channelStatus":"UNKNOWN"}]}
          """).build();

  @BeforeEach void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformChannel.class, new Config());
    when(devices.selectByExample(any())).thenReturn(List.of(device));
    when(channels.selectByExample(any())).thenReturn(List.of(
        VideoPlatformChannel.builder().id(21L).deviceId(11L).channelId("1").build(),
        VideoPlatformChannel.builder().id(22L).deviceId(11L).channelId("1_sub").build(),
        VideoPlatformChannel.builder().id(25L).deviceId(11L).channelId("5").build()));
    when(devices.updateByExampleSelective(any(), any())).thenReturn(1);
    when(channels.updateByExampleSelective(any(), any())).thenReturn(1);
  }

  @ParameterizedTest @ValueSource(booleans = {false, true})
  void realSignalUpdatesOnlyExistingStatusAndPreservesNamesRoutingAndHistory(boolean lost) throws Exception {
    assertEquals(2, service.refresh(device, snapshot(signal("1", 0, "main", lost),
        signal("1_sub", 0, "sub", lost), signal("2", 1, "main", lost))));
    var row = ArgumentCaptor.forClass(VideoPlatformChannel.class);
    var scope = ArgumentCaptor.forClass(Example.class);
    verify(channels, times(2)).updateByExampleSelective(row.capture(), scope.capture());
    for (var update : row.getAllValues()) {
      assertEquals(lost ? "OFF" : "ON", update.getChannelStatus());
      assertNull(update.getEnabled()); assertNull(update.getChannelName());
      assertNull(update.getStreamConfig()); assertNull(update.getExtField3());
    }
    assertTrue(scope.getValue().getOredCriteria().getFirst().getAllCriteria().stream()
        .anyMatch(c -> c.getCondition().startsWith("tenant_id") && "tenant".equals(c.getValue())));
    var saved = ArgumentCaptor.forClass(VideoPlatformDevice.class);
    verify(devices).updateByExampleSelective(saved.capture(), any());
    Map<String, Object> config = VideoDeviceNames.configuration(saved.getValue().getConfiguration());
    assertEquals(Map.of("enabled", true), config.get("fusion"));
    assertEquals(Map.of("1", "平台别名"), config.get("channelNameOverrides"));
    var list = (List<?>) config.get("channelList"); assertEquals(3, list.size());
    assertEquals("平台别名", VideoDeviceNames.object(list.getFirst()).get("channelName"));
    assertEquals(lost, VideoDeviceNames.object(list.getFirst()).get("videoLost"));
    assertEquals("UNKNOWN", VideoDeviceNames.object(list.getLast()).get("channelStatus"));
    assertEquals(false, config.get("channelCatalogAuthoritative"));
    assertNull(saved.getValue().getDeviceStatus()); assertNull(saved.getValue().getDeviceName());
    verify(channels, never()).deleteByExample(any()); verify(channels, never()).insertSelective(any());
    var locked = ArgumentCaptor.forClass(Example.class); verify(devices).selectByExample(locked.capture());
    assertTrue(locked.getValue().isForUpdate());
    assertNotNull(DahuaIpcChannelStatusRefresh.class.getMethod("refresh", VideoPlatformDevice.class, Map.class)
        .getAnnotation(Transactional.class));
    verify(cache).publishPlatformChange("dh");
  }

  @ParameterizedTest @ValueSource(strings = {"fallback", "NET_QUERY_GET_CAMERA_STATE", ""})
  void unknownOrOtherSourcesCannotUseTheIpcException(String source) {
    Map<String, Object> signal = signal("1", 0, "main", false); signal.put("channelStateSource", source);
    assertEquals(0, service.refresh(device, snapshot(signal))); verifyNoInteractions(devices, channels);
  }

  @Test void missingBooleanAndAuthoritativeSnapshotsCannotBypassDirectorySync() {
    var signal = signal("1", 0, "main", false); signal.put("videoLost", "false");
    assertEquals(0, service.refresh(device, snapshot(signal)));
    var authoritative = snapshot(signal("1", 0, "main", false)); authoritative.put("channelCatalogAuthoritative", true);
    assertEquals(0, service.refresh(device, authoritative)); verifyNoInteractions(devices, channels);
  }

  @Test void mismatchedSdkChannelStreamAndDuplicateIdentityCannotWrite() {
    assertEquals(0, service.refresh(device, snapshot(signal("1", 1, "main", false))));
    assertEquals(0, service.refresh(device, snapshot(signal("1", 0, "sub", false))));
    assertEquals(0, service.refresh(device, snapshot(signal("1", 0, "main", false), signal("1", 0, "main", true))));
    verifyNoInteractions(devices, channels);
    device.setConfiguration("{\"channelList\":[{\"channelId\":\"1\",\"sdkChannel\":2,\"streamType\":\"main\"}]}");
    assertEquals(0, service.refresh(device, snapshot(signal("1", 0, "main", false))));
    verify(channels, never()).updateByExampleSelective(any(), any());
    verify(devices, never()).updateByExampleSelective(any(), any());
  }

  @Test void removedDeviceOrChannelCannotBeReportedAsSuccessfulRefresh() {
    when(devices.selectByExample(any())).thenReturn(List.of());
    assertThrows(BaseException.class, () -> service.refresh(device, snapshot(signal("1", 0, "main", false))));
    when(devices.selectByExample(any())).thenReturn(List.of(device));
    when(channels.updateByExampleSelective(any(), any())).thenReturn(0);
    assertThrows(BaseException.class, () -> service.refresh(device, snapshot(signal("1", 0, "main", false))));
    verify(devices, never()).updateByExampleSelective(any(), any());
    verifyNoInteractions(cache);
  }

  @Test void transactionProxyRollsBackChannelWritesWhenSavingTheDeviceSnapshotFails() {
    var dataSource = new org.h2.jdbcx.JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:dahua-status-rollback;DB_CLOSE_DELAY=-1");
    var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    jdbc.execute("create table channel_status (id bigint primary key, status varchar(20))");
    jdbc.update("insert into channel_status values (21, 'UNKNOWN'), (22, 'UNKNOWN')");
    when(channels.updateByExampleSelective(any(), any())).thenAnswer(invocation -> {
      VideoPlatformChannel row = invocation.getArgument(0);
      Example scope = invocation.getArgument(1);
      Object id = scope.getOredCriteria().getFirst().getAllCriteria().getFirst().getValue();
      return jdbc.update("update channel_status set status=? where id=?", row.getChannelStatus(), id);
    });
    when(devices.updateByExampleSelective(any(), any())).thenReturn(0);
    var proxy = new org.springframework.aop.framework.ProxyFactory(service);
    proxy.setProxyTargetClass(true);
    proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource),
        new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
    var transactional = (DahuaIpcChannelStatusRefresh) proxy.getProxy();

    assertThrows(BaseException.class, () -> transactional.refresh(device,
        snapshot(signal("1", 0, "main", false), signal("1_sub", 0, "sub", false))));

    assertEquals(List.of("UNKNOWN", "UNKNOWN"), jdbc.queryForList(
        "select status from channel_status order by id", String.class));
    verify(channels, times(2)).updateByExampleSelective(any(), any());
    verifyNoInteractions(cache);
  }

  @SafeVarargs private static Map<String, Object> snapshot(Map<String, Object>... channels) {
    return new LinkedHashMap<>(Map.of("channelCatalogAuthoritative", false, "channelList", List.of(channels)));
  }
  private static Map<String, Object> signal(String id, int sdk, String stream, boolean lost) {
    return new LinkedHashMap<>(Map.of("channelId", id, "sdkChannel", sdk, "streamType", stream,
        "channelStateSource", "NET_DEVSTATE_VIDEOLOST", "videoLost", lost));
  }
}
