package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前租户平台品牌配置")
public class TenantBrandingBO {

  @Schema(description = "平台名称")
  private String siteName;

  @Schema(description = "平台 Logo")
  private String siteLogo;

  @Schema(description = "浏览器图标")
  private String siteIcon;

  @Schema(description = "是否启用页面水印")
  private Boolean watermarkEnabled;

  @Schema(description = "页面水印文字")
  private String watermarkText;
}
