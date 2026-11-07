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
public class PushTransformPreviewVO implements Serializable {

  private static final long serialVersionUID = 1L;

  private Boolean success;

  private String inputJson;

  private String outputJson;

  private String message;
}
