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

package cn.universal.rule.transmit.impl;

import cn.hutool.json.JSONObject;
import cn.universal.persistence.base.CommonRequest;
import cn.universal.rule.model.RuleTarget;
import cn.universal.rule.transmit.RuleTransmit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * http转发 @Author gitee.com/NexIoT
 *
 * @since 2023/1/14 16:31
 */
@Component
@Slf4j
public class RuleHttpTransmitStrategy implements RuleTransmit {

  @Override
  public String type() {
    return "http";
  }

  @Override
  public String transmit(JSONObject data, RuleTarget target) {
    if (data == null || target == null) {
      return null;
    }
    String body = data.toString();
    log.info("规则引擎,id={},url={},body={}", target.getId(), target.getUrl(), body);
    return CommonRequest.doRequest(target.getUrl(), body);
  }

  @Override
  public String testTransmit(JSONObject data, RuleTarget target) {
    String body = data.toString();
    log.info("规则引擎测试,id={},url={},body={}", target.getId(), target.getUrl(), body);
    return CommonRequest.doRequest(target.getUrl(), body);
  }
}
