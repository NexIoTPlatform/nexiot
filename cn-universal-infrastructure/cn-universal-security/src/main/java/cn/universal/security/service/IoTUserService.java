/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.security.service;

import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTUserBO;
import java.util.List;

/** 用户业务层接口 */
public interface IoTUserService {

  /**
   * 查询用户列表
   *
   * @param ioTUser 用户信息
   * @return 用户信息集合信息
   */
  List<IoTUser> selectUserList(IoTUser ioTUser);

  /**
   * 查询已分配用户角色列表
   *
   * @param ioTUser 用户信息
   * @return 用户信息集合信息
   */
  List<IoTUser> selectAllocatedList(IoTUserBO ioTUser);

  /**
   * 根据条件分页查询未分配用户角色列表
   *
   * @param ioTUserBO 用户信息BO
   * @return 用户信息集合信息
   */
  List<IoTUser> selectUnallocatedList(IoTUserBO ioTUserBO);

  /**
   * 通过用户名查询用户
   *
   * @param userName 用户名
   * @return 用户对象信息
   */
  IoTUser selectUserByUserName(String userName);

  /**
   * 通过用户ID查询用户
   *
   * @param userId 用户ID
   * @return 用户对象信息
   */
  IoTUser selectUserById(Long userId);

  /**
   * 通过用户手机号查询用户
   *
   * @param mobile 用户手机号
   * @return 用户对象信息
   */
  IoTUser selectUserByMobile(String mobile);

  /**
   * 通过用户unionId查询用户
   *
   * @param
   * @return 用户对象信息
   */
  IoTUser selectUserByUnionId(String unionId);

  /**
   * 查询用户
   *
   * @param ioTUser 查询条件
   * @return
   */
  IoTUser queryOne(IoTUser ioTUser);

  /**
   * 校验用户名称是否唯一
   *
   * @param ioTUser 用户
   * @return 结果
   */
  String checkUserNameUnique(IoTUser ioTUser);

  /**
   * 校验手机号码是否唯一
   *
   * @param ioTUser 用户
   * @return 结果
   */
  String checkPhoneUnique(IoTUser ioTUser);

  /**
   * 校验用户是否允许操作
   *
   * @param ioTUser 用户信息
   */
  void checkUserAllowed(IoTUser ioTUser);

  /**
   * 新增用户信息
   *
   * @param ioTUserBO 用户信息
   * @return 结果
   */
  int insertUser(IoTUserBO ioTUserBO);

  /**
   * 修改用户信息
   *
   * @param ioTUserBO 用户信息
   * @return 结果
   */
  int updateUser(IoTUserBO ioTUserBO);

  void updateUserById(IoTUser iotUser);

  /**
   * 用户授权角色
   *
   * @param unionId 用户ID
   * @param roleIds 角色组
   */
  public void insertUserAuth(String unionId, Long[] roleIds);

  /**
   * 通过用户ID删除用户
   *
   * @param userId 用户ID
   * @return 结果
   */
  int deleteUserById(Long userId);

  /**
   * 批量删除用户信息
   *
   * @param userIds 需要删除的用户ID
   * @return 结果
   */
  int deleteUserByIds(Long[] userIds);
}
