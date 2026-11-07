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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2023/1/14 16:02
 */
@Data
@Schema
public class RuleConfig {

  @Schema(description = "查询字段")
  private String fields;

  @Schema(description = "应用id")
  private String appId;

  @Schema(description = "过滤条件")
  private String condition;

  @Schema(description = "执行sql")
  private List<RuleTarget> targets;

  public String getSql() {
    return String.format(
        "select %s from %s %s",
        fields, appId, StringUtils.isEmpty(condition) ? "" : " where " + condition);
  }
}
