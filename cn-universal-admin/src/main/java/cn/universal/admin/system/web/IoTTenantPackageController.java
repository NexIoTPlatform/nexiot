package cn.universal.admin.system.web;

import cn.universal.admin.system.service.IIoTTenantPackageService;
import cn.universal.admin.system.service.ISysMenuService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenantPackage;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysMenu;
import cn.universal.persistence.entity.admin.TreeSelect;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/tenant-package")
@Tag(name = "租户套餐管理")
public class IoTTenantPackageController extends BaseController {

  @Resource private IIoTTenantPackageService tenantPackageService;
  @Resource private ISysMenuService menuService;

  @GetMapping("/list")
  public TableDataInfo<IoTTenantPackage> list(IoTTenantPackage query) {
    ensurePlatformAdmin();
    startPage();
    return getDataTable(tenantPackageService.selectPackageList(query));
  }

  @GetMapping("/simple-list")
  public AjaxResult<List<IoTTenantPackage>> simpleList() {
    ensurePlatformAdmin();
    return AjaxResult.success(tenantPackageService.selectActivePackages());
  }

  @GetMapping("/menu-treeselect")
  public AjaxResult<List<TreeSelect>> menuTreeselect() {
    ensurePlatformAdmin();
    List<SysMenu> menus = menuService.selectMenuList(new SysMenu(), SecurityUtils.getUnionId());
    return AjaxResult.success(menuService.buildMenuTreeSelect(filterPackageMenus(menus)));
  }

  @GetMapping("/{packageId}")
  public AjaxResult<IoTTenantPackage> getInfo(@PathVariable Long packageId) {
    ensurePlatformAdmin();
    return AjaxResult.success(tenantPackageService.selectPackageById(packageId));
  }

  @PostMapping
  @Log(title = "租户套餐", businessType = BusinessType.INSERT)
  public AjaxResult<Void> add(@RequestBody IoTTenantPackage tenantPackage) {
    ensurePlatformAdmin();
    return toAjax(tenantPackageService.insertPackage(tenantPackage));
  }

  @PutMapping
  @Log(title = "租户套餐", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> edit(@RequestBody IoTTenantPackage tenantPackage) {
    ensurePlatformAdmin();
    return toAjax(tenantPackageService.updatePackage(tenantPackage));
  }

  @PutMapping("/changeStatus")
  @Log(title = "租户套餐状态", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> changeStatus(@RequestBody IoTTenantPackage tenantPackage) {
    ensurePlatformAdmin();
    return toAjax(tenantPackageService.updatePackageStatus(tenantPackage));
  }

  @DeleteMapping("/{packageIds}")
  @Log(title = "租户套餐", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable Long[] packageIds) {
    ensurePlatformAdmin();
    return toAjax(tenantPackageService.deletePackageByIds(packageIds));
  }

  private void ensurePlatformAdmin() {
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || !currentUser.isAdmin()) {
      throw new IoTException("仅平台管理员可操作租户套餐");
    }
  }

  private List<SysMenu> filterPackageMenus(List<SysMenu> menus) {
    List<SysMenu> results = new ArrayList<>();
    if (menus == null || menus.isEmpty()) {
      return results;
    }
    for (SysMenu menu : menus) {
      if (menu == null || isPlatformTenantMenu(menu)) {
        continue;
      }
      menu.setChildren(filterPackageMenus(menu.getChildren()));
      results.add(menu);
    }
    return results;
  }

  private boolean isPlatformTenantMenu(SysMenu menu) {
    if (menu == null) {
      return true;
    }
    String perms = menu.getPerms() == null ? "" : menu.getPerms();
    String path = menu.getPath() == null ? "" : menu.getPath();
    return perms.startsWith("system:tenant:")
        || perms.startsWith("system:tenant-package:")
        || "tenant".equals(path)
        || "tenant-package".equals(path);
  }
}
