package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.common.security.CertificateReferenceChecker;
import cn.universal.persistence.entity.IoTCertificate;
import cn.universal.persistence.mapper.IoTCertificateMapper;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

@ExtendWith(MockitoExtension.class)
class CertificateServiceImplTest {

  @BeforeAll
  static void registerEntityTable() {
    EntityHelper.initEntityNameMap(IoTCertificate.class, new Config());
  }

  @Mock private IoTCertificateMapper certificateMapper;
  @Mock private ObjectProvider<CertificateReferenceChecker> referenceCheckers;
  @InjectMocks private CertificateServiceImpl service;

  @Test
  void metadataUpdateDoesNotEraseSecretMaterialWhenEditorSubmitsBlanks() {
    IoTCertificate existing =
        IoTCertificate.builder()
            .id(7L)
            .sslKey("opcua-bundle")
            .certificateType("OPCUA_BUNDLE")
            .trustMode("PINNED")
            .certContent("client-cert")
            .keyContent("client-key")
            .trustCertContent("server-cert")
            .keyPassword("key-password")
            .build();
    when(certificateMapper.selectOneByExample(any())).thenReturn(existing);
    when(referenceCheckers.orderedStream()).thenReturn(Stream.empty());

    IoTCertificate update =
        IoTCertificate.builder()
            .sslKey("opcua-bundle")
            .name("renamed")
            .certificateType("OPCUA_BUNDLE")
            .trustMode("PINNED")
            .certContent("  ")
            .keyContent("")
            .trustCertContent("\t")
            .certPassword("")
            .keyPassword(" ")
            .build();

    service.updateCertificate(update);

    ArgumentCaptor<IoTCertificate> captor = ArgumentCaptor.forClass(IoTCertificate.class);
    verify(certificateMapper).updateByPrimaryKeySelective(captor.capture());
    IoTCertificate persisted = captor.getValue();
    assertNull(persisted.getCertContent());
    assertNull(persisted.getKeyContent());
    assertNull(persisted.getTrustCertContent());
    assertNull(persisted.getCertPassword());
    assertNull(persisted.getKeyPassword());
  }
}
