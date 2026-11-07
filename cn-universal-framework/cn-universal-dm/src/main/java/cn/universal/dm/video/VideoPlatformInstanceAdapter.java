/*
 *
 * Copyright (c) 2025, IoT-universal. All Rights Reserved.
 *
 * @Description: 本文件由 AleoXin 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: AleoXin
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */
package cn.universal.dm.video;

import cn.universal.persistence.entity.VideoPlatformInstance;

/** 视频实例 */
public interface VideoPlatformInstanceAdapter {

  VideoPlatformInstance getVideoPlatformInstance(String instanceKey);
}
