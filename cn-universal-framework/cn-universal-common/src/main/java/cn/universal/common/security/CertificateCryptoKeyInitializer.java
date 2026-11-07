package cn.universal.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Bridges the external certificate master key into the persistence type handler. */
@Component
public class CertificateCryptoKeyInitializer {

  public CertificateCryptoKeyInitializer(
      @Value("${nexiot.security.certificate-master-key:}") String keySeed) {
    CertificateFieldCrypto.configure(keySeed);
  }
}
