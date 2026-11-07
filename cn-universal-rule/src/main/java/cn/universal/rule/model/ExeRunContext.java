package cn.universal.rule.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExeRunContext implements Serializable {

  private String trigger;
  private String target;
  private String targetName;
  private Object param;
  private Object result;
  private boolean success;
}
