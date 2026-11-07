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

package cn.universal.rule.function.inner;

import cn.hutool.json.JSONUtil;
import cn.universal.rule.function.RuleFunction;
import org.springframework.stereotype.Component;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2023/1/17 9:27
 */
@Component
public class ToJsonObjFunction implements RuleFunction {

  @Override
  public String functionName() {
    return "toJsonObj";
  }

  @Override
  public Object executeFunction(Object[] param) {
    return JSONUtil.parseObj(param[0].toString());
  }
}
