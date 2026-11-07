package cn.universal.persistence.entity.vo;

import lombok.Data;

/** 产品选择器使用的轻量选项，不包含物模型、配置或设备统计。 */
@Data
public class IoTProductOptionVO {

  private Long id;
  private String productKey;
  private String name;
}
