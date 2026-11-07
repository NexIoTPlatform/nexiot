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

package cn.universal.core.protocol.request;

import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/9 21:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProtocolDecodeRequest {

  private ProtocolSupportDefinition definition;
  // 原始消息
  private String payload;
  // 上下文
  private Object context;

  public ProtocolDecodeRequest(ProtocolSupportDefinition definition, String payload) {
    this.definition = definition;
    this.payload = payload;
  }
}
