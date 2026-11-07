package cn.universal.web.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class EmqxDeviceCredentialResolverTest {

  @Test
  public void resolveAliStyleUsername() {
    EmqxDeviceCredentialResolver.DeviceCredential credential =
        EmqxDeviceCredentialResolver.resolve(
            "abc12222&5eWKSeSBCz6s", "5eWKSeSBCz6s.abc12222");

    assertNotNull(credential);
    assertEquals("5eWKSeSBCz6s", credential.getProductKey());
    assertEquals("abc12222", credential.getDeviceId());
  }

  @Test
  public void resolveHtmlEscapedUsername() {
    EmqxDeviceCredentialResolver.DeviceCredential credential =
        EmqxDeviceCredentialResolver.resolve(
            "abc12222&amp;5eWKSeSBCz6s", "5eWKSeSBCz6s.abc12222");

    assertNotNull(credential);
    assertEquals("5eWKSeSBCz6s", credential.getProductKey());
    assertEquals("abc12222", credential.getDeviceId());
  }
}
