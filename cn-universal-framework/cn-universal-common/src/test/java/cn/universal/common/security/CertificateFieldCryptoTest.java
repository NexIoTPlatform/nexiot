package cn.universal.common.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Test;

public class CertificateFieldCryptoTest {

  @After
  public void clearKey() {
    CertificateFieldCrypto.configure(null);
  }

  @Test
  public void encryptsWithRandomIvAndDecrypts() {
    CertificateFieldCrypto.configure("test-master-key");
    String first = CertificateFieldCrypto.encrypt("private-material");
    String second = CertificateFieldCrypto.encrypt("private-material");

    assertNotEquals(first, second);
    assertEquals("private-material", CertificateFieldCrypto.decrypt(first));
    assertEquals("private-material", CertificateFieldCrypto.decrypt(second));
  }

  @Test
  public void refusesNewSecretWritesWithoutExternalKey() {
    CertificateFieldCrypto.configure(null);
    try {
      CertificateFieldCrypto.encrypt("private-material");
      org.junit.Assert.fail("expected missing key failure");
    } catch (IllegalStateException expected) {
      // expected
    }
  }

  @Test
  public void preservesLegacyPlaintextForCompatibility() {
    CertificateFieldCrypto.configure(null);
    assertEquals("legacy-pem", CertificateFieldCrypto.decrypt("legacy-pem"));
  }
}
