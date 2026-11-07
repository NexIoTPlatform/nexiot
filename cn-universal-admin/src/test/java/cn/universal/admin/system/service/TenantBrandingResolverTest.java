package cn.universal.admin.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.persistence.entity.IoTTenant;
import cn.universal.security.support.TenantBrandingResolver;
import org.junit.jupiter.api.Test;

class TenantBrandingResolverTest {

  @Test
  void resolveBrandingShouldFallbackToTenantNameWhenCfgMissing() {
    IoTTenant tenant = IoTTenant.builder().tenantCode("qinhaohao").tenantName("秦浩浩物联网平台").build();

    TenantBrandingResolver.TenantBranding branding =
        TenantBrandingResolver.resolveBranding(tenant, "iot.qinhaohao.com");

    assertEquals("秦浩浩物联网平台", branding.getSiteName());
    assertEquals("秦浩浩物联网平台", branding.getLoginTitle());
    assertEquals(TenantBrandingResolver.DEFAULT_LOGIN_TEMPLATE, branding.getLoginTemplate());
    assertEquals(false, branding.isWatermarkEnabled());
  }

  @Test
  void resolveBrandingShouldPreferHostSpecificBranding() {
    IoTTenant tenant =
        IoTTenant.builder()
            .tenantCode("qinhaohao")
            .tenantName("秦浩浩物联网平台")
            .cfg(
                """
                {
                  "branding": {
                    "siteName": "秦浩浩默认站点",
                    "loginTitle": "秦浩浩默认登录",
                    "loginTemplate": "glow",
                    "domains": {
                      "https://iot.qinhaohao.com/login": {
                        "siteName": "秦浩浩二级域名站点",
                        "loginTitle": "秦浩浩二级域名登录",
                        "loginTemplate": "contrast"
                      },
                      "portal.iot.qinhaohao.com": "秦浩浩三级域名站点"
                    }
                  }
                }
                """)
            .build();

    TenantBrandingResolver.TenantBranding secondLevelBranding =
        TenantBrandingResolver.resolveBranding(tenant, "https://iot.qinhaohao.com:443/index.html");
    TenantBrandingResolver.TenantBranding thirdLevelBranding =
        TenantBrandingResolver.resolveBranding(tenant, "portal.iot.qinhaohao.com");

    assertEquals("秦浩浩二级域名站点", secondLevelBranding.getSiteName());
    assertEquals("秦浩浩二级域名登录", secondLevelBranding.getLoginTitle());
    assertEquals("contrast", secondLevelBranding.getLoginTemplate());
    assertEquals("秦浩浩三级域名站点", thirdLevelBranding.getSiteName());
    assertEquals("秦浩浩三级域名站点", thirdLevelBranding.getLoginTitle());
    assertEquals("glow", thirdLevelBranding.getLoginTemplate());
  }

  @Test
  void resolveBrandingShouldReadDefaultWatermark() {
    IoTTenant tenant =
        IoTTenant.builder()
            .tenantName("演示租户")
            .cfg(
                """
                {
                  "branding": {
                    "default": {
                      "siteName": "演示平台",
                      "watermark": { "enabled": true, "text": "内部演示" }
                    }
                  }
                }
                """)
            .build();

    TenantBrandingResolver.TenantBranding branding =
        TenantBrandingResolver.resolveBranding(tenant, null);

    assertEquals(true, branding.isWatermarkEnabled());
    assertEquals("内部演示", branding.getWatermarkText());
  }
}
