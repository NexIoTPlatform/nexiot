package cn.universal.persistence.dto;

import java.util.Date;

/** Secret-free certificate selector item. */
public record IoTCertificateOptionDTO(
    String sslKey,
    String name,
    String certificateType,
    String trustMode,
    boolean hasPrivateKey,
    boolean hasTrustCertificate,
    Date expireTime,
    boolean expired,
    String fingerprintSha256) {}
