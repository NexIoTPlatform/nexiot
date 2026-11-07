package cn.universal.rule.scene.deviceDown;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.IoTDeviceQuery;
import cn.universal.rule.model.ExeRunContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SceneFunctionParameterTest {

  private final SenceIoTDeviceDownService service = new SenceIoTDeviceDownService();
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final IoTProductMapper products = mock(IoTProductMapper.class);
  private final IoTDevice device = new IoTDevice();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "ioTDeviceMapper", devices);
    ReflectionTestUtils.setField(service, "ioTProductMapper", products);
    device.setDeviceId("alarm-9");
    device.setProductKey("alarm-product");
    when(devices.getOneByDeviceId(any())).thenReturn(device);
    IoTProduct product = new IoTProduct();
    product.setMetadata("""
        {"functions":[{"id":"controlLight","inputs":[
          {"id":"slaveAddress","valueType":{"type":"int"}},
          {"id":"lightMode","valueType":{"type":"string"}}]}]}
        """);
    when(products.getProductByProductKey("alarm-product")).thenReturn(product);
  }

  @Test
  void jsonNumbersStayNumbersAndHistoricalStringsFollowCurrentMetadata() {
    for (String parameter : List.of("9", "\"9\"")) {
      TriggerBO action = action(parameter);
      if ("9".equals(parameter)) assertInstanceOf(Number.class, action.getExecData().getFirst().getParams());
      String request = ReflectionTestUtils.invokeMethod(service, "getDownRequest", action, "owner");
      JSONObject data = JSONUtil.parseObj(request).getJSONObject("function").getJSONObject("data");
      assertInstanceOf(Number.class, data.get("slaveAddress"));
      assertEquals(9, data.getInt("slaveAddress"));
      assertEquals("06", data.get("lightMode"));
    }
    ArgumentCaptor<IoTDeviceQuery> query = ArgumentCaptor.forClass(IoTDeviceQuery.class);
    verify(devices, times(2)).getOneByDeviceId(query.capture());
    assertEquals("alarm-product", query.getValue().getProductKey());
  }

  @Test
  void derivedDeviceMetadataTakesPrecedenceOverProductType() {
    device.setDeriveMetadata("""
        {"functions":[{"id":"controlLight","inputs":[
          {"id":"slaveAddress","valueType":{"type":"string"}}]}]}
        """);
    String request = ReflectionTestUtils.invokeMethod(service, "getDownRequest", action("\"009\""), "owner");
    assertEquals("009", JSONUtil.parseObj(request).getJSONObject("function").getJSONObject("data").get("slaveAddress"));
  }

  @Test
  void invalidActionDoesNotPreventFollowingActionsFromRunning() {
    when(devices.getOneByDeviceId(any())).thenReturn(device).thenReturn(null);
    SceneLinkage scene = new SceneLinkage();
    scene.setCreateBy("owner");
    scene.setExecAction("[" + JSONUtil.toJsonStr(action("\"invalid\"")) + "," + JSONUtil.toJsonStr(action("9")) + "]");
    List<ExeRunContext> results = service.doIoTDeviceFunction(new UPRequest(), scene);
    assertEquals(2, results.size());
    assertFalse(results.getFirst().isSuccess());
    assertTrue(String.valueOf(results.getFirst().getResult()).contains("slaveAddress"));
    assertEquals("设备不存在", results.getLast().getResult());
  }

  private TriggerBO action(String address) {
    return BeanUtil.toBean(JSONUtil.parseObj("""
        {"trigger":"device","deviceId":"alarm-9","productKey":"alarm-product","modelId":"controlLight",
         "execData":[{"id":"slaveAddress","params":%s},{"id":"lightMode","params":"06"}]}
        """.formatted(address)), TriggerBO.class);
  }
}
