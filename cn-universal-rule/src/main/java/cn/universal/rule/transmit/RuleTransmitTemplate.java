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

package cn.universal.rule.transmit;

import cn.hutool.json.JSONObject;
import cn.universal.common.exception.IoTException;
import cn.universal.rule.model.RuleTarget;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 规则转发模板 @Author gitee.com/NexIoT
 *
 * @since 2023/1/14 16:33
 */
@Component
public class RuleTransmitTemplate {

  private final Map<String, RuleTransmit> transmitMap;

  public RuleTransmitTemplate(List<RuleTransmit> transmits) {
    this.transmitMap =
        transmits.stream().collect(Collectors.toMap(RuleTransmit::type, Function.identity()));
  }

  /**
   * 数据转发代理
   *
   * @param param 数据
   * @param target 转发模板
   */
  public String transmit(JSONObject param, RuleTarget target) {
    RuleTransmit ruleTransmit = transmitMap.get(target.getType());
    if (Objects.nonNull(ruleTransmit)) {
      return ruleTransmit.transmit(param, target);
    }
    throw new IoTException("不支持转发类型");
  }

  /**
   * 测试数据转发代理
   *
   * @param param 数据
   * @param target 转发模板
   */
  public String testTransmit(JSONObject param, RuleTarget target) {
    RuleTransmit ruleTransmit = transmitMap.get(target.getType());
    if (Objects.nonNull(ruleTransmit)) {
      return ruleTransmit.testTransmit(param, target);
    }
    throw new IoTException("不支持转发类型");
  }
}
