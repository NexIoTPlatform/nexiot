package cn.universal.persistence.entity.bo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPushConfigEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private String appUniqueId;

  private String reason;

  private Long ruleId;
}
