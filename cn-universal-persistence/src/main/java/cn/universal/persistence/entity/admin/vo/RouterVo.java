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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 路由配置信息 @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "路由VO")
public class RouterVo {

  /** 路由名字 */
  @Schema(description = "路由名称")
  private String name;

  /** 路由地址 */
  @Schema(description = "路由地址")
  private String path;

  /** 是否隐藏路由，当设置 true 的时候该路由不会再侧边栏出现 */
  @Schema(description = "是否隐藏路由")
  private boolean hidden;

  /** 重定向地址，当设置 noRedirect 的时候该路由在面包屑导航中不可被点击 */
  @Schema(description = "重定向地址")
  private String redirect;

  /** 组件地址 */
  @Schema(description = "组件地址")
  private String component;

  /** 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面 */
  @Schema(description = "是否总是显示")
  private Boolean alwaysShow;

  /** 其他元素 */
  @Schema(description = "元数据VO")
  private MetaVo meta;

  /** 子路由 */
  @Schema(description = "子路由列表")
  private List<RouterVo> children;
}
