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

package cn.universal.rule.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2025/12/2 9:02
 */
@Data
public class RuleParserResult {

  private List<RuleField> fields;

  private List<String> topics;

  private String condition;

  @Data
  @AllArgsConstructor
  public static class RuleField {

    private String name;
    private String alias;
  }
}
