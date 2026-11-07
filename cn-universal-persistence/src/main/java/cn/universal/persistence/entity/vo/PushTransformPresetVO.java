package cn.universal.persistence.entity.vo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTransformPresetVO implements Serializable {

  private static final long serialVersionUID = 1L;

  private String code;

  private String name;

  private String description;

  private Boolean implemented;
}
