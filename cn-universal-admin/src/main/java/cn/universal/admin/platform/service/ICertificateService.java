package cn.universal.admin.platform.service;

import cn.universal.persistence.entity.IoTCertificate;
import cn.universal.persistence.dto.IoTCertificateOptionDTO;
import java.util.List;

public interface ICertificateService {

  IoTCertificate getBySslKey(String sslKey);

  List<IoTCertificate> listAll();

  void addCertificate(IoTCertificate cert);

  void updateCertificate(IoTCertificate cert);

  void deleteCertificate(Long id);

  List<String> findReferences(String sslKey);

  List<IoTCertificateOptionDTO> listOptions(
      String protocol, String currentUnionId, String currentTenantId, boolean isAdmin);

  List<IoTCertificate> listByUser(String userId);

  List<IoTCertificate> searchCertificates(
      String name,
      String userId,
      String sslKey,
      String expireStart,
      String expireEnd,
      boolean isAdmin);
}
