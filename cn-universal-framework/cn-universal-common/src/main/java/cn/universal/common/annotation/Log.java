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

package cn.universal.common.annotation;

import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 自定义操作日志记录注解 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

  /** 名称 */
  String title() default "";

  /** 业务类型 */
  BusinessType businessType() default BusinessType.OTHER;

  /** 操作终端类别 */
  OperatorType operatorType() default OperatorType.MANAGE;

  /** 是否保存请求的参数 */
  boolean isSaveRequestData() default true;
}
