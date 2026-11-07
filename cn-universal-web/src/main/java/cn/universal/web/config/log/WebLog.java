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

package cn.universal.web.config.log;

import cn.hutool.core.text.StrPool;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/04/19
 */
@Slf4j(topic = "api_log")
@Builder
@Data
public class WebLog {

  private static final String SEPARATION = "\t";

  private String reqParams;
  private String traceId;
  private String headerInfo;
  private long entryTime;
  private long returnTime;
  private String RespBody;

  public void log() {
    long lastTime = System.currentTimeMillis();
    StringBuffer sb = new StringBuffer();
    sb.append("requestId={}")
        .append(StrPool.C_COMMA)
        .append("header={}")
        .append(StrPool.C_COMMA)
        .append("params={}")
        .append(StrPool.C_COMMA)
        //        .append("return={}")
        //        .append(StrPool.C_COMMA)
        .append("costTime={}ms");
    log.info(sb.toString(), traceId, headerInfo, reqParams, (lastTime - entryTime));
  }
}
