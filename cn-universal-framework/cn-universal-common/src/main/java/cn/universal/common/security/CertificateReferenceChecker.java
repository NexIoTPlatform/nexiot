package cn.universal.common.security;

import java.util.List;

/** Extension point used by certificate owners to prevent deletion while still referenced. */
public interface CertificateReferenceChecker {

  List<String> findReferences(String sslKey);

  default void onCertificateChanged(String sslKey) {}
}
