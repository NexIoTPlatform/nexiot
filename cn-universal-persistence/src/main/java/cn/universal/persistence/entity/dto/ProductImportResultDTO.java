package cn.universal.persistence.entity.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 产品配置整批导入结果，仅在事务提交成功后返回。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportResultDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  private int productCount;
  private int protocolCount;
  private int networkCount;
  private int metadataVersionCount;
}
