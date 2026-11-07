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

package cn.universal.persistence.common.inteceptor;

import cn.hutool.core.lang.Snowflake;
import tk.mybatis.mapper.genid.GenId;

/** 雪花Id生成器 */
public class SQenGenId implements GenId<Long> {

  public static final Snowflake snowflake = new Snowflake();

  public static Long nextId() {
    return snowflake.nextId();
  }

  @Override
  public Long genId(String s, String s1) {
    return nextId();
  }
}
