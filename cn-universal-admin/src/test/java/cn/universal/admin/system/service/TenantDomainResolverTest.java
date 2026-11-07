package cn.universal.admin.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.security.support.TenantDomainResolver;
import org.junit.jupiter.api.Test;

class TenantDomainResolverTest {

  @Test
  void normalizeHostShouldStripProtocolPortAndPath() {
    assertEquals(
        "tenant.example.com",
        TenantDomainResolver.normalizeHost("https://Tenant.Example.com:8443/login/index.html?x=1"));
  }

  @Test
  void normalizeBindingValueShouldDeduplicateHosts() {
    TenantDomainResolver resolver = new TenantDomainResolver();

    assertEquals(
        "tenant.example.com,portal.example.com",
        resolver.normalizeBindingValue(
            "https://tenant.example.com/app,\nportal.example.com/home tenant.example.com:8080"));
  }
}
