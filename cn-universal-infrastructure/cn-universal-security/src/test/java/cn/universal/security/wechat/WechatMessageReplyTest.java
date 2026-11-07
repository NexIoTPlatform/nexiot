package cn.universal.security.wechat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.security.support.TenantDomainResolver;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class WechatMessageReplyTest {
  private final SysDictDataMapper dictionaries = mock(SysDictDataMapper.class);
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final WechatAccountConfigService configs = mock(WechatAccountConfigService.class);
  private final WechatLoginConfig config = new WechatLoginConfig();
  private WechatAuthService service;

  @BeforeEach
  void setup() {
    config.setAppId("wx_test");
    service = new WechatAuthService(redis, configs, mock(WechatQrApiClient.class),
        mock(IoTUserMapper.class), mock(IoTTenantMapper.class),
        mock(TenantDomainResolver.class), dictionaries);
  }

  @Test
  void defaultCoversUnmatchedTextScansEventsAndMedia() {
    String body = "欢迎 👋\n请发送“帮助”。";
    when(dictionaries.selectDictDataByType("wechat_reply"))
        .thenReturn(List.of(item("默认回复", "default", body)));

    assertEquals(body, service.handleMessage(config, text("没有匹配")));
    assertEquals(body, service.handleMessage(config, event("SCAN", "unrelated-scene")));
    assertEquals(body, service.handleMessage(config, event("subscribe", "")));
    assertEquals(body, service.handleMessage(config, event("CLICK", "unknown")));
    assertEquals(body, service.handleMessage(config,
        Map.of("FromUserName", "openid", "MsgType", "image")));
    verifyNoInteractions(redis, configs);
  }

  @Test
  void optionalEventRepliesFallBackToTheConfiguredDefault() {
    when(dictionaries.selectDictDataByType("wechat_reply")).thenReturn(List.of(
        item("默认", "default", "默认回复"), item("关注", "subscribe", "关注成功 👋"),
        item("扫码", "scan", "扫码提示")));

    assertEquals("关注成功 👋", service.handleMessage(config, event("subscribe", "")));
    assertEquals("扫码提示", service.handleMessage(config, event("SCAN", "other-scene")));
    assertEquals("默认回复", service.handleMessage(config, event("CLICK", "unknown")));
  }

  @Test
  void keywordMatchesLabelExactlyAndPreservesReplyWhitespaceAndEmoji() {
    String body = "  操作说明 👩‍💻\n第一步：打开页面。\n第二步：扫码。\n";
    when(dictionaries.selectDictDataByType("wechat_keyword_reply"))
        .thenReturn(List.of(item("帮助", "help", body)));

    assertEquals(body, service.handleMessage(config, text("  帮助  ")));
    assertNotEquals(body, service.handleMessage(config, text("请帮助我")));
    assertNotEquals(body, service.handleMessage(config, text("help")));
    assertNotEquals(body, service.handleMessage(config, text("qrscene_帮助")));
  }

  @Test
  void keywordsMatchEventKeysAndScannedSceneKeys() {
    when(dictionaries.selectDictDataByType("wechat_keyword_reply"))
        .thenReturn(List.of(item("help", "help", "使用说明")));

    assertEquals("使用说明", service.handleMessage(config, event("CLICK", "HELP")));
    assertEquals("使用说明", service.handleMessage(config, event("SCAN", "help")));
    assertEquals("使用说明", service.handleMessage(config, event("subscribe", "qrscene_help")));
  }

  @Test
  void disabledOrBlankEntriesFallBackAndEditsApplyOnNextMessage() {
    SysDictData disabled = item("帮助", "help", "禁用正文").setStatus("1");
    when(dictionaries.selectDictDataByType("wechat_keyword_reply"))
        .thenReturn(List.of(disabled, item("帮助", "empty", " \n")));
    when(dictionaries.selectDictDataByType("wechat_reply"))
        .thenReturn(List.of(item("默认", "default", "默认一")))
        .thenReturn(List.of(item("默认", "default", "默认二")))
        .thenReturn(List.of(item("默认", "default", "\n")));

    assertEquals("默认一", service.handleMessage(config, text("帮助")));
    assertEquals("默认二", service.handleMessage(config, text("帮助")));
    assertTrue(service.handleMessage(config, text("帮助")).contains("欢迎关注 NexIoT"));
  }

  @Test
  void firstEnabledNonblankKeywordReplyWinsInMapperSortOrder() {
    when(dictionaries.selectDictDataByType("wechat_keyword_reply")).thenReturn(List.of(
        item("帮助", "first", "优先回复"), item("帮助", "second", "后续回复")));
    assertEquals("优先回复", service.handleMessage(config, text("帮助")));
  }

  @Test
  void dictionaryOutageKeepsBuiltInFallbackAvailable() {
    when(dictionaries.selectDictDataByType(anyString()))
        .thenThrow(new DataAccessResourceFailureException("dictionary unavailable"));
    assertTrue(service.handleMessage(config, text("任意文本")).contains("欢迎关注 NexIoT"));
  }

  @Test
  void unsubscribeStillClearsBindingsWithoutReplyOrDictionaryQueries() {
    assertNull(service.handleMessage(config, event("unsubscribe", "")));
    verify(configs).clearBindings("wx_test", "openid");
    verifyNoInteractions(redis, dictionaries);
  }

  @Test
  void expiredLoginSceneUsesConfiguredErrorInsteadOfGenericReply() {
    when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
    when(dictionaries.selectDictDataByType("wechat_reply"))
        .thenReturn(List.of(item("过期提示", "session_expired", "二维码过期 ⏱️\n请刷新页面。")));

    assertEquals("二维码过期 ⏱️\n请刷新页面。",
        service.handleMessage(config, event("SCAN", "nx_" + "a".repeat(32))));
    verify(dictionaries, never()).selectDictDataByType("wechat_keyword_reply");
  }

  @ParameterizedTest
  @ValueSource(strings = {"123456", "1234567", "12345678"})
  void generatedCodeLengthsTakePriorityOverKeywordRules(String code) {
    when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
    when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

    assertTrue(service.handleMessage(config, text("登录 " + code)).contains("验证码无效或已过期"));
    verify(dictionaries, never()).selectDictDataByType("wechat_keyword_reply");
  }

  private static SysDictData item(String label, String value, String body) {
    return SysDictData.builder().dictLabel(label).dictValue(value).remark(body).status("0").build();
  }

  private static Map<String, String> text(String content) {
    return Map.of("FromUserName", "openid", "MsgType", "text", "Content", content);
  }

  private static Map<String, String> event(String event, String key) {
    return Map.of("FromUserName", "openid", "MsgType", "event", "Event", event, "EventKey", key);
  }
}
