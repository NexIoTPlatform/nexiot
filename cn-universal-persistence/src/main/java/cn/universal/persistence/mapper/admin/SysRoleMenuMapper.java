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

package cn.universal.persistence.mapper.admin;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.admin.SysRoleMenu;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 角色与菜单关联表 数据层 @Author ruoyi */
// @Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

  int batchInsert(@Param("roleMenus") List<SysRoleMenu> roleMenus);
}
