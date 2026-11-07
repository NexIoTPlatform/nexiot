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

package cn.universal.common.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 读取项目相关配置 @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Component
@ConfigurationProperties(prefix = "univ")
public class UnivUPloadConfig {

  /** 上传路径 */
  @Getter private static String profile;

  public void setProfile(String profile) {
    UnivUPloadConfig.profile = profile;
  }

  /** 获取导入上传路径 */
  public static String getImportPath() {
    return profile + "/import";
  }

  /** 获取头像上传路径 */
  public static String getAvatarPath() {
    return profile + "/avatar";
  }

  /** 获取下载路径 */
  public static String getDownloadPath() {
    return profile + "/download/";
  }

  /** 获取上传路径 */
  public static String getUploadPath() {
    return profile + "/upload";
  }
}
