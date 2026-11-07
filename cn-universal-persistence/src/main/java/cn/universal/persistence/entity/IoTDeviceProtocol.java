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

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
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
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_device_protocol")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IoT设备协议实体")
public class IoTDeviceProtocol implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "协议名称")
  private String name;

  @Schema(description = "协议描述")
  private String description;

  @Schema(description = "协议状态")
  private Byte state;

  @Id
  @Schema(description = "协议ID")
  private String id;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Schema(description = "协议类型")
  private String type;

  @Schema(description = "协议配置")
  private String configuration;

  @Schema(description = "示例数据")
  private String example;

  /** 版本号 */
  @Column(name = "version")
  @Schema(description = "协议版本")
  private String version;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

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

  public ProtocolSupportDefinition toDefinition() {
    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    JSONObject object = JSONUtil.parseObj(getConfiguration());
    definition.setConfiguration(object);
    definition.setDescription(getDescription());
    definition.setId(getId());
    definition.setName(getName());
    definition.setProvider(object.getStr("provider"));
    definition.setType(getType());
    definition.setState(getState());
    if (object != null && object.containsKey("supportMethods")) {
      JSONArray jsonArray = object.getJSONArray("supportMethods");
      definition.setSupportMethods(
          jsonArray.stream()
              .map(
                  s -> {
                    return (String) s;
                  })
              .collect(Collectors.toSet()));
    }
    return definition;
  }

  public ProtocolSupportDefinition toDefinitionNoScript() {
    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    JSONObject object = JSONUtil.parseObj(getConfiguration());
    // 把几百行的内容删掉
    if (object != null && object.containsKey("location")) {
      object.remove("location");
    }
    definition.setConfiguration(object);
    definition.setDescription(getDescription());
    definition.setId(getId());
    definition.setName(getName());
    definition.setProvider(object.getStr("provider"));
    definition.setType(getType());
    definition.setState(getState());
    if (object != null && object.containsKey("supportMethods")) {
      JSONArray jsonArray = object.getJSONArray("supportMethods");
      definition.setSupportMethods(
          jsonArray.stream()
              .map(
                  s -> {
                    return (String) s;
                  })
              .collect(Collectors.toSet()));
    }
    return definition;
  }
}
