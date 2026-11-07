package cn.universal.admin.video.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTDeviceSubscribeService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SdkVideoAlarmUpServiceTest {
  @Test void independentFrameBudgetDoesNotReduceOriginalAlarmFlowFrequency() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);request.setChannelId("7");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"eventPolicy\":{\"pictureSource\":\"platform_frame\",\"frameCooldownSeconds\":30}}}");
    var receipts=mock(VideoSdkAlarmReceiptService.class);
    when(receipts.accept(any(),any())).thenReturn(Map.of("receiptId","frame-receipt"));
    when(receipts.frameCooledDown(any(),eq(30))).thenReturn(false,true);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);
    var first=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();first.setId("first");first.setTenantId("tenant-one");first.setLogPersisted(0);
    assertEquals(true,service.publishReceipt(request,first).get("ruleDispatched"));
    request.setEventId("next-real-event");
    var second=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();second.setId("second");second.setTenantId("tenant-one");second.setLogPersisted(0);
    var result=service.publishReceipt(request,second);
    assertEquals(true,result.get("ruleDispatched"));assertEquals(true,result.get("platformFrameCooled"));assertEquals(2,upDispatches);
    verify(receipts,times(1)).accept(any(),argThat(r->"platform_frame_request".equals(r.getAlarmType())));
    verify(logs,times(2)).saveDeviceLog(any(BaseUPRequest.class),any(),any());
    verify(receipts,times(2)).cooledDown(any(),eq(0));
  }
  @Test void sharedCooldownStillSuppressesOriginalDispatchWithoutTakingFrameSlot() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);request.setChannelId("7");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"eventPolicy\":{\"pictureSource\":\"platform_frame\",\"cooldownSeconds\":120}}}");
    var receipts=mock(VideoSdkAlarmReceiptService.class);when(receipts.cooledDown(any(),eq(120))).thenReturn(true);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setId("receipt");receipt.setTenantId("tenant-one");receipt.setLogPersisted(0);
    assertEquals(false,service.publishReceipt(request,receipt).get("ruleDispatched"));assertEquals(0,upDispatches);
    verify(receipts,never()).frameCooledDown(any(),anyInt());verify(receipts,never()).accept(any(),any());
  }
  @Test void failedFrameEnqueueRetriesTheOriginalSlotAndThenDispatchesOriginalAlarm() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);request.setChannelId("7");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"eventPolicy\":{\"pictureSource\":\"platform_frame\"}}}");
    var receipts=mock(VideoSdkAlarmReceiptService.class);
    when(receipts.accept(any(),any())).thenThrow(new cn.universal.common.exception.BaseException("inbox unavailable")).thenReturn(Map.of("receiptId","durable-frame"));
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setId("same-original");receipt.setTenantId("tenant-one");receipt.setLogPersisted(0);
    assertThrows(cn.universal.common.exception.BaseException.class,()->service.publishReceipt(request,receipt));assertEquals(0,upDispatches);
    assertEquals(true,service.publishReceipt(request,receipt).get("ruleDispatched"));assertEquals(1,upDispatches);
    verify(receipts,times(2)).frameCooledDown(eq(receipt),eq(60));
    verify(receipts,times(2)).accept(eq(instance),argThat(r->"frame-request:same-original".equals(r.getEventId())));
  }
  @Test void missingFrameCooldownUsesSafeDefaultAndNoneNeverReservesImageBudget() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);request.setChannelId("7");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"eventPolicy\":{\"pictureSource\":\"platform_frame\"}}}");
    var receipts=mock(VideoSdkAlarmReceiptService.class);when(receipts.accept(any(),any())).thenReturn(Map.of("receiptId","frame"));
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setId("receipt");receipt.setTenantId("tenant-one");receipt.setLogPersisted(0);
    service.publishReceipt(request,receipt);verify(receipts).frameCooledDown(eq(receipt),eq(60));
    clearInvocations(receipts);instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"eventPolicy\":{\"pictureSource\":\"none\"}}}");
    assertEquals(true,service.publishReceipt(request,receipt).get("ruleDispatched"));verify(receipts,never()).frameCooledDown(any(),anyInt());
  }
  @Test void durableMotionProcessUsesNormalizedEventAndDoesNotDecideFlowDispatch() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);request.setChannelId("7");
    var projection=mock(VideoSdkAlarmProcessService.class);when(projection.record(any(),any(),any())).thenReturn("process-real");
    var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(projection);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmProcesses",provider);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",mock(VideoSdkAlarmReceiptService.class));
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setId("raw-receipt");receipt.setTenantId("tenant-one");receipt.setLogPersisted(0);
    var result=service.publishReceipt(request,receipt);
    assertEquals("process-real",result.get("processId"));assertEquals("process-real",published.getData().get("processId"));assertEquals(true,result.get("ruleDispatched"));
    verify(projection).record(eq(instance),eq(receipt),argThat(r->"motion".equals(r.getAlarmType())&&"trace-123".equals(r.getEventId())));
    assertEquals("motion_alias",request.getAlarmType());
  }
  @Test void frameCaptureIsDurableAndDoesNotDelayOriginalFlowAndClipAssociation() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);request.setChannelId("7");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"eventPolicy\":{\"pictureSource\":\"platform_frame\",\"alarmClip\":true}}}");
    var receipts=mock(VideoSdkAlarmReceiptService.class);when(receipts.accept(any(),any())).thenReturn(Map.of("receiptId","frame-receipt"));
    var recordings=mock(cn.universal.admin.video.service.VideoSdkRecordingTaskService.class);when(recordings.create(any(),anyMap(),any())).thenReturn(Map.of("id","clip-task"));
    var provider=mock(org.springframework.beans.factory.ObjectProvider.class);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);
    var recordingProvider=mock(org.springframework.beans.factory.ObjectProvider.class);when(recordingProvider.getObject()).thenReturn(recordings);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"recordingTasks",recordingProvider);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"videoInstances",provider);
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setId("receipt");receipt.setTenantId("tenant-one");receipt.setLogPersisted(0);
    assertEquals(true,service.publishReceipt(request,receipt).get("ruleDispatched"));assertNotNull(published);
    assertEquals("frame-receipt",published.getData().get("platformFrameReceiptId"));assertEquals("clip-task",published.getData().get("alarmClipTaskId"));
    verify(receipts).accept(eq(instance),argThat(frame->"platform_frame_request".equals(frame.getAlarmType())&&"trace-123".equals(frame.getSourceEventId())));
    verify(recordings).create(eq(instance),argThat(input->"trace-123".equals(input.get("sourceEventId"))&&"7".equals(input.get("channelId"))),any());
    verifyNoInteractions(provider);
  }
  @Test void frameWorkerPublishesOriginalEventEvidenceWithoutFlowAgain() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);request.setChannelId("7");request.setAlarmType("platform_frame_request");request.setSourceEventId("source");request.setSourceEventType("motion");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"eventPolicy\":{\"pictureSource\":\"platform_frame\"}}}");
    cache.platformDevice("one","camera",null).setTenantId("tenant-one");
    var receipts=mock(VideoSdkAlarmReceiptService.class);
    var api=mock(cn.universal.admin.video.service.VideoPlatformInstanceService.class);
    when(api.captureAlarmPlatformFrame(anyString(),eq("tenant-one"),anyString(),anyString())).thenReturn(Map.of("ossUrl","https://media.test/frame.jpg"));
    var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getObject()).thenReturn(api);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);org.springframework.test.util.ReflectionTestUtils.setField(service,"videoInstances",provider);
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setId("frame-receipt");receipt.setTenantId("tenant-one");receipt.setCreatedAt(System.currentTimeMillis());
    assertEquals(false,service.publishReceipt(request,receipt).get("ruleDispatched"));assertNull(published);verifyNoInteractions(logs);
    verify(receipts).accept(eq(instance),argThat(evidence->"evidence_ready".equals(evidence.getAlarmType())&&"gateway".equals(evidence.getSdkEventFamily())&&"source".equals(evidence.getSourceEventId())&&"ready".equals(evidence.getImageStatus())));
  }

  @Test void archivedFrameAndClipUseActualCatalogAndStayOutsideAlarmFlow() {
    instance.setPlatformType("hik_isup");instance.setEnabled(1);
    var actualCatalog = new VideoSdkEventCatalog(
        mock(cn.universal.persistence.mapper.admin.SysDictDataMapper.class),
        mock(cn.universal.persistence.mapper.admin.SysDictTypeMapper.class),
        new com.fasterxml.jackson.databind.ObjectMapper());
    org.springframework.test.util.ReflectionTestUtils.setField(service,"eventCatalog",actualCatalog);
    var receipts=mock(VideoSdkAlarmReceiptService.class);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);
    var task=new cn.universal.persistence.entity.VideoSdkRecordTask();
    task.setId("clip-task");task.setDeviceId("camera");task.setChannelId("7");task.setCreatedAt(System.currentTimeMillis());
    service.publishRecordingEvidence(instance,task,Map.of("sourceEventId","original","sourceEventType","motion"),Map.of("url","https://media.test/clip.mp4","status","partial","actualDurationSeconds",6.842,"requestedDurationSeconds",8,"coverageStatus","partial"));
    var captor=org.mockito.ArgumentCaptor.forClass(SdkVideoAlarmCallbackRequest.class);
    verify(receipts).accept(eq(instance),captor.capture());
    var update=captor.getValue();
    assertEquals("gateway",update.getSdkEventFamily());
    var reference=(Map<?,?>)((List<?>)update.getEvidence()).getFirst();assertEquals("partial",reference.get("status"));assertEquals(6.842,reference.get("actualDurationSeconds"));assertEquals(8,reference.get("requestedDurationSeconds"));assertEquals("partial",reference.get("coverageStatus"));
    var receipt=new cn.universal.persistence.entity.VideoSdkAlarmReceipt();receipt.setTenantId("tenant-one");receipt.setId("clip-receipt");receipt.setLogPersisted(0);
    var projection=mock(VideoSdkAlarmProcessService.class);when(projection.record(any(),any(),any())).thenReturn("original-process");
    var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(projection);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmProcesses",provider);
    var result=service.publishReceipt(update,receipt);
    assertEquals("sdkEvidenceReady",result.get("event"));
    assertEquals(true,result.get("evidenceUpdate"));assertEquals(false,result.get("ruleDispatched"));
    assertEquals("original-process",result.get("processId"));assertNull(published);
    verify(projection).record(eq(instance),eq(receipt),argThat(e->"evidence_ready".equals(e.getAlarmType())&&"original".equals(e.getSourceEventId())));
    var event=org.mockito.ArgumentCaptor.forClass(BaseUPRequest.class);
    verify(logs).saveDeviceLog(event.capture(),any(),any());
    assertEquals("original-process",event.getValue().getData().get("processId"));
    verify(shadows).doShadow(eq(event.getValue()),any());
    update.setSourceEventType("intrusion");
    assertEquals(true,service.publishReceipt(update,receipt).get("filtered"));
  }
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final SdkVideoAlarmContextCache cache = mock(SdkVideoAlarmContextCache.class);
  private final VideoSdkEventCatalog catalog = mock(VideoSdkEventCatalog.class);
  private final IIoTDeviceDataService logs = mock(IIoTDeviceDataService.class);
  private final IoTDeviceShadowService shadows = mock(IoTDeviceShadowService.class);
  private final IoTDeviceSubscribeService subscriptions = mock(IoTDeviceSubscribeService.class);
  private BaseUPRequest published;
  private int upDispatches;
  private final SdkVideoAlarmUpService service =
      new SdkVideoAlarmUpService(instances, cache, catalog, logs, shadows, subscriptions) {
        @Override
        protected String doUp(List<BaseUPRequest> requests) {
          published = requests.getFirst();
          upDispatches++;
          return "ok";
        }
      };
  private final VideoPlatformInstance instance = new VideoPlatformInstance();
  private final SdkVideoAlarmCallbackRequest request = new SdkVideoAlarmCallbackRequest();

  @BeforeEach
  void setup() {
    org.springframework.test.util.ReflectionTestUtils.setField(service, "thingModelSynchronizer",
        mock(cn.universal.dm.video.VideoThingModelSynchronizer.class));
    instance.setInstanceKey("one");
    instance.setPlatformType("dahua_sdk");
    instance.setTenantId("tenant-one");
    instance.setOptions(
        "{\"subscription\":{\"enabled\":true,\"token\":\"test-token\",\"topics\":[\"motion\"]}}");
    when(instances.selectOne(any())).thenReturn(instance);
    request.setInstanceKey("one");
    request.setDeviceId("camera");
    request.setAlarmType("motion_alias");
    request.setEventId("trace-123");
    when(catalog.resolve(anyString(), anyString(), anyMap()))
        .thenReturn(
            new VideoSdkEventCatalog.Event("motion", "motionDetection", "移动侦测", "dahua_sdk", ""));
    var platformDevice = new VideoPlatformDevice();
    platformDevice.setDeviceId("camera");
    platformDevice.setEnabled(1);
    when(cache.platformDevice("one", "camera", null)).thenReturn(platformDevice);
    var device = new IoTDevice();
    device.setDeviceId("camera");
    device.setProductKey("one");
    device.setIotId("iot-camera");
    device.setTenantId("tenant-one");
    when(cache.device("one", "camera", null)).thenReturn(device);
    var product = new IoTProduct();
    product.setProductKey("one");
    product.setTenantId("tenant-one");
    when(cache.product(instance)).thenReturn(product);
  }

  @Test
  void vendorDataIsAvailableForConditionsWithoutCallbackCredentials() {
    request.setRaw(Map.of("score", 0.92, "token", "private-token",
        "analysis", Map.of("objectCount", 2, "password", "private-password")));
    service.publish(request, "test-token");
    assertEquals(0.92, published.getData().get("score"));
    assertFalse(published.getData().containsKey("token"));
    assertFalse(published.getData().containsKey("rawData"));
    assertNull(published.getRawData());
    assertEquals(Map.of("objectCount", 2), published.getData().get("analysis"));
  }

  @Test
  void wvpIntrusionFlowsThroughSinkCatalogAndUnifiedEventPipeline() {
    instance.setPlatformType("wvp");
    instance.setEnabled(1);
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"cameraAlarm\"]}}");
    var dictData = mock(cn.universal.persistence.mapper.admin.SysDictDataMapper.class);
    var dictTypes = mock(cn.universal.persistence.mapper.admin.SysDictTypeMapper.class);
    var dictType = new cn.universal.persistence.entity.admin.SysDictType();
    dictType.setStatus("0");
    when(dictTypes.selectOne(any())).thenReturn(dictType);
    when(dictData.selectDictDataByType("video_sdk_event_wvp")).thenReturn(List.of(
        new cn.universal.persistence.entity.admin.SysDictData().setDictCode(1L).setDictSort(1)
            .setDictValue("5:6").setDictLabel("区域入侵").setStatus("0")
            .setRemark("{\"type\":\"intrusion\",\"eventId\":\"regionIntrusion\"}"),
        new cn.universal.persistence.entity.admin.SysDictData().setDictCode(2L).setDictSort(2)
            .setDictValue("5:2").setDictLabel("移动侦测").setStatus("0")
            .setRemark("{\"type\":\"motion\",\"eventId\":\"motionDetection\"}")));
    var actualCatalog = new VideoSdkEventCatalog(dictData, dictTypes, new com.fasterxml.jackson.databind.ObjectMapper());
    org.springframework.test.util.ReflectionTestUtils.setField(service, "eventCatalog", actualCatalog);
    var sink = new WvpVideoAlarmSink(service);
    var alarm = Map.<String, Object>of("id", 24151, "deviceId", "camera", "channelId", "channel-2",
        "alarmMethod", "5", "alarmType", "6", "alarmPriority", "1", "alarmTime", "2026-10-16 12:02:44");
    sink.accept("wvp", "one", alarm);
    assertEquals("regionIntrusion", published.getEvent());
    assertEquals("区域入侵", published.getEventName());
    assertFalse(published.getData().containsKey("alarmType"));
    assertEquals("intrusion", published.getData().get("cameraEventType"));
    assertFalse(published.getData().containsKey("alarmName"));
    assertEquals("5:6", published.getData().get("vendorEventCode"));
    assertFalse(published.getData().containsKey("vendorEventCodeHex"));
    assertEquals("6", published.getData().get("gbAlarmType"));
    assertEquals("GB28181", published.getData().get("sdkEventFamily"));
    assertEquals("unknown", published.getData().get("alarmState"));
    assertFalse(published.getData().containsKey("rawData"));
    assertEquals("5", published.getData().get("alarmMethod"));
    assertEquals("iot-camera", published.getIotId());
    verify(logs).saveDeviceLogDirect(eq(published), any(), any());
    verify(shadows).doShadow(eq(published), any());
    // 具体事件筛选可用，原来的 cameraAlarm 全量订阅继续匹配。
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"]}}");
    sink.accept("wvp", "one", alarm);
    assertEquals(1, upDispatches);
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"intrusion\"]}}");
    sink.accept("wvp", "one", alarm);
    assertEquals(2, upDispatches);
    // 同一通道的另一种国标报警必须切换外层 event，而非只改变 data 中的类型。
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"cameraAlarm\"]}}");
    var motion = new java.util.LinkedHashMap<>(alarm);
    motion.put("id", 24153);
    motion.put("alarmType", "2");
    sink.accept("wvp", "one", motion);
    assertEquals("motionDetection", published.getEvent());
    assertEquals("移动侦测", published.getEventName());
    assertEquals("motion", published.getData().get("cameraEventType"));
    assertEquals("5:2", published.getData().get("vendorEventCode"));
    assertFalse(published.getData().containsKey("rawData"));
    assertEquals(3, upDispatches);
    motion.put("alarmType", "999");
    sink.accept("wvp", "one", motion);
    assertEquals("wvpGb28181Event", published.getEvent());
    assertEquals("vendor_raw", published.getData().get("cameraEventType"));
    assertEquals(4, upDispatches);
  }

  @Test
  void wvpUsesDeviceLogsButCannotBypassSdkHttpAuthentication() {
    instance.setPlatformType("wvp");
    instance.setEnabled(1);
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"cameraAlarm\"]}}");
    request.setAlarmType("vendor_raw");
    when(catalog.resolve(anyString(), anyString(), anyMap()))
        .thenReturn(new VideoSdkEventCatalog.Event("vendor_raw", "wvpGb28181Event", "国标未解析事件", "GB28181", ""));
    assertThrows(RuntimeException.class, () -> service.publish(request, "test-token"));
    assertEquals("wvpGb28181Event", service.publishFromWvp(request).get("event"));
    verify(logs).saveDeviceLogDirect(eq(published), any(), any());
    assertEquals("iot-camera", published.getIotId());
    instance.setTenantId("other-tenant");
    assertThrows(RuntimeException.class, () -> service.publishFromWvp(request));
  }

  @Test
  void aliasesUseCanonicalSubscriptionAndOneTimestampWithoutMistakingTraceForVendorCode() {
    request.setData(Map.of("instanceKey", "spoofed"));
    var result = service.publish(request, "test-token");
    assertEquals("motionDetection", result.get("event"));
    assertEquals(published.getTime(), published.getData().get("alarmTime"));
    assertEquals("one", published.getData().get("instanceKey"));
    assertEquals("trace-123", published.getData().get("eventId"));
    assertFalse(published.getData().containsKey("vendorEventCode"));
    verify(logs).saveDeviceLog(any(BaseUPRequest.class), any(), any());
    verify(shadows).doShadow(any(), any());
  }

  @Test
  void tokenChangesTakeEffectOnNextCallback() {
    service.publish(request, "test-token");
    clearInvocations(cache, logs);
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"token\":\"rotated\"}}");
    assertThrows(RuntimeException.class, () -> service.publish(request, "test-token"));
    verifyNoInteractions(cache, logs);
    verify(instances, times(2)).selectOne(any());
  }

  @Test
  void unselectedEventsNeverLoadDevicesAndSelectedWithoutIdsFailsClosed() {
    instance.setOptions(
        "{\"subscription\":{\"enabled\":true,\"token\":\"test-token\",\"topics\":[\"intrusion\"]}}");
    assertEquals(true, service.publish(request, "test-token").get("filtered"));
    verifyNoInteractions(cache, logs);
    instance.setOptions(
        "{\"subscription\":{\"enabled\":true,\"token\":\"test-token\",\"scope\":\"selected\"}}");
    assertEquals(true, service.publish(request, "test-token").get("filtered"));
    verify(cache, never()).device(any(), any(), any());
  }

  @Test void emptyMissingOrDisabledTopicsNeverLoadDevicesOrDispatchForAllSdkPlatforms() {
    for (String platform : List.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "hik_isup")) {
      instance.setPlatformType(platform);
      for (String subscription : List.of("{\"enabled\":true,\"topics\":[]}", "{\"enabled\":true}", "{\"enabled\":false,\"topics\":[\"motion\"]}")) {
        instance.setOptions("{\"subscription\":" + subscription.substring(0, subscription.length()-1) + ",\"token\":\"test-token\"}}");
        assertEquals(true, service.publish(request, "test-token").get("filtered"));
      }
    }
    verifyNoInteractions(cache, logs, shadows);
  }

  @Test void nativeCodeResolutionCannotBeBypassedByOldRequestAlarmType() {
    request.setAlarmType("motion"); request.setVendorEventCode("0x9999");
    when(catalog.resolve(anyString(), anyString(), anyMap())).thenReturn(
        new VideoSdkEventCatalog.Event("vendor_raw", "dahuaSdkEvent", "原始事件", "dahua_sdk", ""));
    assertEquals(true, service.publish(request, "test-token").get("filtered"));
    verifyNoInteractions(cache, logs, shadows);
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"token\":\"test-token\",\"topics\":[\"vendor_raw\"]}}");
    assertEquals("dahuaSdkEvent",service.publish(request,"test-token").get("event"));
  }

  @Test void isupUnsubscribedEventsAreRejectedBeforeDurableInbox() {
    instance.setPlatformType("hik_isup");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"token\":\"test-token\",\"topics\":[\"intrusion\"]}}");
    var receipts = mock(VideoSdkAlarmReceiptService.class);
    org.springframework.test.util.ReflectionTestUtils.setField(service,"alarmReceipts",receipts);
    assertEquals(true,service.publish(request,"test-token").get("filtered"));
    verifyNoInteractions(receipts,cache,logs);
  }

  @Test
  void missingProductAndTenantMismatchNeverPublish() {
    when(cache.product(instance)).thenReturn(null);
    assertThrows(RuntimeException.class, () -> service.publish(request, "test-token"));
    var product = new IoTProduct();
    product.setProductKey("one");
    product.setTenantId("other-tenant");
    when(cache.product(instance)).thenReturn(product);
    assertThrows(RuntimeException.class, () -> service.publish(request, "test-token"));
    verifyNoInteractions(logs, shadows);
  }

  @Test
  void topLevelSdkIdentifiersReachCatalogAndEventData() {
    request.setVendorEventCode("0x2102");
    request.setVendorAlarmType("0x03");
    request.setSdkEventFamily("ALARM");
    service.publish(request, "test-token");
    verify(catalog)
        .resolve(
            eq("dahua_sdk"),
            eq("motion_alias"),
            argThat(
                attributes ->
                    "0x2102".equals(attributes.get("vendorEventCode"))
                        && "0x03".equals(attributes.get("vendorAlarmType"))));
    assertEquals("0x2102", published.getData().get("vendorEventCode"));
    assertEquals("motion", published.getData().get("cameraEventType"));
  }

  @Test
  void isupIsapiTopLevelTypeReachesCatalogAndPreservesVendorName() throws Exception {
    instance.setPlatformType("hik_isup");
    request.setPlatformType("hik_sdk"); // 路由来自可信实例，不信任网关声明的平台类型。
    request.setAlarmType("motion");
    request.setCameraEventType("VMD");
    request.setVendorEventCode("13");
    service.publish(request, "test-token");
    verify(catalog).resolve(eq("hik_isup"), eq("motion"),
        argThat(attributes -> "VMD".equals(attributes.get("cameraEventType"))
            && "13".equals(attributes.get("vendorEventCode"))));
    assertEquals("motion", published.getData().get("cameraEventType"));
    assertFalse(published.getData().containsKey("vendorCameraEventType"));
    assertEquals("hik_isup", published.getData().get("platformType"));
    var decoded = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
        "{\"cameraEventType\":\"VMD\",\"alarmTime\":1791889200000}", SdkVideoAlarmCallbackRequest.class);
    assertEquals("VMD", decoded.getCameraEventType());
    assertEquals(1791889200000L, decoded.getAlarmTime());
  }

  @Test
  void originalIsupAlarmStoresNativeImagesAndSourceFieldsInTheSameEvent() throws Exception {
    instance.setPlatformType("hik_isup");
    request.setAlarmType("motion");
    request.setSourceEventId("vendor-event-1");
    request.setActivePostCount(3);
    request.setPictureCount(1);
    request.setImageStatus("stored");
    request.setImages(List.of(Map.of("snapshotUrl", "https://media.example.test/event.jpg",
        "gatewayStored", true, "sha256", "image-sha", "token", "private-token")));
    service.publish(request, "test-token");
    assertEquals("vendor-event-1", published.getData().get("sourceEventId"));
    assertEquals(3, published.getData().get("activePostCount"));
    assertEquals(1, published.getData().get("pictureCount"));
    assertEquals("stored", published.getData().get("imageStatus"));
    assertEquals(List.of(Map.of("url", "https://media.example.test/event.jpg",
        "status", "stored", "sha256", "image-sha")), published.getData().get("images"));
    verify(logs).saveDeviceLog(eq(published), any(), any());
    assertEquals("motionDetection", published.getEvent());
    var decoded = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
        "{\"images\":[{\"snapshotUrl\":\"https://media.example.test/event.jpg\"}],"
            + "\"sourceEventId\":\"vendor-event-1\",\"pictureCount\":1}", SdkVideoAlarmCallbackRequest.class);
    assertEquals(1, decoded.getImages().size());
    assertEquals("vendor-event-1", decoded.getSourceEventId());
  }

  @Test
  void nestedImageAndSourceDataTakePriorityOverTopLevelCompatibilityFields() {
    request.setData(Map.of("images", List.of(Map.of("url", "https://media.example.test/nested.jpg")),
        "imageStatus", "stored", "sourceEventId", "nested-source", "pictureCount", 2));
    request.setImages(List.of(Map.of("url", "https://media.example.test/top.jpg")));
    request.setImageStatus("failed");
    request.setSourceEventId("top-source");
    request.setPictureCount(1);
    service.publish(request, "test-token");
    assertEquals(List.of(Map.of("url", "https://media.example.test/nested.jpg", "status", "stored")),
        published.getData().get("images"));
    assertEquals("nested-source", published.getData().get("sourceEventId"));
    assertEquals("stored", published.getData().get("imageStatus"));
    assertEquals(2, published.getData().get("pictureCount"));
  }

  @Test
  void isupLateEvidenceIsLoggedAndUpdatesShadowWithoutPublishingToFlowAgain() {
    instance.setPlatformType("hik_isup");
    request.setAlarmType("evidence_ready");
    request.setSourceEventType("motion");
    request.setSourceEventId("original-gateway-event");
    request.setImageStatus("stored");
    request.setImages(List.of(Map.of("snapshotUrl", "https://media.example.test/late.jpg")));
    when(catalog.resolve(eq("hik_isup"), eq("evidence_ready"), anyMap()))
        .thenReturn(new VideoSdkEventCatalog.Event("evidence_ready", "sdkEvidenceReady", "告警取证更新", "hik_isup", ""));
    Map<String, Object> result = service.publish(request, "test-token");
    assertEquals("sdkEvidenceReady", result.get("event"));
    assertEquals(true, result.get("evidenceUpdate"));
    assertEquals(false, result.get("ruleDispatched"));
    assertNull(published);
    var captor = org.mockito.ArgumentCaptor.forClass(BaseUPRequest.class);
    verify(logs).saveDeviceLog(captor.capture(), any(), any());
    verify(shadows).doShadow(eq(captor.getValue()), any());
    assertEquals("original-gateway-event", captor.getValue().getData().get("sourceEventId"));
    assertEquals(List.of(Map.of("url", "https://media.example.test/late.jpg", "status", "stored")), captor.getValue().getData().get("images"));
  }

  @Test
  void unselectedOriginalTypeCannotAdmitIsupLateEvidence() {
    instance.setPlatformType("hik_isup");
    request.setAlarmType("evidence_ready");
    request.setSourceEventType("intrusion");
    when(catalog.resolve(eq("hik_isup"), eq("evidence_ready"), anyMap()))
        .thenReturn(new VideoSdkEventCatalog.Event("evidence_ready", "sdkEvidenceReady", "告警取证更新", "hik_isup", ""));
    assertEquals(true, service.publish(request, "test-token").get("filtered"));
    assertNull(published);
    verifyNoInteractions(cache, logs, shadows);
  }

  @Test
  void compatibilityEvidenceReferencesExcludeNestedCredentials() {
    request.setEvidence(List.of(Map.of("url", "https://media.example.test/evidence.jpg",
        "status", "ready", "secret", "private-secret",
        "metadata", Map.of("size", 300, "authorization", "private-authorization"))));
    service.publish(request, "test-token");
    assertEquals(List.of(Map.of("url", "https://media.example.test/evidence.jpg", "status", "ready",
        "metadata", Map.of("size", 300))), published.getData().get("evidence"));
  }

  @Test
  void existingDirectSdkEvidencePublishingRetainsItsBehavior() {
    request.setAlarmType("evidence_ready");
    request.setSourceEventType("motion");
    when(catalog.resolve(eq("dahua_sdk"), eq("evidence_ready"), anyMap()))
        .thenReturn(new VideoSdkEventCatalog.Event("evidence_ready", "sdkEvidenceReady", "告警取证更新", "dahua_sdk", ""));
    Map<String, Object> result = service.publish(request, "test-token");
    assertEquals("sdkEvidenceReady", published.getEvent());
    assertFalse(result.containsKey("ruleDispatched"));
    verify(logs).saveDeviceLog(eq(published), any(), any());
  }
}
