package cn.universal.admin.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.hutool.json.JSONObject;
import cn.universal.admin.platform.service.ICertificateService;
import cn.universal.common.security.CertificateReferenceChecker;
import cn.universal.common.utils.CertInfoUtil;
import cn.universal.persistence.dto.IoTCertificateOptionDTO;
import cn.universal.persistence.entity.IoTCertificate;
import cn.universal.persistence.mapper.IoTCertificateMapper;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

@Service
public class CertificateServiceImpl extends BaseServiceImpl implements ICertificateService {

  private static final Logger log = LoggerFactory.getLogger(CertificateServiceImpl.class);

  @Autowired private IoTCertificateMapper certificateMapper;
  @Autowired private ObjectProvider<CertificateReferenceChecker> referenceCheckers;

  @Override
  public IoTCertificate getBySslKey(String sslKey) {
    Example example = new Example(IoTCertificate.class);
    example.createCriteria().andEqualTo("sslKey", sslKey);
    return certificateMapper.selectOneByExample(example);
  }

  @Override
  public List<IoTCertificate> listAll() {
    return certificateMapper.selectAll();
  }

  @Override
  public void addCertificate(IoTCertificate cert) {
    prepareCertificate(cert, null);
    if (getBySslKey(cert.getSslKey()) != null) {
      throw new IllegalArgumentException("sslKey已存在: " + cert.getSslKey());
    }
    fillCertInfo(cert);
    certificateMapper.insertSelective(cert);
    notifyCertificateChanged(cert.getSslKey());
  }

  @Override
  public void updateCertificate(IoTCertificate cert) {
    IoTCertificate old = cert == null ? null : getBySslKey(cert.getSslKey());
    if (old == null) {
      throw new IllegalArgumentException("证书不存在");
    }
    cert.setId(old.getId());
    prepareCertificate(cert, old);
    fillCertInfo(cert);
    certificateMapper.updateByPrimaryKeySelective(cert);
    notifyCertificateChanged(cert.getSslKey());
  }

  @Override
  public void deleteCertificate(Long id) {
    IoTCertificate certificate = certificateMapper.selectByPrimaryKey(id);
    if (certificate == null) {
      return;
    }
    List<String> references = findReferences(certificate.getSslKey());
    if (!references.isEmpty()) {
      throw new IllegalStateException("证书仍被引用: " + String.join(", ", references));
    }
    certificateMapper.deleteByPrimaryKey(id);
  }

  @Override
  public List<String> findReferences(String sslKey) {
    if (StrUtil.isBlank(sslKey)) {
      return List.of();
    }
    List<String> references = new ArrayList<>();
    referenceCheckers.orderedStream()
        .forEach(checker -> references.addAll(checker.findReferences(sslKey)));
    return references.stream().filter(StrUtil::isNotBlank).distinct().toList();
  }

  @Override
  public List<IoTCertificateOptionDTO> listOptions(
      String protocol, String currentUnionId, String currentTenantId, boolean isAdmin) {
    if (StrUtil.isNotBlank(protocol) && !"OPCUA".equalsIgnoreCase(protocol.trim())) {
      return List.of();
    }
    Date now = new Date();
    return certificateMapper.selectAll().stream()
        .filter(cert -> isAdmin || canAccessCertificate(cert, currentUnionId, currentTenantId))
        .filter(cert -> "OPCUA_BUNDLE".equalsIgnoreCase(cert.getCertificateType()))
        .filter(cert -> StrUtil.isNotBlank(cert.getKeyContent()))
        .filter(cert -> StrUtil.isNotBlank(cert.getTrustCertContent()))
        .filter(cert -> "PINNED".equalsIgnoreCase(cert.getTrustMode()) || "CA".equalsIgnoreCase(cert.getTrustMode()))
        .map(cert -> new IoTCertificateOptionDTO(
            cert.getSslKey(), cert.getName(), cert.getCertificateType(), cert.getTrustMode(),
            true, true, cert.getExpireTime(), cert.getExpireTime() != null && cert.getExpireTime().before(now),
            cert.getFingerprintSha256()))
        .toList();
  }

  @Override
  public List<IoTCertificate> listByUser(String userId) {
    Example example = new Example(IoTCertificate.class);
    example.createCriteria().andEqualTo("createUser", userId);
    return certificateMapper.selectByExample(example);
  }

  @Override
  public List<IoTCertificate> searchCertificates(
      String name,
      String userId,
      String sslKey,
      String expireStart,
      String expireEnd,
      boolean isAdmin) {
    Example example = new Example(IoTCertificate.class);
    Example.Criteria c = example.createCriteria();
    if (name != null && !name.isEmpty()) {
      c.andLike("name", "%" + name + "%");
    }
    if (sslKey != null && !sslKey.isEmpty()) {
      c.andEqualTo("sslKey", sslKey);
    }
    if (expireStart != null && !expireStart.isEmpty()) {
      c.andGreaterThanOrEqualTo("expireTime", expireStart);
    }
    if (expireEnd != null && !expireEnd.isEmpty()) {
      c.andLessThanOrEqualTo("expireTime", expireEnd);
    }
    example.orderBy("createTime").desc();
    List<IoTCertificate> certificates = certificateMapper.selectByExample(example);
    if (isAdmin || StrUtil.isBlank(userId)) {
      return certificates;
    }
    String currentTenantId = resolveTenantId(userId);
    return certificates.stream()
        .filter(cert -> canAccessCertificate(cert, userId, currentTenantId))
        .toList();
  }

  private boolean canAccessCertificate(
      IoTCertificate certificate, String currentUnionId, String currentTenantId) {
    if (certificate == null) {
      return false;
    }
    String resourceTenantId = resolveTenantId(certificate.getCreateUser());
    if (StrUtil.isBlank(resourceTenantId)) {
      return StrUtil.equals(currentUnionId, certificate.getCreateUser());
    }
    return StrUtil.equals(currentTenantId, resourceTenantId)
        || StrUtil.equals(currentUnionId, certificate.getCreateUser());
  }

  /** 自动提取证书详细信息并填充到certInfo字段 */
  private void fillCertInfo(IoTCertificate cert) {
    if (cert == null || cert.getCertContent() == null) {
      return;
    }
    try {
      JSONObject jsonObject = CertInfoUtil.extractCertInfo(cert.getCertContent());
      if (jsonObject != null) {
        cert.setCertInfo(jsonObject.toString());
        if (jsonObject.getDate("notAfter") != null) {
          cert.setExpireTime(jsonObject.getDate("notAfter"));
        }
        cert.setFingerprintSha256(fingerprint(CertInfoUtil.parseCertificateChain(cert.getCertContent()).get(0)));
      } else {
        log.warn("证书详细信息提取失败: sslKey={}", cert.getSslKey());
      }
    } catch (Exception e) {
      if ("OPCUA_BUNDLE".equalsIgnoreCase(cert.getCertificateType())) {
        throw new IllegalArgumentException("客户端证书解析失败: " + e.getMessage(), e);
      }
      log.warn("证书详细信息提取异常: sslKey={}, err={}", cert.getSslKey(), e.getMessage());
    }
    if (StrUtil.isNotBlank(cert.getTrustCertContent())) {
      try {
        List<X509Certificate> certificates = CertInfoUtil.parseCertificateChain(cert.getTrustCertContent());
        if (certificates.isEmpty()) {
          throw new IllegalArgumentException("信任证书内容不是有效的X.509证书");
        }
        if ("PINNED".equalsIgnoreCase(cert.getTrustMode()) && certificates.size() != 1) {
          throw new IllegalArgumentException("PINNED模式必须且只能上传一张服务端证书");
        }
        if ("CA".equalsIgnoreCase(cert.getTrustMode())
            && certificates.stream().noneMatch(item -> item.getBasicConstraints() >= 0)) {
          throw new IllegalArgumentException("CA模式必须上传至少一张CA证书");
        }
        cert.setTrustFingerprintSha256(fingerprint(certificates.get(0)));
      } catch (Exception ex) {
        throw new IllegalArgumentException("信任证书解析失败: " + ex.getMessage(), ex);
      }
    }
  }

  private void prepareCertificate(IoTCertificate cert, IoTCertificate old) {
    if (cert == null || StrUtil.isBlank(cert.getSslKey())) {
      throw new IllegalArgumentException("sslKey不能为空");
    }
    // Secret-free detail responses make editors submit empty strings. Treat blanks as
    // "unchanged" so metadata-only edits can never erase the persisted key material.
    cert.setCertContent(blankToNull(cert.getCertContent()));
    cert.setKeyContent(blankToNull(cert.getKeyContent()));
    cert.setTrustCertContent(blankToNull(cert.getTrustCertContent()));
    cert.setCertPassword(blankToNull(cert.getCertPassword()));
    cert.setKeyPassword(blankToNull(cert.getKeyPassword()));
    String type = firstNonBlank(cert.getCertificateType(), old == null ? null : old.getCertificateType(), "GENERIC");
    type = type.trim().toUpperCase(Locale.ROOT);
    if (!List.of("GENERIC", "CLIENT", "CA", "SERVER", "OPCUA_BUNDLE").contains(type)) {
      throw new IllegalArgumentException("不支持的证书类型: " + type);
    }
    cert.setCertificateType(type);
    if (!"OPCUA_BUNDLE".equals(type)) {
      return;
    }
    String trustMode = firstNonBlank(cert.getTrustMode(), old == null ? null : old.getTrustMode(), null);
    if (StrUtil.isBlank(trustMode)) {
      throw new IllegalArgumentException("OPC UA证书包必须设置trustMode");
    }
    trustMode = trustMode.trim().toUpperCase(Locale.ROOT);
    if (!List.of("PINNED", "CA").contains(trustMode)) {
      throw new IllegalArgumentException("trustMode必须为PINNED或CA");
    }
    cert.setTrustMode(trustMode);
    requireMaterial("客户端证书", cert.getCertContent(), old == null ? null : old.getCertContent());
    requireMaterial("客户端私钥", cert.getKeyContent(), old == null ? null : old.getKeyContent());
    requireMaterial("服务端信任证书", cert.getTrustCertContent(), old == null ? null : old.getTrustCertContent());
  }

  private void requireMaterial(String name, String updated, String existing) {
    if (StrUtil.isBlank(updated) && StrUtil.isBlank(existing)) {
      throw new IllegalArgumentException("OPC UA证书包缺少" + name);
    }
  }

  private String firstNonBlank(String first, String second, String fallback) {
    if (StrUtil.isNotBlank(first)) {
      return first;
    }
    if (StrUtil.isNotBlank(second)) {
      return second;
    }
    return fallback;
  }

  private String blankToNull(String value) {
    return StrUtil.isBlank(value) ? null : value;
  }

  private String fingerprint(X509Certificate certificate) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
    StringBuilder result = new StringBuilder(digest.length * 2);
    for (byte value : digest) {
      result.append(String.format("%02X", value));
    }
    return result.toString();
  }

  private void notifyCertificateChanged(String sslKey) {
    referenceCheckers.orderedStream().forEach(checker -> checker.onCertificateChanged(sslKey));
  }
}
