package cn.universal.dm.device.service.log.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.log.IIoTDeviceLogService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DeviceLogWriteBufferTest {

  @Test
  void shouldBatchAcceptedRequestsWithoutMqFanout() throws Exception {
    CapturingLogService service = new CapturingLogService(2);
    CapturingPublisher publisher = new CapturingPublisher();
    DeviceLogWriteBuffer buffer =
        new DeviceLogWriteBuffer(
            "buffer", "kafka", false, true, true, 16, 10, 1, 8, 50, 50, 1000,
            List.of(publisher));

    try {
      assertTrue(buffer.submitRequest("mysql", service, request("dev-1", 1), device("dev-1"), product()));
      assertTrue(buffer.submitRequest("mysql", service, request("dev-1", 2), device("dev-1"), product()));

      assertTrue(service.awaitRequests(2, TimeUnit.SECONDS));
      assertEquals(2, service.requests.size());
      assertEquals(0, publisher.records.size());

      Map<String, Object> metrics = buffer.policyMetrics("mysql");
      assertEquals(2L, metrics.get("insertedDeviceLogCount"));
      assertEquals(0L, metrics.get("droppedCount"));
      assertEquals(0L, metrics.get("oldestDeviceLogQueueLagMs"));
    } finally {
      buffer.shutdown();
    }
  }

  @Test
  void shouldPublishDirectWritesOnlyAfterTheySucceed() {
    CapturingLogService service = new CapturingLogService(1);
    CapturingPublisher publisher = new CapturingPublisher();
    DeviceLogWriteBuffer buffer =
        new DeviceLogWriteBuffer(
            "direct", "kafka", false, false, true, 1, 1, 1, 8, 10, 0, 1000,
            List.of(publisher));

    try {
      assertTrue(buffer.submitRequest("mysql", service, request("dev-1", 1), device("dev-1"), product()));
      assertEquals(1, service.requests.size());
      assertEquals(0, publisher.records.size());
    } finally {
      buffer.shutdown();
    }

    CapturingLogService failingService = new CapturingLogService(1);
    failingService.failFlush = true;
    CapturingPublisher failingPublisher = new CapturingPublisher();
    DeviceLogWriteBuffer failingBuffer =
        new DeviceLogWriteBuffer(
            "direct", "kafka", false, false, true, 1, 1, 1, 8, 10, 0, 1000,
            List.of(failingPublisher));

    try {
      assertFalse(
          failingBuffer.submitRequest(
              "mysql", failingService, request("dev-1", 1), device("dev-1"), product()));
      assertEquals(0, failingPublisher.records.size());
    } finally {
      failingBuffer.shutdown();
    }
  }

  @Test
  void mqModeShouldPublishWithoutLocalWrite() throws Exception {
    CapturingLogService service = new CapturingLogService(1);
    CapturingPublisher publisher = new CapturingPublisher();
    DeviceLogWriteBuffer buffer =
        new DeviceLogWriteBuffer(
            "buffer", "kafka", true, true, true, 16, 10, 1, 8, 20, 20, 1000,
            List.of(publisher));

    try {
      assertTrue(buffer.submitRequest("mysql", service, request("dev-1", 1), device("dev-1"), product()));
      assertTrue(publisher.await(2, TimeUnit.SECONDS));
      assertEquals(1, publisher.records.size());
      assertEquals("pk001dev-1", publisher.records.get(0).partitionKey());
      assertTrue(service.requests.isEmpty());
      assertEquals("mq", buffer.metrics().get("mode"));
    } finally {
      buffer.shutdown();
    }
  }

  @Test
  void mqFailureShouldFallbackToDirectWrite() throws Exception {
    CapturingLogService service = new CapturingLogService(1);
    CapturingPublisher publisher = new CapturingPublisher();
    publisher.fail = true;
    DeviceLogWriteBuffer buffer =
        new DeviceLogWriteBuffer(
            "buffer", "kafka", true, true, true, 16, 10, 1, 8, 20, 20, 1000,
            List.of(publisher));

    try {
      assertTrue(buffer.submitRequest("mysql", service, request("dev-1", 1), device("dev-1"), product()));
      assertTrue(service.awaitRequests(2, TimeUnit.SECONDS));
      assertEquals(1, service.requests.size());
      assertEquals(1L, buffer.policyMetrics("mysql").get("fallbackDirectCount"));
    } finally {
      buffer.shutdown();
    }
  }

  @Test
  void shouldRejectNewWritesAfterShutdown() {
    CapturingLogService service = new CapturingLogService(1);
    DeviceLogWriteBuffer buffer =
        new DeviceLogWriteBuffer(
            "direct", "kafka", false, false, true, 1, 1, 1, 8, 10, 0, 1000,
            List.of());

    buffer.shutdown();

    assertFalse(buffer.submitRequest("mysql", service, request("dev-1", 1), device("dev-1"), product()));
    assertTrue(service.requests.isEmpty());
  }

  private static BaseUPRequest request(String deviceId, int value) {
    BaseUPRequest request = new BaseUPRequest();
    request.setMessageType(MessageType.PROPERTIES);
    request.setIotId("pk001" + deviceId);
    request.setProductKey("pk001");
    request.setDeviceId(deviceId);
    request.setDeviceName(deviceId);
    request.setTime(System.currentTimeMillis());
    request.setProperties(Map.of("temperature", value));
    request.setData(request.getProperties());
    request.setPayload("{\"temperature\":" + value + "}");
    return request;
  }

  private static IoTDeviceDTO device(String deviceId) {
    return IoTDeviceDTO.builder()
        .iotId("pk001" + deviceId)
        .productKey("pk001")
        .deviceId(deviceId)
        .deviceName(deviceId)
        .build();
  }

  private static IoTProduct product() {
    return IoTProduct.builder().productKey("pk001").storePolicy("mysql").build();
  }

  private static final class CapturingPublisher implements DeviceLogWritePublisher {
    private final List<DeviceLogWriteRecord> records = new CopyOnWriteArrayList<>();
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile boolean fail;

    @Override
    public String provider() {
      return "kafka";
    }

    @Override
    public void publish(List<DeviceLogWriteRecord> records) {
      if (fail) {
        throw new IllegalStateException("publish failed");
      }
      this.records.addAll(records);
      latch.countDown();
    }

    private boolean await(long timeout, TimeUnit unit) throws InterruptedException {
      return latch.await(timeout, unit);
    }
  }

  private static final class CapturingLogService implements IIoTDeviceLogService {
    private final CountDownLatch requestLatch;
    private final List<DeviceLogWriteRequest> requests = new CopyOnWriteArrayList<>();
    private volatile boolean failFlush;

    private CapturingLogService(int expectedRequests) {
      this.requestLatch = new CountDownLatch(expectedRequests);
    }

    private boolean awaitRequests(long timeout, TimeUnit unit) throws InterruptedException {
      return requestLatch.await(timeout, unit);
    }

    @Override
    public String getPolicy() {
      return "mysql";
    }

    @Override
    public void saveDeviceLog(
        BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
      throw new AssertionError("DeviceLogWriteBuffer should use flushDeviceLogRequests");
    }

    @Override
    public void flushDeviceLogRequests(List<DeviceLogWriteRequest> requests) {
      if (failFlush) {
        throw new IllegalStateException("flush failed");
      }
      this.requests.addAll(new ArrayList<>(requests));
      for (int i = 0; i < requests.size(); i++) {
        requestLatch.countDown();
      }
    }

    @Override
    public PageBean<IoTDeviceLogVO> pageList(LogQuery logQuery) {
      return null;
    }

    @Override
    public IoTDeviceLogVO queryById(LogQuery logQuery) {
      return null;
    }

    @Override
    public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
      return null;
    }

    @Override
    public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery logQuery) {
      return null;
    }

    @Override
    public JSONObject configMetadata() {
      return new JSONObject();
    }

    @Override
    public void saveDeviceLog(
        IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
      throw new AssertionError("DeviceLogWriteBuffer should use flushDeviceLogEntities");
    }
  }
}
