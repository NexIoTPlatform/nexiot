package cn.universal.security.wechat;

import cn.hutool.json.JSONUtil;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/** 微信公众平台消息协议；密文签名与接收方 AppID 均需校验。 */
@Component
public class WechatCallbackCodec {
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  /** Verified transport metadata stays separate from WeChat's message fields. */
  public record DecodedMessage(Map<String, String> message, boolean encrypted) {}

  public enum ValidationFailureReason {
    TIMESTAMP_INVALID,
    SIGNATURE_INVALID,
    AES_KEY_MISSING,
    ENCRYPTED_MESSAGE_REQUIRED,
    DECRYPT_FAILED,
    MESSAGE_FORMAT_INVALID,
    OTHER
  }

  /** Fixed diagnostic code only; never retain request values or underlying exception messages. */
  public static final class ValidationException extends IllegalArgumentException {
    private final ValidationFailureReason reason;

    private ValidationException(ValidationFailureReason reason) {
      super("微信消息验证失败");
      this.reason = reason;
    }

    public ValidationFailureReason getReason() {
      return reason;
    }
  }

  public WechatCallbackCodec() {
    this(Clock.systemUTC());
  }

  WechatCallbackCodec(Clock clock) {
    this.clock = clock;
  }

  public String verifyUrl(String token, String signature, String timestamp, String nonce,
      String echo) {
    verifyTimestamp(timestamp);
    verifySignature(signature, token, timestamp, nonce);
    if (echo == null || echo.length() > 2048) throw invalid(ValidationFailureReason.MESSAGE_FORMAT_INVALID);
    return echo;
  }

  public String verifyUrl(String token, String aesKey, String appId, String signature,
      String messageSignature, String timestamp, String nonce, String encryptType, String echo) {
    if (!hasEncryptionHint(encryptType, messageSignature)) {
      return verifyUrl(token, signature, timestamp, nonce, echo);
    }
    verifyTimestamp(timestamp);
    verifySignature(messageSignature, token, timestamp, nonce, echo);
    if (echo.length() > 2048) throw invalid(ValidationFailureReason.MESSAGE_FORMAT_INVALID);
    return decrypt(aesKey, appId, echo);
  }

  public Map<String, String> decode(String token, String aesKey, String appId,
      String signature, String messageSignature, String timestamp, String nonce, String body) {
    return decodeMessage(token, aesKey, appId, signature, messageSignature, timestamp, nonce, null, body)
        .message();
  }

  public DecodedMessage decodeMessage(String token, String aesKey, String appId,
      String signature, String messageSignature, String timestamp, String nonce,
      String encryptType, String body) {
    verifyTimestamp(timestamp);
    Map<String, String> envelope = parse(body);
    String encrypted = envelope.get("Encrypt");
    if (envelope.containsKey("Encrypt")) {
      if (encrypted == null || encrypted.isBlank()) throw invalid(ValidationFailureReason.ENCRYPTED_MESSAGE_REQUIRED);
      verifySignature(messageSignature, token, timestamp, nonce, encrypted);
      return new DecodedMessage(parse(decrypt(aesKey, appId, encrypted)), true);
    }
    // A signed encrypted request must never fall back to unverified outer plaintext fields.
    if (hasEncryptionHint(encryptType, messageSignature)) {
      throw invalid(ValidationFailureReason.ENCRYPTED_MESSAGE_REQUIRED);
    }
    verifySignature(signature, token, timestamp, nonce);
    return new DecodedMessage(envelope, false);
  }

  private boolean hasEncryptionHint(String encryptType, String messageSignature) {
    return encryptType != null || messageSignature != null;
  }

  public String textReply(DecodedMessage decoded, String text, String token,
      String aesKey, String appId) {
    return textReply(decoded.message(), text, token, aesKey, appId, decoded.encrypted());
  }

  public String textReply(Map<String, String> message, String text, String token,
      String aesKey, String appId) {
    return textReply(message, text, token, aesKey, appId, aesKey != null && !aesKey.isBlank());
  }

  private String textReply(Map<String, String> message, String text, String token,
      String aesKey, String appId, boolean encryptedReply) {
    String xml = "<xml><ToUserName><![CDATA[" + cdata(message.get("FromUserName"))
        + "]]></ToUserName><FromUserName><![CDATA[" + cdata(message.get("ToUserName"))
        + "]]></FromUserName><CreateTime>" + clock.instant().getEpochSecond()
        + "</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA["
        + cdata(text) + "]]></Content></xml>";
    if (!encryptedReply) return xml;
    String encrypted = encrypt(aesKey, appId, xml);
    String timestamp = Long.toString(clock.instant().getEpochSecond());
    String nonce = HexFormat.of().formatHex(random.generateSeed(12));
    String signature = signature(token, timestamp, nonce, encrypted);
    return "<xml><Encrypt><![CDATA[" + encrypted + "]]></Encrypt><MsgSignature><![CDATA["
        + signature + "]]></MsgSignature><TimeStamp>" + timestamp
        + "</TimeStamp><Nonce><![CDATA[" + nonce + "]]></Nonce></xml>";
  }

  String decrypt(String encodingKey, String appId, String encrypted) {
    try {
      byte[] key = key(encodingKey);
      byte[] input = Base64.getDecoder().decode(encrypted);
      if (input.length == 0 || input.length % 16 != 0) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
          new IvParameterSpec(Arrays.copyOf(key, 16)));
      byte[] padded = cipher.doFinal(input);
      int padding = padded[padded.length - 1] & 0xff;
      if (padding < 1 || padding > 32 || padding > padded.length) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
      for (int i = padded.length - padding; i < padded.length; i++) {
        if ((padded[i] & 0xff) != padding) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
      }
      int size = padded.length - padding;
      if (size < 20) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
      int messageLength = ByteBuffer.wrap(padded, 16, 4).getInt();
      if (messageLength < 0 || messageLength > size - 20) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
      byte[] receiver = Arrays.copyOfRange(padded, 20 + messageLength, size);
      if (!MessageDigest.isEqual(receiver, appId.getBytes(StandardCharsets.UTF_8))) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
      return new String(padded, 20, messageLength, StandardCharsets.UTF_8);
    } catch (ValidationException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw invalid(ValidationFailureReason.DECRYPT_FAILED);
    } catch (Exception e) {
      throw invalid(ValidationFailureReason.DECRYPT_FAILED);
    }
  }

  String encrypt(String encodingKey, String appId, String message) {
    try {
      byte[] key = key(encodingKey);
      byte[] content = message.getBytes(StandardCharsets.UTF_8);
      byte[] receiver = appId.getBytes(StandardCharsets.UTF_8);
      int size = 20 + content.length + receiver.length;
      int padding = 32 - size % 32;
      ByteBuffer buffer = ByteBuffer.allocate(size + padding);
      byte[] prefix = new byte[16];
      random.nextBytes(prefix);
      buffer.put(prefix).putInt(content.length).put(content).put(receiver);
      while (buffer.hasRemaining()) buffer.put((byte) padding);
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
          new IvParameterSpec(Arrays.copyOf(key, 16)));
      return Base64.getEncoder().encodeToString(cipher.doFinal(buffer.array()));
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw invalid(ValidationFailureReason.OTHER);
    }
  }

  private byte[] key(String value) {
    if (value == null || value.isBlank()) throw invalid(ValidationFailureReason.AES_KEY_MISSING);
    if (!value.matches("[A-Za-z0-9+/]{43}")) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
    byte[] key = Base64.getDecoder().decode(value + "=");
    if (key.length != 32) throw invalid(ValidationFailureReason.DECRYPT_FAILED);
    return key;
  }

  private void verifyTimestamp(String value) {
    try {
      long timestamp = Long.parseLong(value);
      long now = clock.instant().getEpochSecond();
      if (timestamp < now - 300 || timestamp > now + 300) throw invalid(ValidationFailureReason.TIMESTAMP_INVALID);
    } catch (RuntimeException e) {
      throw invalid(ValidationFailureReason.TIMESTAMP_INVALID);
    }
  }

  private void verifySignature(String provided, String... values) {
    if (provided == null || !provided.matches("[a-fA-F0-9]{40}")) throw invalid(ValidationFailureReason.SIGNATURE_INVALID);
    String expected = signature(values);
    if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
        provided.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
      throw invalid(ValidationFailureReason.SIGNATURE_INVALID);
    }
  }

  static String signature(String... values) {
    try {
      if (Arrays.stream(values).anyMatch(v -> v == null || v.isBlank() || v.length() > 100000)) {
        throw invalid(ValidationFailureReason.SIGNATURE_INVALID);
      }
      String[] sorted = values.clone();
      Arrays.sort(sorted);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
          .digest(String.join("", sorted).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw invalid(ValidationFailureReason.SIGNATURE_INVALID);
    }
  }

  Map<String, String> parse(String body) {
    if (body == null || body.length() > 65536 || body.isBlank()) throw invalid(ValidationFailureReason.MESSAGE_FORMAT_INVALID);
    try {
      Map<String, String> result = new LinkedHashMap<>();
      if (body.stripLeading().startsWith("{")) {
        JSONUtil.parseObj(body).forEach((k, v) -> {
          if ("Encrypt".equals(k) && !(v instanceof String)) throw invalid(ValidationFailureReason.MESSAGE_FORMAT_INVALID);
          if (v instanceof String || v instanceof Number) result.put(k, v.toString());
        });
        return result;
      }
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      var builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
        @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
        @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
      });
      Element root = builder.parse(new InputSource(new StringReader(body)))
          .getDocumentElement();
      if (!"xml".equals(root.getTagName())) throw invalid(ValidationFailureReason.MESSAGE_FORMAT_INVALID);
      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node instanceof Element element) {
          if (result.putIfAbsent(element.getTagName(), element.getTextContent()) != null) throw invalid(ValidationFailureReason.MESSAGE_FORMAT_INVALID);
        }
      }
      return result;
    } catch (Exception e) {
      throw invalid(ValidationFailureReason.MESSAGE_FORMAT_INVALID);
    }
  }

  private static String cdata(String value) {
    return value == null ? "" : value.replace("]]>", "]]]]><![CDATA[>");
  }

  private static ValidationException invalid(ValidationFailureReason reason) {
    return new ValidationException(reason);
  }
}
