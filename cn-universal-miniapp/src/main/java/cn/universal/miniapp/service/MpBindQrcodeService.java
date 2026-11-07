/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.service;

import cn.hutool.core.util.IdUtil;
import cn.universal.miniapp.config.MpBusinessException;
import cn.universal.miniapp.entity.MpBindQrcode;
import cn.universal.miniapp.entity.MpUser;
import cn.universal.miniapp.mapper.MpBindQrcodeMapper;
import cn.universal.miniapp.mapper.MpUserMapper;
import cn.universal.miniapp.mapper.MpUserPermissionMapper;
import cn.universal.miniapp.security.MpTokenService;
import cn.universal.miniapp.vo.MpUserInfoVO;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 二维码绑定服务
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Service
public class MpBindQrcodeService {

  /** 二维码有效期（分钟） */
  private static final int QRCODE_EXPIRE_MINUTES = 5;

  /** Redis二维码前缀 */
  private static final String QRCODE_PREFIX = "mp:qrcode:";

  @Autowired private MpBindQrcodeMapper mpBindQrcodeMapper;

  @Autowired private MpUserMapper mpUserMapper;

  @Autowired private MpTokenService mpTokenService;

  @Autowired private StringRedisTemplate stringRedisTemplate;

  @Autowired private MpUserPermissionMapper mpUserPermissionMapper;

  /**
   * 生成绑定二维码
   *
   * @param appId 小程序APPID
   * @param targetUserId 目标绑定用户ID（可选）
   * @return 二维码code
   */
  public String generateQrcode(String appId, Long targetUserId) {
    String code = IdUtil.fastSimpleUUID();
    Date expireTime =
        Date.from(
            LocalDateTime.now()
                .plusMinutes(QRCODE_EXPIRE_MINUTES)
                .atZone(ZoneId.systemDefault())
                .toInstant());

    MpBindQrcode qrcode =
        MpBindQrcode.builder()
            .code(code)
            .appId(appId)
            .targetUserId(targetUserId)
            .expireTime(expireTime)
            .status(MpBindQrcode.Status.PENDING)
            .createTime(new Date())
            .build();

    mpBindQrcodeMapper.insertSelective(qrcode);

    // 同时存入Redis便于快速查询
    stringRedisTemplate
        .opsForValue()
        .set(
            QRCODE_PREFIX + code,
            appId + ":" + (targetUserId != null ? targetUserId : ""),
            QRCODE_EXPIRE_MINUTES,
            TimeUnit.MINUTES);

    log.info("生成绑定二维码: code={}, appId={}, targetUserId={}", code, appId, targetUserId);
    return code;
  }

  /**
   * 扫码绑定
   *
   * @param code 二维码code
   * @param openId 微信OpenID
   * @param unionId 微信UnionID
   * @return 用户信息及Token
   */
  @Transactional(rollbackFor = Exception.class)
  public MpUserInfoVO scanAndBind(String code, String openId, String unionId) {
    // 查询二维码
    MpBindQrcode qrcode = mpBindQrcodeMapper.selectByCode(code);
    if (qrcode == null) {
      throw new MpBusinessException("二维码不存在");
    }

    if (qrcode.getStatus() == MpBindQrcode.Status.USED) {
      throw new MpBusinessException("二维码已被使用");
    }

    if (qrcode.getStatus() == MpBindQrcode.Status.EXPIRED
        || qrcode.getExpireTime().before(new Date())) {
      throw new MpBusinessException("二维码已过期");
    }

    String appId = qrcode.getAppId();
    MpUser user;

    // 如果指定了目标用户，则绑定到该用户
    if (qrcode.getTargetUserId() != null) {
      user = mpUserMapper.selectByPrimaryKey(qrcode.getTargetUserId());
      if (user == null) {
        throw new MpBusinessException("目标用户不存在");
      }
      if (user.getStatus() == MpUser.Status.DISABLED) {
        throw new MpBusinessException("用户已被禁用");
      }
      // 更新绑定信息
      mpUserMapper.updateBindInfo(user.getId(), openId, unionId, MpUser.Status.BINDOUND);
      user.setOpenId(openId);
      user.setUnionId(unionId);
      user.setStatus(MpUser.Status.BINDOUND);
    } else {
      // 检查是否已有绑定用户
      user = mpUserMapper.selectByAppIdAndOpenId(appId, openId);
      if (user == null) {
        throw new MpBusinessException("请先由管理员创建账户后再扫码绑定");
      }
    }

    // 标记二维码已使用
    mpBindQrcodeMapper.markAsUsed(code, openId);

    // 生成Token
    String token = mpTokenService.generateToken(user);

    // 查询权限
    List<String> permissions =
        mpUserPermissionMapper.selectPermissionsByUserAndApp(user.getId(), appId);

    log.info("扫码绑定成功: code={}, appId={}, userId={}, openId={}", code, appId, user.getId(), openId);

    return MpUserInfoVO.builder()
        .userId(user.getId())
        .appId(appId)
        .nickname(user.getNickname())
        .avatar(user.getAvatar())
        .phone(user.getPhone())
        .status(user.getStatus())
        .permissions(permissions)
        .token(token)
        .tokenExpireTime(System.currentTimeMillis() + mpTokenService.getTokenExpireTime() * 1000)
        .build();
  }

  /** 清理过期二维码 */
  public void cleanExpiredQrcodes() {
    int count = mpBindQrcodeMapper.markExpiredQrcodes();
    if (count > 0) {
      log.info("清理过期二维码: count={}", count);
    }
  }
}
