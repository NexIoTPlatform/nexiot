package cn.universal.persistence.entity.vo;

import cn.universal.persistence.entity.IoTPushTransformRule;
import cn.universal.persistence.entity.bo.UPPushBO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTransformOverviewVO implements Serializable {

  private static final long serialVersionUID = 1L;

  private UPPushBO.TransformConfig config;

  @Schema(description = "当前应用唯一的转换规则")
  private IoTPushTransformRule rule;

  @Schema(description = "兼容旧版前端的规则列表，最多返回一条")
  private List<IoTPushTransformRule> rules;

  private List<PushTransformPresetVO> presets;
}
