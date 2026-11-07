/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.mapper;

import cn.universal.miniapp.entity.MpBindQrcode;
import cn.universal.persistence.common.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 小程序绑定二维码Mapper
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public interface MpBindQrcodeMapper extends BaseMapper<MpBindQrcode> {

  /** 根据code查询二维码 */
  MpBindQrcode selectByCode(@Param("code") String code);

  /** 更新二维码状态为已使用 */
  int markAsUsed(@Param("code") String code, @Param("usedOpenId") String usedOpenId);

  /** 将过期的二维码状态更新为已过期 */
  int markExpiredQrcodes();
}
