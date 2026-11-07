package cn.universal.admin.platform.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.universal.admin.platform.service.IIoTProductService;
import cn.universal.persistence.entity.vo.IoTProductOptionVO;
import cn.universal.persistence.query.IoTProductQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IoTProductOptionsControllerTest {

  @Test
  void selectorDefaultsToTwentyOptionsAndReturnsOnlyItsThreeDisplayFields() throws Exception {
    IIoTProductService service = mock(IIoTProductService.class);
    IoTProductOptionVO option = new IoTProductOptionVO();
    option.setId(9L);
    option.setName("传感器");
    option.setProductKey("sensor-1");
    when(service.selectProductOptions(any())).thenReturn(List.of(option));
    IoTProductController controller = new IoTProductController();
    ReflectionTestUtils.setField(controller, "devProductService", service);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(get("/admin/v1/product/options")
            .param("name", "sensor")
            .param("tenantId", "untrusted")
            .param("scopeDenied", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.rows[0].id").value(9))
        .andExpect(jsonPath("$.rows[0].name").value("传感器"))
        .andExpect(jsonPath("$.rows[0].productKey").value("sensor-1"))
        .andExpect(jsonPath("$.rows[0].metadata").doesNotExist())
        .andExpect(jsonPath("$.rows[0].devNum").doesNotExist());

    ArgumentCaptor<IoTProductQuery> query = ArgumentCaptor.forClass(IoTProductQuery.class);
    verify(service).selectProductOptions(query.capture());
    assertEquals(1, query.getValue().getPageNum());
    assertEquals(20, query.getValue().getPageSize());
    assertEquals("sensor", query.getValue().getName());
    assertNull(query.getValue().getTenantId());
    assertTrue(query.getValue().getScopeUnionIds().isEmpty());
  }
}
