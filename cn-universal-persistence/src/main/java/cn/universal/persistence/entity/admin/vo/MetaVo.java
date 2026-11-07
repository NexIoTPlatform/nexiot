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

package cn.universal.persistence.entity.admin.vo;

import cn.hutool.core.lang.Validator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 路由显示信息 @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "路由元数据VO")
public class MetaVo {

  /** 设置该路由在侧边栏和面包屑中展示的名字 */
  @Schema(description = "路由标题")
  private String title;

  /** 设置该路由的图标，对应路径src/assets/icons/svg */
  @Schema(description = "路由图标")
  private String icon;

  /** 设置为true，则不会被 <keep-alive>缓存 */
  @Schema(description = "是否不缓存")
  private boolean noCache;

  /** 内链地址（http(s)://开头） */
  @Schema(description = "内链地址")
  private String link;

  public MetaVo(String title, String icon) {
    this.title = title;
    this.icon = icon;
  }

  public MetaVo(String title, String icon, boolean noCache) {
    this.title = title;
    this.icon = icon;
    this.noCache = noCache;
  }

  public MetaVo(String title, String icon, String link) {
    this.title = title;
    this.icon = icon;
    this.link = link;
  }

  public MetaVo(String title, String icon, boolean noCache, String link) {
    this.title = title;
    this.icon = icon;
    this.noCache = noCache;
    if (Validator.isUrl(link)) {
      this.link = link;
    }
  }
}
