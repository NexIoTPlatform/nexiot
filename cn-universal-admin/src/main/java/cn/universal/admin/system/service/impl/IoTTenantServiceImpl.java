package cn.universal.admin.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.system.service.IIoTTenantPackageService;
import cn.universal.admin.system.service.IIoTTenantService;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.ossm.service.ISysOssConfigService;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTTenantPackage;
import cn.universal.persistence.entity.IoTTenantPackageMenu;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.entity.RuleModel;
import cn.universal.persistence.entity.RulegoChain;
import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.admin.SysRole;
import cn.universal.persistence.entity.admin.SysRoleDept;
import cn.universal.persistence.entity.admin.SysRoleMenu;
import cn.universal.persistence.entity.admin.SysUserRole;
import cn.universal.persistence.entity.bo.IoTUserBO;
import cn.universal.persistence.entity.bo.TenantBrandingBO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMapper;
import cn.universal.persistence.mapper.IoTTenantPackageMenuMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import cn.universal.persistence.mapper.RuleModelMapper;
import cn.universal.persistence.mapper.RulegoChainMapper;
import cn.universal.persistence.mapper.admin.SysDeptMapper;
import cn.universal.persistence.mapper.admin.SysRoleMapper;
import cn.universal.persistence.mapper.admin.SysRoleDeptMapper;
import cn.universal.persistence.mapper.admin.SysRoleMenuMapper;
import cn.universal.persistence.mapper.admin.SysUserRoleMapper;
import cn.universal.security.support.EvictMenuPermissions;
import cn.universal.security.support.TenantBrandingResolver;
import cn.universal.security.support.TenantDomainResolver;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

@Service
public class IoTTenantServiceImpl implements IIoTTenantService {

  private static final String TENANT_ADMIN_ROLE_KEY = "tenant:admin";
  private static final String ROLE_TYPE_TENANT_ADMIN = "TENANT_ADMIN";
  private static final String TENANT_ADMIN_ROLE_NAME_SUFFIX = "-租户管理员";
  private static final int ROLE_NAME_MAX_LENGTH = 30;
  private static final String ROOT_DEPT_NAME = "租户根部门";
  private static final String LEGACY_ROOT_DEPT_NAME = "默认部门";

  @Resource private IoTTenantMapper tenantMapper;
  @Resource private IoTTenantPackageMapper tenantPackageMapper;
  @Resource private IoTTenantPackageMenuMapper tenantPackageMenuMapper;
  @Resource private IoTUserMapper userMapper;
  @Resource private IoTUserService ioTUserService;
  @Resource private IIoTTenantPackageService tenantPackageService;
  @Resource private ISysDeptService deptService;
  @Resource private SysDeptMapper sysDeptMapper;
  @Resource private SysRoleMapper sysRoleMapper;
  @Resource private SysRoleDeptMapper sysRoleDeptMapper;
  @Resource private SysRoleMenuMapper sysRoleMenuMapper;
  @Resource private SysUserRoleMapper sysUserRoleMapper;
  @Resource private ISysOssConfigService sysOssConfigService;
  @Resource private IoTProductMapper productMapper;
  @Resource private IoTDeviceMapper deviceMapper;
  @Resource private IoTUserApplicationMapper userApplicationMapper;
  @Resource private NetworkMapper networkMapper;
  @Resource private RuleModelMapper ruleModelMapper;
  @Resource private RulegoChainMapper rulegoChainMapper;
  @Resource private TenantDomainResolver tenantDomainResolver;

  @Override
  public List<IoTTenant> selectTenantList(IoTTenant query) {
    IoTTenant filter = query == null ? new IoTTenant() : query;
    Example example = new Example(IoTTenant.class);
    Example.Criteria criteria = example.createCriteria().andEqualTo("deleted", 0);
    if (StrUtil.isNotBlank(filter.getTenantName())) {
      criteria.andLike("tenantName", "%" + filter.getTenantName() + "%");
    }
    if (StrUtil.isNotBlank(filter.getTenantCode())) {
      criteria.andLike("tenantCode", "%" + filter.getTenantCode() + "%");
    }
    if (StrUtil.isNotBlank(filter.getWebsite())) {
      criteria.andLike("website", "%" + filter.getWebsite() + "%");
    }
    if (StrUtil.isNotBlank(filter.getDomain())) {
      criteria.andLike("domain", "%" + filter.getDomain() + "%");
    }
    if (filter.getStatus() != null) {
      criteria.andEqualTo("status", filter.getStatus());
    }
    if (filter.getPackageId() != null) {
      criteria.andEqualTo("packageId", filter.getPackageId());
    }
    example.setOrderByClause("create_time desc");
    List<IoTTenant> tenants = tenantMapper.selectByExample(example);
    tenants.forEach(this::fillTenantMeta);
    return tenants;
  }

  @Override
  public List<IoTTenant> selectLoginTenantList() {
    Example example = new Example(IoTTenant.class);
    example.createCriteria().andEqualTo("deleted", 0).andEqualTo("status", 0);
    example.setOrderByClause("tenant_name asc, create_time asc");
    Date now = new Date();
    return tenantMapper.selectByExample(example).stream()
        .filter(tenant -> tenant.getExpireTime() == null || !tenant.getExpireTime().before(now))
        .map(
            tenant -> {
              TenantBrandingResolver.TenantBranding branding =
                  TenantBrandingResolver.resolveBranding(tenant, null);
              return IoTTenant.builder()
                  .tenantId(tenant.getTenantId())
                  .tenantCode(tenant.getTenantCode())
                  .tenantName(tenant.getTenantName())
                  .siteName(branding.getSiteName())
                  .loginTitle(branding.getLoginTitle())
                  .siteLogo(branding.getSiteLogo())
                  .siteIcon(branding.getSiteIcon())
                  .loginTemplate(branding.getLoginTemplate())
                  .build();
            })
        .collect(Collectors.toList());
  }

  @Override
  public IoTTenant resolveLoginTenantByHost(String host) {
    IoTTenant tenant = tenantDomainResolver.resolveTenantByHost(host);
    if (tenant == null) {
      return null;
    }
    TenantBrandingResolver.TenantBranding branding =
        TenantBrandingResolver.resolveBranding(tenant, host);
    return IoTTenant.builder()
        .tenantId(tenant.getTenantId())
        .tenantCode(tenant.getTenantCode())
        .tenantName(tenant.getTenantName())
        .website(tenant.getWebsite())
        .domain(tenant.getDomain())
        .siteName(branding.getSiteName())
        .loginTitle(branding.getLoginTitle())
        .siteLogo(branding.getSiteLogo())
        .siteIcon(branding.getSiteIcon())
        .loginTemplate(branding.getLoginTemplate())
        .build();
  }

  @Override
  public IoTTenant selectTenantById(String tenantId) {
    if (StrUtil.isBlank(tenantId)) {
      return null;
    }
    IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
      return null;
    }
    fillTenantMeta(tenant);
    return tenant;
  }

  @Override
  public TenantBrandingBO selectTenantBranding(String tenantId) {
    IoTTenant tenant = requireTenant(tenantId);
    TenantBrandingResolver.TenantBranding branding =
        TenantBrandingResolver.resolveBranding(tenant, null);
    return TenantBrandingBO.builder()
        .siteName(branding.getSiteName())
        .siteLogo(branding.getSiteLogo())
        .siteIcon(branding.getSiteIcon())
        .watermarkEnabled(branding.isWatermarkEnabled())
        .watermarkText(branding.getWatermarkText())
        .build();
  }

  @Override
  @Transactional
  public TenantBrandingBO updateTenantBranding(String tenantId, TenantBrandingBO branding) {
    IoTTenant tenant = requireTenant(tenantId);
    if (branding == null || StrUtil.isBlank(branding.getSiteName())) {
      throw new IoTException("平台名称不能为空");
    }
    String siteName = StrUtil.trim(branding.getSiteName());
    if (siteName.length() > 50) {
      throw new IoTException("平台名称不能超过50个字符");
    }
    String siteLogo = StrUtil.trimToEmpty(branding.getSiteLogo());
    String siteIcon = StrUtil.trimToEmpty(branding.getSiteIcon());
    String watermarkText = StrUtil.trimToEmpty(branding.getWatermarkText());
    if (siteLogo.length() > 500 || siteIcon.length() > 500) {
      throw new IoTException("品牌图片地址不能超过500个字符");
    }
    if (watermarkText.length() > 50) {
      throw new IoTException("水印文字不能超过50个字符");
    }

    JSONObject cfg =
        StrUtil.isBlank(tenant.getCfg()) ? JSONUtil.createObj() : JSONUtil.parseObj(tenant.getCfg());
    JSONObject brandingConfig = cfg.getJSONObject("branding");
    if (brandingConfig == null) {
      brandingConfig = JSONUtil.createObj();
      cfg.set("branding", brandingConfig);
    }
    JSONObject defaultBranding = brandingConfig.getJSONObject("default");
    if (defaultBranding == null) {
      defaultBranding = JSONUtil.createObj();
      brandingConfig.set("default", defaultBranding);
    }
    defaultBranding.set("siteName", siteName);
    defaultBranding.set("loginTitle", siteName);
    setOrRemove(defaultBranding, "siteLogo", siteLogo);
    setOrRemove(defaultBranding, "siteIcon", siteIcon);
    defaultBranding.set(
        "watermark",
        JSONUtil.createObj()
            .set("enabled", Boolean.TRUE.equals(branding.getWatermarkEnabled()))
            .set("text", watermarkText));

    IoTTenant update =
        IoTTenant.builder()
            .tenantId(tenant.getTenantId())
            .cfg(cfg.toString())
            .updateBy(SecurityUtils.getUnionIdQuietly())
            .updateTime(new Date())
            .build();
    tenantMapper.updateByPrimaryKeySelective(update);
    tenant.setCfg(update.getCfg());
    return selectTenantBranding(tenantId);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int insertTenant(IoTTenant tenant) {
    validateTenantForCreate(tenant);
    Long packageId =
        tenant.getPackageId() == null ? tenantPackageService.getOrCreateDefaultPackageId() : tenant.getPackageId();
    IoTTenantPackage tenantPackage = requireUsablePackage(packageId);
    List<Long> packageMenuIds = selectPackageMenuIds(packageId);
    if (CollectionUtils.isEmpty(packageMenuIds)) {
      throw new IoTException("所选租户套餐还未配置菜单");
    }

    String tenantId = generateTenantId();
    Date now = new Date();
    String operatorUnionId = SecurityUtils.getUnionIdQuietly();
    String adminUnionId = tenant.getAdminUsername();
    tenant.setTenantId(tenantId);
    tenant.setPackageId(tenantPackage.getPackageId());
    tenant.setStatus(tenant.getStatus() == null ? 0 : tenant.getStatus());
    tenant.setDeleted(0);
    tenant.setCreateBy(operatorUnionId);
    tenant.setCreateTime(now);
    tenant.setUpdateBy(operatorUnionId);
    tenant.setUpdateTime(now);
    tenant.setAccountCount(tenant.getAccountCount() == null ? 1 : tenant.getAccountCount());
    tenantMapper.insertSelective(tenant);

    SysDept rootDept = deptService.ensureTenantRootDept(tenantId, adminUnionId, tenant.getTenantName());
    SysRole adminRole = createTenantAdminRole(tenantId, tenant.getTenantName(), adminUnionId, packageMenuIds);
    IoTUserBO adminUser = buildTenantAdminUser(tenant, rootDept, adminRole);
    ioTUserService.insertUser(adminUser);
    syncTenantRoleMenus(tenantId, packageMenuIds);
    sysOssConfigService.ensureTenantDefaultConfigs(tenantId, operatorUnionId);
    return 1;
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int updateTenant(IoTTenant tenant) {
    return updateTenant(tenant, false);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int updateTenant(IoTTenant tenant, boolean clearExpireTime) {
    if (tenant == null || StrUtil.isBlank(tenant.getTenantId())) {
      throw new IoTException("租户不存在");
    }
    IoTTenant persisted = tenantMapper.selectByPrimaryKey(tenant.getTenantId());
    if (persisted == null || Integer.valueOf(1).equals(persisted.getDeleted())) {
      throw new IoTException("租户不存在");
    }
    validateTenantForUpdate(tenant, persisted);
    Long packageId = tenant.getPackageId() == null ? persisted.getPackageId() : tenant.getPackageId();
    requireUsablePackage(packageId);
    if (tenant.getAccountCount() != null && tenant.getAccountCount() < countActiveUsers(tenant.getTenantId())) {
      throw new IoTException("账号上限不能小于当前租户已开通账号数");
    }
    tenant.setPackageId(packageId);
    tenant.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    Date updateTime = new Date();
    tenant.setUpdateTime(updateTime);
    int rows = tenantMapper.updateByPrimaryKeySelective(tenant);
    if (clearExpireTime) {
      rows =
          tenantMapper.updateTenantExpireTime(
              tenant.getTenantId(), null, tenant.getUpdateBy(), updateTime);
    }
    syncTenantRootDeptName(tenant.getTenantId(), tenant.getTenantName(), persisted.getTenantName());
    if (!packageId.equals(persisted.getPackageId())) {
      syncTenantRoleMenus(tenant.getTenantId(), selectPackageMenuIds(packageId));
    }
    syncTenantAdminInfo(tenant);
    return rows;
  }

  @Override
  @EvictMenuPermissions
  public int updateTenantStatus(IoTTenant tenant) {
    if (tenant == null || StrUtil.isBlank(tenant.getTenantId())) {
      throw new IoTException("租户不存在");
    }
    IoTTenant persisted = tenantMapper.selectByPrimaryKey(tenant.getTenantId());
    if (persisted == null || Integer.valueOf(1).equals(persisted.getDeleted())) {
      throw new IoTException("租户不存在");
    }
    tenant.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    tenant.setUpdateTime(new Date());
    return tenantMapper.updateByPrimaryKeySelective(tenant);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int deleteTenantByIds(String[] tenantIds) {
    int rows = 0;
    if (tenantIds == null || tenantIds.length == 0) {
      return rows;
    }
    for (String tenantId : tenantIds) {
      if (StrUtil.isBlank(tenantId)) {
        continue;
      }
      IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
      if (tenant == null) {
        continue;
      }
      long activeUserCount = countActiveUsers(tenantId);
      IoTUser adminUser = selectTenantAdmin(tenantId);
      if (activeUserCount > 0) {
        long memberCount = activeUserCount - (adminUser == null ? 0 : 1);
        if (memberCount > 0) {
          throw new IoTException("租户下仍存在成员账号，请先清理成员账号后再删除");
        }
        if (adminUser == null) {
          throw new IoTException("租户下仍存在账号，但未找到租户管理员，请先人工核对租户账号数据");
        }
      }
      if (countTenantBusinessResources(tenantId) > 0) {
        throw new IoTException("租户下仍存在业务数据，请先清理产品、设备、应用、网络、规则后再删除");
      }
      cleanupTenantAdmin(adminUser);
      Example deptExample = new Example(SysDept.class);
      deptExample.createCriteria().andEqualTo("tenantId", tenantId);
      sysDeptMapper.deleteByExample(deptExample);
      rows += tenantMapper.deleteByPrimaryKey(tenantId);
    }
    return rows;
  }

  @Override
  public void assertTenantUserQuota(String tenantId, int increment) {
    if (StrUtil.isBlank(tenantId)) {
      throw new IoTException("租户标识不能为空");
    }
    IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
      throw new IoTException("租户不存在");
    }
    if (Integer.valueOf(1).equals(tenant.getStatus())) {
      throw new IoTException("当前租户已停用，无法开通账号");
    }
    if (tenant.getExpireTime() != null && tenant.getExpireTime().before(new Date())) {
      throw new IoTException("当前租户已过期，无法开通账号");
    }
    int safeIncrement = Math.max(0, increment);
    if (tenant.getAccountCount() != null
        && tenant.getAccountCount() > 0
        && countActiveUsers(tenantId) + safeIncrement > tenant.getAccountCount()) {
      throw new IoTException("当前租户账号数已达到上限，请先扩容租户套餐或调整账号上限");
    }
  }

  @Override
  public Long countActiveUsers(String tenantId) {
    if (StrUtil.isBlank(tenantId)) {
      return 0L;
    }
    Example example = new Example(IoTUser.class);
    example.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("deleted", 0);
    return (long) userMapper.selectCountByExample(example);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public void ensureTenantAdminRoleBinding(String tenantId, String adminUnionId) {
    if (StrUtil.isBlank(tenantId)) {
      throw new IoTException("租户不存在");
    }
    if (StrUtil.isBlank(adminUnionId)) {
      throw new IoTException("租户管理员账号不能为空");
    }
    IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
      throw new IoTException("租户不存在");
    }
    Long packageId =
        tenant.getPackageId() == null ? tenantPackageService.getOrCreateDefaultPackageId() : tenant.getPackageId();
    IoTTenantPackage tenantPackage = requireUsablePackage(packageId);
    List<Long> packageMenuIds = selectPackageMenuIds(tenantPackage.getPackageId());
    if (CollectionUtils.isEmpty(packageMenuIds)) {
      throw new IoTException("当前租户套餐还未配置菜单");
    }
    syncTenantRoleMenus(tenantId, packageMenuIds);
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public void syncTenantRoleMenusByPackage(Long packageId) {
    if (packageId == null) {
      return;
    }
    Example example = new Example(IoTTenant.class);
    example.createCriteria().andEqualTo("packageId", packageId).andEqualTo("deleted", 0);
    List<IoTTenant> tenants = tenantMapper.selectByExample(example);
    if (CollectionUtils.isEmpty(tenants)) {
      return;
    }
    List<Long> packageMenuIds = selectPackageMenuIds(packageId);
    for (IoTTenant tenant : tenants) {
      if (tenant == null || StrUtil.isBlank(tenant.getTenantId())) {
        continue;
      }
      syncTenantRoleMenus(tenant.getTenantId(), packageMenuIds);
    }
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public IoTTenant createBootstrapTenant(
      String tenantCode,
      String tenantName,
      String contactName,
      String contactMobile,
      String adminUnionId,
      String operatorUnionId) {
    if (StrUtil.isBlank(tenantCode)) {
      throw new IoTException("租户编码不能为空");
    }
    if (StrUtil.isBlank(adminUnionId)) {
      throw new IoTException("租户管理员账号不能为空");
    }
    assertTenantCodeUnique(tenantCode, null);
    Long defaultPackageId = tenantPackageService.getOrCreateDefaultPackageId();
    IoTTenant tenant =
        IoTTenant.builder()
            .tenantId(generateTenantId())
            .tenantCode(tenantCode)
            .tenantName(StrUtil.blankToDefault(tenantName, tenantCode))
            .packageId(defaultPackageId)
            .status(0)
            .accountCount(100)
            .contactName(StrUtil.blankToDefault(contactName, tenantName))
            .contactMobile(contactMobile)
            .createBy(operatorUnionId)
            .createTime(new Date())
            .updateBy(operatorUnionId)
            .updateTime(new Date())
            .deleted(0)
            .remark("通过用户管理自动补齐的租户信息")
            .build();
    tenantMapper.insertSelective(tenant);
    return tenant;
  }

  private void validateTenantForCreate(IoTTenant tenant) {
    if (tenant == null) {
      throw new IoTException("租户参数不能为空");
    }
    if (StrUtil.isBlank(tenant.getTenantName())) {
      throw new IoTException("租户名称不能为空");
    }
    if (StrUtil.isBlank(tenant.getTenantCode())) {
      throw new IoTException("租户编码不能为空");
    }
    if (tenant.getAccountCount() == null || tenant.getAccountCount() < 1) {
      throw new IoTException("账号数量上限至少为 1");
    }
    if (StrUtil.isBlank(tenant.getAdminUsername())) {
      throw new IoTException("租户管理员用户名不能为空");
    }
    if (StrUtil.isBlank(tenant.getAdminAlias())) {
      throw new IoTException("租户管理员昵称不能为空");
    }
    if (StrUtil.isBlank(tenant.getAdminMobile())) {
      throw new IoTException("租户管理员手机号不能为空");
    }
    if (StrUtil.isBlank(tenant.getAdminPassword())) {
      throw new IoTException("租户管理员密码不能为空");
    }
    normalizeTenantBindings(tenant, null);
    assertTenantCodeUnique(tenant.getTenantCode(), null);
    assertTenantNameUnique(tenant.getTenantName(), null);
    tenantDomainResolver.assertHostBindingsUnique(tenant, null);
    if (IoTUserConstants.NOT_UNIQUE.equals(
        ioTUserService.checkUserNameUnique(IoTUser.builder().username(tenant.getAdminUsername()).build()))) {
      throw new IoTException("租户管理员用户名已存在");
    }
    if (IoTUserConstants.NOT_UNIQUE.equals(
        ioTUserService.checkPhoneUnique(IoTUser.builder().mobile(tenant.getAdminMobile()).build()))) {
      throw new IoTException("租户管理员手机号已存在");
    }
  }

  private void validateTenantForUpdate(IoTTenant tenant, IoTTenant persisted) {
    normalizeTenantBindings(tenant, persisted);
    if (StrUtil.isNotBlank(tenant.getTenantCode())) {
      assertTenantCodeUnique(tenant.getTenantCode(), persisted.getTenantId());
    }
    if (StrUtil.isNotBlank(tenant.getTenantName())) {
      assertTenantNameUnique(tenant.getTenantName(), persisted.getTenantId());
    }
    tenantDomainResolver.assertHostBindingsUnique(tenant, persisted.getTenantId());
  }

  private void normalizeTenantBindings(IoTTenant tenant, IoTTenant persisted) {
    if (tenant == null) {
      return;
    }
    if (tenant.getDomain() != null) {
      tenant.setDomain(tenantDomainResolver.normalizeBindingValue(tenant.getDomain()));
    } else if (persisted != null) {
      tenant.setDomain(persisted.getDomain());
    }
    if (tenant.getWebsite() != null) {
      tenant.setWebsite(tenantDomainResolver.normalizeBindingValue(tenant.getWebsite()));
    } else if (persisted != null) {
      tenant.setWebsite(persisted.getWebsite());
    }
    normalizeTenantCfg(tenant);
  }

  private void normalizeTenantCfg(IoTTenant tenant) {
    if (tenant == null || tenant.getCfg() == null) {
      return;
    }
    String rawCfg = StrUtil.trim(tenant.getCfg());
    if (StrUtil.isBlank(rawCfg)) {
      tenant.setCfg("{}");
      return;
    }
    if (!JSONUtil.isTypeJSONObject(rawCfg)) {
      throw new IoTException("扩展配置 cfg 必须是合法 JSON 对象");
    }
    tenant.setCfg(JSONUtil.parseObj(rawCfg).toString());
  }

  private IoTTenant requireTenant(String tenantId) {
    if (StrUtil.isBlank(tenantId)) {
      throw new IoTException("当前账号未关联租户");
    }
    IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
      throw new IoTException("租户不存在");
    }
    return tenant;
  }

  private void setOrRemove(JSONObject object, String key, String value) {
    if (StrUtil.isBlank(value)) {
      object.remove(key);
    } else {
      object.set(key, value);
    }
  }

  private void fillTenantMeta(IoTTenant tenant) {
    if (tenant == null) {
      return;
    }
    IoTTenantPackage tenantPackage =
        tenant.getPackageId() == null ? null : tenantPackageMapper.selectByPrimaryKey(tenant.getPackageId());
    if (tenantPackage != null) {
      tenant.setPackageName(tenantPackage.getPackageName());
      tenant.setMenuCount(selectPackageMenuIds(tenantPackage.getPackageId()).size());
    }
    TenantBrandingResolver.TenantBranding branding = TenantBrandingResolver.resolveBranding(tenant, null);
    tenant.setSiteName(branding.getSiteName());
    tenant.setLoginTitle(branding.getLoginTitle());
    tenant.setSiteLogo(branding.getSiteLogo());
    tenant.setSiteIcon(branding.getSiteIcon());
    tenant.setLoginTemplate(branding.getLoginTemplate());
    tenant.setUserCount(countActiveUsers(tenant.getTenantId()));
    IoTUser adminUser = selectTenantAdmin(tenant.getTenantId());
    if (adminUser != null) {
      tenant.setAdminUnionId(adminUser.getUnionId());
      tenant.setAdminUsername(adminUser.getUsername());
      tenant.setAdminAlias(adminUser.getAlias());
      tenant.setAdminMobile(adminUser.getMobile());
    }
  }

  private String generateTenantId() {
    for (int i = 0; i < 20; i++) {
      String id = IdUtil.getSnowflakeNextIdStr();
      String tenantId = id.length() <= 12 ? id : id.substring(id.length() - 12);
      if (tenantMapper.selectByPrimaryKey(tenantId) == null) {
        return tenantId;
      }
    }
    throw new IoTException("生成租户标识失败，请稍后重试");
  }

  private void assertTenantCodeUnique(String tenantCode, String excludeTenantId) {
    Example example = new Example(IoTTenant.class);
    example.createCriteria().andEqualTo("tenantCode", tenantCode).andEqualTo("deleted", 0);
    List<IoTTenant> tenants = tenantMapper.selectByExample(example);
    if (!CollectionUtils.isEmpty(tenants)) {
      IoTTenant existed = tenants.get(0);
      if (excludeTenantId == null || !StrUtil.equals(existed.getTenantId(), excludeTenantId)) {
        throw new IoTException("租户编码已存在");
      }
    }
  }

  private void assertTenantNameUnique(String tenantName, String excludeTenantId) {
    Example example = new Example(IoTTenant.class);
    example.createCriteria().andEqualTo("tenantName", tenantName).andEqualTo("deleted", 0);
    List<IoTTenant> tenants = tenantMapper.selectByExample(example);
    if (!CollectionUtils.isEmpty(tenants)) {
      IoTTenant existed = tenants.get(0);
      if (excludeTenantId == null || !StrUtil.equals(existed.getTenantId(), excludeTenantId)) {
        throw new IoTException("租户名称已存在");
      }
    }
  }

  private void syncTenantRootDeptName(
      String tenantId, String tenantName, String previousTenantName) {
    if (StrUtil.isBlank(tenantId) || StrUtil.isBlank(tenantName)) {
      return;
    }
    Example example = new Example(SysDept.class);
    example.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("parentId", 0L);
    example.setOrderByClause("create_time asc");
    List<SysDept> rootDeptList = sysDeptMapper.selectByExample(example);
    if (CollectionUtils.isEmpty(rootDeptList)) {
      return;
    }
    SysDept rootDept = rootDeptList.get(0);
    String currentDeptName = StrUtil.trim(rootDept.getDeptName());
    String expectedDeptName = StrUtil.trim(tenantName);
    if (StrUtil.isBlank(expectedDeptName) || StrUtil.equals(currentDeptName, expectedDeptName)) {
      return;
    }
    if (!StrUtil.isBlank(currentDeptName)
        && !StrUtil.equalsAny(currentDeptName, ROOT_DEPT_NAME, LEGACY_ROOT_DEPT_NAME)
        && !StrUtil.equals(currentDeptName, StrUtil.trim(previousTenantName))) {
      return;
    }
    rootDept.setDeptName(expectedDeptName);
    rootDept.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    rootDept.setUpdateTime(new Date());
    sysDeptMapper.updateByPrimaryKeySelective(rootDept);
  }

  private IoTTenantPackage requireUsablePackage(Long packageId) {
    if (packageId == null) {
      throw new IoTException("租户套餐不能为空");
    }
    IoTTenantPackage tenantPackage = tenantPackageMapper.selectByPrimaryKey(packageId);
    if (tenantPackage == null || Integer.valueOf(1).equals(tenantPackage.getDeleted())) {
      throw new IoTException("租户套餐不存在");
    }
    if (Integer.valueOf(1).equals(tenantPackage.getStatus())) {
      throw new IoTException("租户套餐已停用");
    }
    return tenantPackage;
  }

  private List<Long> selectPackageMenuIds(Long packageId) {
    if (packageId == null) {
      return new ArrayList<>();
    }
    Example example = new Example(IoTTenantPackageMenu.class);
    example.createCriteria().andEqualTo("packageId", packageId);
    example.setOrderByClause("create_time asc");
    return tenantPackageMenuMapper.selectByExample(example).stream()
        .map(IoTTenantPackageMenu::getMenuId)
        .collect(Collectors.toList());
  }

  private IoTUserBO buildTenantAdminUser(IoTTenant tenant, SysDept rootDept, SysRole adminRole) {
    return IoTUserBO.builder()
        .id(IdUtil.getSnowflake().nextId())
        .tenantId(tenant.getTenantId())
        .tenantRole("ADMIN")
        .deptId(rootDept == null ? null : rootDept.getDeptId())
        .username(tenant.getAdminUsername())
        .unionId(tenant.getAdminUsername())
        .alias(tenant.getAdminAlias())
        .password(SecurityUtils.encryptPassword(tenant.getAdminPassword()))
        .mobile(tenant.getAdminMobile())
        .email(null)
        .identity(1)
        .parentUnionId("")
        .status(0)
        .deleted(0)
        .createBy(SecurityUtils.getUnionIdQuietly())
        .createDate(new Date())
        .roleIds(new Long[] {adminRole.getRoleId()})
        .remark("租户开通时自动初始化的租户管理员")
        .build();
  }

  private SysRole createTenantAdminRole(
      String tenantId, String tenantName, String adminUnionId, List<Long> menuIds) {
    SysRole role =
        SysRole.builder()
            .roleName(buildTenantAdminRoleName(tenantName, tenantId))
            .roleKey(TENANT_ADMIN_ROLE_KEY)
            .tenantId(tenantId)
            .roleType(ROLE_TYPE_TENANT_ADMIN)
            .roleSort(0)
            .dataScope("1")
            .menuCheckStrictly(true)
            .deptCheckStrictly(true)
            .status("0")
            .createBy(adminUnionId)
            .createTime(new Date())
            .remark("租户开通时自动初始化的管理员角色")
            .build();
    sysRoleMapper.insertUseGeneratedKeys(role);
    saveRoleMenus(role.getRoleId(), menuIds);
    return role;
  }

  private void syncTenantRoleMenus(String tenantId, List<Long> menuIds) {
    if (StrUtil.isBlank(tenantId)) {
      return;
    }
    List<Long> normalizedMenuIds = normalizeMenuIds(menuIds);
    if (CollectionUtils.isEmpty(normalizedMenuIds)) {
      return;
    }
    IoTUser adminUser = selectTenantAdmin(tenantId);
    if (adminUser == null) {
      return;
    }
    IoTTenant tenant = tenantMapper.selectByPrimaryKey(tenantId);
    String tenantName = tenant == null ? tenantId : tenant.getTenantName();
    Long adminRoleId = ensureTenantAdminRole(tenantId, tenantName, adminUser.getUnionId(), normalizedMenuIds);
    ensureUserRoleBinding(adminUser.getUnionId(), adminRoleId);

    Example roleExample = new Example(SysRole.class);
    roleExample.createCriteria().andEqualTo("tenantId", tenantId);
    roleExample.or(roleExample.createCriteria().andIsNull("tenantId").andEqualTo("createBy", adminUser.getUnionId()));
    roleExample.or(roleExample.createCriteria().andEqualTo("tenantId", "").andEqualTo("createBy", adminUser.getUnionId()));
    List<SysRole> roles = sysRoleMapper.selectByExample(roleExample);
    if (CollectionUtils.isEmpty(roles)) {
      return;
    }

    Set<Long> packageMenuIdSet = new LinkedHashSet<>(normalizedMenuIds);
    for (SysRole role : roles) {
      if (role == null || role.getRoleId() == null) {
        continue;
      }
      if (role.getRoleId().equals(adminRoleId)) {
        continue;
      }
      if (StrUtil.equals(role.getRoleKey(), TENANT_ADMIN_ROLE_KEY)) {
        replaceRoleMenus(role.getRoleId(), normalizedMenuIds);
        continue;
      }
      List<Long> retainedMenuIds =
          selectRoleMenuIds(role.getRoleId()).stream()
              .filter(packageMenuIdSet::contains)
              .collect(Collectors.toList());
      replaceRoleMenus(role.getRoleId(), retainedMenuIds);
    }
  }

  private void saveRoleMenus(Long roleId, List<Long> menuIds) {
    if (roleId == null || CollectionUtils.isEmpty(menuIds)) {
      return;
    }
    Set<Long> uniqueMenuIds = new LinkedHashSet<>(menuIds);
    List<SysRoleMenu> roleMenus = new ArrayList<>(uniqueMenuIds.size());
    for (Long menuId : uniqueMenuIds) {
      if (menuId == null) {
        continue;
      }
      roleMenus.add(
          SysRoleMenu.builder()
              .uuid(SQenGenId.nextId())
              .roleId(roleId)
              .menuId(menuId)
              .build());
    }
    if (!roleMenus.isEmpty()) {
      sysRoleMenuMapper.batchInsert(roleMenus);
    }
  }

  private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
    if (roleId == null) {
      return;
    }
    Example roleMenuExample = new Example(SysRoleMenu.class);
    roleMenuExample.createCriteria().andEqualTo("roleId", roleId);
    sysRoleMenuMapper.deleteByExample(roleMenuExample);
    saveRoleMenus(roleId, menuIds);
  }

  private List<Long> selectRoleMenuIds(Long roleId) {
    if (roleId == null) {
      return new ArrayList<>();
    }
    Example example = new Example(SysRoleMenu.class);
    example.createCriteria().andEqualTo("roleId", roleId);
    example.setOrderByClause("uuid asc");
    return sysRoleMenuMapper.selectByExample(example).stream()
        .map(SysRoleMenu::getMenuId)
        .collect(Collectors.toList());
  }

  private List<Long> normalizeMenuIds(List<Long> menuIds) {
    if (CollectionUtils.isEmpty(menuIds)) {
      return new ArrayList<>();
    }
    return menuIds.stream()
        .filter(menuId -> menuId != null)
        .distinct()
        .collect(Collectors.toList());
  }

  private Long ensureTenantAdminRole(
      String tenantId, String tenantName, String adminUnionId, List<Long> menuIds) {
    if (StrUtil.isBlank(adminUnionId)) {
      throw new IoTException("租户管理员账号不能为空");
    }
    if (StrUtil.isBlank(tenantId)) {
      throw new IoTException("租户不存在");
    }
    if (CollectionUtils.isEmpty(menuIds)) {
      throw new IoTException("租户管理员角色缺少菜单权限");
    }
    SysRole role = selectTenantAdminRole(tenantId, adminUnionId);
    if (role == null) {
      role = createTenantAdminRole(tenantId, tenantName, adminUnionId, menuIds);
      return role.getRoleId();
    }
    Example roleMenuExample = new Example(SysRoleMenu.class);
    roleMenuExample.createCriteria().andEqualTo("roleId", role.getRoleId());
    sysRoleMenuMapper.deleteByExample(roleMenuExample);
    role.setRoleName(buildTenantAdminRoleName(tenantName, tenantId));
    role.setTenantId(tenantId);
    role.setRoleType(ROLE_TYPE_TENANT_ADMIN);
    role.setDataScope("1");
    role.setMenuCheckStrictly(true);
    role.setDeptCheckStrictly(true);
    role.setStatus("0");
    role.setUpdateBy(SecurityUtils.getUnionIdQuietly());
    role.setUpdateTime(new Date());
    sysRoleMapper.updateByPrimaryKeySelective(role);
    saveRoleMenus(role.getRoleId(), menuIds);
    return role.getRoleId();
  }

  private String buildTenantAdminRoleName(String tenantName, String tenantId) {
    String prefix = StrUtil.blankToDefault(tenantName, tenantId);
    if (StrUtil.isBlank(prefix)) {
      return "租户管理员";
    }
    String roleName = prefix + TENANT_ADMIN_ROLE_NAME_SUFFIX;
    if (roleName.length() <= ROLE_NAME_MAX_LENGTH) {
      return roleName;
    }
    int prefixMaxLength = ROLE_NAME_MAX_LENGTH - TENANT_ADMIN_ROLE_NAME_SUFFIX.length();
    if (prefixMaxLength <= 0) {
      return "租户管理员";
    }
    return prefix.substring(0, Math.min(prefix.length(), prefixMaxLength)) + TENANT_ADMIN_ROLE_NAME_SUFFIX;
  }

  private SysRole selectTenantAdminRole(String tenantId, String adminUnionId) {
    if (StrUtil.isBlank(tenantId)) {
      return null;
    }
    Example example = new Example(SysRole.class);
    example.createCriteria().andEqualTo("tenantId", tenantId).andEqualTo("roleKey", TENANT_ADMIN_ROLE_KEY);
    if (StrUtil.isNotBlank(adminUnionId)) {
      example.or(
          example
              .createCriteria()
              .andIsNull("tenantId")
              .andEqualTo("createBy", adminUnionId)
              .andEqualTo("roleKey", TENANT_ADMIN_ROLE_KEY));
      example.or(
          example
              .createCriteria()
              .andEqualTo("tenantId", "")
              .andEqualTo("createBy", adminUnionId)
              .andEqualTo("roleKey", TENANT_ADMIN_ROLE_KEY));
    }
    example.setOrderByClause("create_time asc");
    List<SysRole> roles = sysRoleMapper.selectByExample(example);
    return CollectionUtils.isEmpty(roles) ? null : roles.get(0);
  }

  private void ensureUserRoleBinding(String unionId, Long roleId) {
    if (StrUtil.isBlank(unionId) || roleId == null) {
      return;
    }
    Example example = new Example(SysUserRole.class);
    example.createCriteria().andEqualTo("unionId", unionId).andEqualTo("roleId", roleId);
    if (sysUserRoleMapper.selectCountByExample(example) > 0) {
      return;
    }
    sysUserRoleMapper.insertSelective(SysUserRole.builder().unionId(unionId).roleId(roleId).build());
  }

  private IoTUser selectTenantAdmin(String tenantId) {
    Example example = new Example(IoTUser.class);
    example
        .createCriteria()
        .andEqualTo("tenantId", tenantId)
        .andEqualTo("tenantRole", "ADMIN")
        .andEqualTo("deleted", 0);
    example.setOrderByClause("create_date asc");
    List<IoTUser> users = userMapper.selectByExample(example);
    return CollectionUtils.isEmpty(users) ? null : users.get(0);
  }

  private void syncTenantAdminInfo(IoTTenant tenant) {
    if (tenant == null) {
      return;
    }
    IoTUser adminUser = selectTenantAdmin(tenant.getTenantId());
    if (adminUser == null) {
      return;
    }
    boolean needUpdate = false;
    if (StrUtil.isNotBlank(tenant.getAdminUsername())
        && !StrUtil.equals(tenant.getAdminUsername(), adminUser.getUsername())) {
      throw new IoTException("暂不支持在租户管理中修改管理员用户名");
    }
    if (StrUtil.isNotBlank(tenant.getAdminAlias())
        && !StrUtil.equals(tenant.getAdminAlias(), adminUser.getAlias())) {
      adminUser.setAlias(tenant.getAdminAlias());
      needUpdate = true;
    }
    if (StrUtil.isNotBlank(tenant.getAdminMobile())
        && !StrUtil.equals(tenant.getAdminMobile(), adminUser.getMobile())) {
      if (IoTUserConstants.NOT_UNIQUE.equals(
          ioTUserService.checkPhoneUnique(
              IoTUser.builder().id(adminUser.getId()).mobile(tenant.getAdminMobile()).build()))) {
        throw new IoTException("租户管理员手机号已存在");
      }
      adminUser.setMobile(tenant.getAdminMobile());
      needUpdate = true;
    }
    if (StrUtil.isNotBlank(tenant.getAdminPassword())) {
      adminUser.setPassword(SecurityUtils.encryptPassword(tenant.getAdminPassword()));
      needUpdate = true;
    }
    if (needUpdate) {
      adminUser.setUpdateBy(SecurityUtils.getUnionIdQuietly());
      adminUser.setUpdateDate(new Date());
      userMapper.updateByPrimaryKeySelective(adminUser);
    }
  }

  private void cleanupTenantAdmin(IoTUser adminUser) {
    if (adminUser == null) {
      return;
    }
    Example userRoleExample = new Example(SysUserRole.class);
    userRoleExample.createCriteria().andEqualTo("unionId", adminUser.getUnionId());
    sysUserRoleMapper.deleteByExample(userRoleExample);

    Example roleExample = new Example(SysRole.class);
    roleExample.createCriteria().andEqualTo("createBy", adminUser.getUnionId());
    List<SysRole> roles = sysRoleMapper.selectByExample(roleExample);
    for (SysRole role : roles) {
      if (role == null || role.getRoleId() == null) {
        continue;
      }
      Example boundUserRoleExample = new Example(SysUserRole.class);
      boundUserRoleExample.createCriteria().andEqualTo("roleId", role.getRoleId());
      sysUserRoleMapper.deleteByExample(boundUserRoleExample);
      Example roleMenuExample = new Example(SysRoleMenu.class);
      roleMenuExample.createCriteria().andEqualTo("roleId", role.getRoleId());
      sysRoleMenuMapper.deleteByExample(roleMenuExample);
      Example roleDeptExample = new Example(SysRoleDept.class);
      roleDeptExample.createCriteria().andEqualTo("roleId", role.getRoleId());
      sysRoleDeptMapper.deleteByExample(roleDeptExample);
      sysRoleMapper.deleteByPrimaryKey(role.getRoleId());
    }
    userMapper.deleteByPrimaryKey(adminUser.getId());
  }

  private long countTenantBusinessResources(String tenantId) {
    long total = 0L;
    total += productMapper.selectCount(IoTProduct.builder().tenantId(tenantId).build());
    total += deviceMapper.selectCount(IoTDevice.builder().tenantId(tenantId).build());
    total += userApplicationMapper.selectCount(IoTUserApplication.builder().tenantId(tenantId).build());
    total += networkMapper.selectCount(Network.builder().tenantId(tenantId).build());
    total += ruleModelMapper.selectCount(RuleModel.builder().tenantId(tenantId).build());
    total += rulegoChainMapper.selectCount(RulegoChain.builder().tenantId(tenantId).build());
    return total;
  }
}
