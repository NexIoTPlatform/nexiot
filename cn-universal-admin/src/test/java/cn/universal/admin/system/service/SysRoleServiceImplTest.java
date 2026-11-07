package cn.universal.admin.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.universal.admin.system.service.impl.SysRoleServiceImpl;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.admin.SysRoleDept;
import cn.universal.persistence.entity.admin.SysRoleMenu;
import cn.universal.persistence.entity.admin.SysUserRole;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMenuMapper;
import cn.universal.persistence.mapper.admin.SysRoleDeptMapper;
import cn.universal.persistence.mapper.admin.SysRoleMapper;
import cn.universal.persistence.mapper.admin.SysRoleMenuMapper;
import cn.universal.persistence.mapper.admin.SysUserRoleMapper;
import cn.universal.security.service.IoTUserService;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

  @InjectMocks private SysRoleServiceImpl service;

  @Mock private SysRoleDeptMapper roleDeptMapper;
  @Mock private SysRoleMenuMapper roleMenuMapper;
  @Mock private SysUserRoleMapper userRoleMapper;
  @Mock private SysRoleMapper roleMapper;
  @Mock private ISysDeptService deptService;
  @Mock private IoTUserService ioTUserService;
  @Mock private IoTTenantMapper tenantMapper;
  @Mock private IoTTenantPackageMenuMapper tenantPackageMenuMapper;

  @BeforeAll
  static void initEntityTables() {
    TkMapperEntityTestSupport.registerEntityTables(
        SysRole.class, SysRoleMenu.class, SysRoleDept.class, SysUserRole.class);
  }

  @Test
  void insertRoleMenuShouldBatchInsertUniqueMenus() {
    SysRole role = SysRole.builder().roleId(10L).menuIds(new Long[] {100L, 200L, 200L, null}).build();

    service.insertRoleMenu(role);

    ArgumentCaptor<List<SysRoleMenu>> menuCaptor = ArgumentCaptor.forClass(List.class);
    verify(roleMenuMapper).batchInsert(menuCaptor.capture());
    verify(roleMenuMapper, never()).insertSelective(any());

    List<SysRoleMenu> roleMenus = menuCaptor.getValue();
    assertEquals(2, roleMenus.size());
    assertEquals(List.of(100L, 200L), roleMenus.stream().map(SysRoleMenu::getMenuId).toList());
    assertEquals(List.of(10L, 10L), roleMenus.stream().map(SysRoleMenu::getRoleId).toList());
  }

  @Test
  void insertRoleDeptShouldBatchInsertUniqueDepts() {
    SysRole role = SysRole.builder().roleId(20L).deptIds(new Long[] {11L, 12L, 11L, null}).build();

    service.insertRoleDept(role);

    ArgumentCaptor<List<SysRoleDept>> deptCaptor = ArgumentCaptor.forClass(List.class);
    verify(roleDeptMapper).batchInsert(deptCaptor.capture());
    verify(roleDeptMapper, never()).insertSelective(any());

    List<SysRoleDept> roleDepts = deptCaptor.getValue();
    assertEquals(2, roleDepts.size());
    assertEquals(List.of(11L, 12L), roleDepts.stream().map(SysRoleDept::getDeptId).toList());
    assertEquals(List.of(20L, 20L), roleDepts.stream().map(SysRoleDept::getRoleId).toList());
  }

  @Test
  void insertAuthUsersShouldBatchInsertUniqueUsers() {
    service.insertAuthUsers(30L, new String[] {"u1", "u2", "u1", "", null});

    ArgumentCaptor<List<SysUserRole>> userRoleCaptor = ArgumentCaptor.forClass(List.class);
    verify(userRoleMapper).batchInsert(userRoleCaptor.capture());
    verify(userRoleMapper, never()).insertSelective(any());

    List<SysUserRole> userRoles = userRoleCaptor.getValue();
    assertEquals(2, userRoles.size());
    assertEquals(List.of("u1", "u2"), userRoles.stream().map(SysUserRole::getUnionId).toList());
    assertEquals(List.of(30L, 30L), userRoles.stream().map(SysUserRole::getRoleId).toList());
  }
}
