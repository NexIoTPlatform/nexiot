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

package cn.universal.persistence.shadow;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 设备影子 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class State {

  /**
   * 设备的预期状态。仅当设备影子文档具有预期状态时，才包含desired部分。
   *
   * <p>应用程序向desired部分写入数据，更新事物的状态，而无需直接连接到该设备。
   */
  private JSONObject desired;

  /**
   * 设备的报告状态。设备可以在reported部分写入数据，报告其最新状态。
   *
   * <p>应用程序可以通过读取该参数值，获取设备的状态。
   *
   * <p>JSON文档中也可以不包含reported部分，没有reported部分的文档同样为有效影子JSON文档。
   */
  private JSONObject reported;
}
