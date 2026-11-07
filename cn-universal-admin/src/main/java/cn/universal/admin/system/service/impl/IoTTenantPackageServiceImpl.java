package cn.universal.admin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.service.IIoTTenantPackageService;
import cn.universal.admin.system.service.IIoTTenantService;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.entity.IoTTenantPackage;
import cn.universal.persistence.entity.IoTTenantPackageMenu;
import cn.universal.persistence.entity.admin.SysMenu;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMenuMapper;
import cn.universal.persistence.mapper.admin.SysMenuMapper;
import cn.universal.security.support.EvictMenuPermissions;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

@Service
public class IoTTenantPackageServiceImpl implements IIoTTenantPackageService {

  private static final String DEFAULT_PACKAGE_NAME = "默认租户套餐";

  @Resource private IoTTenantPackageMapper tenantPackageMapper;
  @Resource private IoTTenantPackageMenuMapper tenantPackageMenuMapper;
  @Resource private IoTTenantMapper tenantMapper;
  @Resource private SysMenuMapper menuMapper;
  @Resource @Lazy private IIoTTenantService tenantService;

  @Override
  public List<IoTTenantPackage> selectPackageList(IoTTenantPackage query) {
    IoTTenantPackage filter = query == null ? new IoTTenantPackage() : query;
    Example example = new Example(IoTTenantPackage.class);
    Example.Criteria criteria = example.createCriteria().andEqualTo("deleted", 0);
    if (StrUtil.isNotBlank(filter.getPackageName())) {
      criteria.andLike("packageName", "%" + filter.getPackageName() + "%");
    }
    if (filter.getStatus() != null) {
      criteria.andEqualTo("status", filter.getStatus());
    }
    example.setOrderByClause("create_time desc");
    List<IoTTenantPackage> packages = tenantPackageMapper.selectByExample(example);
    fillPackageMeta(packages);
    return packages;
  }

  @Override
  public IoTTenantPackage selectPackageById(Long packageId) {
    if (packageId == null) {
      return null;
    }
    IoTTenantPackage tenantPackage = tenantPackageMapper.selectByPrimaryKey(packageId);
    if (tenantPackage == null || Integer.valueOf(1).equals(tenantPackage.getDeleted())) {
      return null;
    }
    fillPackageMeta(Collections.singletonList(tenantPackage));
    return tenantPackage;
  }

  @Override
  public List<IoTTenantPackage> selectActivePackages() {
    Example example = new Example(IoTTenantPackage.class);
    example.createCriteria().andEqualTo("deleted", 0).andEqualTo("status", 0);
    example.setOrderByClause("create_time asc");
    return tenantPackageMapper.selectByExample(example);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int insertPackage(IoTTenantPackage tenantPackage) {
    validatePackageForWrite(tenantPackage, false);
    Date now = new Date();
    tenantPackage.setPackageId(SQenGenId.nextId());
    tenantPackage.setCreateBy(SecurityUtils.getUnionIdQuietly());
    tenantPackage.setCreateTime(now);
    tenantPackage.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    tenantPackage.setUpdateTime(now);
    tenantPackage.setDeleted(0);
    tenantPackage.setStatus(tenantPackage.getStatus() == null ? 0 : tenantPackage.getStatus());
    int rows = tenantPackageMapper.insertSelective(tenantPackage);
    savePackageMenus(tenantPackage.getPackageId(), tenantPackage.getMenuIds());
    return rows;
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int updatePackage(IoTTenantPackage tenantPackage) {
    if (tenantPackage == null || tenantPackage.getPackageId() == null) {
      throw new IoTException("租户套餐不存在");
    }
    IoTTenantPackage persisted = tenantPackageMapper.selectByPrimaryKey(tenantPackage.getPackageId());
    if (persisted == null || Integer.valueOf(1).equals(persisted.getDeleted())) {
      throw new IoTException("租户套餐不存在");
    }
    validatePackageForWrite(tenantPackage, true);
    Set<Long> oldMenuIds = new LinkedHashSet<>(selectPackageMenuIds(tenantPackage.getPackageId()));
    Set<Long> newMenuIds = normalizeMenuIds(tenantPackage.getMenuIds());
    tenantPackage.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    tenantPackage.setUpdateTime(new Date());
    int rows = tenantPackageMapper.updateByPrimaryKeySelective(tenantPackage);
    if (!oldMenuIds.equals(newMenuIds)) {
      Example example = new Example(IoTTenantPackageMenu.class);
      example.createCriteria().andEqualTo("packageId", tenantPackage.getPackageId());
      tenantPackageMenuMapper.deleteByExample(example);
      savePackageMenus(tenantPackage.getPackageId(), tenantPackage.getMenuIds());
      tenantService.syncTenantRoleMenusByPackage(tenantPackage.getPackageId());
    }
    return rows;
  }

  @Override
  @EvictMenuPermissions
  public int updatePackageStatus(IoTTenantPackage tenantPackage) {
    if (tenantPackage == null || tenantPackage.getPackageId() == null) {
      throw new IoTException("租户套餐不存在");
    }
    IoTTenantPackage persisted = tenantPackageMapper.selectByPrimaryKey(tenantPackage.getPackageId());
    if (persisted == null || Integer.valueOf(1).equals(persisted.getDeleted())) {
      throw new IoTException("租户套餐不存在");
    }
    tenantPackage.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    tenantPackage.setUpdateTime(new Date());
    return tenantPackageMapper.updateByPrimaryKeySelective(tenantPackage);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int deletePackageByIds(Long[] packageIds) {
    List<Long> validPackageIds = normalizePackageIds(packageIds);
    if (CollectionUtils.isEmpty(validPackageIds)) {
      return 0;
    }
    if (!selectPackageTenantCountMap(validPackageIds).isEmpty()) {
      throw new IoTException("租户套餐已被租户使用，不允许删除");
    }
    Example menuExample = new Example(IoTTenantPackageMenu.class);
    menuExample.createCriteria().andIn("packageId", validPackageIds);
    tenantPackageMenuMapper.deleteByExample(menuExample);
    Example packageExample = new Example(IoTTenantPackage.class);
    packageExample.createCriteria().andIn("packageId", validPackageIds);
    return tenantPackageMapper.deleteByExample(packageExample);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public Long getOrCreateDefaultPackageId() {
    Example example = new Example(IoTTenantPackage.class);
    example.createCriteria().andEqualTo("deleted", 0).andEqualTo("packageName", DEFAULT_PACKAGE_NAME);
    example.setOrderByClause("create_time asc");
    List<IoTTenantPackage> packages = tenantPackageMapper.selectByExample(example);
    if (!CollectionUtils.isEmpty(packages)) {
      return packages.get(0).getPackageId();
    }
    List<Long> defaultMenuIds =
        filterDefaultPackageMenus(menuMapper.selectMenuList(SysMenu.builder().status("0").build()));
    IoTTenantPackage tenantPackage =
        IoTTenantPackage.builder()
            .packageName(DEFAULT_PACKAGE_NAME)
            .status(0)
            .remark("系统自动初始化的默认租户套餐")
            .menuIds(defaultMenuIds.toArray(new Long[0]))
            .build();
    insertPackage(tenantPackage);
    return tenantPackage.getPackageId();
  }

  private void validatePackageForWrite(IoTTenantPackage tenantPackage, boolean update) {
    if (tenantPackage == null) {
      throw new IoTException("租户套餐参数不能为空");
    }
    if (StrUtil.isBlank(tenantPackage.getPackageName())) {
      throw new IoTException("租户套餐名称不能为空");
    }
    if (tenantPackage.getMenuIds() == null || tenantPackage.getMenuIds().length == 0) {
      throw new IoTException("租户套餐至少需要选择一个菜单");
    }
    validatePackageMenuScope(tenantPackage.getMenuIds());
    Example example = new Example(IoTTenantPackage.class);
    Example.Criteria criteria =
        example
            .createCriteria()
            .andEqualTo("deleted", 0)
            .andEqualTo("packageName", tenantPackage.getPackageName());
    List<IoTTenantPackage> packages = tenantPackageMapper.selectByExample(example);
    if (!CollectionUtils.isEmpty(packages)) {
      IoTTenantPackage existed = packages.get(0);
      if (!update || !existed.getPackageId().equals(tenantPackage.getPackageId())) {
        throw new IoTException("租户套餐名称已存在");
      }
    }
  }

  private void validatePackageMenuScope(Long[] menuIds) {
    Set<Long> uniqueMenuIds = normalizeMenuIds(menuIds);
    if (uniqueMenuIds.isEmpty()) {
      throw new IoTException("租户套餐至少需要选择一个菜单");
    }
    Example example = new Example(SysMenu.class);
    example.createCriteria().andIn("menuId", new ArrayList<>(uniqueMenuIds));
    List<SysMenu> menus = menuMapper.selectByExample(example);
    if (menus.size() != uniqueMenuIds.size()) {
      throw new IoTException("所选菜单不存在或已删除");
    }
    for (SysMenu menu : menus) {
      if (isPlatformTenantMenu(menu)) {
        throw new IoTException("租户套餐不能包含平台租户治理菜单");
      }
    }
  }

  private void fillPackageMeta(List<IoTTenantPackage> tenantPackages) {
    if (CollectionUtils.isEmpty(tenantPackages)) {
      return;
    }
    for (IoTTenantPackage tenantPackage : tenantPackages) {
      tenantPackage.setMenuIds(new Long[0]);
      tenantPackage.setMenuCount(0);
      tenantPackage.setTenantCount(0L);
    }
    List<Long> packageIds =
        tenantPackages.stream()
            .map(IoTTenantPackage::getPackageId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(packageIds)) {
      return;
    }
    Map<Long, List<Long>> packageMenuIdsMap = selectPackageMenuIdsMap(packageIds);
    Map<Long, Long> packageTenantCountMap = selectPackageTenantCountMap(packageIds);
    for (IoTTenantPackage tenantPackage : tenantPackages) {
      List<Long> menuIds =
          packageMenuIdsMap.getOrDefault(tenantPackage.getPackageId(), Collections.emptyList());
      tenantPackage.setMenuIds(menuIds.toArray(new Long[0]));
      tenantPackage.setMenuCount(menuIds.size());
      tenantPackage.setTenantCount(
          packageTenantCountMap.getOrDefault(tenantPackage.getPackageId(), 0L));
    }
  }

  private List<Long> selectPackageMenuIds(Long packageId) {
    if (packageId == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(
        selectPackageMenuIdsMap(Collections.singletonList(packageId))
            .getOrDefault(packageId, Collections.emptyList()));
  }

  private Map<Long, List<Long>> selectPackageMenuIdsMap(Collection<Long> packageIds) {
    Map<Long, List<Long>> packageMenuIdsMap = new HashMap<>();
    if (CollectionUtils.isEmpty(packageIds)) {
      return packageMenuIdsMap;
    }
    Example example = new Example(IoTTenantPackageMenu.class);
    example.createCriteria().andIn("packageId", new ArrayList<>(packageIds));
    example.setOrderByClause("package_id asc, create_time asc");
    List<IoTTenantPackageMenu> relations = tenantPackageMenuMapper.selectByExample(example);
    for (IoTTenantPackageMenu relation : relations) {
      packageMenuIdsMap
          .computeIfAbsent(relation.getPackageId(), key -> new ArrayList<>())
          .add(relation.getMenuId());
    }
    return packageMenuIdsMap;
  }

  private void savePackageMenus(Long packageId, Long[] menuIds) {
    if (packageId == null) {
      return;
    }
    Set<Long> uniqueMenuIds = normalizeMenuIds(menuIds);
    if (uniqueMenuIds.isEmpty()) {
      return;
    }
    Date now = new Date();
    List<IoTTenantPackageMenu> relations = new ArrayList<>(uniqueMenuIds.size());
    for (Long menuId : uniqueMenuIds) {
      relations.add(
          IoTTenantPackageMenu.builder()
              .id(SQenGenId.nextId())
              .packageId(packageId)
              .menuId(menuId)
              .createTime(now)
              .build());
    }
    tenantPackageMenuMapper.batchInsert(relations);
  }

  private List<Long> filterDefaultPackageMenus(List<SysMenu> menus) {
    if (CollectionUtils.isEmpty(menus)) {
      return new ArrayList<>();
    }
    return menus.stream()
        .filter(menu -> !isPlatformTenantMenu(menu))
        .map(SysMenu::getMenuId)
        .collect(Collectors.toList());
  }

  private boolean isPlatformTenantMenu(SysMenu menu) {
    if (menu == null) {
      return true;
    }
    String perms = StrUtil.blankToDefault(menu.getPerms(), "");
    if (perms.startsWith("system:tenant:") || perms.startsWith("system:tenant-package:")) {
      return true;
    }
    String path = StrUtil.blankToDefault(menu.getPath(), "");
    return "tenant".equals(path) || "tenant-package".equals(path);
  }

  private Set<Long> normalizeMenuIds(Long[] menuIds) {
    if (menuIds == null || menuIds.length == 0) {
      return new LinkedHashSet<>();
    }
    return Arrays.stream(menuIds)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private List<Long> normalizePackageIds(Long[] packageIds) {
    if (packageIds == null || packageIds.length == 0) {
      return new ArrayList<>();
    }
    return Arrays.stream(packageIds)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
  }

  private Map<Long, Long> selectPackageTenantCountMap(Collection<Long> packageIds) {
    Map<Long, Long> packageTenantCountMap = new HashMap<>();
    if (CollectionUtils.isEmpty(packageIds)) {
      return packageTenantCountMap;
    }
    List<Map<String, Object>> tenantCounts = tenantMapper.selectTenantCountByPackageIds(packageIds);
    if (CollectionUtils.isEmpty(tenantCounts)) {
      return packageTenantCountMap;
    }
    for (Map<String, Object> tenantCount : tenantCounts) {
      Long packageId = toLong(tenantCount.get("packageId"));
      if (packageId == null) {
        continue;
      }
      Long count = toLong(tenantCount.get("tenantCount"));
      packageTenantCountMap.put(packageId, count == null ? 0L : count);
    }
    return packageTenantCountMap;
  }

  private Long toLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }
}
