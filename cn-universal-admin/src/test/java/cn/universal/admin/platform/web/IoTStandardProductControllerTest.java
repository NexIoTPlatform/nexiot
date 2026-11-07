package cn.universal.admin.platform.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.admin.platform.service.IIoTStandardCategoryService;
import cn.universal.admin.platform.service.IIoTStandardProductService;
import cn.universal.persistence.entity.IoTStandardCategory;
import cn.universal.persistence.entity.IoTStandardProduct;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.PageHelper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class IoTStandardProductControllerTest {
  private final IIoTStandardProductService products = mock(IIoTStandardProductService.class);
  private final IIoTStandardCategoryService categories = mock(IIoTStandardCategoryService.class);
  private final IoTStandardProductController controller =
      new IoTStandardProductController(products, categories);

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void permitsCreatingAndMovingProductsAtEveryCategoryLevel(int level) {
    var product = new IoTStandardProduct();
    product.setCategoryId(10L);
    var category = new IoTStandardCategory();
    category.setLevel(level);
    when(categories.selectCategoryById(10L)).thenReturn(category);
    when(products.checkProductKeyUnique(product)).thenReturn(true);
    when(products.insertProduct(product)).thenReturn(1);
    when(products.updateProduct(product)).thenReturn(1);

    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionId).thenReturn("tenant-member");
      assertEquals(0, controller.add(product).getCode());
      product.setId(100L);
      assertEquals(0, controller.edit(product).getCode());
    }
    assertEquals("tenant-member", product.getCreateBy());
    assertEquals("tenant-member", product.getUpdateBy());
    verify(products).insertProduct(product);
    verify(products).updateProduct(product);
  }

  @Test
  void rejectsMissingAndInvisibleCategoriesBeforeWriting() {
    var product = new IoTStandardProduct();
    when(products.checkProductKeyUnique(product)).thenReturn(true);
    assertNotEquals(0, controller.add(product).getCode());
    assertNotEquals(0, controller.edit(product).getCode());
    // 分类服务按当前租户查询；不存在或其他租户的分类均返回 null。
    product.setCategoryId(99L);
    assertNotEquals(0, controller.add(product).getCode());
    assertNotEquals(0, controller.edit(product).getCode());
    verify(products, never()).insertProduct(any());
    verify(products, never()).updateProduct(any());
  }

  @Test
  void categoryListingKeepsEachDescendantsActualCategoryPath() {
    var rootProduct = new IoTStandardProduct();
    rootProduct.setCategoryId(1L);
    rootProduct.setCategoryPath("工业");
    var childProduct = new IoTStandardProduct();
    childProduct.setCategoryId(2L);
    childProduct.setCategoryPath("工业/仪表");
    when(products.selectProductByCategoryId(1L)).thenReturn(List.of(rootProduct, childProduct));
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
    try {
      controller.listByCategory(1L);
      assertEquals("工业", rootProduct.getCategoryPath());
      assertEquals("工业/仪表", childProduct.getCategoryPath());
      // 路径由服务层批量填充，Controller 不再重复查询。
      verifyNoInteractions(categories);
    } finally {
      PageHelper.clearPage();
      RequestContextHolder.resetRequestAttributes();
    }
  }

  @Test
  void listingAndDetailReuseServiceCategoryPaths() {
    var query = new IoTStandardProduct();
    var product = new IoTStandardProduct();
    product.setCategoryId(3L);
    product.setCategoryPath("工业/仪表/电表");
    when(products.selectProductList(query)).thenReturn(List.of(product));
    when(products.selectProductById(10L)).thenReturn(product);
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
    try {
      controller.list(query);
      controller.getInfo(10L);
      assertEquals("工业/仪表/电表", product.getCategoryPath());
      verifyNoInteractions(categories);
    } finally {
      PageHelper.clearPage();
      RequestContextHolder.resetRequestAttributes();
    }
  }
}
