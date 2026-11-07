/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.miniapp.config.MpBusinessException;
import cn.universal.miniapp.dto.MpUserCreateDTO;
import cn.universal.miniapp.dto.MpUserQueryDTO;
import cn.universal.miniapp.entity.MpUser;
import cn.universal.miniapp.entity.MpUserPermission;
import cn.universal.miniapp.entity.MpUserDevice;
import cn.universal.miniapp.mapper.MpUserMapper;
import cn.universal.miniapp.mapper.MpUserPermissionMapper;
import cn.universal.miniapp.mapper.MpUserDeviceMapper;
import cn.universal.miniapp.vo.MpUserInfoVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 小程序用户服务
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Service
public class MpUserService {

  private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

  @Autowired private MpUserMapper mpUserMapper;

  @Autowired private MpUserPermissionMapper mpUserPermissionMapper;

  @Autowired private MpUserDeviceMapper mpUserDeviceMapper;

  @Autowired private MpAppService mpAppService;

  /** 账号密码登录 */
  public MpUserInfoVO loginByPassword(String appId, String username, String password) {
    MpUser user = mpUserMapper.selectByAppIdAndUsername(appId, username);
    if (user == null) {
      throw new MpBusinessException("用户名或密码错误");
    }

    if (user.getStatus() == MpUser.Status.DISABLED) {
      throw new MpBusinessException("账号已被禁用");
    }

    if (StrUtil.isBlank(user.getPassword())) {
      throw new MpBusinessException("该账号未设置密码");
    }

    if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
      throw new MpBusinessException("用户名或密码错误");
    }

    // 获取用户权限
    List<String> permissions = getUserPermissions(user.getId(), appId);

    return MpUserInfoVO.builder()
        .userId(user.getId())
        .appId(appId)
        .nickname(user.getNickname())
        .avatar(user.getAvatar())
        .phone(user.getPhone())
        .permissions(permissions)
        .build();
  }

  /** 创建用户（后台管理） */
  @Transactional(rollbackFor = Exception.class)
  public MpUser createUser(MpUserCreateDTO dto, String creatorId) {
    mpAppService.getAccessibleAdminAppByAppId(dto.getAppId());
    // 检查用户名是否已存在
    if (StrUtil.isNotBlank(dto.getUsername())) {
      MpUser existingUser =
          mpUserMapper.selectByAppIdAndUsername(dto.getAppId(), dto.getUsername());
      if (existingUser != null) {
        throw new MpBusinessException("该用户名已被使用");
      }
    }

    // 检查手机号是否已存在
    if (StrUtil.isNotBlank(dto.getPhone())) {
      MpUser existing = mpUserMapper.selectByAppIdAndPhone(dto.getAppId(), dto.getPhone());
      if (existing != null) {
        throw new MpBusinessException("该手机号已被使用");
      }
    }

    // 创建用户
    MpUser user =
        MpUser.builder()
            .appId(dto.getAppId())
            .username(dto.getUsername())
            .password(
                StrUtil.isNotBlank(dto.getPassword())
                    ? PASSWORD_ENCODER.encode(dto.getPassword())
                    : null)
            .phone(dto.getPhone())
            .nickname(dto.getNickname())
            .realName(dto.getRealName())
            .remark(dto.getRemark())
            .status(MpUser.Status.BINDOUND) // 账号密码用户默认已绑定
            .creatorId(creatorId)
            .createTime(new Date())
            .deleted(0)
            .build();

    mpUserMapper.insertSelective(user);
    log.info(
        "创建小程序用户: appId={}, phone={}, userId={}", dto.getAppId(), dto.getPhone(), user.getId());

    // 设置权限
    if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
      updateUserPermissions(user.getId(), dto.getAppId(), dto.getPermissions());
    }

    return user;
  }

  /** 更新用户权限 */
  @Transactional(rollbackFor = Exception.class)
  public void updateUserPermissions(Long userId, String appId, List<String> permissions) {
    MpUser user = requireAccessibleAdminUser(userId, appId);
    // 删除旧权限
    mpUserPermissionMapper.deleteByUserAndApp(userId, user.getAppId());

    // 插入新权限
    if (permissions != null && !permissions.isEmpty()) {
      List<MpUserPermission> permissionList = new ArrayList<>();
      for (String permCode : permissions) {
        permissionList.add(
            MpUserPermission.builder()
                .userId(userId)
                .appId(user.getAppId())
                .permissionCode(permCode)
                .createTime(new Date())
                .build());
      }
      mpUserPermissionMapper.batchInsert(permissionList);
    }
    log.info(
        "更新用户权限: userId={}, appId={}, permissions={}",
        userId,
        user.getAppId(),
        permissions);
  }

  /** 查询用户列表 */
  public PageInfo<MpUser> queryUserList(MpUserQueryDTO query) {
    if (query == null) {
      query = new MpUserQueryDTO();
    }
    MpUser condition =
        MpUser.builder()
            .appId(query.getAppId())
            .phone(query.getPhone())
            .nickname(query.getNickname())
            .status(query.getStatus())
            .build();
    if (StrUtil.isNotBlank(query.getAppId())) {
      mpAppService.getAccessibleAdminAppByAppId(query.getAppId());
    } else {
      List<String> accessibleAppIds = mpAppService.listAccessibleAdminAppIds();
      if (accessibleAppIds != null) {
        if (CollectionUtil.isEmpty(accessibleAppIds)) {
          condition.setScopeDenied(true);
        } else {
          condition.setScopeAppIds(accessibleAppIds);
        }
      }
    }
    PageHelper.startPage(query.getPageNum(), query.getPageSize());
    List<MpUser> list = mpUserMapper.selectUserList(condition);
    return new PageInfo<>(list);
  }

  /** 获取用户详情 */
  public MpUser getUserById(Long userId) {
    return mpUserMapper.selectByPrimaryKey(userId);
  }

  /** 获取后台可访问的用户详情 */
  public MpUser getAccessibleAdminUserById(Long userId) {
    MpUser user = mpUserMapper.selectByPrimaryKey(userId);
    if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
      throw new MpBusinessException("用户不存在");
    }
    mpAppService.getAccessibleAdminAppByAppId(user.getAppId());
    return user;
  }

  /** 获取用户权限列表 */
  public List<String> getUserPermissions(Long userId, String appId) {
    return mpUserPermissionMapper.selectPermissionsByUserAndApp(userId, appId);
  }

  /** 获取后台可访问的用户权限列表 */
  public List<String> getAccessibleAdminUserPermissions(Long userId, String appId) {
    MpUser user = requireAccessibleAdminUser(userId, appId);
    return mpUserPermissionMapper.selectPermissionsByUserAndApp(user.getId(), user.getAppId());
  }

  /** 获取用户绑定的设备iotId列表 */
  public List<String> getUserDeviceIotIds(Long userId, String appId) {
    MpUser user = requireAccessibleAdminUser(userId, appId);
    return mpUserDeviceMapper.selectIotIdsByUser(user.getAppId(), userId);
  }

  /** 更新用户设备绑定关系 */
  @Transactional(rollbackFor = Exception.class)
  public void updateUserDevices(Long userId, String appId, List<String> iotIds) {
    MpUser user = requireAccessibleAdminUser(userId, appId);
    // 删除旧绑定
    mpUserDeviceMapper.deleteByUserAndApp(user.getAppId(), userId);

    // 插入新绑定
    if (iotIds != null && !iotIds.isEmpty()) {
      List<MpUserDevice> records = new ArrayList<>();
      Date now = new Date();
      for (String iotId : iotIds) {
        if (StrUtil.isBlank(iotId)) {
          continue;
        }
        records.add(
            MpUserDevice.builder()
                .appId(user.getAppId())
                .userId(userId)
                .iotId(iotId)
                .createTime(now)
                .build());
      }
      if (!records.isEmpty()) {
        mpUserDeviceMapper.batchInsert(records);
      }
    }
    log.info("更新用户设备绑定: userId={}, appId={}, iotIds={}", userId, user.getAppId(), iotIds);
  }

  /** 禁用用户 */
  @Transactional(rollbackFor = Exception.class)
  public void disableUser(Long userId) {
    MpUser user = getAccessibleAdminUserById(userId);
    user.setStatus(MpUser.Status.DISABLED);
    user.setUpdateTime(new Date());
    mpUserMapper.updateByPrimaryKeySelective(user);
    log.info("禁用用户: userId={}", userId);
  }

  /** 启用用户 */
  @Transactional(rollbackFor = Exception.class)
  public void enableUser(Long userId) {
    MpUser user = getAccessibleAdminUserById(userId);
    // 恢复为之前的状态（已绑定或待绑定）
    user.setStatus(user.getOpenId() != null ? MpUser.Status.BINDOUND : MpUser.Status.PENDING);
    user.setUpdateTime(new Date());
    mpUserMapper.updateByPrimaryKeySelective(user);
    log.info("启用用户: userId={}", userId);
  }

  /** 删除用户（逻辑删除） */
  @Transactional(rollbackFor = Exception.class)
  public void deleteUser(Long userId) {
    MpUser user = getAccessibleAdminUserById(userId);
    user.setDeleted(1);
    user.setUpdateTime(new Date());
    mpUserMapper.updateByPrimaryKeySelective(user);

    // 删除权限
    mpUserPermissionMapper.deleteByUserAndApp(userId, user.getAppId());
    log.info("删除用户: userId={}", userId);
  }

  private MpUser requireAccessibleAdminUser(Long userId, String appId) {
    MpUser user = getAccessibleAdminUserById(userId);
    if (StrUtil.isNotBlank(appId) && !StrUtil.equals(user.getAppId(), appId)) {
      throw new MpBusinessException("用户不属于指定的小程序应用");
    }
    return user;
  }

  /** 根据OpenId查询用户 */
  public MpUser getUserByOpenId(String appId, String openId) {
    return mpUserMapper.selectByAppIdAndOpenId(appId, openId);
  }

  /** 更新用户登录信息 */
  public void updateLoginInfo(Long userId, String ip) {
    MpUser user = new MpUser();
    user.setId(userId);
    user.setLastLoginTime(new Date());
    user.setLastLoginIp(ip);
    user.setUpdateTime(new Date());
    mpUserMapper.updateByPrimaryKeySelective(user);
  }
}
