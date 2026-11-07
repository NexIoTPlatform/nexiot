package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.admin.platform.service.customfield.SystemFieldInjectorManager;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.base.IoTProductAction;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.dto.ProductExportPackageDTO;
import cn.universal.persistence.entity.dto.ProductExportPackageDTO.ProductInfoDTO;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("产品完整包导入租户测试")
class IoTProductServiceImplImportTenantTest {

  @Test
  @DisplayName("导入产品应强制使用当前会话租户")
  void shouldUseCurrentSessionTenant() {
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    when(productMapper.selectDevProductList(any(IoTProduct.class)))
        .thenReturn(Collections.emptyList());
    when(productMapper.insertDevProduct(any(IoTProduct.class))).thenReturn(1);
    when(productMapper.updateByPrimaryKeySelective(any(IoTProduct.class))).thenReturn(1);
    IoTProductServiceImpl service = service(productMapper);

    service.importProductPackages(List.of(productPackage()), "operator-1", "tenant-current");

    ArgumentCaptor<IoTProduct> productCaptor = ArgumentCaptor.forClass(IoTProduct.class);
    verify(productMapper).insertDevProduct(productCaptor.capture());
    assertEquals("tenant-current", productCaptor.getValue().getTenantId());
    assertEquals("operator-1", productCaptor.getValue().getCreatorId());
  }

  @Test
  @DisplayName("当前会话租户为空时应拒绝导入")
  void shouldRejectMissingCurrentSessionTenant() {
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    IoTProductServiceImpl service = service(productMapper);

    assertThrows(
        IoTException.class,
        () -> service.importProductPackages(List.of(productPackage()), "operator-1", " "));

    verify(productMapper, never()).insertDevProduct(any(IoTProduct.class));
  }

  private IoTProductServiceImpl service(IoTProductMapper productMapper) {
    IoTProductServiceImpl service = new IoTProductServiceImpl();
    cn.universal.dm.device.service.log.IIoTDeviceDataService storage =
        mock(cn.universal.dm.device.service.log.IIoTDeviceDataService.class);
    when(storage.isStorePolicyEnabled(any())).thenReturn(true);
    ReflectionTestUtils.setField(service, "iIoTDeviceDataService", storage);
    ReflectionTestUtils.setField(service, "ioTProductMapper", productMapper);
    ReflectionTestUtils.setField(service, "ioTProductAction", mock(IoTProductAction.class));
    ReflectionTestUtils.setField(
        service, "systemFieldInjectorManager", mock(SystemFieldInjectorManager.class));
    return service;
  }

  private ProductExportPackageDTO productPackage() {
    ProductInfoDTO product =
        ProductInfoDTO.builder()
            .productId("source-product")
            .productKey("source-key")
            .name("导入产品")
            .thirdPlatform("mqtt")
            .deviceNode("DEVICE")
            .state((byte) 1)
            .build();
    return ProductExportPackageDTO.builder().product(product).build();
  }
}
