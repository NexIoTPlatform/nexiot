package cn.universal.security.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.IoTTenant;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class TenantBrandingResolver {

  public static final String DEFAULT_LOGIN_TEMPLATE = "default";

  private TenantBrandingResolver() {}

  public static TenantBranding resolveBranding(IoTTenant tenant, String host) {
    String defaultSiteName =
        StrUtil.blankToDefault(
            tenant == null ? null : tenant.getTenantName(), tenant == null ? null : tenant.getTenantCode());
    TenantBranding branding =
        new TenantBranding(
            defaultSiteName, defaultSiteName, null, null, DEFAULT_LOGIN_TEMPLATE, false, null);
    JSONObject cfg = parseCfg(tenant == null ? null : tenant.getCfg());
    if (cfg == null) {
      return branding;
    }
    JSONObject brandingConfig = cfg.getJSONObject("branding");
    if (brandingConfig == null) {
      return branding;
    }
    applyBranding(branding, brandingConfig);
    applyBranding(branding, brandingConfig.getJSONObject("default"));
    applyBranding(branding, resolveDomainBranding(brandingConfig.get("domains"), host));
    if (!StringUtils.hasText(branding.getSiteName())) {
      branding.setSiteName(defaultSiteName);
    }
    if (!StringUtils.hasText(branding.getLoginTitle())) {
      branding.setLoginTitle(branding.getSiteName());
    }
    if (!StringUtils.hasText(branding.getLoginTemplate())) {
      branding.setLoginTemplate(DEFAULT_LOGIN_TEMPLATE);
    }
    return branding;
  }

  private static JSONObject parseCfg(String rawCfg) {
    if (!StringUtils.hasText(rawCfg) || !JSONUtil.isTypeJSON(rawCfg)) {
      return null;
    }
    try {
      return JSONUtil.parseObj(rawCfg);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static Object resolveDomainBranding(Object domainsConfig, String host) {
    String normalizedHost = TenantDomainResolver.normalizeHost(host);
    if (!StringUtils.hasText(normalizedHost) || domainsConfig == null) {
      return null;
    }
    if (domainsConfig instanceof JSONObject jsonObject) {
      Object directValue = jsonObject.get(normalizedHost);
      if (directValue != null) {
        return directValue;
      }
      for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
        if (StrUtil.equals(TenantDomainResolver.normalizeHost(entry.getKey()), normalizedHost)) {
          return entry.getValue();
        }
      }
    }
    if (domainsConfig instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() != null
            && StrUtil.equals(
                TenantDomainResolver.normalizeHost(String.valueOf(entry.getKey())), normalizedHost)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }

  private static void applyBranding(TenantBranding branding, Object config) {
    if (branding == null || config == null) {
      return;
    }
    if (config instanceof CharSequence value) {
      String siteName = StrUtil.trimToNull(value.toString());
      if (StringUtils.hasText(siteName)) {
        branding.setSiteName(siteName);
        branding.setLoginTitle(siteName);
      }
      return;
    }

    JSONObject configObject = null;
    if (config instanceof JSONObject jsonObject) {
      configObject = jsonObject;
    } else if (config instanceof Map<?, ?> map) {
      configObject = JSONUtil.parseObj(map);
    }
    if (configObject == null) {
      return;
    }

    String siteName = StrUtil.trimToNull(configObject.getStr("siteName"));
    String loginTitle = StrUtil.trimToNull(configObject.getStr("loginTitle"));
    String siteLogo = StrUtil.trimToNull(configObject.getStr("siteLogo"));
    String siteIcon = StrUtil.trimToNull(configObject.getStr("siteIcon"));
    String loginTemplate = StrUtil.trimToNull(configObject.getStr("loginTemplate"));
    JSONObject watermark = configObject.getJSONObject("watermark");
    if (StringUtils.hasText(siteName)) {
      branding.setSiteName(siteName);
    }
    if (StringUtils.hasText(loginTitle)) {
      branding.setLoginTitle(loginTitle);
    } else if (StringUtils.hasText(siteName)) {
      branding.setLoginTitle(siteName);
    }
    if (StringUtils.hasText(loginTemplate)) {
      branding.setLoginTemplate(loginTemplate);
    }
    if (StringUtils.hasText(siteLogo)) {
      branding.setSiteLogo(siteLogo);
    }
    if (StringUtils.hasText(siteIcon)) {
      branding.setSiteIcon(siteIcon);
    }
    if (watermark != null) {
      branding.setWatermarkEnabled(watermark.getBool("enabled", false));
      branding.setWatermarkText(StrUtil.trimToNull(watermark.getStr("text")));
    }
  }

  public static final class TenantBranding {

    private String siteName;
    private String loginTitle;
    private String siteLogo;
    private String siteIcon;
    private String loginTemplate;
    private boolean watermarkEnabled;
    private String watermarkText;

    public TenantBranding(
        String siteName,
        String loginTitle,
        String siteLogo,
        String siteIcon,
        String loginTemplate,
        boolean watermarkEnabled,
        String watermarkText) {
      this.siteName = siteName;
      this.loginTitle = loginTitle;
      this.siteLogo = siteLogo;
      this.siteIcon = siteIcon;
      this.loginTemplate = loginTemplate;
      this.watermarkEnabled = watermarkEnabled;
      this.watermarkText = watermarkText;
    }

    public String getSiteName() {
      return siteName;
    }

    public void setSiteName(String siteName) {
      this.siteName = siteName;
    }

    public String getLoginTitle() {
      return loginTitle;
    }

    public void setLoginTitle(String loginTitle) {
      this.loginTitle = loginTitle;
    }

    public String getSiteLogo() {
      return siteLogo;
    }

    public void setSiteLogo(String siteLogo) {
      this.siteLogo = siteLogo;
    }

    public String getSiteIcon() {
      return siteIcon;
    }

    public void setSiteIcon(String siteIcon) {
      this.siteIcon = siteIcon;
    }

    public String getLoginTemplate() {
      return loginTemplate;
    }

    public void setLoginTemplate(String loginTemplate) {
      this.loginTemplate = loginTemplate;
    }

    public boolean isWatermarkEnabled() {
      return watermarkEnabled;
    }

    public void setWatermarkEnabled(boolean watermarkEnabled) {
      this.watermarkEnabled = watermarkEnabled;
    }

    public String getWatermarkText() {
      return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
      this.watermarkText = watermarkText;
    }
  }
}
