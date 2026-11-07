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

package cn.universal.quartz.constant;

/** 任务调度通用常量 @Author ruoyi */
public class ScheduleConstants {

  public static final String TASK_CLASS_NAME = "TASK_CLASS_NAME";

  /** 执行目标key */
  public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

  /** 默认 */
  public static final String MISFIRE_DEFAULT = "0";

  /** 立即触发执行 */
  public static final String MISFIRE_IGNORE_MISFIRES = "1";

  /** 触发一次执行 */
  public static final String MISFIRE_FIRE_AND_PROCEED = "2";

  /** 不触发立即执行 */
  public static final String MISFIRE_DO_NOTHING = "3";

  public enum Status {
    /** 正常 */
    NORMAL("0"),
    /** 暂停 */
    PAUSE("1");

    private String value;

    private Status(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
