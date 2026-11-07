package cn.universal.admin.video.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoSdkRecordingTaskService;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTDeviceSubscribeService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.dm.video.*;
import cn.universal.persistence.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

class VideoAlarmSpringWiringTest {
  @Test void eagerWvpRegistryAndAlarmRecordingServicesStartWithCircularReferencesDisabled() {
    try(var context=new GenericApplicationContext()) {
      context.getDefaultListableBeanFactory().setAllowCircularReferences(false);
      AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
      // Register the registry first, reproducing production's eager WVP construction path.
      context.registerBean(VideoPlatformAdapterRegistry.class,()->new VideoPlatformAdapterRegistry(List.of(context.getBean("wvpAdapter",VideoPlatformAdapter.class))));
      context.registerBean("wvpAdapter",VideoPlatformAdapter.class,()->{
        context.getBean(WvpVideoAlarmSink.class);
        var adapter=mock(VideoPlatformAdapter.class);when(adapter.getSupportedPlatformType()).thenReturn("wvp");return adapter;
      });
      context.registerBean(WvpVideoAlarmSink.class,()->new WvpVideoAlarmSink(context.getBean(SdkVideoAlarmUpService.class)));
      context.registerBean(SdkVideoAlarmUpService.class,()->new SdkVideoAlarmUpService(mock(VideoPlatformInstanceMapper.class),mock(SdkVideoAlarmContextCache.class),mock(VideoSdkEventCatalog.class),mock(IIoTDeviceDataService.class),mock(IoTDeviceShadowService.class),mock(IoTDeviceSubscribeService.class)));
      context.registerBean(VideoSdkAlarmReceiptService.class,()->new VideoSdkAlarmReceiptService(mock(VideoSdkAlarmReceiptMapper.class),new ObjectMapper(),context.getBeanProvider(SdkVideoAlarmUpService.class)));
      context.registerBean(VideoSdkRecordingTaskService.class,()->new VideoSdkRecordingTaskService(mock(VideoSdkRecordTaskMapper.class),mock(VideoPlatformInstanceMapper.class),mock(VideoPlatformDeviceMapper.class),mock(VideoPlatformChannelMapper.class),context.getBean(VideoPlatformAdapterRegistry.class),new ObjectMapper()));
      context.getBeanFactory().registerSingleton("thingModelSynchronizer",mock(VideoThingModelSynchronizer.class));
      for(var field:IoTUPPushAdapter.class.getDeclaredFields()) {
        if((field.isAnnotationPresent(Autowired.class)||field.isAnnotationPresent(Resource.class)) && field.getType()!=List.class)
          context.getBeanFactory().registerSingleton(field.getName(),mock(field.getType()));
      }
      context.refresh();
      assertTrue(context.getBean(VideoPlatformAdapterRegistry.class).isSupported("wvp"));
      assertNotNull(context.getBean(VideoSdkRecordingTaskService.class));
      assertNotNull(context.getBean(SdkVideoAlarmUpService.class));
    }
  }
}
