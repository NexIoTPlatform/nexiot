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

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_product")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT产品实体")
public class IoTProduct implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 产品编号 */
  @Column(name = "product_id")
  @Schema(description = "产品编号")
  private String productId;

  /** 产品标签 */
  @Column(name = "tags")
  @Schema(description = "产品标签")
  private String tags;

  /** 产品KEY */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 产品密钥（1型1密） */
  @Column(name = "product_secret")
  //  @SensitiveField(showPrefix = 3, showSuffix = 3)
  @Schema(description = "产品密钥")
  private String productSecret;

  /** 接入方式 */
  @Column(name = "third_platform")
  @Schema(description = "第三方平台")
  private String thirdPlatform;

  /** 第三方平台配置信息 */
  @Column(name = "third_configuration")
  @Schema(description = "第三方平台配置信息")
  private String thirdConfiguration;

  /** 厂商编号 */
  @Column(name = "company_no")
  @Schema(description = "公司编号")
  private String companyNo;

  /** 分类ID */
  @Column(name = "classified_id")
  @Schema(description = "分类ID")
  private String classifiedId;

  /** 网络组件：关联network表 */
  @Column(name = "network_union_id")
  @Schema(description = "网络联合ID")
  private String networkUnionId;

  /** 设备类型: 网关，设备 */
  @Column(name = "device_node")
  @Schema(description = "设备节点")
  private String deviceNode;

  /** 所属网关设备的ProductKey */
  @Column(name = "gw_product_key")
  @Schema(description = "网关产品Key")
  private String gwProductKey;

  /** 分类名称 */
  @Column(name = "classified_name")
  @Schema(description = "分类名称")
  private String classifiedName;

  /** 消息协议: */
  @Column(name = "message_protocol")
  @Schema(description = "消息协议")
  private String messageProtocol;

  /** 名称 */
  @Schema(description = "产品名称")
  private String name;

  /** 创建者id */
  @Column(name = "creator_id")
  @Schema(description = "创建者ID")
  private String creatorId;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 产品状态 */
  @Schema(description = "产品状态")
  private Byte state;

  /** 说明 */
  @Column(name = "product_description")
  @Schema(description = "产品描述")
  private String describe;

  /** 数据存储策略 */
  @Column(name = "store_policy")
  @Schema(description = "存储策略")
  private String storePolicy;

  /** 传输协议: MQTT,COAP,UDP */
  @Column(name = "transport_protocol")
  @Schema(description = "传输协议")
  private String transportProtocol;

  /** 图片地址 */
  @Column(name = "photo_url")
  @Schema(description = "图片URL")
  private String photoUrl;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 协议配置 */
  @Schema(description = "配置信息")
  private String configuration;

  /** 数据存储策略配置 */
  @Column(name = "store_policy_configuration")
  @Schema(description = "存储策略配置")
  private String storePolicyConfiguration;

  /** 物模型 */
  @Schema(description = "物模型数据")
  private String metadata;

  /** 第三方平台产品下发信息 */
  @Schema(description = "第三方平台下发请求")
  private String thirdDownRequest;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Column(name = "instance")
  @Schema(description = "实例名称")
  private String instance;

  @Column(name = "is_deleted")
  @Schema(description = "是否删除")
  private Integer isDeleted;

  @Transient
  @Schema(description = "数据权限部门ID")
  private List<Long> scopeDeptIds = new ArrayList<>();

  @Transient
  @Schema(description = "数据权限可见用户ID")
  private List<String> scopeUnionIds = new ArrayList<>();

  @Transient
  @Schema(description = "数据权限用户ID")
  private String scopeUnionId;

  @Transient
  @Schema(description = "是否无权限")
  private boolean scopeDenied;
}
