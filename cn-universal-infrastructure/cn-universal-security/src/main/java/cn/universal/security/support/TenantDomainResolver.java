package cn.universal.security.support;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.ServletUtils;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.mapper.IoTTenantMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

@Component
public class TenantDomainResolver {

  @Resource private IoTTenantMapper ioTTenantMapper;

  public IoTTenant resolveCurrentTenant() {
    return resolveTenantByHost(null);
  }

  public IoTTenant resolveTenantByHost(String host) {
    String normalizedHost = normalizeHost(StrUtil.blankToDefault(host, resolveCurrentRequestHost()));
    if (!StringUtils.hasText(normalizedHost)) {
      return null;
    }
    List<IoTTenant> matchedTenants = new ArrayList<>();
    for (IoTTenant tenant : selectActiveTenants()) {
      if (tenant != null && isTenantAvailableForLogin(tenant) && extractTenantHosts(tenant).contains(normalizedHost)) {
        matchedTenants.add(tenant);
      }
    }
    if (matchedTenants.isEmpty()) {
      return null;
    }
    if (matchedTenants.size() > 1) {
      throw new IoTException("当前域名已绑定多个租户，请联系管理员检查租户域名配置");
    }
    return matchedTenants.get(0);
  }

  public void assertHostBindingsUnique(IoTTenant tenant, String excludeTenantId) {
    if (tenant == null) {
      return;
    }
    Set<String> currentHosts = extractTenantHosts(tenant);
    if (CollectionUtils.isEmpty(currentHosts)) {
      return;
    }
    for (IoTTenant existed : selectActiveTenants()) {
      if (existed == null || StrUtil.equals(existed.getTenantId(), excludeTenantId)) {
        continue;
      }
      Set<String> existedHosts = extractTenantHosts(existed);
      if (CollectionUtils.isEmpty(existedHosts)) {
        continue;
      }
      for (String currentHost : currentHosts) {
        if (existedHosts.contains(currentHost)) {
          throw new IoTException(
              "域名 "
                  + currentHost
                  + " 已绑定租户 "
                  + StrUtil.blankToDefault(existed.getTenantName(), existed.getTenantCode()));
        }
      }
    }
  }

  public Set<String> extractTenantHosts(IoTTenant tenant) {
    LinkedHashSet<String> hosts = new LinkedHashSet<>();
    if (tenant == null) {
      return hosts;
    }
    collectHosts(hosts, tenant.getWebsite());
    collectHosts(hosts, tenant.getDomain());
    return hosts;
  }

  public String normalizeBindingValue(String rawValue) {
    if (rawValue == null) {
      return null;
    }
    LinkedHashSet<String> hosts = new LinkedHashSet<>();
    collectHosts(hosts, rawValue);
    return hosts.isEmpty() ? "" : String.join(",", hosts);
  }

  public String resolveCurrentRequestHost() {
    HttpServletRequest request = currentRequest();
    if (request == null) {
      return null;
    }
    String forwardedHost = firstForwardedHost(request.getHeader("X-Forwarded-Host"));
    if (StringUtils.hasText(forwardedHost)) {
      return forwardedHost;
    }
    String originalHost = firstForwardedHost(request.getHeader("X-Original-Host"));
    if (StringUtils.hasText(originalHost)) {
      return originalHost;
    }
    String host = request.getHeader("Host");
    if (StringUtils.hasText(host)) {
      return firstForwardedHost(host);
    }
    return request.getServerName();
  }

  public static String normalizeHost(String rawHost) {
    if (!StringUtils.hasText(rawHost)) {
      return null;
    }
    String host = rawHost.trim();
    if (!StringUtils.hasText(host)) {
      return null;
    }
    host = firstForwardedHost(host);
    if (host.startsWith("//")) {
      host = "https:" + host;
    }
    if (host.contains("://")) {
      try {
        host = URI.create(host).getHost();
      } catch (Exception ignored) {
        host = stripHostDecorations(host);
      }
    } else {
      host = stripHostDecorations(host);
    }
    if (!StringUtils.hasText(host)) {
      return null;
    }
    if (host.startsWith("[") && host.endsWith("]")) {
      host = host.substring(1, host.length() - 1);
    }
    return host.toLowerCase(Locale.ROOT);
  }

  private List<IoTTenant> selectActiveTenants() {
    Example example = new Example(IoTTenant.class);
    example.createCriteria().andEqualTo("deleted", 0);
    example.setOrderByClause("create_time asc");
    return ioTTenantMapper.selectByExample(example);
  }

  private boolean isTenantAvailableForLogin(IoTTenant tenant) {
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
      return false;
    }
    if (Integer.valueOf(1).equals(tenant.getStatus())) {
      return false;
    }
    return tenant.getExpireTime() == null || !tenant.getExpireTime().before(new Date());
  }

  private void collectHosts(Set<String> hosts, String rawValues) {
    if (!StringUtils.hasText(rawValues)) {
      return;
    }
    String[] tokens = rawValues.split("[,;\\n\\r\\t ]+");
    for (String token : tokens) {
      String normalized = normalizeHost(token);
      if (StringUtils.hasText(normalized)) {
        hosts.add(normalized);
      }
    }
  }

  private HttpServletRequest currentRequest() {
    try {
      return ServletUtils.getRequest();
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String firstForwardedHost(String rawHostHeader) {
    if (!StringUtils.hasText(rawHostHeader)) {
      return null;
    }
    String[] hosts = rawHostHeader.split(",");
    return hosts.length == 0 ? null : hosts[0].trim();
  }

  private static String stripHostDecorations(String rawHost) {
    String host = rawHost.trim();
    int schemeIndex = host.indexOf("://");
    if (schemeIndex >= 0) {
      host = host.substring(schemeIndex + 3);
    }
    if (host.startsWith("//")) {
      host = host.substring(2);
    }
    int slashIndex = host.indexOf('/');
    if (slashIndex >= 0) {
      host = host.substring(0, slashIndex);
    }
    int questionIndex = host.indexOf('?');
    if (questionIndex >= 0) {
      host = host.substring(0, questionIndex);
    }
    int hashIndex = host.indexOf('#');
    if (hashIndex >= 0) {
      host = host.substring(0, hashIndex);
    }
    int atIndex = host.lastIndexOf('@');
    if (atIndex >= 0) {
      host = host.substring(atIndex + 1);
    }
    if (host.startsWith("[")) {
      int rightBracket = host.indexOf(']');
      return rightBracket > 0 ? host.substring(0, rightBracket + 1) : host;
    }
    int colonIndex = host.indexOf(':');
    return colonIndex >= 0 ? host.substring(0, colonIndex) : host;
  }
}
