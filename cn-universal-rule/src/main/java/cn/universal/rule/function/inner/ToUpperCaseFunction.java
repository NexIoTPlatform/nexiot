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

import cn.universal.rule.function.RuleFunction;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2025/12/3 13:58
 */
@Component
public class ToUpperCaseFunction implements RuleFunction {

  @Override
  public String functionName() {
    return "toUpperCase";
  }

  @Override
  public Object executeFunction(Object[] param) {
    return param[0].toString().toUpperCase(Locale.ROOT);
  }
}
