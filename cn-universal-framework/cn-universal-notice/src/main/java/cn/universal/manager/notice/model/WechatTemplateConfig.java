package cn.universal.manager.notice.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 每个通知模板独立保存微信模板标识、跳转和字段映射。 */
@Data
public class WechatTemplateConfig {
  private String templateId;
  private String url;
  private String miniProgramAppId;
  private String miniProgramPage;
  private List<Field> fields = new ArrayList<>();

  @Data
  public static class Field {
    private String key;
    private String label;
    private String value;
  }
}
