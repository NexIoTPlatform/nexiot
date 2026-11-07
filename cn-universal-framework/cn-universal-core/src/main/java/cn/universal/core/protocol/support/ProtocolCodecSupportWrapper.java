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

package cn.universal.core.protocol.support;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import java.util.Map;

/**
 * 编解码支持类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/5/20 19:08
 */
public abstract class ProtocolCodecSupportWrapper {

  protected String str(Object obj) {
    if (null == obj || "null".equalsIgnoreCase(obj + "")) {
      return CharSequenceUtil.EMPTY;
    } else if (obj instanceof String) {
      return obj.toString();
    } else if (obj instanceof Map<?, ?> map && map.isEmpty()) {
      return CharSequenceUtil.EMPTY;
    } else {
      return JSONUtil.toJsonStr(obj);
    }
  }
}
