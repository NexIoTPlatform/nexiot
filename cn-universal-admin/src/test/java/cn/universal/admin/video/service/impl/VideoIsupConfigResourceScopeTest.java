package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.video.VideoOperationRegistry;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoIsupConfigResourceScopeTest {
  @Test void alarmInputUsesOwnedNativeVideoChannelAndChannelResourcesNeverDefaultToOne() {
    var service=mock(VideoPlatformInstanceServiceImpl.class,CALLS_REAL_METHODS);
    var devices=mock(VideoPlatformDeviceMapper.class);var channels=mock(VideoPlatformChannelMapper.class);
    ReflectionTestUtils.setField(service,"deviceMapper",devices);ReflectionTestUtils.setField(service,"channelMapper",channels);
    var instance=VideoPlatformInstance.builder().instanceKey("i").tenantId("tenant").enabled(1).build();
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().id(17L).deviceId("nvr").tenantId("tenant").enabled(1).build());
    var operation=VideoOperationRegistry.requirePublic("deviceConfigRead");
    var input=Map.<String,Object>of("deviceId","nvr","resource","alarmInput","alarmInputNo",1,"ptzChannelNo",33);
    assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"validateIsupOperationResource",instance,operation,input));
    when(channels.selectCount(argThat(channel->channel!=null&&Long.valueOf(17L).equals(channel.getDeviceId())&&"i".equals(channel.getInstanceKey())&&"33".equals(channel.getChannelNo())))).thenReturn(1);
    assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(service,"validateIsupOperationResource",instance,operation,input));
    for(String resource:java.util.List.of("serial485","armingSchedule","alarmLinkage","algorithmPassenger","encodingNative"))
      assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"validateIsupOperationResource",instance,operation,Map.of("deviceId","nvr","resource",resource)));
  }
}
