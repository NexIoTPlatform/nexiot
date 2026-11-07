package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_push_transform_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "北向推送数据转换规则")
public class IoTPushTransformRule implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "app_unique_id")
  @Schema(description = "应用唯一标识")
  private String appUniqueId;

  @Column(name = "rule_name")
  @Schema(description = "规则名称")
  private String ruleName;

  @Column(name = "scope_type")
  @Schema(description = "作用范围：GLOBAL/PRODUCT")
  private String scopeType;

  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  @Column(name = "rule_type")
  @Schema(description = "规则类型：PRESET/TEMPLATE/SCRIPT")
  private String ruleType;

  @Column(name = "preset_code")
  @Schema(description = "内置格式编码")
  private String presetCode;

  @Schema(description = "模板内容或脚本内容")
  private String content;

  @Schema(description = "附加配置JSON")
  private String options;

  @Column(name = "sample_input")
  @Schema(description = "调试输入样例")
  private String sampleInput;

  @Column(name = "sample_output")
  @Schema(description = "调试输出样例")
  private String sampleOutput;

  @Schema(description = "状态：0-草稿，1-已发布，2-停用")
  private Integer status;

  @Schema(description = "版本号")
  private Integer version;

  @Schema(description = "删除标记")
  private Integer deleted;

  @Column(name = "create_by")
  @Schema(description = "创建人")
  private String createBy;

  @Column(name = "update_by")
  @Schema(description = "更新人")
  private String updateBy;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Column(name = "publish_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "发布时间")
  private Date publishTime;
}
