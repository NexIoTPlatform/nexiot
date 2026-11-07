package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class UPProcessorManagerTest {

  @Test
  void beforePushShouldPreserveUnsupportedRequests() {
    BaseUPRequest unsupported = request("unsupported");
    BaseUPRequest supported = request("supported");
    UPProcessorManager manager = new UPProcessorManager(List.of(new FilteringProcessor()));

    List<BaseUPRequest> result = manager.executeBeforePush(List.of(unsupported, supported));

    assertEquals(2, result.size());
    assertSame(unsupported, result.get(0));
    assertSame(supported, result.get(1));
    assertEquals("processed", supported.getDeviceName());
  }

  private BaseUPRequest request(String deviceId) {
    BaseUPRequest request = new BaseUPRequest();
    request.setDeviceId(deviceId);
    return request;
  }

  private static final class FilteringProcessor implements UPProcessor<BaseUPRequest> {

    @Override
    public String getName() {
      return "filtering";
    }

    @Override
    public String getDescription() {
      return "test";
    }

    @Override
    public int getOrder() {
      return 1;
    }

    @Override
    public boolean supports(BaseUPRequest request) {
      return "supported".equals(request.getDeviceId());
    }

    @Override
    public List<BaseUPRequest> beforePush(List<BaseUPRequest> upRequests) {
      upRequests.forEach(request -> request.setDeviceName("processed"));
      return upRequests;
    }
  }
}
