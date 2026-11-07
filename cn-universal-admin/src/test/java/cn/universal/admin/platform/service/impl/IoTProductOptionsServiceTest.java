package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.vo.IoTProductOptionVO;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.IoTProductQuery;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IoTProductOptionsServiceTest {

  @AfterEach
  void clearPaging() {
    PageHelper.clearPage();
  }

  @Test
  void selectionAppliesCurrentUserScopeBeforeRunningOnlyTheLightweightMapper() {
    IoTProductMapper mapper = mock(IoTProductMapper.class);
    ScopedService service = service(mapper);
    IoTProductQuery query = new IoTProductQuery();
    query.setName("  温度  ");
    query.setProductKey("  sensor-1  ");
    query.setTenantId("untrusted-tenant");
    query.setSelf(true);
    query.setPageNum(-3);
    query.setPageSize(10000);
    IoTProductOptionVO option = new IoTProductOptionVO();
    option.setId(9L);
    option.setName("温度传感器");
    option.setProductKey("sensor-1");
    List<IoTProductOptionVO> options = List.of(option);
    when(mapper.selectProductOptions(any())).thenAnswer(invocation -> {
      IoTProductQuery scopedQuery = invocation.getArgument(0);
      assertEquals("tenant-current", scopedQuery.getTenantId());
      assertEquals(List.of(10L), scopedQuery.getScopeDeptIds());
      assertEquals(List.of("user-current"), scopedQuery.getScopeUnionIds());
      assertEquals("user-current", scopedQuery.getCreatorId());
      assertEquals("温度", scopedQuery.getName());
      assertEquals("sensor-1", scopedQuery.getProductKey());
      Page<?> page = PageHelper.getLocalPage();
      assertNotNull(page);
      assertEquals(1, page.getPageNum());
      assertEquals(50, page.getPageSize());
      return options;
    });

    assertSame(options, service.selectProductOptions(query));
    assertTrue(service.scopeApplied);
    verify(mapper).selectProductOptions(query);
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void invalidOrMissingPagingUsesBoundedDefaultsAndBlankSearchIsOmitted() {
    IoTProductMapper mapper = mock(IoTProductMapper.class);
    ScopedService service = service(mapper);
    for (Integer size : new Integer[] {null, 0, -2}) {
      IoTProductQuery query = new IoTProductQuery();
      query.setPageNum(null);
      query.setPageSize(size);
      query.setName("  ");
      query.setProductKey("  ");

      service.selectProductOptions(query);

      assertEquals(1, query.getPageNum());
      assertEquals(20, query.getPageSize());
      assertNull(query.getName());
      assertNull(query.getProductKey());
      PageHelper.clearPage();
    }
  }

  private ScopedService service(IoTProductMapper mapper) {
    ScopedService service = new ScopedService();
    ReflectionTestUtils.setField(service, "ioTProductMapper", mapper);
    return service;
  }

  private static class ScopedService extends IoTProductServiceImpl {
    private final IoTUser user = IoTUser.builder().unionId("user-current").build();
    private boolean scopeApplied;

    @Override
    protected IoTUser currentTenantUser() {
      return user;
    }

    @Override
    protected void applyDataPermissionScope(Object target, IoTUser fallbackUser) {
      assertSame(user, fallbackUser);
      IoTProductQuery query = (IoTProductQuery) target;
      query.setTenantId("tenant-current");
      query.setScopeDeptIds(List.of(10L));
      query.setScopeUnionIds(List.of(user.getUnionId()));
      scopeApplied = true;
    }
  }
}
