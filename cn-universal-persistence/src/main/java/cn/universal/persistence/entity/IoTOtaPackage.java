/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA升级包实体
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * OTA升级包实体 iot_ota_package
 *
 * @since 2026-03-21
 */
@Table(name = "iot_ota_package")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "OTA升级包实体")
public class IoTOtaPackage implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final int FLAG_YES = 1;
  private static final int FLAG_NO = 0;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "product_id")
  @Schema(description = "产品ID")
  private Long productId;

  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  @Column(name = "package_name")
  @Schema(description = "升级包名称")
  private String packageName;

  @Schema(description = "升级包版本")
  private String version;

  @Column(name = "package_type")
  @Schema(description = "升级包类型")
  private String packageType;

  @Column(name = "custom_type")
  @Schema(description = "自定义类型")
  private String customType;

  @Schema(description = "升级模块")
  private String module;

  @Schema(description = "升级包描述")
  private String description;

  @Column(name = "file_name")
  @Schema(description = "文件名称")
  private String fileName;

  @Column(name = "file_url")
  @Schema(description = "文件地址")
  private String fileUrl;

  @Column(name = "file_size")
  @Schema(description = "文件大小")
  private Long fileSize;

  @Schema(description = "校验值")
  private String checksum;

  @Column(name = "checksum_algorithm")
  @Schema(description = "校验算法")
  private String checksumAlgorithm;

  @Column(name = "signature_url")
  @Schema(description = "签名文件地址")
  private String signatureUrl;

  @Column(name = "min_hardware_version")
  @Schema(description = "最低硬件版本")
  private String minHardwareVersion;

  @Column(name = "max_hardware_version")
  @Schema(description = "最高硬件版本")
  private String maxHardwareVersion;

  @Column(name = "min_software_version")
  @Schema(description = "最低软件版本")
  private String minSoftwareVersion;

  @Column(name = "force_upgrade")
  @Schema(description = "是否强制升级")
  private Integer forceUpgrade;

  @Schema(description = "状态")
  private String status;

  @Column(name = "release_notes")
  @Schema(description = "发布说明")
  private String releaseNotes;

  @Column(name = "extra_info")
  @Schema(description = "扩展信息")
  private String extraInfo;

  @Column(name = "creator_id")
  @Schema(description = "创建人")
  private String creatorId;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Transient
  @Schema(description = "产品名称")
  private String productName;

  @Transient
  @Schema(description = "标签ID文本")
  private String tagIdsText;

  @Transient
  @Schema(description = "标签名称文本")
  private String tagNamesText;

  @Transient
  @Schema(description = "标签ID列表")
  private List<Long> tagIds;

  @Transient
  @Schema(description = "标签名称列表")
  private List<String> tagNames;

  @JsonSetter("forceUpgrade")
  public void setForceUpgradeValue(Object forceUpgrade) {
    this.forceUpgrade = toFlagValue(forceUpgrade);
  }

  public static Integer toFlagValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue ? FLAG_YES : FLAG_NO;
    }
    if (value instanceof Number numberValue) {
      return numberValue.intValue() == FLAG_YES ? FLAG_YES : FLAG_NO;
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return null;
    }
    if ("true".equalsIgnoreCase(text)) {
      return FLAG_YES;
    }
    if ("false".equalsIgnoreCase(text)) {
      return FLAG_NO;
    }
    try {
      return Integer.parseInt(text) == FLAG_YES ? FLAG_YES : FLAG_NO;
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  public boolean isForceUpgradeFlag() {
    return forceUpgrade != null && forceUpgrade == FLAG_YES;
  }

  public static class IoTOtaPackageBuilder {
    public IoTOtaPackageBuilder forceUpgrade(Boolean forceUpgrade) {
      this.forceUpgrade = IoTOtaPackage.toFlagValue(forceUpgrade);
      return this;
    }
  }
}
