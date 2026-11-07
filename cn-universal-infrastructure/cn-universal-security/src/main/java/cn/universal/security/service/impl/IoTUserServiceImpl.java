/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 *
 * @Author: gitee.com/NexIoT
 *
 * @Email: wo8335224@gmail.com
 *
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.security.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.StringUtils;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysUserRole;
import cn.universal.persistence.entity.bo.IoTUserBO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.admin.SysUserRoleMapper;
import cn.universal.persistence.page.PageContextRunner;
import cn.universal.security.support.EvictMenuPermissions;
import cn.universal.security.service.IoTUserService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

/** 用户 业务层处理 @Author ruoyi */
@Slf4j
@Service
public class IoTUserServiceImpl implements IoTUserService {

  @Resource private IoTUserMapper iotUserMapper;
  @Resource private SysUserRoleMapper userRoleMapper;
  // @Resource
  // private OauthClientDetailsMapper oauthClientDetailsMapper;
  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Override
  // @DataScope(alias = "", created = "u.create_by")
  public List<IoTUser> selectUserList(IoTUser user) {
    return iotUserMapper.selectList(user);
  }

  @Override
  public IoTUser selectUserById(Long userId) {
    return PageContextRunner.runWithoutPaging(() -> iotUserMapper.selectByPrimaryKey(userId));
  }

  @Override
  public IoTUser selectUserByMobile(String mobile) {
    return PageContextRunner.runWithoutPaging(() -> iotUserMapper.selectUserByMobile(mobile));
  }

  @Override
  @Cacheable(cacheNames = "iot_user_info", key = "#unionId", unless = "#result == null")
  public IoTUser selectUserByUnionId(String unionId) {
    return PageContextRunner.runWithoutPaging(
        () -> {
          Example ex = new Example(IoTUser.class);
          ex.createCriteria().andEqualTo("unionId", unionId).andEqualTo("deleted", 0);
          return iotUserMapper.selectOneByExample(ex);
        });
  }

  /**
   * 根据条件分页查询已分配用户角色列表
   *
   * @param user 用户信息
   * @return 用户信息集合信息
   */
  @Override
  public List<IoTUser> selectAllocatedList(IoTUserBO user) {
    return iotUserMapper.selectAllocatedList(user);
  }

  /**
   * 根据条件分页查询未分配用户角色列表
   *
   * @param user 用户信息
   * @return 用户信息集合信息
   */
  @Override
  public List<IoTUser> selectUnallocatedList(IoTUserBO user) {
    return iotUserMapper.selectUnallocatedList(user);
  }

  @Override
  public IoTUser queryOne(IoTUser iotUser) {
    return PageContextRunner.runWithoutPaging(() -> iotUserMapper.selectOne(iotUser));
  }

  @Override
  @Cacheable(cacheNames = "selectUserByUserName", key = "#userName", unless = "#result == null")
  public IoTUser selectUserByUserName(String userName) {
    return PageContextRunner.runWithoutPaging(
        () -> {
          Example ex = new Example(IoTUser.class);
          ex.createCriteria().andEqualTo("username", userName).andEqualTo("deleted", 0);
          return iotUserMapper.selectOneByExample(ex);
        });
  }

  @Override
  public String checkUserNameUnique(IoTUser user) {
    Long userId = StringUtils.isNull(user.getId()) ? -1L : user.getId();
    // unionId必须唯一 新增是unionId为usrname
    List<IoTUser> users =
        iotUserMapper.selectList(IoTUser.builder().unionId(user.getUsername()).build());
    if (!CollectionUtils.isEmpty(users) && users.get(0).getId().longValue() != userId.longValue()) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }

  @Override
  public String checkPhoneUnique(IoTUser user) {
    Long userId = StringUtils.isNull(user.getId()) ? -1L : user.getId();
    List<IoTUser> users =
        iotUserMapper.selectList(IoTUser.builder().mobile(user.getMobile()).build());
    if (!CollectionUtils.isEmpty(users) && users.get(0).getId().longValue() != userId.longValue()) {
      return IoTUserConstants.NOT_UNIQUE;
    }
    return IoTUserConstants.UNIQUE;
  }

  /**
   * 校验用户是否允许操作
   *
   * @param user 用户信息
   */
  @Override
  public void checkUserAllowed(IoTUser user) {
    if (Validator.isNotNull(user.getId()) && user.isAdmin()) {
      throw new IoTException("不允许操作超级管理员用户");
    }
  }

  @Override
  @Transactional
  @EvictMenuPermissions
  public int insertUser(IoTUserBO userbo) {
    userbo.setId(IdUtil.getSnowflake().nextId());
    IoTUser iotUser = BeanUtil.toBean(userbo, IoTUser.class);
    int rows = iotUserMapper.insert(iotUser);
    insertUserRole(userbo);
    return rows;
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(cacheNames = {"iot_user_info"}, allEntries = true),
        @CacheEvict(cacheNames = {"selectUserByUserName"}, allEntries = true),
        @CacheEvict(cacheNames = {"user_parent_entity"}, allEntries = true)
      })
  @Transactional
  @EvictMenuPermissions
  public int updateUser(IoTUserBO userbo) {
    IoTUser iotUser = BeanUtil.toBean(userbo, IoTUser.class);
    if (iotUser.getPassword() == null || "".equals(iotUser.getPassword())) {
      iotUser.setPassword(null);
    }
    int rows = iotUserMapper.updateByPrimaryKeySelective(iotUser);
    if (rows > 0 && userbo.getRoleIds() != null) {
      deleteUserRoleByUnionId(userbo);
      insertUserRole(userbo);
    }
    return rows;
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(cacheNames = {"iot_user_info"}, allEntries = true),
        @CacheEvict(cacheNames = {"selectUserByUserName"}, allEntries = true),
        @CacheEvict(cacheNames = {"user_parent_entity"}, allEntries = true)
      })
  @EvictMenuPermissions
  public void updateUserById(IoTUser iotUser) {
    iotUserMapper.updateByPrimaryKeySelective(iotUser);
  }

  @Override
  @EvictMenuPermissions
  public int deleteUserById(Long userId) {
    return iotUserMapper.deleteByPrimaryKey(userId);
  }

  @Override
  @EvictMenuPermissions
  public int deleteUserByIds(Long[] userIds) {
    String ids = "";
    for (int i = 0; i < userIds.length; i++) {
      checkUserAllowed(new IoTUser(userIds[i]));
      if (i == 0) {
        ids = ids + userIds[i];
      } else {
        ids = ids + "," + userIds[i];
      }
    }

    List<IoTUser> list = iotUserMapper.selectByIds(ids);
    String[] unionIds =
        list.stream()
            .map(IoTUser::getUnionId)
            .collect(Collectors.toList())
            .toArray(new String[list.size()]);
    Example ex = new Example(IoTDevice.class);
    ex.createCriteria().andIn("creatorId", Arrays.asList(unionIds));
    int count = ioTDeviceMapper.selectCountByExample(ex);
    if (count > 0) {
      throw new IoTException("该账号下存在设备，不允许删除！");
    }
    userRoleMapper.deleteUserRoleByUserIds(unionIds);
    return iotUserMapper.deleteByIds(ids);
  }

  /**
   * 新增用户角色信息
   *
   * @param user 用户对象
   */
  @EvictMenuPermissions
  public void insertUserRole(IoTUserBO user) {
    Long[] roles = user.getRoleIds();
    if (StringUtils.isNotNull(roles)) {
      for (Long roleId : roles) {
        if (roleId == null) {
          continue;
        }
        SysUserRole ur = new SysUserRole();
        ur.setUnionId(user.getUnionId());
        ur.setRoleId(roleId);
        userRoleMapper.insertSelective(ur);
      }
    }
  }

  /**
   * 删除用户角色信息
   *
   * @param ioTUserBO 用户对象
   */
  @EvictMenuPermissions
  public void deleteUserRoleByUnionId(IoTUserBO ioTUserBO) {
    if (ioTUserBO.getRoleIds() != null) {
      Example ex = new Example(SysUserRole.class);
      ex.createCriteria().andEqualTo("unionId", ioTUserBO.getUnionId());
      userRoleMapper.deleteByExample(ex);
    }
  }

  /**
   * 用户授权角色
   *
   * @param unionId 用户ID
   * @param roleIds 角色组
   */
  @Override
  @Transactional
  @EvictMenuPermissions
  public void insertUserAuth(String unionId, Long[] roleIds) {
    Example ex = new Example(SysUserRole.class);
    ex.createCriteria().andEqualTo("unionId", unionId);
    userRoleMapper.deleteByExample(ex);
    insertUserRole(IoTUserBO.builder().unionId(unionId).roleIds(roleIds).build());
  }
}
