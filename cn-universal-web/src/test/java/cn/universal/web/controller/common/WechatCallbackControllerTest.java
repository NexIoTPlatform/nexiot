package cn.universal.web.controller.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.security.wechat.WechatAccountConfigService;
import cn.universal.security.wechat.WechatAuthService;
import cn.universal.security.wechat.WechatCallbackCodec;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class WechatCallbackControllerTest {
  private static final String TOKEN = "callback-fixture-token";
  private static final String APP_ID = "wx-test-only-app-id";
  private static final String AES_KEY =
      Base64.getEncoder().withoutPadding().encodeToString(new byte[32]);
  private static final String PATH = "/wechat/callback/test";

  private WechatAccountConfigService configs;
  private WechatAuthService auth;
  private WechatCallbackCodec codec;
  private WechatLoginConfig config;
  private MockMvc mvc;
  private Logger callbackLogger;
  private Level originalLogLevel;
  private ListAppender<ILoggingEvent> logs;

  @BeforeEach
  void setup() {
    configs = mock(WechatAccountConfigService.class);
    auth = mock(WechatAuthService.class);
    codec = new WechatCallbackCodec();
    config = new WechatLoginConfig();
    config.setTenantId("tenant-test");
    config.setAppId(APP_ID);
    config.setCallbackToken(TOKEN);
    config.setEncodingAESKey(AES_KEY);
    when(configs.findByTenantCode("test")).thenReturn(config);
    mvc =
        MockMvcBuilders.standaloneSetup(new WechatCallbackController(configs, codec, auth)).build();
    callbackLogger = (Logger) LoggerFactory.getLogger(WechatCallbackController.class);
    originalLogLevel = callbackLogger.getLevel();
    callbackLogger.setLevel(Level.INFO);
    logs = new ListAppender<>();
    logs.start();
    callbackLogger.addAppender(logs);
  }

  @AfterEach
  void stopCapturingLogs() {
    callbackLogger.detachAppender(logs);
    logs.stop();
    callbackLogger.setLevel(originalLogLevel);
  }

  @Test
  void verifiesUrlSignatureWithoutInvokingLoginLogic() throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "verify-fixture";
    mvc.perform(
            get(PATH)
                .param("signature", signature(TOKEN, timestamp, nonce))
                .param("timestamp", timestamp)
                .param("nonce", nonce)
                .param("echostr", "wechat-echo"))
        .andExpect(status().isOk())
        .andExpect(content().string("wechat-echo"))
        .andExpect(header().string("Cache-Control", "no-store"));
    assertTrue(logs.list.getFirst().getFormattedMessage().contains("mode=UNDETERMINED"));
    assertTrue(logs.list.getLast().getFormattedMessage().contains("mode=PLAINTEXT"));
    verifyNoInteractions(auth);
  }

  @Test
  void verifiesExplicitEncryptedEchoAndReturnsDecryptedChallengeWithoutLoggingIt() throws Exception {
    String encrypted = incomingMessage();
    Element envelope = envelope(encrypted);
    String cipher = field(envelope, "Encrypt");
    String timestamp = field(envelope, "TimeStamp");
    String nonce = field(envelope, "Nonce");
    String messageSignature = field(envelope, "MsgSignature");
    String response = mvc.perform(get(PATH).param("encrypt_type", "aes")
            .param("msg_signature", messageSignature).param("timestamp", timestamp)
            .param("nonce", nonce).param("echostr", cipher))
        .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertEquals("scan-fixture", field(envelope(response), "Content"));
    assertStages("received", "processed");
    assertTrue(logs.list.getLast().getFormattedMessage().contains("mode=ENCRYPTED"));
    assertNoSensitiveLogs(TOKEN, APP_ID, AES_KEY, encrypted, cipher, timestamp, nonce,
        messageSignature, response, "scan-fixture", "openid-fixture");
    verifyNoInteractions(auth);
  }

  @Test
  void encryptedEchoFailuresNeverFallBackToTheOrdinarySignature() throws Exception {
    String encrypted = incomingMessage();
    Element envelope = envelope(encrypted);
    String cipher = field(envelope, "Encrypt");
    String timestamp = field(envelope, "TimeStamp");
    String nonce = field(envelope, "Nonce");
    String ordinarySignature = signature(TOKEN, timestamp, nonce);
    for (String messageSignature : List.of("0".repeat(40), "")) {
      logs.list.clear();
      mvc.perform(get(PATH).param("signature", ordinarySignature).param("encrypt_type", "aes")
              .param("msg_signature", messageSignature).param("timestamp", timestamp)
              .param("nonce", nonce).param("echostr", cipher))
          .andExpect(status().isForbidden()).andExpect(content().string("invalid signature"));
      assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=SIGNATURE_INVALID"));
      assertTrue(logs.list.getLast().getFormattedMessage().contains("mode=UNDETERMINED"));
      assertNoSensitiveLogs(TOKEN, AES_KEY, cipher, timestamp, nonce, ordinarySignature);
    }
    for (String encryptType : List.of("", " ", "unsupported")) {
      logs.list.clear();
      mvc.perform(get(PATH).param("signature", ordinarySignature).param("encrypt_type", encryptType)
              .param("timestamp", timestamp).param("nonce", nonce).param("echostr", "private-echo"))
          .andExpect(status().isForbidden()).andExpect(content().string("invalid signature"));
      assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=SIGNATURE_INVALID"));
      assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, ordinarySignature, "private-echo");
    }
    logs.list.clear();
    config.setAppId("wx-wrong-echo-receiver");
    mvc.perform(get(PATH).param("signature", ordinarySignature).param("encrypt_type", "aes")
            .param("msg_signature", field(envelope, "MsgSignature")).param("timestamp", timestamp)
            .param("nonce", nonce).param("echostr", cipher))
        .andExpect(status().isForbidden()).andExpect(content().string("invalid signature"));
    assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=DECRYPT_FAILED"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, cipher, timestamp, nonce, ordinarySignature,
        field(envelope, "MsgSignature"));
    verifyNoInteractions(auth);
  }

  @Test
  void rejectsInvalidUrlAndMessageSignaturesBeforeLoginLogic() throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    mvc.perform(
            get(PATH)
                .param("signature", "0".repeat(40))
                .param("timestamp", timestamp)
                .param("nonce", "invalid-fixture")
                .param("echostr", "must-not-echo"))
        .andExpect(status().isForbidden())
        .andExpect(content().string("invalid signature"));

    String encrypted = incomingMessage();
    Element envelope = envelope(encrypted);
    mvc.perform(
            post(PATH)
                .param("msg_signature", "0".repeat(40))
                .param("timestamp", field(envelope, "TimeStamp"))
                .param("nonce", field(envelope, "Nonce"))
                .contentType(MediaType.APPLICATION_XML)
                .content(encrypted))
        .andExpect(status().isForbidden())
        .andExpect(content().string("invalid message"))
        .andExpect(header().string("Cache-Control", "no-store"));
    verifyNoInteractions(auth);
  }

  @Test
  void rejectsBodiesLargerThan64KiBBeforeLoginLogic() throws Exception {
    mvc.perform(post(PATH).contentType(MediaType.APPLICATION_XML).content(new byte[65_537]))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(content().string("message too large"));
    verifyNoInteractions(auth);
  }

  @Test
  void routesDecodedMessageAndEncryptsReplyForTheSameAccount() throws Exception {
    String reply = "扫码已确认，请返回浏览器。";
    when(auth.handleMessage(eq(config), anyMap())).thenReturn(reply);
    String response =
        mvc.perform(signedPost(incomingMessage()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> decoded = ArgumentCaptor.forClass(Map.class);
    verify(auth).handleMessage(eq(config), decoded.capture());
    assertEquals("openid-fixture", decoded.getValue().get("FromUserName"));
    assertEquals("gh-fixture", decoded.getValue().get("ToUserName"));
    assertEquals("scan-fixture", decoded.getValue().get("Content"));

    assertFalse(response.contains(reply));
    assertFalse(response.contains("openid-fixture"));
    Element envelope = envelope(response);
    Map<String, String> decrypted =
        codec.decode(
            TOKEN,
            AES_KEY,
            APP_ID,
            null,
            field(envelope, "MsgSignature"),
            field(envelope, "TimeStamp"),
            field(envelope, "Nonce"),
            response);
    assertEquals("openid-fixture", decrypted.get("ToUserName"));
    assertEquals("gh-fixture", decrypted.get("FromUserName"));
    assertEquals(reply, decrypted.get("Content"));
    assertTrue(logs.list.getFirst().getFormattedMessage().contains("mode=UNDETERMINED"));
    assertTrue(logs.list.get(1).getFormattedMessage().contains("mode=ENCRYPTED"));
    assertTrue(logs.list.getLast().getFormattedMessage().contains("mode=ENCRYPTED"));
  }

  @Test
  void acknowledgesMessagesWithoutReplyAndDoesNotExposePersistenceFailures() throws Exception {
    mvc.perform(signedPost(incomingMessage()))
        .andExpect(status().isOk())
        .andExpect(content().string("success"));

    when(auth.handleMessage(eq(config), anyMap()))
        .thenThrow(new IllegalStateException("redis://private-node/secret"));
    mvc.perform(signedPost(incomingMessage()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("temporarily unavailable"));
  }

  @Test
  void unknownTenantDoesNotInvokeLoginLogic() throws Exception {
    mvc.perform(get("/wechat/callback/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(content().string("not configured"));
    mvc.perform(post("/wechat/callback/unknown").content("<xml/>"))
        .andExpect(status().isNotFound())
        .andExpect(content().string("not configured"));
    verifyNoInteractions(auth);
  }

  @Test
  void configuredAesKeyStillAllowsVerifiedPlaintextAndRepliesInPlaintextWithoutLeakingFields()
      throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "private-upstream-nonce";
    String openId = "oFixture_PrivateOpenId123456789";
    String messageId = "7812345678901234567";
    String code = "74061829";
    String reply = "private-reply-content";
    String body = plaintextMessage(timestamp, openId, messageId, code);
    when(auth.handleMessage(eq(config), anyMap())).thenReturn(reply);

    String response = mvc.perform(signedPlaintextPost(body, timestamp, nonce))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    Element responseXml = envelope(response);
    assertEquals(0, responseXml.getElementsByTagName("Encrypt").getLength());
    assertEquals(reply, field(responseXml, "Content"));
    assertEquals(openId, field(responseXml, "ToUserName"));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> decoded = ArgumentCaptor.forClass(Map.class);
    verify(auth).handleMessage(eq(config), decoded.capture());
    assertEquals(openId, decoded.getValue().get("FromUserName"));
    assertEquals("gh_private_fixture_account", decoded.getValue().get("ToUserName"));
    assertEquals(messageId, decoded.getValue().get("MsgId"));
    assertEquals(code, decoded.getValue().get("Content"));
    assertStages("received", "decoded", "processed");
    assertTrue(logs.list.get(1).getFormattedMessage().contains("security=verified"));
    assertTrue(logs.list.getFirst().getFormattedMessage().contains("mode=UNDETERMINED"));
    assertTrue(logs.list.get(1).getFormattedMessage().contains("mode=PLAINTEXT"));
    assertTrue(logs.list.getLast().getFormattedMessage().contains("mode=PLAINTEXT"));
    assertTrue(logs.list.get(2).getFormattedMessage().contains("phase=build_reply"));
    assertTrue(logs.list.get(2).getFormattedMessage().contains("httpStatus=200"));
    assertTrue(logs.list.get(1).getFormattedMessage()
        .contains("bytes=" + body.getBytes(StandardCharsets.UTF_8).length));
    assertNoSensitiveLogs(TOKEN, APP_ID, AES_KEY, timestamp, nonce, openId, messageId, code,
        reply, "gh_private_fixture_account", body, signature(TOKEN, timestamp, nonce));
  }

  @Test
  void failedSignatureLogsOnlyFixedDiagnosticsAndNeverAuthenticationValues() throws Exception {
    config.setEncodingAESKey(null);
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "private-rejected-nonce";
    String openId = "oFixture_RejectedOpenId123456";
    String messageId = "7912345678901234567";
    String code = "63051728";
    String invalidSignature = "9".repeat(40);
    String body = plaintextMessage(timestamp, openId, messageId, code);

    mvc.perform(post(PATH)
            .param("signature", invalidSignature)
            .param("timestamp", timestamp)
            .param("nonce", nonce)
            .contentType(MediaType.TEXT_XML)
            .content(body.getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isForbidden())
        .andExpect(content().string("invalid message"));

    verifyNoInteractions(auth);
    assertStages("received", "rejected");
    ILoggingEvent rejected = logs.list.get(1);
    assertEquals(Level.WARN, rejected.getLevel());
    assertTrue(rejected.getFormattedMessage().contains("phase=decode_message security=failed"));
    assertTrue(rejected.getFormattedMessage()
        .contains("httpStatus=403 exception=ValidationException reason=SIGNATURE_INVALID"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, openId, messageId, code,
        invalidSignature, body);
  }

  @Test
  void processingFailureLogsExceptionClassWithoutItsSensitiveMessageOrStackTrace()
      throws Exception {
    config.setEncodingAESKey(null);
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "private-error-nonce";
    String openId = "oFixture_ErrorOpenId123456789";
    String messageId = "8012345678901234567";
    String code = "52041637";
    String browserProof = "private-browser-proof-value";
    String session = "private-session-identity";
    String body = plaintextMessage(timestamp, openId, messageId, code);
    String exceptionMessage = body + " proof=" + browserProof + " session=" + session
        + " token=" + TOKEN + " nonce=" + nonce;
    when(auth.handleMessage(eq(config), anyMap()))
        .thenThrow(new IllegalStateException(exceptionMessage));

    mvc.perform(signedPlaintextPost(body, timestamp, nonce))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("temporarily unavailable"));

    assertStages("received", "decoded", "error");
    ILoggingEvent error = logs.list.get(2);
    assertEquals(Level.WARN, error.getLevel());
    assertTrue(error.getFormattedMessage().contains("phase=process_message security=verified"));
    assertTrue(error.getFormattedMessage()
        .contains("httpStatus=503 exception=IllegalStateException reason=OTHER"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, openId, messageId, code,
        browserProof, session, body, exceptionMessage, signature(TOKEN, timestamp, nonce));
  }

  @Test
  void urlVerificationRejectionDoesNotLogEchoOrSignatureValues() throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "private-verification-nonce";
    String echo = "private-verification-echo";
    String invalidSignature = "8".repeat(40);
    mvc.perform(get(PATH)
            .param("signature", invalidSignature)
            .param("timestamp", timestamp)
            .param("nonce", nonce)
            .param("echostr", echo))
        .andExpect(status().isForbidden());

    assertStages("received", "rejected");
    assertTrue(logs.list.get(1).getFormattedMessage()
        .contains("method=GET stage=rejected phase=verify_url security=failed"));
    assertTrue(logs.list.get(1).getFormattedMessage().contains("reason=SIGNATURE_INVALID"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, echo, invalidSignature);
  }

  @Test
  void timestampAndXmlFailuresHaveFixedReasonCodesWithoutRequestValues() throws Exception {
    config.setEncodingAESKey(null);
    String nonce = "private-reason-code-nonce";
    String oldTimestamp = Long.toString(Instant.now().getEpochSecond() - 301);
    String openId = "oFixture_ReasonCodeOpenId12345";
    String messageId = "8112345678901234567";
    String code = "41031526";
    String body = plaintextMessage(oldTimestamp, openId, messageId, code);

    mvc.perform(signedPlaintextPost(body, oldTimestamp, nonce))
        .andExpect(status().isForbidden())
        .andExpect(content().string("invalid message"));
    assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=TIMESTAMP_INVALID"));
    assertNoSensitiveLogs(oldTimestamp, nonce, openId, messageId, code, body,
        signature(TOKEN, oldTimestamp, nonce));

    logs.list.clear();
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String malformedXml = "<xml><Content><![CDATA[" + code + "]]></Content>";
    mvc.perform(signedPlaintextPost(malformedXml, timestamp, nonce))
        .andExpect(status().isForbidden())
        .andExpect(content().string("invalid message"));
    assertStages("received", "rejected");
    assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=MESSAGE_FORMAT_INVALID"));
    assertNoSensitiveLogs(timestamp, nonce, code, malformedXml, signature(TOKEN, timestamp, nonce));
    verifyNoInteractions(auth);
  }

  @Test
  void missingAesKeyAndMissingEncryptedEnvelopeHaveDistinctSafeReasonCodes() throws Exception {
    String encrypted = incomingMessage();
    Element envelope = envelope(encrypted);
    config.setEncodingAESKey(null);
    mvc.perform(signedPost(encrypted))
        .andExpect(status().isForbidden())
        .andExpect(content().string("invalid message"));
    assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=AES_KEY_MISSING"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, encrypted, field(envelope, "Encrypt"),
        field(envelope, "MsgSignature"), field(envelope, "TimeStamp"), field(envelope, "Nonce"));

    logs.list.clear();
    config.setEncodingAESKey(AES_KEY);
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "private-downgrade-nonce";
    String body = plaintextMessage(timestamp, "oFixture_DowngradeOpenId1234",
        "8212345678901234567", "30021415");
    mvc.perform(signedPlaintextPost(body, timestamp, nonce).param("encrypt_type", "aes"))
        .andExpect(status().isForbidden())
        .andExpect(content().string("invalid message"));
    assertStages("received", "rejected");
    assertTrue(logs.list.getLast().getFormattedMessage()
        .contains("reason=ENCRYPTED_MESSAGE_REQUIRED"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, body, "30021415",
        "oFixture_DowngradeOpenId1234", "8212345678901234567");
    verifyNoInteractions(auth);
  }

  @Test
  void encryptedHintsAndEmptyOrInvalidEncryptFieldsNeverReachAuthentication() throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "private-encrypted-hint-nonce";
    String openId = "oFixture_EncryptedHintOpenId123";
    String code = "29011304";
    String body = plaintextMessage(timestamp, openId, "8312345678901234567", code);
    String messageSignature = "private-message-signature-hint";

    for (MockHttpServletRequestBuilder request : List.of(
        signedPlaintextPost(body, timestamp, nonce).param("encrypt_type", "AES"),
        signedPlaintextPost(body, timestamp, nonce).param("encrypt_type", "unsupported"),
        signedPlaintextPost(body, timestamp, nonce).param("encrypt_type", ""),
        signedPlaintextPost(body, timestamp, nonce).param("encrypt_type", " "),
        signedPlaintextPost(body, timestamp, nonce).param("msg_signature", ""),
        signedPlaintextPost(body, timestamp, nonce).param("msg_signature", messageSignature),
        signedPlaintextPost(body.replace("</xml>", "<Encrypt/></xml>"), timestamp, nonce),
        signedPlaintextPost(body.replace("</xml>", "<Encrypt> \n </Encrypt></xml>"), timestamp, nonce))) {
      logs.list.clear();
      mvc.perform(request).andExpect(status().isForbidden())
          .andExpect(content().string("invalid message"));
      assertStages("received", "rejected");
      assertTrue(logs.list.getLast().getFormattedMessage()
          .contains("reason=ENCRYPTED_MESSAGE_REQUIRED"));
      assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, openId, code, body, messageSignature,
          signature(TOKEN, timestamp, nonce));
    }
    for (String encryptedValue : List.of("null", "{}", "[]", "123", "true")) {
      logs.list.clear();
      String malformed = "{\"FromUserName\":\"" + openId + "\",\"MsgType\":\"text\","
          + "\"Content\":\"" + code + "\",\"Encrypt\":" + encryptedValue + "}";
      mvc.perform(signedPlaintextPost(malformed, timestamp, nonce).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
      assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=MESSAGE_FORMAT_INVALID"));
      assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, openId, code, malformed,
          signature(TOKEN, timestamp, nonce));
    }
    verifyNoInteractions(auth);
  }

  @Test
  void badCiphertextAndWrongReceiverNeverFallBackToSignedOuterPlaintext() throws Exception {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "private-corrupt-envelope-nonce";
    String openId = "oFixture_CorruptEnvelopeOpenId12";
    String code = "18001293";
    String encrypted = "private-not-base64-ciphertext";
    String body = plaintextMessage(timestamp, openId, "8412345678901234567", code)
        .replace("</xml>", "<Encrypt><![CDATA[" + encrypted + "]]></Encrypt></xml>");
    String plainSignature = signature(TOKEN, timestamp, nonce);
    String messageSignature = signature(TOKEN, timestamp, nonce, encrypted);
    mvc.perform(signedPlaintextPost(body, timestamp, nonce)
            .param("msg_signature", messageSignature).param("encrypt_type", "aes"))
        .andExpect(status().isForbidden()).andExpect(content().string("invalid message"));
    assertStages("received", "rejected");
    assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=DECRYPT_FAILED"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, timestamp, nonce, openId, code, body, encrypted,
        plainSignature, messageSignature);

    logs.list.clear();
    String validEncryptedEnvelope = incomingMessage();
    config.setAppId("wx-wrong-configured-receiver");
    mvc.perform(signedPost(validEncryptedEnvelope)).andExpect(status().isForbidden());
    assertTrue(logs.list.getLast().getFormattedMessage().contains("reason=DECRYPT_FAILED"));
    assertNoSensitiveLogs(TOKEN, AES_KEY, validEncryptedEnvelope,
        field(envelope(validEncryptedEnvelope), "Encrypt"));
    verifyNoInteractions(auth);
  }

  private String plaintextMessage(String timestamp, String openId, String messageId,
      String content) {
    return """
        <xml>
          <ToUserName><![CDATA[gh_private_fixture_account]]></ToUserName>
          <FromUserName><![CDATA[%s]]></FromUserName>
          <CreateTime>%s</CreateTime>
          <MsgType><![CDATA[text]]></MsgType>
          <Content><![CDATA[%s]]></Content>
          <MsgId>%s</MsgId>
        </xml>
        """.formatted(openId, timestamp, content, messageId);
  }

  private MockHttpServletRequestBuilder signedPlaintextPost(String body, String timestamp,
      String nonce) throws Exception {
    return post(PATH)
        .param("signature", signature(TOKEN, timestamp, nonce))
        .param("timestamp", timestamp)
        .param("nonce", nonce)
        .contentType(MediaType.TEXT_XML)
        .content(body.getBytes(StandardCharsets.UTF_8));
  }

  private void assertStages(String... stages) {
    assertEquals(List.of(stages), logs.list.stream().map(event -> {
      String message = event.getFormattedMessage();
      assertTrue(message.startsWith("wechat_callback tenant=test method="));
      assertTrue(message.matches(".* elapsedMs=[0-9]+ httpStatus=.*"));
      int start = message.indexOf(" stage=") + " stage=".length();
      return message.substring(start, message.indexOf(' ', start));
    }).toList());
  }

  private void assertNoSensitiveLogs(String... sensitiveValues) {
    for (ILoggingEvent event : logs.list) {
      assertNull(event.getThrowableProxy(), "callback diagnostics must not attach exceptions");
      for (String value : sensitiveValues) {
        assertFalse(event.getFormattedMessage().contains(value), "sensitive value in formatted log");
        if (event.getArgumentArray() != null) {
          for (Object argument : event.getArgumentArray()) {
            assertFalse(String.valueOf(argument).contains(value), "sensitive value in log argument");
          }
        }
      }
    }
  }

  private String incomingMessage() {
    return codec.textReply(
        Map.of("FromUserName", "gh-fixture", "ToUserName", "openid-fixture"),
        "scan-fixture",
        TOKEN,
        AES_KEY,
        APP_ID);
  }

  private MockHttpServletRequestBuilder signedPost(String body) throws Exception {
    Element envelope = envelope(body);
    return post(PATH)
        .param("encrypt_type", "aes")
        .param("msg_signature", field(envelope, "MsgSignature"))
        .param("timestamp", field(envelope, "TimeStamp"))
        .param("nonce", field(envelope, "Nonce"))
        .contentType(MediaType.APPLICATION_XML)
        .content(body);
  }

  private static Element envelope(String body) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(body)))
        .getDocumentElement();
  }

  private static String field(Element envelope, String name) {
    return envelope.getElementsByTagName(name).item(0).getTextContent();
  }

  private static String signature(String... values) throws Exception {
    String[] sorted = values.clone();
    Arrays.sort(sorted);
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-1")
                .digest(String.join("", sorted).getBytes(StandardCharsets.UTF_8)));
  }
}
