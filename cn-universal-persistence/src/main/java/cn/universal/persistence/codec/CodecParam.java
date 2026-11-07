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

package cn.universal.persistence.codec;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 请求参数实体 @Author gitee.com/NexIoT
 *
 * @since 2023/11/23 9:10
 */
@Data
@AllArgsConstructor
public class CodecParam {

  /** 唯一编号 */
  private String codeKey;

  /** 编解码内容 */
  private Object codeBody;
}
