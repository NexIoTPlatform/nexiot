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

package cn.universal.core.downlink;

/**
 * 拦截器执行阶段
 *
 * @version 1.0
 * @since 2025/10/24
 */
public enum InterceptorPhase {

  /** 前置阶段：在消息转换（convert）之前执行 适用场景：鉴权、限流、参数校验、日志记录等 */
  PRE,

  /** 中置阶段：在消息转换（convert）之后，具体处理器（processor）之前执行 适用场景：数据增强、编解码、影子服务、业务规则检查等 */
  MID,

  /** 后置阶段：在具体处理器（processor）执行之后 适用场景：结果处理、通知推送、审计日志、清理资源等 */
  POST
}
