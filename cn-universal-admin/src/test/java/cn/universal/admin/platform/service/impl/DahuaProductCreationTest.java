package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.customfield.SystemFieldInjectorManager;
import cn.universal.core.product.metadata.ProductMetadataProvider;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.base.IoTProductAction;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DahuaProductCreationTest {
  @Test
  void creationPersistsProtocolProviderEventsInsteadOfControllerMetadata() {
    var service = new IoTProductServiceImpl();
    var mapper = mock(IoTProductMapper.class);
    when(mapper.insertDevProduct(any())).thenReturn(1);
    var logs = mock(IIoTDeviceDataService.class);
    when(logs.isStorePolicyEnabled(anyString())).thenReturn(true);
    var provider = mock(ProductMetadataProvider.class);
    when(provider.getProtocolCode()).thenReturn("dahua_cloud");
    when(provider.getMetadata())
        .thenReturn(
            "{\"properties\":[],\"functions\":[],\"events\":[{\"id\":\"videoMotion\",\"name\":\"动态检测\",\"valueType\":{\"type\":\"object\",\"properties\":[]}}]}");
    ReflectionTestUtils.setField(service, "ioTProductMapper", mapper);
    ReflectionTestUtils.setField(service, "ioTProductAction", mock(IoTProductAction.class));
    ReflectionTestUtils.setField(
        service, "systemFieldInjectorManager", mock(SystemFieldInjectorManager.class));
    ReflectionTestUtils.setField(service, "iIoTDeviceDataService", logs);
    ReflectionTestUtils.setField(service, "productMetadataProviders", List.of(provider));
    var product =
        IoTProduct.builder()
            .name("大华摄像机")
            .thirdPlatform("dahua_cloud")
            .deviceNode("DEVICE")
            .tenantId("tenant")
            .creatorId("owner")
            .configuration("{}")
            .metadata("{\"events\":[]}")
            .build();
    assertEquals(1, service.insertDevProduct(product));
    verify(mapper).insertDevProduct(product);
    assertEquals("VIDEO_DEVICE", product.getDeviceNode());
    assertEquals(
        "videoMotion",
        JSONUtil.parseObj(product.getMetadata())
            .getJSONArray("events")
            .getJSONObject(0)
            .getStr("id"));
  }

  @Test
  void existingProductMergeRetainsCustomEventsAndTagsAndRefreshesMetadata() {
    var user =
        cn.universal.persistence.entity.IoTUser.builder()
            .unionId("owner")
            .tenantId("tenant")
            .build();
    var service =
        new IoTProductServiceImpl() {
          @Override
          protected cn.universal.persistence.entity.IoTUser currentTenantUser() {
            return user;
          }

          @Override
          public cn.universal.persistence.entity.IoTUser queryIotUser(String id) {
            return user;
          }
        };
    var users = mock(cn.universal.security.service.IoTUserService.class);
    when(users.selectUserByUnionId("owner")).thenReturn(user);
    ReflectionTestUtils.setField(service,"ioTUserService",users);
    var mapper = mock(IoTProductMapper.class);
    var action = mock(IoTProductAction.class);
    var product =
        IoTProduct.builder()
            .productKey("p")
            .creatorId("owner")
            .tenantId("tenant")
            .metadata(
                "{\"properties\":[],\"functions\":[],\"events\":[{\"id\":\"customEvent\",\"name\":\"自定义事件\",\"valueType\":{\"type\":\"string\"}}],\"tags\":[{\"id\":\"location\",\"valueType\":{\"type\":\"string\"}}]}")
            .build();
    when(mapper.selectOne(any())).thenReturn(product);
    when(mapper.updateByPrimaryKeySelective(any())).thenReturn(1);
    ReflectionTestUtils.setField(service, "ioTProductMapper", mapper);
    ReflectionTestUtils.setField(service, "ioTProductAction", action);
    var update = new cn.universal.persistence.entity.bo.IoTProductMetadataBatchBO();
    update.setProductKey("p");
    update.setMode("merge");
    update.setEvents(
        List.of(
            cn.universal.persistence.entity.bo.IoTProductMetadataBO.builder()
                .id("videoMotion")
                .name("动态检测")
                .valueType(java.util.Map.of("type", "object", "properties", List.of()))
                .build()));
    assertEquals(1, service.batchUpdateMetadata(update));
    var model = JSONUtil.parseObj(product.getMetadata());
    assertEquals(2, model.getJSONArray("events").size());
    assertEquals("location", model.getJSONArray("tags").getJSONObject(0).getStr("id"));
    verify(action).metadataCreate("p");
  }
}
