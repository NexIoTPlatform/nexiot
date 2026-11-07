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

package cn.universal.quartz.task;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 定时任务调度测试 @Author ruoyi */
@Component("ryTask")
@Slf4j
public class RyTask {

  public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i) {
    log.info(StrUtil.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
  }

  public void ryParams(String params) {
    log.info("执行有参方法：" + params);
  }

  public void ryNoParams() {
    log.info("执行无参方法");
  }
}
