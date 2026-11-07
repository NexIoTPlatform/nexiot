package cn.universal.persistence.dto;

import cn.universal.persistence.query.BasePage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@Schema(description = "IoT证书查询DTO")
public class IoTCertificateQueryDTO extends BasePage {

  @Schema(description = "证书名称")
  private String name;

  @Schema(description = "SSL密钥")
  private String sslKey;

  @Schema(description = "过期开始时间")
  private String expireStart;

  @Schema(description = "过期结束时间")
  private String expireEnd;

  @Schema(description = "页码")
  private Integer pageNum = 1;

  @Schema(description = "每页大小")
  private Integer pageSize = 10;
}
