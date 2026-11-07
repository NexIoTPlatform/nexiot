package cn.universal.security.wechat;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.security.wechat.WechatCallbackCodec.ValidationException;
import cn.universal.security.wechat.WechatCallbackCodec.ValidationFailureReason;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class WechatCallbackCodecTest {
  private static final String TOKEN = "testToken";
  private static final String APP = "wx0123456789abcdef";
  private static final String KEY = Base64.getEncoder().withoutPadding().encodeToString(new byte[32]);
  private static final String TS = "1793664000";
  private static final String NONCE = "testNonce";
  private final WechatCallbackCodec codec = new WechatCallbackCodec(
      Clock.fixed(Instant.ofEpochSecond(Long.parseLong(TS)), ZoneOffset.UTC));

  @Test void verifiesUrlAndRejectsTamperingOrStaleTimestamp() {
    String signature = WechatCallbackCodec.signature(TOKEN, TS, NONCE);
    assertEquals("hello", codec.verifyUrl(TOKEN, signature, TS, NONCE, "hello"));
    assertFailure(ValidationFailureReason.SIGNATURE_INVALID,
        () -> codec.verifyUrl(TOKEN, signature, TS, "other", "hello"));
    String old = Long.toString(Long.parseLong(TS) - 301);
    assertFailure(ValidationFailureReason.TIMESTAMP_INVALID, () -> codec.verifyUrl(TOKEN,
        WechatCallbackCodec.signature(TOKEN, old, NONCE), old, NONCE, "hello"));
    assertFailure(ValidationFailureReason.TIMESTAMP_INVALID,
        () -> codec.verifyUrl(TOKEN, signature, "private-invalid-timestamp", NONCE, "hello"));
  }

  @Test void decryptsIndependentProtocolFixtureAndChecksSignature() throws Exception {
    String xml = "<xml><FromUserName>openid_fixture</FromUserName><MsgType>text</MsgType><Content>登录 12345678</Content></xml>";
    String encrypted = fixture(xml, APP, 0);
    String signature = WechatCallbackCodec.signature(TOKEN, TS, NONCE, encrypted);
    assertEquals("登录 12345678", codec.decode(TOKEN, KEY, APP, null, signature, TS, NONCE, envelope(encrypted)).get("Content"));
    assertFailure(ValidationFailureReason.SIGNATURE_INVALID, () -> codec.decode(TOKEN, KEY, APP, null,
        "0".repeat(40), TS, NONCE, envelope(encrypted)));
    assertFailure(ValidationFailureReason.DECRYPT_FAILED, () -> codec.decode(TOKEN, KEY, "wxwrongreceiver", null,
        signature, TS, NONCE, envelope(encrypted)));
  }

  @Test void urlVerificationAutomaticallyHandlesPlaintextAndExplicitEncryptedEchoes() throws Exception {
    String echo = "official-echo-fixture-12345678";
    String plainSignature = WechatCallbackCodec.signature(TOKEN, TS, NONCE);
    assertEquals(echo, codec.verifyUrl(TOKEN, KEY, APP, plainSignature, null, TS, NONCE, null, echo));
    String encrypted = fixture(echo, APP, 0);
    String messageSignature = WechatCallbackCodec.signature(TOKEN, TS, NONCE, encrypted);
    assertEquals(echo, codec.verifyUrl(TOKEN, KEY, APP, null, messageSignature, TS, NONCE, "aes", encrypted));
    assertEquals(echo, codec.verifyUrl(TOKEN, KEY, APP, null, messageSignature, TS, NONCE, null, encrypted));
    assertFailure(ValidationFailureReason.SIGNATURE_INVALID,
        () -> codec.verifyUrl(TOKEN, KEY, APP, plainSignature, "0".repeat(40), TS, NONCE, "aes", encrypted));
    assertFailure(ValidationFailureReason.DECRYPT_FAILED,
        () -> codec.verifyUrl(TOKEN, KEY, "wx_wrong_receiver", null, messageSignature, TS, NONCE, "aes", encrypted));
    assertFailure(ValidationFailureReason.AES_KEY_MISSING,
        () -> codec.verifyUrl(TOKEN, null, APP, null, messageSignature, TS, NONCE, "aes", encrypted));
    assertFailure(ValidationFailureReason.SIGNATURE_INVALID,
        () -> codec.verifyUrl(TOKEN, KEY, APP, plainSignature, null, TS, NONCE, "aes", echo));
    assertFailure(ValidationFailureReason.SIGNATURE_INVALID,
        () -> codec.verifyUrl(TOKEN, KEY, APP, plainSignature, "", TS, NONCE, null, echo));
    for (String encryptType : new String[] {"", " ", "unsupported"}) {
      assertFailure(ValidationFailureReason.SIGNATURE_INVALID,
          () -> codec.verifyUrl(TOKEN, KEY, APP, plainSignature, null, TS, NONCE, encryptType, echo));
    }
    String badCiphertext = "private-invalid-encrypted-echo";
    assertFailure(ValidationFailureReason.DECRYPT_FAILED,
        () -> codec.verifyUrl(TOKEN, KEY, APP, plainSignature,
            WechatCallbackCodec.signature(TOKEN, TS, NONCE, badCiphertext), TS, NONCE, "aes", badCiphertext));
  }

  @Test void reportsMissingAesKeyAfterValidatingTheEncryptedMessageSignature() throws Exception {
    String encrypted = fixture("<xml><MsgType>text</MsgType><Content>12345678</Content></xml>", APP, 0);
    String signature = WechatCallbackCodec.signature(TOKEN, TS, NONCE, encrypted);
    for (String missingKey : new String[] {null, ""}) {
      assertFailure(ValidationFailureReason.AES_KEY_MISSING, () -> codec.decode(TOKEN, missingKey,
          APP, null, signature, TS, NONCE, envelope(encrypted)));
    }
    // Diagnostics must not change validation order for an unauthenticated request.
    assertFailure(ValidationFailureReason.SIGNATURE_INVALID, () -> codec.decode(TOKEN, null,
        APP, null, "0".repeat(40), TS, NONCE, envelope(encrypted)));
  }

  @Test void rejectsInvalidPaddingAndMessageLength() throws Exception {
    assertFailure(ValidationFailureReason.DECRYPT_FAILED,
        () -> codec.decrypt(KEY, APP, fixture("<xml/>", APP, 1)));
    assertFailure(ValidationFailureReason.DECRYPT_FAILED,
        () -> codec.decrypt(KEY, APP, fixture("<xml/>", APP, 2)));
    assertFailure(ValidationFailureReason.DECRYPT_FAILED, () -> codec.decrypt(KEY, APP, "AAAA"));
  }

  @Test void automaticallyAcceptsVerifiedPlaintextWithOrWithoutConfiguredKey() {
    String xml = "<xml><MsgType>text</MsgType><Content>12345678</Content></xml>";
    String signature = WechatCallbackCodec.signature(TOKEN, TS, NONCE);
    for (String key : new String[] {null, KEY}) {
      WechatCallbackCodec.DecodedMessage decoded =
          codec.decodeMessage(TOKEN, key, APP, signature, null, TS, NONCE, null, xml);
      assertFalse(decoded.encrypted());
      assertEquals(Map.of("MsgType", "text", "Content", "12345678"), decoded.message());
      assertEquals(decoded.message(), codec.decode(TOKEN, key, APP, signature, null, TS, NONCE, xml));
      assertFailure(ValidationFailureReason.SIGNATURE_INVALID,
          () -> codec.decodeMessage(TOKEN, key, APP, "0".repeat(40), null, TS, NONCE, null, xml));
    }
  }

  @Test void encryptedHintsRequireAnEncryptedEnvelopeEvenWithAValidPlaintextSignature() {
    String xml = "<xml><MsgType>text</MsgType><Content>12345678</Content></xml>";
    String signature = WechatCallbackCodec.signature(TOKEN, TS, NONCE);
    for (String encryptType : new String[] {"aes", "AES", "aes ", "unsupported", "", " "}) {
      assertFailure(ValidationFailureReason.ENCRYPTED_MESSAGE_REQUIRED,
          () -> codec.decodeMessage(TOKEN, KEY, APP, signature, null, TS, NONCE, encryptType, xml));
    }
    assertFailure(ValidationFailureReason.ENCRYPTED_MESSAGE_REQUIRED,
        () -> codec.decode(TOKEN, KEY, APP, signature, "a".repeat(40), TS, NONCE, xml));
    assertFailure(ValidationFailureReason.ENCRYPTED_MESSAGE_REQUIRED,
        () -> codec.decode(TOKEN, KEY, APP, signature, "", TS, NONCE, xml));
    for (String encrypt : new String[] {"", " \n "}) {
      assertFailure(ValidationFailureReason.ENCRYPTED_MESSAGE_REQUIRED,
          () -> codec.decode(TOKEN, KEY, APP, signature, null, TS, NONCE,
              xml.replace("</xml>", "<Encrypt>" + encrypt + "</Encrypt></xml>")));
    }
    for (String encrypt : new String[] {"null", "{}", "[]", "123"}) {
      assertFailure(ValidationFailureReason.MESSAGE_FORMAT_INVALID,
          () -> codec.decode(TOKEN, KEY, APP, signature, null, TS, NONCE,
              "{\"MsgType\":\"text\",\"Content\":\"12345678\",\"Encrypt\":" + encrypt + "}"));
    }
  }

  @Test void neverFallsBackToOuterPlaintextWhenEncryptedValidationFails() throws Exception {
    String plainSignature = WechatCallbackCodec.signature(TOKEN, TS, NONCE);
    String xml = "<xml><MsgType>text</MsgType><Content>12345678</Content></xml>";
    String encrypted = fixture(xml, APP, 1);
    String body = xml.replace("</xml>", "<Encrypt>" + encrypted + "</Encrypt></xml>");
    String messageSignature = WechatCallbackCodec.signature(TOKEN, TS, NONCE, encrypted);
    assertFailure(ValidationFailureReason.DECRYPT_FAILED,
        () -> codec.decodeMessage(TOKEN, KEY, APP, plainSignature, messageSignature,
            TS, NONCE, "aes", body));
    assertFailure(ValidationFailureReason.SIGNATURE_INVALID,
        () -> codec.decodeMessage(TOKEN, KEY, APP, plainSignature, "0".repeat(40),
            TS, NONCE, null, body));

    String wrongReceiver = fixture(xml, "wx_other_receiver", 0);
    assertFailure(ValidationFailureReason.DECRYPT_FAILED,
        () -> codec.decodeMessage(TOKEN, KEY, APP, plainSignature,
            WechatCallbackCodec.signature(TOKEN, TS, NONCE, wrongReceiver), TS, NONCE, null,
            xml.replace("</xml>", "<Encrypt>" + wrongReceiver + "</Encrypt></xml>")));
  }

  @Test void replyFollowsVerifiedTransportInsteadOfWhetherAKeyIsConfigured() throws Exception {
    String xml = "<xml><FromUserName>openid</FromUserName><ToUserName>account</ToUserName></xml>";
    WechatCallbackCodec.DecodedMessage plaintext = codec.decodeMessage(TOKEN, KEY, APP,
        WechatCallbackCodec.signature(TOKEN, TS, NONCE), null, TS, NONCE, null, xml);
    Map<String, String> plainReply = codec.parse(codec.textReply(plaintext, "明文回复", TOKEN, KEY, APP));
    assertFalse(plainReply.containsKey("Encrypt"));
    assertEquals("明文回复", plainReply.get("Content"));
    assertEquals("openid", plainReply.get("ToUserName"));

    String encrypted = fixture(xml, APP, 0);
    WechatCallbackCodec.DecodedMessage secured = codec.decodeMessage(TOKEN, KEY, APP, null,
        WechatCallbackCodec.signature(TOKEN, TS, NONCE, encrypted), TS, NONCE, "aes", envelope(encrypted));
    assertTrue(secured.encrypted());
    assertEquals(plaintext.message(), secured.message());
    Map<String, String> securedReply = codec.parse(codec.textReply(secured, "密文回复", TOKEN, KEY, APP));
    assertEquals(WechatCallbackCodec.signature(TOKEN, securedReply.get("TimeStamp"),
        securedReply.get("Nonce"), securedReply.get("Encrypt")), securedReply.get("MsgSignature"));
    assertEquals("密文回复", codec.parse(codec.decrypt(KEY, APP, securedReply.get("Encrypt"))).get("Content"));
    assertFailure(ValidationFailureReason.AES_KEY_MISSING,
        () -> codec.textReply(secured, "不得回退明文", TOKEN, null, APP));
  }

  @Test void rejectsXmlEntitiesDuplicatesAndOversizeBodies() {
    assertFailure(ValidationFailureReason.MESSAGE_FORMAT_INVALID, () -> codec.parse(
        "<!DOCTYPE xml [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><xml><Content>&x;</Content></xml>"));
    assertFailure(ValidationFailureReason.MESSAGE_FORMAT_INVALID,
        () -> codec.parse("<xml><Content>a</Content><Content>b</Content></xml>"));
    assertFailure(ValidationFailureReason.MESSAGE_FORMAT_INVALID, () -> codec.parse("x".repeat(65537)));
    assertFailure(ValidationFailureReason.MESSAGE_FORMAT_INVALID,
        () -> codec.parse("<xml><Content>private-malformed-body</xml>"));
  }

  @Test void encryptedTextReplyHasValidSignatureAndEscapesCdata() {
    Map<String, String> request = Map.of("FromUserName", "openid", "ToUserName", "account");
    String body = "已处理 ]]> 好 👩‍💻\n下一行 🎉";
    Map<String, String> reply = codec.parse(codec.textReply(request, body, TOKEN, KEY, APP));
    assertEquals(WechatCallbackCodec.signature(TOKEN, reply.get("TimeStamp"), reply.get("Nonce"), reply.get("Encrypt")), reply.get("MsgSignature"));
    Map<String, String> content = codec.parse(codec.decrypt(KEY, APP, reply.get("Encrypt")));
    assertEquals("openid", content.get("ToUserName"));
    assertEquals("account", content.get("FromUserName"));
    assertEquals(body, content.get("Content"));
  }

  @Test void plaintextReplyPreservesMultilineEmojiAndCdata() {
    Map<String, String> request = Map.of("FromUserName", "openid", "ToUserName", "account");
    String body = "第一行 👋\n第二行 ]]> <内容> & 🎉\n";
    assertEquals(body, codec.parse(codec.textReply(request, body, TOKEN, null, APP)).get("Content"));
  }

  private void assertFailure(ValidationFailureReason reason, Executable action) {
    ValidationException exception = assertThrows(ValidationException.class, action);
    assertEquals(reason, exception.getReason());
    assertEquals("微信消息验证失败", exception.getMessage());
  }

  // Independently constructs the documented network-byte-order payload, including corrupt fixtures.
  private String fixture(String xml, String receiver, int corruption) throws Exception {
    byte[] message = xml.getBytes(StandardCharsets.UTF_8);
    byte[] app = receiver.getBytes(StandardCharsets.UTF_8);
    int size = 20 + message.length + app.length;
    int padding = 32 - size % 32;
    ByteBuffer payload = ByteBuffer.allocate(size + padding);
    payload.put("0123456789abcdef".getBytes(StandardCharsets.US_ASCII));
    payload.putInt(corruption == 2 ? Integer.MAX_VALUE : message.length).put(message).put(app);
    while (payload.hasRemaining()) payload.put((byte) padding);
    if (corruption == 1) payload.put(payload.capacity() - 1, (byte) 0);
    byte[] key = Base64.getDecoder().decode(KEY + "=");
    Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(Arrays.copyOf(key, 16)));
    return Base64.getEncoder().encodeToString(cipher.doFinal(payload.array()));
  }

  private String envelope(String encrypted) { return "<xml><Encrypt>" + encrypted + "</Encrypt></xml>"; }
}
