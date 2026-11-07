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

package cn.universal.persistence.mapper;

import cn.universal.persistence.entity.IoTProductCategory;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 产品分类Mapper接口 @Author gitee.com/NexIoT
 *
 * @since 2025-12-29
 */
public interface IoTProductSortMapper {

  /**
   * 查询产品分类
   *
   * @param id 产品分类主键
   * @return 产品分类
   */
  public IoTProductCategory selectDevProductSortById(String id);

  /**
   * 查询产品分类列表
   *
   * @param ioTProductCategory 产品分类
   * @return 产品分类集合
   */
  public List<IoTProductCategory> selectDevProductSortList(IoTProductCategory ioTProductCategory);

  /**
   * 新增产品分类
   *
   * @param ioTProductCategory 产品分类
   * @return 结果
   */
  public int insertDevProductSort(IoTProductCategory ioTProductCategory);

  /**
   * 修改产品分类
   *
   * @param ioTProductCategory 产品分类
   * @return 结果
   */
  public int updateDevProductSort(IoTProductCategory ioTProductCategory);

  /**
   * 删除产品分类
   *
   * @param id 产品分类主键
   * @return 结果
   */
  public int deleteDevProductSortById(String id);

  /**
   * 批量删除产品分类
   *
   * @param ids 需要删除的数据主键集合
   * @return 结果
   */
  public int deleteDevProductSortByIds(String[] ids);

  /**
   * 根据父id倒序查出列表
   *
   * @param parentId 需要删除的数据主键集合
   * @return 结果
   */
  public List<IoTProductCategory> getListByParentId(
      @Param("parentId") String parentId, @Param("tenantId") String tenantId);
}
