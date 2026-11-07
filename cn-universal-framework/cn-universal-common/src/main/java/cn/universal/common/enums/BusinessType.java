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

package cn.universal.common.enums;

/** 业务操作类型枚举 */
public enum BusinessType {
  /** 其它 */
  OTHER,

  /** 新增 */
  INSERT,

  /** 修改 */
  UPDATE,

  /** 删除 */
  DELETE,

  /** 授权 */
  GRANT,

  /** 导出 */
  EXPORT,

  /** 导入 */
  IMPORT,

  /** 强退 */
  FORCE,

  /** 生成代码 */
  GENCODE,

  /** 清空数据 */
  CLEAN,
}
