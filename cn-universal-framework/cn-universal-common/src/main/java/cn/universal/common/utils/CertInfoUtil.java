package cn.universal.common.utils;

import cn.hutool.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CertInfoUtil {

  public static JSONObject extractCertInfo(String certContent) {
    List<X509Certificate> certs = parseCertificateChain(certContent);
    X509Certificate cert = certs.get(0);
    JSONObject info = new JSONObject();
    info.set("subject", cert.getSubjectX500Principal().getName());
    info.set("issuer", cert.getIssuerX500Principal().getName());
    info.set("notBefore", cert.getNotBefore());
    info.set("notAfter", cert.getNotAfter());
    info.set("serialNumber", cert.getSerialNumber().toString(16));
    info.set("signatureAlgorithm", cert.getSigAlgName());
    info.set("version", cert.getVersion());
    info.set("publicKeyAlgorithm", cert.getPublicKey().getAlgorithm());
    info.set("chainLength", certs.size());
    // 解析SAN
    try {
      Collection<List<?>> sans = cert.getSubjectAlternativeNames();
      if (sans != null) {
        List<String> sanList = new ArrayList<>();
        for (List<?> san : sans) {
          sanList.add(san.get(1).toString());
        }
        info.set("subjectAlternativeNames", sanList);
      }
    } catch (Exception ignore) {
    }
    // KeyUsage
    boolean[] keyUsage = cert.getKeyUsage();
    if (keyUsage != null) {
      info.set("keyUsage", Arrays.toString(keyUsage));
    }
    // ExtendedKeyUsage
    try {
      List<String> extKeyUsage = cert.getExtendedKeyUsage();
      if (extKeyUsage != null) {
        info.set("extendedKeyUsage", extKeyUsage);
      }
    } catch (Exception ignore) {
    }
    return info;
  }

  /** 自动提取证书支持的所有域名（SAN、CN、通配符） */
  public static List<String> parseDomainsFromCert(String certContent) {
    List<String> domains = new ArrayList<>();
    List<X509Certificate> certs = parseCertificateChain(certContent);
    if (certs.isEmpty()) {
      return domains;
    }
    X509Certificate cert = certs.get(0);
    // 1. 解析SAN
    try {
      Collection<List<?>> sans = cert.getSubjectAlternativeNames();
      if (sans != null) {
        for (List<?> san : sans) {
          if (san.size() > 1 && san.get(1) != null) {
            domains.add(san.get(1).toString());
          }
        }
      }
    } catch (Exception ignore) {
    }
    // 2. 解析CN
    try {
      String subject = cert.getSubjectX500Principal().getName();
      for (String part : subject.split(",")) {
        if (part.trim().startsWith("CN=")) {
          String cn = part.trim().substring(3);
          if (!domains.contains(cn)) {
            domains.add(cn);
          }
        }
      }
    } catch (Exception ignore) {
    }
    return domains;
  }

  public static List<X509Certificate> parseCertificateChain(String pem) {
    List<X509Certificate> certs = new ArrayList<>();
    try {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      try (ByteArrayInputStream bis = new ByteArrayInputStream(pem.getBytes())) {
        while (bis.available() > 0) {
          Certificate cert = cf.generateCertificate(bis);
          if (cert instanceof X509Certificate) {
            certs.add((X509Certificate) cert);
          }
        }
      }
    } catch (Exception e) {
      log.warn("证书校验失败");
    }
    return certs;
  }
}
