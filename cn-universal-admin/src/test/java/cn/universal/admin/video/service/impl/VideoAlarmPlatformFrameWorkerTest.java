package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.universal.admin.video.service.VideoIsupPreviewService;
import cn.universal.admin.video.service.VideoOperationAccess;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.util.VideoSnapshotOssHelper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoAlarmPlatformFrameWorkerTest {
  final VideoPlatformInstanceServiceImpl service=mock(VideoPlatformInstanceServiceImpl.class,CALLS_REAL_METHODS);
  final VideoPlatformInstanceMapper instances=mock(VideoPlatformInstanceMapper.class);
  final VideoPlatformDeviceMapper devices=mock(VideoPlatformDeviceMapper.class);
  final VideoPlatformChannelMapper channels=mock(VideoPlatformChannelMapper.class);
  final VideoPlatformDeviceExtMapper extensions=mock(VideoPlatformDeviceExtMapper.class);
  final VideoIsupPreviewService preview=mock(VideoIsupPreviewService.class);
  final VideoZlmStreamProxyService media=mock(VideoZlmStreamProxyService.class);
  final VideoSnapshotOssHelper oss=mock(VideoSnapshotOssHelper.class);
  final VideoOperationAccess permissions=mock(VideoOperationAccess.class);
  final VideoPlatformInstance instance=VideoPlatformInstance.builder().instanceKey("i").tenantId("tenant").platformType("hik_isup").enabled(1)
      .options("{\"mediaServerId\":\"7\",\"subscription\":{\"enabled\":true,\"eventPolicy\":{\"pictureSource\":\"platform_frame\"}}}").build();
  @BeforeEach void setup(){
    for(Class<?> type:List.of(VideoPlatformDevice.class,VideoPlatformChannel.class,VideoPlatformDeviceExt.class))EntityHelper.initEntityNameMap(type,new Config());
    ReflectionTestUtils.setField(service,"instanceMapper",instances);ReflectionTestUtils.setField(service,"deviceMapper",devices);ReflectionTestUtils.setField(service,"channelMapper",channels);
    ReflectionTestUtils.setField(service,"deviceExtMapper",extensions);ReflectionTestUtils.setField(service,"isupPreviewService",preview);ReflectionTestUtils.setField(service,"snapshotOssHelper",oss);
    ReflectionTestUtils.setField(service,"objectMapper",new com.fasterxml.jackson.databind.ObjectMapper());ReflectionTestUtils.setField(service,"videoOperationAccess",permissions);
    var provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(media);ReflectionTestUtils.setField(service,"zlmStreamProxyServiceProvider",provider);
    when(instances.selectOne(any())).thenReturn(instance);
    var device=VideoPlatformDevice.builder().id(17L).instanceKey("i").deviceId("nvr").tenantId("tenant").enabled(1).build();
    when(devices.selectByExample(any())).thenReturn(List.of(device));when(devices.selectByPrimaryKey(17L)).thenReturn(device);
    when(channels.selectByExample(any())).thenReturn(List.of(VideoPlatformChannel.builder().deviceId(17L).instanceKey("i").channelId("33").build()));
    when(extensions.selectByExample(any())).thenReturn(List.of());
    when(preview.start(eq(instance),eq("nvr"),eq("33"),eq("main"),anyMap(),eq("tenant"),eq("7"))).thenReturn(Map.of("mediaServerPrimaryId","7","app","rtp","stream","owned-frame-session"));
    when(media.isRtpPlaybackReady("tenant","7","owned-frame-session")).thenReturn(true);
    when(media.captureStreamSnapshot(eq("tenant"),eq("7"),any(),eq("rtp"),eq("owned-frame-session"))).thenReturn(Map.of("snapshotBase64","camera-frame"));
    when(oss.uploadSnapshotIfPresent(eq(instance),anyMap(),anyMap(),eq("hik_isup"))).thenAnswer(invocation->{assertEquals("tenant",OssTenantContext.getTenantId());return Map.of("ossUrl","https://media.test/real-frame.jpg");});
  }
  @Test void backgroundWorkerUsesSavedTenantAndOwnSessionAndRestoresOssContext(){
    var result=service.captureAlarmPlatformFrame("i","tenant","nvr","33");assertEquals("https://media.test/real-frame.jpg",result.get("ossUrl"));assertNull(OssTenantContext.getTenantId());
    verify(preview).stop(eq(instance),eq("nvr"),eq("33"),eq(Map.of("stream","owned-frame-session","streamType","main")));verifyNoInteractions(permissions);
  }
  @Test void failedCaptureStillReleasesOnlyItsSession(){
    when(media.captureStreamSnapshot(any(),any(),any(),any(),any())).thenThrow(new IllegalStateException("capture unavailable"));
    assertThrows(IllegalStateException.class,()->service.captureAlarmPlatformFrame("i","tenant","nvr","33"));
    verify(preview).stop(eq(instance),eq("nvr"),eq("33"),eq(Map.of("stream","owned-frame-session","streamType","main")));verifyNoInteractions(oss);
  }
  @Test void tenantPolicyAndDeviceChangesRejectBeforeOpeningMedia(){
    assertThrows(RuntimeException.class,()->service.captureAlarmPlatformFrame("i","foreign","nvr","33"));
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"eventPolicy\":{\"pictureSource\":\"device\"}}}");
    assertThrows(RuntimeException.class,()->service.captureAlarmPlatformFrame("i","tenant","nvr","33"));verifyNoInteractions(preview,media,oss);
  }
}
