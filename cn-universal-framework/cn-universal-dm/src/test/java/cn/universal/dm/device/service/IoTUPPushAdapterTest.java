package cn.universal.dm.device.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.event.DeviceUplinkEvent;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.push.UPProcessorManager;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.base.IoTDeviceExtendService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.bo.UPPushBO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IoTUPPushAdapter 测试")
class IoTUPPushAdapterTest {

  @Test
  @DisplayName("设备DTO应用为空但请求携带应用时应继续推送")
  void shouldUseRequestApplicationIdWhenDeviceDtoApplicationIsBlank() throws Exception {
    CapturingPushConfigService pushConfigService = new CapturingPushConfigService();
    CapturingPushExecutor pushExecutor = new CapturingPushExecutor();
    TestableAdapter adapter = adapter(pushConfigService, pushExecutor);
    UPPushBO config = UPPushBO.builder().build();
    IoTPushResult pushResult =
        IoTPushResult.success("industrial", "pk-1", "dev-1", "HTTP", "{}", 1L);
    pushConfigService.config = config;
    pushExecutor.results = List.of(pushResult);

    BaseUPRequest request =
        BaseUPRequest.builder()
            .ioTDeviceDTO(
                IoTDeviceDTO.builder()
                    .productKey("pk-1")
                    .deviceId("dev-1")
                    .applicationId("")
                    .build())
            .productKey("pk-1")
            .deviceId("dev-1")
            .applicationId("app-request")
            .build();

    List<IoTPushResult> results = adapter.execute(List.of(request));

    assertEquals(List.of(pushResult), results);
    assertEquals("app-request", request.getApplicationId());
    assertEquals("app-request", pushConfigService.lastApplicationId);
    assertSame(config, pushExecutor.lastConfig);
    assertEquals(1, pushExecutor.lastRequests.size());
    assertSame(request, pushExecutor.lastRequests.get(0));
  }

  @Test
  @DisplayName("设备DTO应用优先于请求应用")
  void shouldPreferDeviceDtoApplicationId() throws Exception {
    CapturingPushConfigService pushConfigService = new CapturingPushConfigService();
    CapturingPushExecutor pushExecutor = new CapturingPushExecutor();
    TestableAdapter adapter = adapter(pushConfigService, pushExecutor);
    UPPushBO config = UPPushBO.builder().build();
    pushConfigService.config = config;

    BaseUPRequest request =
        BaseUPRequest.builder()
            .ioTDeviceDTO(
                IoTDeviceDTO.builder()
                    .productKey("pk-1")
                    .deviceId("dev-1")
                    .applicationId("app-device")
                    .build())
            .productKey("pk-1")
            .deviceId("dev-1")
            .applicationId("app-request")
            .build();

    adapter.execute(List.of(request));

    assertEquals("app-device", request.getApplicationId());
    assertEquals("app-device", pushConfigService.lastApplicationId);
  }

  @Test
  @DisplayName("规则前置处理应在best-effort推送入队前完成")
  void shouldCompleteBeforePushBeforeDispatchingBestEffortPush() throws Exception {
    CapturingPushConfigService pushConfigService = new CapturingPushConfigService();
    CapturingPushExecutor pushExecutor = new CapturingPushExecutor();
    TestableAdapter adapter = adapter(pushConfigService, pushExecutor);
    CapturingDispatcher dispatcher = new CapturingDispatcher();
    setField(adapter, "upPushDispatcher", dispatcher);
    setField(adapter, "upRequestSnapshotter", new UPRequestSnapshotter());
    setField(adapter, "upProcessorManager", new UPProcessorManager(List.of()));

    BaseUPRequest request =
        BaseUPRequest.builder()
            .iotId("iot-1")
            .productKey("pk-1")
            .deviceId("dev-1")
            .applicationId("app-1")
            .ioTDeviceDTO(
                IoTDeviceDTO.builder()
                    .iotId("iot-1")
                    .productKey("pk-1")
                    .deviceId("dev-1")
                    .applicationId("app-1")
                    .build())
            .build();

    assertTrue(adapter.submit(List.of(request)));
    assertEquals(1, adapter.beforeCalls);
    assertEquals(0, adapter.pushCalls);

    dispatcher.task.run();

    assertEquals(1, adapter.beforeCalls);
    assertEquals(1, adapter.pushCalls);
  }

  @Test
  @DisplayName("队列已满时不应创建异步请求快照")
  void shouldSkipSnapshotWhenPushStripeIsAlreadyFull() throws Exception {
    TestableAdapter adapter =
        adapter(new CapturingPushConfigService(), new CapturingPushExecutor());
    CapturingDispatcher dispatcher = new CapturingDispatcher();
    dispatcher.accepts = false;
    CapturingSnapshotter snapshotter = new CapturingSnapshotter();
    setField(adapter, "upPushDispatcher", dispatcher);
    setField(adapter, "upRequestSnapshotter", snapshotter);
    setField(adapter, "upProcessorManager", new UPProcessorManager(List.of()));

    BaseUPRequest request =
        BaseUPRequest.builder().iotId("iot-1").productKey("pk-1").deviceId("dev-1").build();

    assertFalse(adapter.submit(List.of(request)));
    assertEquals(1, adapter.beforeCalls);
    assertEquals(0, snapshotter.calls);
    assertEquals(1, dispatcher.rejectedMessages);
  }

  @Test
  void nativeFlowSurvivesApplicationQueueSaturation() throws Exception {
    var adapter = adapter(new CapturingPushConfigService(), new CapturingPushExecutor());
    var dispatcher = new CapturingDispatcher();
    dispatcher.accepts = false;
    setField(adapter, "upPushDispatcher", dispatcher);
    List<DeviceUplinkEvent> events = new ArrayList<>();
    setField(
        adapter,
        "deviceUplinkPublisher",
        new DeviceUplinkPublisher(
            List.of((identity, snapshot) -> events.add(snapshot.get())),
            new SimpleMeterRegistry()));

    assertFalse(adapter.submit(List.of(nativeRequest("application"))));
    assertEquals(1, events.size());
    assertEquals("application", events.getFirst().identity().applicationId());
    assertEquals(1, dispatcher.rejectedMessages);
  }

  @Test
  void nativeFlowSurvivesMissingAndDisabledApplication() throws Exception {
    for (String application : new String[] {null, "disabled-app"}) {
      var config = new CapturingPushConfigService();
      config.enabled = false;
      var pushes = new CapturingPushExecutor();
      var adapter = adapter(config, pushes);
      List<DeviceUplinkEvent> events = new ArrayList<>();
      setField(
          adapter,
          "deviceUplinkPublisher",
          new DeviceUplinkPublisher(
              List.of((identity, snapshot) -> events.add(snapshot.get())),
              new SimpleMeterRegistry()));

      adapter.doUpDirect(List.of(nativeRequest(application)));

      assertEquals(1, events.size());
      assertEquals(application, events.getFirst().identity().applicationId());
      assertTrue(pushes.lastRequests.isEmpty());
    }
  }

  private BaseUPRequest nativeRequest(String application) {
    return BaseUPRequest.builder()
        .messageType(MessageType.PROPERTIES)
        .iotId("pd")
        .productKey("p")
        .deviceId("d")
        .ioTDeviceDTO(
            IoTDeviceDTO.builder()
                .tenantId("tenant")
                .iotId("pd")
                .productKey("p")
                .deviceId("d")
                .applicationId(application)
                .build())
        .build();
  }

  private TestableAdapter adapter(
      UPPushConfigService pushConfigService, UPPushExecutor pushExecutor) throws Exception {
    TestableAdapter adapter = new TestableAdapter();
    setField(adapter, "deviceExtTemplate", new IoTDeviceExtendService(List.of()));
    setField(adapter, "upPushConfigService", pushConfigService);
    setField(adapter, "upPushExecutor", pushExecutor);
    return adapter;
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = IoTUPPushAdapter.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static final class TestableAdapter extends IoTUPPushAdapter<BaseUPRequest> {
    private int beforeCalls;
    private int pushCalls;

    private List<IoTPushResult> execute(List<BaseUPRequest> requests) {
      return executePush(requests);
    }

    private boolean submit(List<BaseUPRequest> requests) {
      return submitUp(requests);
    }

    @Override
    protected void onBeforePush(List<BaseUPRequest> upRequests) {
      beforeCalls++;
    }

    @Override
    protected List<IoTPushResult> executePush(List<BaseUPRequest> upRequests) {
      pushCalls++;
      return super.executePush(upRequests);
    }
  }

  private static final class CapturingDispatcher extends UPPushDispatcher {
    private Runnable task;
    private boolean accepts = true;
    private int rejectedMessages;

    private CapturingDispatcher() {
      super(new SimpleMeterRegistry(), 1, 1, 1_000, 1_000);
    }

    @Override
    public boolean dispatch(String orderingKey, int messageCount, Runnable runnable) {
      this.task = runnable;
      return true;
    }

    @Override
    boolean canAccept(String orderingKey) {
      return accepts;
    }

    @Override
    void recordRejected(int messageCount) {
      rejectedMessages += messageCount;
    }
  }

  private static final class CapturingSnapshotter extends UPRequestSnapshotter {
    private int calls;

    @Override
    public <T extends BaseUPRequest> List<T> snapshot(List<T> requests) {
      calls++;
      return super.snapshot(requests);
    }
  }

  private static final class CapturingPushConfigService extends UPPushConfigService {
    private boolean enabled = true;
    private String lastApplicationId;
    private UPPushBO config = UPPushBO.builder().build();

    @Override
    public UPPushBO getPushConfig(String applicationId) {
      this.lastApplicationId = applicationId;
      return config;
    }

    @Override
    public boolean isApplicationEnabled(String applicationId) {
      return enabled;
    }
  }

  private static final class CapturingPushExecutor extends UPPushExecutor {
    private List<BaseUPRequest> lastRequests = new ArrayList<>();
    private UPPushBO lastConfig;
    private List<IoTPushResult> results = List.of();

    @Override
    public boolean hasEnabledChannel(BaseUPRequest request, UPPushBO config) {
      return true;
    }

    @Override
    public List<IoTPushResult> executeBatchPush(List<BaseUPRequest> requests, UPPushBO config) {
      this.lastRequests = new ArrayList<>(requests);
      this.lastConfig = config;
      return results;
    }
  }
}
