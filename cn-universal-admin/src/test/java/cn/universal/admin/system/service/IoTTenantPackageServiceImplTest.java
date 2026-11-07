package cn.universal.admin.system.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.admin.system.service.impl.IoTTenantPackageServiceImpl;
import cn.universal.persistence.entity.IoTTenantPackage;
import cn.universal.persistence.entity.IoTTenantPackageMenu;
import cn.universal.persistence.entity.admin.SysMenu;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMenuMapper;
import cn.universal.persistence.mapper.admin.SysMenuMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IoTTenantPackageServiceImplTest {

  @InjectMocks private IoTTenantPackageServiceImpl service;

  @Mock private IoTTenantPackageMapper tenantPackageMapper;
  @Mock private IoTTenantPackageMenuMapper tenantPackageMenuMapper;
  @Mock private IoTTenantMapper tenantMapper;
  @Mock private SysMenuMapper menuMapper;
  @Mock private IIoTTenantService tenantService;

  @BeforeAll
  static void initEntityTables() {
    TkMapperEntityTestSupport.registerEntityTables(
        IoTTenantPackage.class, IoTTenantPackageMenu.class, SysMenu.class);
  }

  @Test
  void insertPackageShouldBatchInsertMenus() {
    IoTTenantPackage tenantPackage =
        IoTTenantPackage.builder()
            .packageName("标准套餐")
            .status(0)
            .menuIds(new Long[] {1L, 2L, 2L})
            .build();

    when(tenantPackageMapper.selectByExample(any())).thenReturn(List.of());
    when(menuMapper.selectByExample(any())).thenReturn(List.of(menu(1L, "device"), menu(2L, "dashboard")));

    service.insertPackage(tenantPackage);

    ArgumentCaptor<List<IoTTenantPackageMenu>> menuCaptor = ArgumentCaptor.forClass(List.class);
    verify(tenantPackageMenuMapper).batchInsert(menuCaptor.capture());
    verify(tenantPackageMenuMapper, never()).insertSelective(any());

    List<IoTTenantPackageMenu> relations = menuCaptor.getValue();
    assertEquals(2, relations.size());
    assertEquals(List.of(1L, 2L), relations.stream().map(IoTTenantPackageMenu::getMenuId).toList());
    assertEquals(tenantPackage.getPackageId(), relations.get(0).getPackageId());
    assertEquals(tenantPackage.getPackageId(), relations.get(1).getPackageId());
  }

  @Test
  void updatePackageShouldSyncTenantRoleMenusWhenMenuIdsChanged() {
    IoTTenantPackage persisted =
        IoTTenantPackage.builder().packageId(100L).packageName("标准套餐").deleted(0).build();
    IoTTenantPackage update =
        IoTTenantPackage.builder()
            .packageId(100L)
            .packageName("标准套餐")
            .status(0)
            .menuIds(new Long[] {1L, 3L})
            .build();

    when(tenantPackageMapper.selectByPrimaryKey(100L)).thenReturn(persisted);
    when(tenantPackageMapper.selectByExample(any())).thenReturn(List.of(persisted));
    when(tenantPackageMenuMapper.selectByExample(any()))
        .thenReturn(List.of(packageMenu(100L, 1L), packageMenu(100L, 2L)));
    when(menuMapper.selectByExample(any()))
        .thenReturn(List.of(menu(1L, "device"), menu(3L, "dashboard")));

    service.updatePackage(update);

    verify(tenantPackageMenuMapper).batchInsert(any());
    verify(tenantService).syncTenantRoleMenusByPackage(100L);
  }

  @Test
  void updatePackageShouldNotRewriteMenusWhenMenuIdsUnchanged() {
    IoTTenantPackage persisted =
        IoTTenantPackage.builder().packageId(100L).packageName("标准套餐").deleted(0).build();
    IoTTenantPackage update =
        IoTTenantPackage.builder()
            .packageId(100L)
            .packageName("标准套餐")
            .status(0)
            .menuIds(new Long[] {2L, 1L})
            .build();

    when(tenantPackageMapper.selectByPrimaryKey(100L)).thenReturn(persisted);
    when(tenantPackageMapper.selectByExample(any())).thenReturn(List.of(persisted));
    when(tenantPackageMenuMapper.selectByExample(any()))
        .thenReturn(List.of(packageMenu(100L, 1L), packageMenu(100L, 2L)));
    when(menuMapper.selectByExample(any()))
        .thenReturn(List.of(menu(1L, "device"), menu(2L, "dashboard")));

    service.updatePackage(update);

    verify(tenantPackageMenuMapper, never()).deleteByExample(any());
    verify(tenantPackageMenuMapper, never()).batchInsert(any());
    verify(tenantService, never()).syncTenantRoleMenusByPackage(100L);
  }

  @Test
  void selectPackageListShouldFillMetaInBatch() {
    IoTTenantPackage packageA =
        IoTTenantPackage.builder().packageId(100L).packageName("标准套餐").deleted(0).build();
    IoTTenantPackage packageB =
        IoTTenantPackage.builder().packageId(200L).packageName("高级套餐").deleted(0).build();

    when(tenantPackageMapper.selectByExample(any())).thenReturn(List.of(packageA, packageB));
    when(tenantPackageMenuMapper.selectByExample(any()))
        .thenReturn(
            List.of(
                packageMenu(100L, 1L),
                packageMenu(100L, 2L),
                packageMenu(200L, 3L)));
    when(tenantMapper.selectTenantCountByPackageIds(any()))
        .thenReturn(List.of(countRow(100L, 2L), countRow(200L, 5L)));

    List<IoTTenantPackage> packages = service.selectPackageList(new IoTTenantPackage());

    assertEquals(2, packages.size());
    assertArrayEquals(new Long[] {1L, 2L}, packages.get(0).getMenuIds());
    assertEquals(2, packages.get(0).getMenuCount());
    assertEquals(2L, packages.get(0).getTenantCount());
    assertArrayEquals(new Long[] {3L}, packages.get(1).getMenuIds());
    assertEquals(1, packages.get(1).getMenuCount());
    assertEquals(5L, packages.get(1).getTenantCount());
    verify(tenantMapper).selectTenantCountByPackageIds(any());
    verify(tenantMapper, never()).selectCount(any());
  }

  private static IoTTenantPackageMenu packageMenu(Long packageId, Long menuId) {
    return IoTTenantPackageMenu.builder().id(menuId).packageId(packageId).menuId(menuId).build();
  }

  private static SysMenu menu(Long menuId, String path) {
    return SysMenu.builder().menuId(menuId).path(path).perms("iot:" + path + ":view").build();
  }

  private static Map<String, Object> countRow(Long packageId, Long tenantCount) {
    Map<String, Object> row = new HashMap<>();
    row.put("packageId", packageId);
    row.put("tenantCount", tenantCount);
    return row;
  }
}
