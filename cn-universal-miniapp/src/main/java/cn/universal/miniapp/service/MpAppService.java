/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.miniapp.config.MpBusinessException;
import cn.universal.miniapp.dto.MpAppCreateDTO;
import cn.universal.miniapp.entity.MpApp;
import cn.universal.miniapp.mapper.MpAppMapper;
import cn.universal.miniapp.mapper.MpUserMapper;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.page.PageContextRunner;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.DeptDataPermissionSupport;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 小程序应用服务
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Service
public class MpAppService {

  @Autowired private MpAppMapper mpAppMapper;

  @Autowired private MpUserMapper mpUserMapper;

  @Autowired private MpDeviceService mpDeviceService;

  @Autowired private IoTUserApplicationMapper ioTUserApplicationMapper;

  @Autowired private IoTUserService ioTUserService;

  @Autowired private DeptDataPermissionSupport deptDataPermissionSupport;

  public MpApp getMpApp(String appid) {
    // 检查appId是否已存在
    MpApp mpApp = mpAppMapper.selectByAppId(appid);
    return mpApp;
  }

  /** 创建小程序应用 */
  @Transactional(rollbackFor = Exception.class)
  public MpApp createApp(MpAppCreateDTO dto, String creatorId) {
    IoTUser currentUser = requireCurrentUser();
    // 检查appId是否已存在
    MpApp existing = mpAppMapper.selectByAppId(dto.getAppId());
    if (existing != null) {
      throw new MpBusinessException("该AppId已存在");
    }

    // 查询关联应用获取AK/SK
    IoTUserApplication iotApp = null;
    if (StrUtil.isNotBlank(dto.getLinkedAppId())) {
      iotApp = getAccessibleLinkedApp(dto.getLinkedAppId(), currentUser);
    }

    MpApp app =
        MpApp.builder()
            .appId(dto.getAppId())
            .appSecret(iotApp != null && iotApp.getAppSecret() != null ? iotApp.getAppSecret() : "")
            .tenantId(iotApp != null ? resolveIotAppTenantId(iotApp) : resolveTenantId(currentUser))
            .name(dto.getName())
            .logo(dto.getLogo())
            .description(dto.getDescription())
            .dataSourceType(dto.getDataSourceType())
            .linkedAppId(dto.getLinkedAppId())
            .ak(iotApp != null ? iotApp.getAppId() : dto.getAk())
            .sk(iotApp != null ? iotApp.getAppSecret() : dto.getSk())
            .primaryColor(dto.getPrimaryColor())
            .qrcodeImage(dto.getQrcodeImage())
            .accessUrl(dto.getAccessUrl())
            .welcomeText(dto.getWelcomeText())
            .contactPhone(dto.getContactPhone())
            .contactEmail(dto.getContactEmail())
            .status(1)
            .userCount(0)
            .deviceCount(0)
            .onlineDeviceCount(0)
            .creatorId(currentUser.getUnionId())
            .createTime(new Date())
            .deleted(0)
            .build();

    mpAppMapper.insertSelective(app);
    log.info(
        "创建小程序应用: appId={}, name={}, linkedAppId={}",
        dto.getAppId(),
        dto.getName(),
        dto.getLinkedAppId());
    return app;
  }

  /** 更新小程序应用 */
  @Transactional(rollbackFor = Exception.class)
  public void updateApp(MpApp app) {
    MpApp stored = getAccessibleAdminAppByAppId(app.getAppId());
    app.setId(stored.getId());
    app.setCreatorId(stored.getCreatorId());
    app.setTenantId(stored.getTenantId());
    app.setCreateTime(stored.getCreateTime());
    app.setDeleted(stored.getDeleted());
    app.setUpdateTime(new Date());
    // 查询关联应用的最新AK/SK
    if (StrUtil.isNotBlank(app.getLinkedAppId())) {
      IoTUserApplication iotApp = getAccessibleLinkedApp(app.getLinkedAppId(), requireCurrentUser());
      app.setSk(iotApp.getAppSecret());
      app.setAk(iotApp.getAppId());
      app.setAppSecret(StrUtil.blankToDefault(iotApp.getAppSecret(), app.getAppSecret()));
    } else {
      app.setLinkedAppId(null);
    }
    mpAppMapper.updateByPrimaryKeySelective(app);
    log.info("更新小程序应用: appId={}", app.getAppId());
  }

  /** 关联应用的密钥改变时，同步更新小程序的AK/SK */
  @Transactional(rollbackFor = Exception.class)
  public void syncAppSecretFromLinkedApp(String linkedAppId) {
    // 查询关联应用的最新AK/SK
    IoTUserApplication iotApp = ioTUserApplicationMapper.selectIotUserApplicationById(linkedAppId);
    if (iotApp == null) {
      log.warn("关联应用不存在, linkedAppId={}", linkedAppId);
      return;
    }

    // 查询所有关联此应用的小程序应用
    List<MpApp> mpApps = mpAppMapper.selectByLinkedAppId(linkedAppId);
    for (MpApp mpApp : mpApps) {
      syncSingleMpAppSecret(mpApp, iotApp);
    }
  }

  /** 查询应用列表 */
  public PageInfo<MpApp> queryAppList(String name, Integer status, int pageNum, int pageSize) {
    IoTUser currentUser = requireCurrentUser();
    MpApp condition = MpApp.builder().name(name).status(status).build();
    applyAdminScope(condition, currentUser);
    PageHelper.startPage(pageNum, pageSize);
    List<MpApp> list = mpAppMapper.selectAppList(condition);

    return new PageInfo<>(list);
  }

  /** 查询当前后台用户可访问的小程序 appId 集合；平台管理员返回 null 表示不限制 */
  public List<String> listAccessibleAdminAppIds() {
    IoTUser currentUser = requireCurrentUser();
    if (currentUser.isAdmin()) {
      return null;
    }
    MpApp condition = MpApp.builder().build();
    applyAdminScope(condition, currentUser);
    List<MpApp> scopedApps =
        PageContextRunner.runWithoutPaging(() -> mpAppMapper.selectAppList(condition));
    if (CollectionUtil.isEmpty(scopedApps)) {
      return new ArrayList<>();
    }
    return scopedApps.stream()
        .map(MpApp::getAppId)
        .filter(StrUtil::isNotBlank)
        .distinct()
        .collect(Collectors.toList());
  }

  /** 获取应用详情 */
  public MpApp getAppByAppId(String appId) {
    return mpAppMapper.selectByAppId(appId);
  }

  /** 获取后台可访问的应用详情 */
  public MpApp getAccessibleAdminAppByAppId(String appId) {
    MpApp app = getAppByAppId(appId);
    assertAppAccess(app);
    if (app != null && StrUtil.isNotBlank(app.getLinkedAppId())) {
      IoTUserApplication linkedApp =
          PageContextRunner.runWithoutPaging(
              () -> ioTUserApplicationMapper.selectIotUserApplicationById(app.getLinkedAppId()));
      if (linkedApp != null) {
        app.setAppName(linkedApp.getAppName());
      }
    }
    return app;
  }

  /** 刷新应用统计数据 */
  public void refreshAppStatistics(String appId) {
    getAccessibleAdminAppByAppId(appId);
    int userCount = mpUserMapper.countByAppId(appId);
    // 通过 linked_app_id 关联查询设备数量
    int deviceCount = mpDeviceService.getDeviceCount(appId);
    int onlineDeviceCount = mpDeviceService.getDeviceOnlineCount(appId);
    mpAppMapper.updateAppStatistics(appId, userCount, deviceCount, onlineDeviceCount);
    log.debug("刷新应用统计: appId={}, users={}, devices={}", appId, userCount, deviceCount);
  }

  /** 禁用应用 */
  @Transactional(rollbackFor = Exception.class)
  public void disableApp(String appId) {
    MpApp app = getAccessibleAdminAppByAppId(appId);
    app.setStatus(0);
    app.setUpdateTime(new Date());
    mpAppMapper.updateByPrimaryKeySelective(app);
    log.info("禁用小程序应用: appId={}", appId);
  }

  /** 启用应用 */
  @Transactional(rollbackFor = Exception.class)
  public void enableApp(String appId) {
    MpApp app = getAccessibleAdminAppByAppId(appId);
    app.setStatus(1);
    app.setUpdateTime(new Date());
    mpAppMapper.updateByPrimaryKeySelective(app);
    log.info("启用小程序应用: appId={}", appId);
  }

  /** 删除应用（物理删除） */
  @Transactional(rollbackFor = Exception.class)
  public void deleteApp(String appId) {
    MpApp app = getAccessibleAdminAppByAppId(appId);
    mpAppMapper.deleteByPrimaryKey(app.getId());
    log.info("删除小程序应用: appId={}", appId);
  }

  /** 同步所有小程序应用的密钥 当IoT应用密钥变更时，将所有关联的小程序应用密钥一齐同步 */
  @Transactional(rollbackFor = Exception.class)
  public void syncAllAppSecrets() {
    // 查询所有未删除的小程序应用
    MpApp condition = MpApp.builder().deleted(0).build();
    List<MpApp> allMpApps = mpAppMapper.selectAppList(condition);

    if (allMpApps == null) {
      allMpApps = new ArrayList<>();
    }

    for (MpApp mpApp : allMpApps) {
      if (mpApp.getLinkedAppId() != null) {
        try {
          IoTUserApplication linkedApp =
              PageContextRunner.runWithoutPaging(
                  () -> ioTUserApplicationMapper.selectIotUserApplicationById(mpApp.getLinkedAppId()));
          if (linkedApp != null) {
            syncSingleMpAppSecret(mpApp, linkedApp);
          }
        } catch (Exception e) {
          log.warn(
              "同步小程序应用密钥失败, mpAppId={}, linkedAppId={}",
              mpApp.getAppId(),
              mpApp.getLinkedAppId(),
              e);
        }
      }
    }
    log.info("同步成功: 共处理{}个小程序应用", allMpApps.size());
  }

  /** 后台同步可访问关联应用的密钥 */
  @Transactional(rollbackFor = Exception.class)
  public void syncAccessibleAppSecretFromLinkedApp(String linkedAppId) {
    IoTUser currentUser = requireCurrentUser();
    if (!currentUser.isAdmin() && !isTenantAdmin(currentUser)) {
      throw new MpBusinessException("无权批量同步应用密钥");
    }
    getAccessibleLinkedApp(linkedAppId, currentUser);
    syncAppSecretFromLinkedApp(linkedAppId);
  }

  /** 后台同步当前可管理范围内的所有应用密钥 */
  @Transactional(rollbackFor = Exception.class)
  public void syncAllAdminAppSecrets() {
    IoTUser currentUser = requireCurrentUser();
    if (currentUser.isAdmin()) {
      syncAllAppSecrets();
      return;
    }
    if (!isTenantAdmin(currentUser)) {
      throw new MpBusinessException("无权批量同步所有应用密钥");
    }
    MpApp condition = MpApp.builder().build();
    applyAdminScope(condition, currentUser);
    List<MpApp> scopedApps = mpAppMapper.selectAppList(condition);
    for (MpApp mpApp : scopedApps) {
      if (StrUtil.isBlank(mpApp.getLinkedAppId())) {
        continue;
      }
      IoTUserApplication linkedApp =
          PageContextRunner.runWithoutPaging(
              () -> ioTUserApplicationMapper.selectIotUserApplicationById(mpApp.getLinkedAppId()));
      if (linkedApp == null) {
        continue;
      }
      syncSingleMpAppSecret(mpApp, linkedApp);
    }
  }

  private void applyAdminScope(MpApp query, IoTUser currentUser) {
    if (query == null || currentUser == null || currentUser.isAdmin()) {
      return;
    }
    query.setTenantId(resolveTenantId(currentUser));
    if (isTenantAdmin(currentUser)) {
      query.setScopeUnionIds(loadTenantUnionIds(currentUser));
      query.setScopeDenied(CollectionUtil.isEmpty(query.getScopeUnionIds()));
      return;
    }
    DeptDataPermissionScope scope =
        PageContextRunner.runWithoutPaging(
            () -> deptDataPermissionSupport.resolveScope(currentUser.getUnionId()));
    if (scope == null || scope.isDenied()) {
      query.setScopeDenied(true);
      return;
    }
    query.setScopeDeptIds(new ArrayList<>(scope.getScopeDeptIds()));
    query.setScopeUnionIds(new ArrayList<>(scope.getScopeUnionIds()));
    query.setScopeUnionId(scope.getCurrentUnionId());
  }

  private void assertAppAccess(MpApp app) {
    if (app == null) {
      throw new MpBusinessException("应用不存在");
    }
    IoTUser currentUser = requireCurrentUser();
    if (currentUser.isAdmin()) {
      return;
    }
    String resourceTenantId = resolveMpAppTenantId(app);
    String resourceUnionId = resolveMpAppOwnerUnionId(app);
    if (isTenantAdmin(currentUser)) {
      if (!StrUtil.equals(resolveTenantId(currentUser), resourceTenantId)) {
        throw new MpBusinessException("无权访问该小程序应用");
      }
      return;
    }
    DeptDataPermissionScope scope =
        PageContextRunner.runWithoutPaging(
            () -> deptDataPermissionSupport.resolveScope(currentUser.getUnionId()));
    Long creatorDeptId = resolveUserDeptId(resourceUnionId);
    if (!deptDataPermissionSupport.canAccessResource(
        scope, resourceTenantId, creatorDeptId, resourceUnionId)) {
      throw new MpBusinessException("无权访问该小程序应用");
    }
  }

  private IoTUserApplication getAccessibleLinkedApp(String linkedAppId, IoTUser currentUser) {
    IoTUserApplication linkedApp =
        PageContextRunner.runWithoutPaging(
            () -> ioTUserApplicationMapper.selectIotUserApplicationById(linkedAppId));
    if (linkedApp == null) {
      throw new MpBusinessException("关联应用不存在");
    }
    if (currentUser == null || currentUser.isAdmin()) {
      return linkedApp;
    }
    String resourceTenantId = resolveIotAppTenantId(linkedApp);
    if (isTenantAdmin(currentUser)) {
      if (!StrUtil.equals(resolveTenantId(currentUser), resourceTenantId)) {
        throw new MpBusinessException("无权访问关联应用");
      }
      return linkedApp;
    }
    DeptDataPermissionScope scope =
        PageContextRunner.runWithoutPaging(
            () -> deptDataPermissionSupport.resolveScope(currentUser.getUnionId()));
    if (!deptDataPermissionSupport.canAccessResource(
        scope, resourceTenantId, null, linkedApp.getUnionId())) {
      throw new MpBusinessException("无权访问关联应用");
    }
    return linkedApp;
  }

  private IoTUser requireCurrentUser() {
    String unionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(unionId)) {
      throw new MpBusinessException("当前登录状态无效");
    }
    IoTUser currentUser =
        PageContextRunner.runWithoutPaging(() -> ioTUserService.selectUserByUnionId(unionId));
    if (currentUser == null) {
      throw new MpBusinessException("当前用户不存在或已失效");
    }
    return currentUser;
  }

  private boolean isTenantAdmin(IoTUser user) {
    return user != null
        && !user.isAdmin()
        && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN");
  }

  private String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    if (StrUtil.isNotBlank(user.getTenantId())) {
      return user.getTenantId();
    }
    if (StrUtil.isNotBlank(user.getParentUnionId())) {
      IoTUser parent =
          PageContextRunner.runWithoutPaging(
              () -> ioTUserService.selectUserByUnionId(user.getParentUnionId()));
      if (parent != null) {
        return StrUtil.blankToDefault(parent.getTenantId(), parent.getUnionId());
      }
    }
    return user.getUnionId();
  }

  private String resolveMpAppTenantId(MpApp app) {
    if (app != null && StrUtil.isNotBlank(app.getTenantId())) {
      return app.getTenantId();
    }
    IoTUserApplication linkedApp =
        app == null || StrUtil.isBlank(app.getLinkedAppId())
            ? null
            : PageContextRunner.runWithoutPaging(
                () -> ioTUserApplicationMapper.selectIotUserApplicationById(app.getLinkedAppId()));
    String linkedTenantId = resolveIotAppTenantId(linkedApp);
    if (StrUtil.isNotBlank(linkedTenantId)) {
      return linkedTenantId;
    }
    IoTUser creator = queryUser(app == null ? null : app.getCreatorId());
    if (creator != null) {
      return resolveTenantId(creator);
    }
    if (linkedApp != null && StrUtil.isNotBlank(linkedApp.getUnionId())) {
      return linkedApp.getUnionId();
    }
    return app == null ? null : app.getCreatorId();
  }

  private String resolveMpAppOwnerUnionId(MpApp app) {
    if (app != null && StrUtil.isNotBlank(app.getLinkedAppId())) {
      IoTUserApplication linkedApp =
          PageContextRunner.runWithoutPaging(
              () -> ioTUserApplicationMapper.selectIotUserApplicationById(app.getLinkedAppId()));
      if (linkedApp != null && StrUtil.isNotBlank(linkedApp.getUnionId())) {
        return linkedApp.getUnionId();
      }
    }
    if (app != null && StrUtil.isNotBlank(app.getCreatorId())) {
      return app.getCreatorId();
    }
    return null;
  }

  private String resolveIotAppTenantId(IoTUserApplication linkedApp) {
    if (linkedApp == null) {
      return null;
    }
    if (StrUtil.isNotBlank(linkedApp.getTenantId())) {
      return linkedApp.getTenantId();
    }
    IoTUser creator = queryUser(linkedApp.getUnionId());
    if (creator != null) {
      return resolveTenantId(creator);
    }
    return linkedApp.getUnionId();
  }

  private Long resolveUserDeptId(String unionId) {
    IoTUser user = queryUser(unionId);
    return user == null ? null : user.getDeptId();
  }

  private IoTUser queryUser(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    return PageContextRunner.runWithoutPaging(() -> ioTUserService.selectUserByUnionId(unionId));
  }

  private List<String> loadTenantUnionIds(IoTUser currentUser) {
    String tenantId = resolveTenantId(currentUser);
    Set<String> unionIds = new LinkedHashSet<>();
    if (StrUtil.isNotBlank(tenantId)) {
      List<IoTUser> tenantUsers =
          PageContextRunner.runWithoutPaging(
              () -> ioTUserService.selectUserList(IoTUser.builder().tenantId(tenantId).build()));
      if (CollectionUtil.isNotEmpty(tenantUsers)) {
        tenantUsers.stream()
            .map(IoTUser::getUnionId)
            .filter(StrUtil::isNotBlank)
            .forEach(unionIds::add);
      }
    }
    if (currentUser != null && StrUtil.isNotBlank(currentUser.getUnionId())) {
      unionIds.add(currentUser.getUnionId());
    }
    if (currentUser != null && StrUtil.isNotBlank(currentUser.getParentUnionId())) {
      unionIds.add(currentUser.getParentUnionId());
    }
    if (currentUser != null && StrUtil.isNotBlank(currentUser.getUnionId())) {
      List<IoTUser> legacyChildren =
          PageContextRunner.runWithoutPaging(
              () ->
                  ioTUserService.selectUserList(
                      IoTUser.builder().parentUnionId(currentUser.getUnionId()).build()));
      if (CollectionUtil.isNotEmpty(legacyChildren)) {
        legacyChildren.stream()
            .map(IoTUser::getUnionId)
            .filter(StrUtil::isNotBlank)
            .forEach(unionIds::add);
      }
    }
    return unionIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  private void syncSingleMpAppSecret(MpApp mpApp, IoTUserApplication linkedApp) {
    if (mpApp == null || linkedApp == null) {
      return;
    }
    mpApp.setAk(linkedApp.getAppId());
    mpApp.setSk(linkedApp.getAppSecret());
    mpApp.setAppSecret(linkedApp.getAppSecret() != null ? linkedApp.getAppSecret() : "");
    mpApp.setUpdateTime(new Date());
    mpAppMapper.updateByPrimaryKeySelective(mpApp);
    log.info("同步小程序应用密钥: mpAppId={}, linkedAppId={}", mpApp.getAppId(), mpApp.getLinkedAppId());
  }
}
