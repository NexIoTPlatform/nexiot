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

package cn.universal.admin.system.service;

import cn.universal.common.utils.IPUtil;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 白名单服务
 *
 * @version 1.0
 * @since 2023/8/11
 */
@Component
public class IPWhitelistService {

  private static final String REDIS_WHITE_KEY = "whitelist";

  @Value("${whitelist.enable:true}")
  private boolean whiteEnable;

  @Resource private ISysDictDataService iSysDictDataService;
  @Resource private StringRedisTemplate stringRedisTemplate;

  public boolean isWhitelisted(List<String> whitelistKeys, String ipAddress) {
    if (!whiteEnable) {
      return Boolean.TRUE;
    }
    boolean exist = stringRedisTemplate.hasKey(REDIS_WHITE_KEY + ":" + ipAddress);
    if (exist) {
      return Boolean.TRUE;
    }
    Set<String> ipLists = iSysDictDataService.selectWhitelist(whitelistKeys);
    boolean perMit = IPUtil.checkIPWhitelist(ipAddress, ipLists);
    if (perMit) {
      stringRedisTemplate
          .opsForValue()
          .set(REDIS_WHITE_KEY + ":" + ipAddress, "1", 1, TimeUnit.HOURS);
      return Boolean.TRUE;
    } else {
      return Boolean.FALSE;
    }
  }
}
