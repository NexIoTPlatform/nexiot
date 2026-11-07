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

package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2025/8/26 18:51
 */
@Data
@Schema(description = "设备告警规则")
public class TriggerBO {

  // 触发方式,定时,设备
  @Schema(description = "触发方式")
  private String trigger = TriggerType.device.name();

  // trigger为定时任务时的cron表达式
  @Schema(description = "定时触发cron表达式")
  private String cron;

  // 类型,属性或者事件.
  @Schema(description = "触发消息类型")
  private String type;

  @Schema(description = "设备ID")
  private String deviceId;

  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "产品Key")
  private String productKey;

  // 物模型属性或者事件的标识 如: fire_alarm
  @Schema(description = "物模型表示,如:属性ID,事件ID")
  private String modelId;

  // 过滤条件
  @Schema(description = "条件")
  private List<ConditionFilter> filters;

  @Schema(description = "动作参数")
  private List<ExecData> execData;

  @Schema(description = "通知模板ID")
  private String noticeTemplateId;

  @Schema(description = "通知模板名称")
  private String noticeTemplateName;

  @Schema(description = "消息接受者")
  private String receivers;

  @AllArgsConstructor
  @Getter
  public enum MessageType {
    // 属性
    properties,
    // 事件
    event,
    // 功能调用回复
    functions;
  }

  @Getter
  @AllArgsConstructor
  public enum TriggerType implements Serializable {
    // 设备消息
    device(Arrays.asList(MessageType.properties, MessageType.event)),
    // 定时,定时获取只支持获取设备属性和调用功能.
    timer(Arrays.asList(MessageType.properties, MessageType.functions));

    final List<MessageType> supportMessageTypes;
  }

  @Getter
  @Setter
  public static class ExecData implements Serializable {

    @Schema(description = "参数id")
    private String id;

    @Schema(description = "参数名")
    private String name;

    @Schema(description = "参数值")
    private Object params;
  }

  @Getter
  @Setter
  public static class ConditionFilter implements Serializable {

    // 过滤条件key 如: temperature
    @Schema(description = "条件key")
    private String key;

    // 过滤条件值
    @Schema(description = "值")
    private String value;

    // 操作符, 等于,大于,小于....
    @Schema(description = "比对方式")
    private String operator = Operator.eq.name();

    public void validate() {
      if (StringUtils.isEmpty(key)) {
        throw new IllegalArgumentException("条件key不能为空");
      }
      if (StringUtils.isEmpty(value)) {
        throw new IllegalArgumentException("条件值不能为空");
      }
    }
  }

  @AllArgsConstructor
  @Getter
  public enum Operator {
    eq("=="),
    not("!="),
    gt(">"),
    lt("<"),
    gte(">="),
    lte("<="),
    like("like");

    private final String symbol;

    public Object convert(String value) {
      return value;
    }
  }

  @Getter
  @AllArgsConstructor
  public enum ExecTriggerType implements Serializable {
    device,
    notice;
  }

  @Schema(description = "动作触发类型: device/notice")
  private ExecTriggerType execTriggerType;
}
