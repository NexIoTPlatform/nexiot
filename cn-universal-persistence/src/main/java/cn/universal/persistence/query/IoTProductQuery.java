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

package cn.universal.persistence.query;

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/15
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@Schema(description = "IoT产品查询条件")
public class IoTProductQuery extends BasePage {

  @Schema(description = "主键ID")
  private Long id;

  @Schema(description = "产品ID")
  private String productId;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "网络联合ID")
  private String networkUnionId;

  @Schema(description = "分类名称")
  private String classifiedName;

  @Schema(description = "公司编号")
  private String companyNo;

  @Schema(description = "消息协议")
  private String messageProtocol;

  @Schema(description = "描述")
  private String describe;

  @Schema(description = "存储策略")
  private String storePolicy;

  @Schema(description = "网关产品Key")
  private String gwProductKey;

  @Schema(description = "第三方平台")
  private String thirdPlatform;

  @Schema(description = "设备节点")
  private String deviceNode;

  @Schema(description = "创建者ID")
  private String creatorId;

  @Schema(description = "租户ID")
  private String tenantId;

  @Schema(description = "名称")
  private String name;

  @Schema(description = "传输协议")
  private String transportProtocol;

  @Schema(description = "状态")
  private Integer state;

  @Schema(description = "创建时间")
  private String createTime;

  @Schema(description = "分类ID")
  private String classifiedId;

  @Schema(description = "标签")
  private String tags;

  public String[] getTags() {
    if (StrUtil.isNotBlank(tags)) {
      return tags.split(",");
    }
    return null;
  }

  /** 存在设备 */
  @Schema(description = "是否存在设备")
  private boolean hasDevice;

  /** 是否自有 */
  @Schema(description = "是否自有产品")
  private boolean self;

  @Builder.Default
  @Schema(description = "数据权限部门ID")
  private List<Long> scopeDeptIds = new ArrayList<>();

  @Builder.Default
  @Schema(description = "数据权限可见用户ID")
  private List<String> scopeUnionIds = new ArrayList<>();

  @Schema(description = "数据权限用户ID")
  private String scopeUnionId;

  @Schema(description = "是否无权限")
  private boolean scopeDenied;
}
