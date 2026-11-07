/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.mqtt.protocol.ssl;

import cn.hutool.core.util.StrUtil;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.persistence.entity.IoTCertificate;
import cn.universal.persistence.mapper.IoTCertificateMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/** MQTT TLS/SSL helper shared by system MQTT and third-party MQTT clients. */
@Slf4j(topic = "mqtt")
@Service
public class MqttTlsSupport {

  private static final Pattern PRIVATE_KEY_BLOCK =
      Pattern.compile(
          "-----BEGIN ([A-Z ]*PRIVATE KEY)-----([\\s\\S]*?)-----END \\1-----",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern CERTIFICATE_BLOCK =
      Pattern.compile(
          "-----BEGIN CERTIFICATE-----([\\s\\S]*?)-----END CERTIFICATE-----",
          Pattern.CASE_INSENSITIVE);
  private static final char[] EMPTY_PASSWORD = new char[0];
  private static final byte[] PKCS8_RSA_ALGORITHM_IDENTIFIER =
      hex("300d06092a864886f70d0101010500");

  @Autowired(required = false)
  private IoTCertificateMapper certificateMapper;

  public String resolveBrokerUri(String brokerUri, MQTTProductConfig config) {
    if (StrUtil.isBlank(brokerUri)) {
      return brokerUri;
    }
    String uri = brokerUri.trim();
    if (StrUtil.startWithIgnoreCase(uri, "mqtts://")) {
      return "ssl://" + uri.substring("mqtts://".length());
    }
    if (isTlsEnabled(config) && StrUtil.startWithIgnoreCase(uri, "tcp://")) {
      return "ssl://" + uri.substring("tcp://".length());
    }
    return uri;
  }

  public boolean isTlsEnabled(MQTTProductConfig config) {
    if (config == null) {
      return false;
    }
    return config.isSsl()
        || isTlsScheme(config.getHost())
        || resolveMode(config) != SslMode.DISABLED;
  }

  public void applyTls(MqttConnectionOptions options, MQTTProductConfig config, String context) {
    if (!isTlsEnabled(config)) {
      return;
    }
    SslMode mode = resolveMode(config);
    options.setHttpsHostnameVerificationEnabled(config.isSslHostnameVerify());
    if (!config.isSslHostnameVerify()) {
      options.setSSLHostnameVerifier((hostname, session) -> true);
    }

    try {
      TlsMaterial caMaterial = resolveMaterial(firstNonBlank(config.getSslCaCertKey(), config.getSslCaPath()));
      TlsMaterial clientMaterial = resolveClientMaterial(config);
      boolean hasCustomCa = caMaterial.hasCertificate();
      boolean hasClientMaterial = clientMaterial.hasCertificate() || clientMaterial.hasPrivateKey();

      if (mode == SslMode.MUTUAL_AUTH && !clientMaterial.isCompleteClientMaterial()) {
        throw new IllegalStateException("MQTT双向TLS需要配置客户端证书和私钥");
      }
      if (hasCustomCa || hasClientMaterial) {
        options.setSocketFactory(buildSocketFactory(caMaterial, clientMaterial, mode));
      }
      log.info(
          "[MQTT_TLS] TLS启用: context={}, mode={}, customCa={}, clientCert={}, hostnameVerify={}",
          context,
          mode,
          hasCustomCa,
          clientMaterial.isCompleteClientMaterial(),
          config.isSslHostnameVerify());
    } catch (Exception e) {
      throw new IllegalStateException("MQTT TLS配置初始化失败: " + context, e);
    }
  }

  private SSLSocketFactory buildSocketFactory(
      TlsMaterial caMaterial, TlsMaterial clientMaterial, SslMode mode) throws Exception {
    TrustManager[] trustManagers = null;
    if (caMaterial.hasCertificate()) {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      List<X509Certificate> certificates = parseCertificates(caMaterial.certificateContent);
      for (int i = 0; i < certificates.size(); i++) {
        trustStore.setCertificateEntry("ca-" + i, certificates.get(i));
      }
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);
      trustManagers = trustManagerFactory.getTrustManagers();
    }

    KeyManager[] keyManagers = null;
    if (mode == SslMode.MUTUAL_AUTH || clientMaterial.isCompleteClientMaterial()) {
      PrivateKey privateKey =
          parsePrivateKey(clientMaterial.privateKeyContent, clientMaterial.passwordChars());
      List<X509Certificate> certificates = parseCertificates(clientMaterial.certificateContent);
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStore.setKeyEntry(
          "mqtt-client",
          privateKey,
          clientMaterial.passwordChars(),
          certificates.toArray(new Certificate[0]));
      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, clientMaterial.passwordChars());
      keyManagers = keyManagerFactory.getKeyManagers();
    }

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagers, trustManagers, new SecureRandom());
    return sslContext.getSocketFactory();
  }

  private TlsMaterial resolveClientMaterial(MQTTProductConfig config) throws IOException {
    String clientCertSource = firstNonBlank(config.getSslClientCertKey(), config.getSslCertPath());
    TlsMaterial material = resolveMaterial(clientCertSource);
    if (material.hasCertificate() && material.hasPrivateKey()) {
      return material;
    }
    String keySource = config.getSslKeyPath();
    if (StrUtil.isBlank(keySource)) {
      return material;
    }
    TlsMaterial keyMaterial = resolveMaterial(keySource);
    if (!material.hasCertificate() && keyMaterial.hasCertificate()) {
      material.certificateContent = keyMaterial.certificateContent;
    }
    if (!material.hasPrivateKey() && keyMaterial.hasPrivateKey()) {
      material.privateKeyContent = keyMaterial.privateKeyContent;
      material.keyPassword = keyMaterial.keyPassword;
    }
    return material;
  }

  private TlsMaterial resolveMaterial(String source) throws IOException {
    if (StrUtil.isBlank(source)) {
      return new TlsMaterial();
    }
    String materialSource = source.trim();
    if (isPemContent(materialSource)) {
      return materialFromPem(materialSource, null);
    }
    IoTCertificate certificate = findCertificate(materialSource);
    if (certificate != null) {
      return new TlsMaterial(
          certificate.getCertContent(), certificate.getKeyContent(), certificate.getKeyPassword());
    }
    Path path = Path.of(materialSource);
    if (Files.isRegularFile(path)) {
      return materialFromPem(Files.readString(path, StandardCharsets.UTF_8), null);
    }
    throw new IllegalArgumentException("证书或私钥来源不存在: " + materialSource);
  }

  private TlsMaterial materialFromPem(String pem, String keyPassword) {
    return new TlsMaterial(extractCertificateBlocks(pem), extractPrivateKeyBlock(pem), keyPassword);
  }

  private boolean isPemContent(String content) {
    return StrUtil.containsIgnoreCase(content, "-----BEGIN CERTIFICATE-----")
        || PRIVATE_KEY_BLOCK.matcher(content).find();
  }

  private String extractCertificateBlocks(String pem) {
    Matcher matcher = CERTIFICATE_BLOCK.matcher(pem);
    StringBuilder builder = new StringBuilder();
    while (matcher.find()) {
      if (builder.length() > 0) {
        builder.append(System.lineSeparator());
      }
      builder.append(matcher.group());
    }
    return builder.toString();
  }

  private String extractPrivateKeyBlock(String pem) {
    Matcher matcher = PRIVATE_KEY_BLOCK.matcher(pem);
    return matcher.find() ? matcher.group() : null;
  }

  private IoTCertificate findCertificate(String sslKey) {
    if (certificateMapper == null || StrUtil.isBlank(sslKey)) {
      return null;
    }
    try {
      Example example = new Example(IoTCertificate.class);
      example.createCriteria().andEqualTo("sslKey", sslKey);
      return certificateMapper.selectOneByExample(example);
    } catch (Exception e) {
      log.debug("[MQTT_TLS] 查询证书失败: sslKey={}", sslKey, e);
      return null;
    }
  }

  private List<X509Certificate> parseCertificates(String pem) throws Exception {
    if (StrUtil.isBlank(pem)) {
      throw new IllegalArgumentException("证书内容为空");
    }
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    Collection<? extends Certificate> certificates =
        certificateFactory.generateCertificates(
            new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    List<X509Certificate> result = new ArrayList<>();
    for (Certificate certificate : certificates) {
      if (certificate instanceof X509Certificate x509Certificate) {
        result.add(x509Certificate);
      }
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("未解析到X.509证书");
    }
    return result;
  }

  private PrivateKey parsePrivateKey(String pem, char[] password) throws Exception {
    if (StrUtil.isBlank(pem)) {
      throw new IllegalArgumentException("私钥内容为空");
    }
    Matcher matcher = PRIVATE_KEY_BLOCK.matcher(pem);
    if (!matcher.find()) {
      throw new IllegalArgumentException("未找到PEM私钥块");
    }
    String keyType = matcher.group(1).trim().toUpperCase(Locale.ROOT);
    byte[] encoded = Base64.getMimeDecoder().decode(matcher.group(2));

    if (keyType.contains("ENCRYPTED")) {
      if (password == null || password.length == 0) {
        throw new IllegalArgumentException("加密私钥需要配置私钥密码");
      }
      EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(encoded);
      SecretKeyFactory secretKeyFactory =
          SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
      SecretKey secretKey = secretKeyFactory.generateSecret(new PBEKeySpec(password));
      Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
      cipher.init(Cipher.DECRYPT_MODE, secretKey, encryptedPrivateKeyInfo.getAlgParameters());
      return generatePrivateKey(encryptedPrivateKeyInfo.getKeySpec(cipher));
    }

    if ("RSA PRIVATE KEY".equals(keyType)) {
      encoded = wrapPkcs1RsaPrivateKey(encoded);
    } else if ("EC PRIVATE KEY".equals(keyType)) {
      throw new IllegalArgumentException("暂不支持SEC1格式EC私钥，请转换为PKCS#8格式");
    }
    return generatePrivateKey(new PKCS8EncodedKeySpec(encoded));
  }

  private PrivateKey generatePrivateKey(PKCS8EncodedKeySpec keySpec) throws Exception {
    Exception last = null;
    for (String algorithm : List.of("RSA", "EC", "DSA")) {
      try {
        return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
      } catch (Exception e) {
        last = e;
      }
    }
    throw new IllegalArgumentException("无法识别私钥算法", last);
  }

  private SslMode resolveMode(MQTTProductConfig config) {
    String mode = StrUtil.blankToDefault(config.getSslMode(), "").trim().toUpperCase(Locale.ROOT);
    if (mode.isEmpty() || "AUTO".equals(mode)) {
      if (config.isSsl() || isTlsScheme(config.getHost())) {
        return hasClientSource(config) ? SslMode.MUTUAL_AUTH : SslMode.SERVER_AUTH;
      }
      return SslMode.DISABLED;
    }
    return switch (mode) {
      case "DISABLED", "NONE", "OFF", "FALSE" ->
          (config.isSsl() || isTlsScheme(config.getHost()))
              ? (hasClientSource(config) ? SslMode.MUTUAL_AUTH : SslMode.SERVER_AUTH)
              : SslMode.DISABLED;
      case "MUTUAL_AUTH", "MUTUAL", "TWO_WAY", "2WAY", "CLIENT_AUTH" -> SslMode.MUTUAL_AUTH;
      default -> SslMode.SERVER_AUTH;
    };
  }

  private boolean hasClientSource(MQTTProductConfig config) {
    return StrUtil.isNotBlank(config.getSslClientCertKey())
        || StrUtil.isNotBlank(config.getSslCertPath())
        || StrUtil.isNotBlank(config.getSslKeyPath());
  }

  private boolean isTlsScheme(String host) {
    return StrUtil.startWithIgnoreCase(host, "ssl://")
        || StrUtil.startWithIgnoreCase(host, "mqtts://");
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StrUtil.isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }

  private static byte[] wrapPkcs1RsaPrivateKey(byte[] pkcs1) {
    return derSequence(
        derIntegerZero(),
        PKCS8_RSA_ALGORITHM_IDENTIFIER,
        derOctetString(pkcs1));
  }

  private static byte[] derSequence(byte[]... parts) {
    return der(0x30, concat(parts));
  }

  private static byte[] derIntegerZero() {
    return new byte[] {0x02, 0x01, 0x00};
  }

  private static byte[] derOctetString(byte[] content) {
    return der(0x04, content);
  }

  private static byte[] der(int tag, byte[] content) {
    byte[] length = derLength(content.length);
    byte[] result = new byte[1 + length.length + content.length];
    result[0] = (byte) tag;
    System.arraycopy(length, 0, result, 1, length.length);
    System.arraycopy(content, 0, result, 1 + length.length, content.length);
    return result;
  }

  private static byte[] derLength(int length) {
    if (length < 128) {
      return new byte[] {(byte) length};
    }
    int temp = length;
    int bytes = 0;
    while (temp > 0) {
      bytes++;
      temp >>= 8;
    }
    byte[] result = new byte[bytes + 1];
    result[0] = (byte) (0x80 | bytes);
    for (int i = bytes; i > 0; i--) {
      result[i] = (byte) (length & 0xff);
      length >>= 8;
    }
    return result;
  }

  private static byte[] concat(byte[]... parts) {
    int length = 0;
    for (byte[] part : parts) {
      length += part.length;
    }
    byte[] result = new byte[length];
    int offset = 0;
    for (byte[] part : parts) {
      System.arraycopy(part, 0, result, offset, part.length);
      offset += part.length;
    }
    return result;
  }

  private static byte[] hex(String value) {
    byte[] result = new byte[value.length() / 2];
    for (int i = 0; i < result.length; i++) {
      result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
    }
    return result;
  }

  private enum SslMode {
    DISABLED,
    SERVER_AUTH,
    MUTUAL_AUTH
  }

  private static class TlsMaterial {
    private String certificateContent;
    private String privateKeyContent;
    private String keyPassword;

    private TlsMaterial() {}

    private TlsMaterial(String certificateContent, String privateKeyContent, String keyPassword) {
      this.certificateContent = certificateContent;
      this.privateKeyContent = privateKeyContent;
      this.keyPassword = keyPassword;
    }

    private boolean hasCertificate() {
      return StrUtil.isNotBlank(certificateContent);
    }

    private boolean hasPrivateKey() {
      return StrUtil.isNotBlank(privateKeyContent);
    }

    private boolean isCompleteClientMaterial() {
      return hasCertificate() && hasPrivateKey();
    }

    private char[] passwordChars() {
      return StrUtil.isBlank(keyPassword) ? EMPTY_PASSWORD : keyPassword.toCharArray();
    }
  }
}
