package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.persistence.entity.IoTStandardCategory;
import cn.universal.persistence.entity.IoTStandardProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTStandardCategoryMapper;
import cn.universal.persistence.mapper.IoTStandardProductMapper;
import com.github.pagehelper.Page;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IoTStandardProductListingTest {
  private final IoTUser tenantUser =
      IoTUser.builder().unionId("test-owner").tenantId("test-tenant").identity(1).build();
  private final IoTStandardCategoryMapper categories = mock(IoTStandardCategoryMapper.class);
  private final IoTStandardProductMapper products = mock(IoTStandardProductMapper.class);
  private final IoTStandardCategoryServiceImpl categoryService =
      new IoTStandardCategoryServiceImpl(categories) {
        @Override
        protected IoTUser currentTenantUser() {
          return tenantUser;
        }
      };
  private final IoTStandardProductServiceImpl service =
      new IoTStandardProductServiceImpl(products, null, categoryService, null) {
        @Override
        protected IoTUser currentTenantUser() {
          return tenantUser;
        }
      };

  @Test
  void listingReadsVisibleCategoriesOnceAndPreservesPagination() {
    when(categories.selectCategoryList(any())).thenReturn(List.of(
        category(1L, 0L, "工业"), category(2L, 1L, "仪表"), category(3L, 2L, "电表")));
    var page = new Page<IoTStandardProduct>(1, 10);
    page.setTotal(16);
    for (int i = 0; i < 10; i++) {
      page.add(product(i % 2 == 0 ? 2L : 3L));
    }
    var query = new IoTStandardProduct();
    when(products.selectProductList(query)).thenReturn(page);

    var result = service.selectProductList(query);

    assertSame(page, result);
    assertEquals(16, page.getTotal());
    assertEquals("工业/仪表", result.get(0).getCategoryPath());
    assertEquals("工业/仪表/电表", result.get(1).getCategoryPath());
    assertEquals("test-tenant", query.getTenantId());
    var categoryQuery = ArgumentCaptor.forClass(IoTStandardCategory.class);
    verify(categories, times(1)).selectCategoryList(categoryQuery.capture());
    assertEquals("test-tenant", categoryQuery.getValue().getTenantId());
    verify(categories, never()).selectCategoryById(any(), any());
  }

  @Test
  void categoryListingRetainsEachProductsOwnPathAndHandlesMissingAncestors() {
    when(categories.selectCategoryList(any())).thenReturn(List.of(
        category(1L, 0L, "工业"), category(2L, 1L, "仪表"), category(4L, 99L, "其他")));
    when(products.selectProductByCategoryId(1L, "test-tenant"))
        .thenReturn(List.of(product(1L), product(2L), product(4L), product(99L)));

    var result = service.selectProductByCategoryId(1L);

    assertEquals(List.of("工业", "工业/仪表", "其他", ""),
        result.stream().map(IoTStandardProduct::getCategoryPath).toList());
    verify(categories, times(1)).selectCategoryList(any());
    verify(categories, never()).selectCategoryById(any(), any());
  }

  @Test
  void emptyAndUncategorizedPagesNeedNoCategoryQuery() {
    var query = new IoTStandardProduct();
    when(products.selectProductList(query)).thenReturn(List.of(), List.of(product(null)));
    service.selectProductList(query);
    service.selectProductList(query);
    verifyNoInteractions(categories);
  }

  @Test
  void malformedCategoryCyclesCannotHangListing() {
    when(categories.selectCategoryList(any())).thenReturn(List.of(
        category(1L, 2L, "甲"), category(2L, 1L, "乙")));
    assertEquals("乙/甲", categoryService.getCategoryPaths(List.of(1L)).get(1L));
  }

  private static IoTStandardCategory category(Long id, Long parentId, String name) {
    var category = new IoTStandardCategory();
    category.setId(id);
    category.setParentId(parentId);
    category.setCategoryName(name);
    return category;
  }

  private static IoTStandardProduct product(Long categoryId) {
    var product = new IoTStandardProduct();
    product.setCategoryId(categoryId);
    return product;
  }
}
