package cn.universal.admin.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.admin.system.service.impl.IoTTenantServiceImpl;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.common.typehandler.PostgreSqlJsonbStringTypeHandler;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTTenantPackage;
import cn.universal.persistence.entity.IoTTenantPackageMenu;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.TenantBrandingBO;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.admin.SysRoleMenu;
import cn.universal.persistence.entity.admin.SysUserRole;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMenuMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.admin.SysRoleMapper;
import cn.universal.persistence.mapper.admin.SysRoleMenuMapper;
import cn.universal.persistence.mapper.admin.SysUserRoleMapper;
import cn.universal.security.support.TenantDomainResolver;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

@ExtendWith(MockitoExtension.class)
class IoTTenantServiceImplTest {

  @InjectMocks private IoTTenantServiceImpl service;

  @Mock private IoTTenantMapper tenantMapper;
  @Mock private IoTTenantPackageMapper tenantPackageMapper;
  @Mock private IoTTenantPackageMenuMapper tenantPackageMenuMapper;
  @Mock private IoTUserMapper userMapper;
  @Mock private SysRoleMapper sysRoleMapper;
  @Mock private SysRoleMenuMapper sysRoleMenuMapper;
  @Mock private SysUserRoleMapper sysUserRoleMapper;
  @Mock private TenantDomainResolver tenantDomainResolver;

  @BeforeAll
  static void initEntityTables() {
    TkMapperEntityTestSupport.registerEntityTables(
        IoTTenant.class,
        IoTTenantPackageMenu.class,
        IoTUser.class,
        SysRole.class,
        SysRoleMenu.class,
        SysUserRole.class);
  }

  @Test
  void tenantCfgShouldUseJsonbTypeHandler() {
    EntityColumn cfgColumn =
        EntityHelper.getColumns(IoTTenant.class).stream()
            .filter(column -> "cfg".equals(column.getProperty()))
            .findFirst()
            .orElseThrow();

    assertEquals(JdbcType.OTHER, cfgColumn.getJdbcType());
    assertEquals(PostgreSqlJsonbStringTypeHandler.class, cfgColumn.getTypeHandler());
  }

  @Test
  void normalizeTenantBindingsShouldConvertBlankCfgToEmptyJsonObject() throws Exception {
    IoTTenant tenant = IoTTenant.builder().cfg("   ").build();

    invokeNormalizeTenantBindings(tenant, null);

    assertEquals("{}", tenant.getCfg());
  }

  @Test
  void normalizeTenantBindingsShouldRejectInvalidCfgJson() {
    IoTTenant tenant = IoTTenant.builder().cfg("[]").build();

    IoTException exception =
        assertThrows(
            IoTException.class,
            () -> {
              invokeNormalizeTenantBindings(tenant, null);
            });

    assertEquals("扩展配置 cfg 必须是合法 JSON 对象", exception.getMessage());
  }

  @Test
  void syncTenantRoleMenusByPackageShouldTrimRoleMenusToPackageScope() {
    IoTUser tenantAdmin =
        IoTUser.builder()
            .id(1L)
            .tenantId("tenant-1")
            .tenantRole("ADMIN")
            .username("tenant-admin")
            .unionId("tenant-admin")
            .deleted(0)
            .build();
    SysRole tenantAdminRole =
        SysRole.builder().roleId(10L).roleKey("tenant:admin").createBy("tenant-admin").build();
    SysRole customRole =
        SysRole.builder().roleId(20L).roleKey("tenant:ops").createBy("tenant-admin").build();

    when(tenantMapper.selectByExample(any()))
        .thenReturn(List.of(IoTTenant.builder().tenantId("tenant-1").packageId(100L).deleted(0).build()));
    when(tenantPackageMenuMapper.selectByExample(any()))
        .thenReturn(
            List.of(
                IoTTenantPackageMenu.builder().id(1L).packageId(100L).menuId(1000L).build(),
                IoTTenantPackageMenu.builder().id(2L).packageId(100L).menuId(2000L).build()));
    when(userMapper.selectByExample(any())).thenReturn(List.of(tenantAdmin));
    when(sysRoleMapper.selectByExample(any()))
        .thenReturn(List.of(tenantAdminRole))
        .thenReturn(List.of(tenantAdminRole, customRole));
    when(sysUserRoleMapper.selectCountByExample(any())).thenReturn(1);
    when(sysRoleMenuMapper.selectByExample(any()))
        .thenReturn(
            List.of(
                SysRoleMenu.builder().uuid(1L).roleId(20L).menuId(1000L).build(),
                SysRoleMenu.builder().uuid(2L).roleId(20L).menuId(3000L).build()));

    service.syncTenantRoleMenusByPackage(100L);

    ArgumentCaptor<List<SysRoleMenu>> menuCaptor = ArgumentCaptor.forClass(List.class);
    verify(sysRoleMenuMapper, atLeastOnce()).batchInsert(menuCaptor.capture());
    verify(sysRoleMenuMapper, never()).insertSelective(any());

    List<SysRoleMenu> insertedMenus = new ArrayList<>();
    menuCaptor.getAllValues().forEach(insertedMenus::addAll);

    boolean memberRoleKeptAllowedMenu =
        insertedMenus.stream()
            .anyMatch(
                menu ->
                    Long.valueOf(20L).equals(menu.getRoleId())
                        && Long.valueOf(1000L).equals(menu.getMenuId()));
    boolean memberRoleKeptForbiddenMenu =
        insertedMenus.stream()
            .anyMatch(
                menu ->
                    Long.valueOf(20L).equals(menu.getRoleId())
                        && Long.valueOf(3000L).equals(menu.getMenuId()));

    assertTrue(memberRoleKeptAllowedMenu);
    assertFalse(memberRoleKeptForbiddenMenu);
    verify(sysRoleMapper).updateByPrimaryKeySelective(any(SysRole.class));
  }

  @Test
  void updateTenantShouldClearExpireTimeWhenExplicitlyRequested() {
    IoTTenant persisted =
        IoTTenant.builder()
            .tenantId("tenant-1")
            .tenantName("租户一")
            .packageId(100L)
            .deleted(0)
            .build();
    IoTTenantPackage tenantPackage =
        IoTTenantPackage.builder().packageId(100L).packageName("默认套餐").status(0).deleted(0).build();
    IoTTenant update = IoTTenant.builder().tenantId("tenant-1").expireTime(null).build();

    when(tenantMapper.selectByPrimaryKey("tenant-1")).thenReturn(persisted);
    when(tenantPackageMapper.selectByPrimaryKey(100L)).thenReturn(tenantPackage);
    when(tenantMapper.updateByPrimaryKeySelective(any(IoTTenant.class))).thenReturn(1);
    when(tenantMapper.updateTenantExpireTime(any(), any(), any(), any())).thenReturn(1);
    when(userMapper.selectByExample(any())).thenReturn(List.of());

    int rows = service.updateTenant(update, true);

    assertEquals(1, rows);
    verify(tenantMapper).updateTenantExpireTime(eq("tenant-1"), isNull(), any(), any());
  }

  @Test
  void updateTenantBrandingShouldPreserveOtherCfgSections() {
    IoTTenant tenant =
        IoTTenant.builder()
            .tenantId("tenant-1")
            .tenantName("租户一")
            .deleted(0)
            .cfg(
                """
                {
                  "feature": { "enabled": true },
                  "branding": {
                    "loginTemplate": "glow",
                    "domains": { "iot.example.com": { "siteName": "专属域名" } }
                  }
                }
                """)
            .build();
    when(tenantMapper.selectByPrimaryKey("tenant-1")).thenReturn(tenant);
    when(tenantMapper.updateByPrimaryKeySelective(any(IoTTenant.class))).thenReturn(1);

    TenantBrandingBO result =
        service.updateTenantBranding(
            "tenant-1",
            TenantBrandingBO.builder()
                .siteName("租户一平台")
                .siteLogo("https://example.com/logo.png")
                .watermarkEnabled(true)
                .watermarkText("内部使用")
                .build());

    ArgumentCaptor<IoTTenant> tenantCaptor = ArgumentCaptor.forClass(IoTTenant.class);
    verify(tenantMapper).updateByPrimaryKeySelective(tenantCaptor.capture());
    String cfg = tenantCaptor.getValue().getCfg();
    assertTrue(cfg.contains("\"feature\""));
    assertTrue(cfg.contains("iot.example.com"));
    assertTrue(cfg.contains("租户一平台"));
    assertTrue(cfg.contains("内部使用"));
    assertEquals("租户一平台", result.getSiteName());
    assertTrue(result.getWatermarkEnabled());
  }

  @Test
  void selectLoginTenantListShouldExposeDefaultBranding() {
    IoTTenant tenant =
        IoTTenant.builder()
            .tenantId("tenant-1")
            .tenantCode("care")
            .tenantName("养老租户")
            .status(0)
            .deleted(0)
            .cfg(
                """
                {
                  "branding": {
                    "default": {
                      "siteName": "智慧养老平台",
                      "siteLogo": "/profile/tenant/logo.png",
                      "siteIcon": "/profile/tenant/favicon.png",
                      "loginTemplate": "contrast"
                    }
                  }
                }
                """)
            .build();
    when(tenantMapper.selectByExample(any())).thenReturn(List.of(tenant));

    List<IoTTenant> result = service.selectLoginTenantList();

    assertEquals(1, result.size());
    assertEquals("care", result.get(0).getTenantCode());
    assertEquals("智慧养老平台", result.get(0).getSiteName());
    assertEquals("智慧养老平台", result.get(0).getLoginTitle());
    assertEquals("/profile/tenant/logo.png", result.get(0).getSiteLogo());
    assertEquals("/profile/tenant/favicon.png", result.get(0).getSiteIcon());
    assertEquals("contrast", result.get(0).getLoginTemplate());
  }

  private void invokeNormalizeTenantBindings(IoTTenant tenant, IoTTenant persisted) throws Exception {
    Method method =
        IoTTenantServiceImpl.class.getDeclaredMethod(
            "normalizeTenantBindings", IoTTenant.class, IoTTenant.class);
    method.setAccessible(true);
    try {
      method.invoke(service, tenant, persisted);
    } catch (InvocationTargetException ex) {
      if (ex.getTargetException() instanceof Exception target) {
        throw target;
      }
      throw ex;
    }
  }
}
