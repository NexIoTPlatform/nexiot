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

package cn.universal.admin.system.service.impl;

import cn.universal.admin.system.service.IOAuthClientDetailsService;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 用户 业务层处理 @Author ruoyi */
@Slf4j
@Service
public class OAuth2ClientDetailsServiceImpl implements IOAuthClientDetailsService {

  @Resource private OauthClientDetailsMapper oauthClientDetailsMapper;

  @Override
  public int insert(OAuth2ClientDetails OAuth2ClientDetails) {
    return oauthClientDetailsMapper.insert(OAuth2ClientDetails);
  }

  @Override
  public int updateSecret(OAuth2ClientDetails OAuth2ClientDetails) {
    return oauthClientDetailsMapper.updateByPrimaryKeySelective(OAuth2ClientDetails);
  }

  @Override
  public int deleteByClientIds(String[] ids) {
    //    String a=String.join(",",ids);
    return oauthClientDetailsMapper.deleteByClientIds(ids);
  }
}
