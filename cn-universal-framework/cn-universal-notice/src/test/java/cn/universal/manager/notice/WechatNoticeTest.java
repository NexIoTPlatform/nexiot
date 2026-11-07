package cn.universal.manager.notice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import cn.universal.manager.notice.mapper.NoticeTemplateMapper;
import cn.universal.manager.notice.model.*;
import cn.universal.manager.notice.service.*;
import cn.universal.manager.notice.service.channel.*;
import cn.universal.manager.notice.service.impl.NoticeServiceImpl;
import cn.universal.manager.notice.util.WechatTemplateRenderer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WechatNoticeTest {
  private NoticeTemplate template() {
    NoticeTemplate template = new NoticeTemplate();
    template.setId(7L); template.setChannelId(8L); template.setChannelType("wechat");
    template.setStatus("1"); template.setCreator("tenant-a");
    template.setContent("姓名: {{deviceName.DATA}} 金额: {{amount01.DATA}} 行程: {{thing01.DATA}} 日期: {{date01.DATA}}");
    WechatTemplateConfig config = new WechatTemplateConfig();
    config.setTemplateId("f74lsL45O3dD7xbT67FP4QanYDy-tOf_z67GqfqYawE");
    config.setUrl("https://example.com/device/#{deviceId}");
    config.setMiniProgramAppId("wx123"); config.setMiniProgramPage("pages/device?id=#{deviceId}");
    List<WechatTemplateConfig.Field> fields = new ArrayList<>();
    for (String[] mapping : new String[][] {{"deviceName", "deviceName"}, {"amount01", "properties.amount"}, {"thing01", "properties.trip"}, {"date01", "execTime"}}) {
      WechatTemplateConfig.Field field = new WechatTemplateConfig.Field();
      field.setKey(mapping[0]); field.setValue("#{" + mapping[1] + "}"); fields.add(field);
    }
    config.setFields(fields); template.setWechat(config); return template;
  }
  private NoticeChannel channel() {
    NoticeChannel channel = new NoticeChannel(); channel.setId(8L); channel.setChannelType("wechat"); channel.setStatus("0");
    channel.setConfig("{\"appId\":\"wx123\",\"appSecret\":\"test-only\",\"templateId\":\"legacy\"}"); return channel;
  }
  private Map<String, Object> params() {
    return new HashMap<>(Map.of("deviceName", "设备\"A\"\n一号", "deviceId", "dev-1", "properties", Map.of("amount", 0, "trip", "上海→杭州"), "execTime", "2026-09-14 16:00:00", "unrelated", "must not leak"));
  }
  @Test void rendersExactWechatFieldsFromRuleContextAndBothJumps() {
    JSONObject body = WechatTemplateRenderer.render(template(), channel(), params());
    assertEquals("f74lsL45O3dD7xbT67FP4QanYDy-tOf_z67GqfqYawE", body.getStr("template_id"));
    assertEquals(Set.of("deviceName", "amount01", "thing01", "date01"), body.getJSONObject("data").keySet());
    assertEquals("0", body.getJSONObject("data").getJSONObject("amount01").getStr("value"));
    assertEquals(params().get("deviceName"), JSONUtil.parseObj(body.toString()).getJSONObject("data").getJSONObject("deviceName").getStr("value"));
    assertEquals("https://example.com/device/dev-1", body.getStr("url"));
    assertEquals("pages/device?id=dev-1", body.getJSONObject("miniprogram").getStr("pagepath"));
  }
  @Test void flatUiParametersAndNestedExternalParametersRenderIdentically() {
    Map<String,Object> flat = params(); flat.remove("properties"); flat.put("properties.amount", 0); flat.put("properties.trip", "上海→杭州");
    assertEquals(WechatTemplateRenderer.render(template(), channel(), params()).toString(), WechatTemplateRenderer.render(template(), channel(), flat).toString());
  }
  @Test void missingValuesAndInvalidMappingsFailBeforeSending() {
    Map<String,Object> missing = params(); missing.remove("properties");
    assertTrue(assertThrows(IllegalArgumentException.class, () -> WechatTemplateRenderer.render(template(), channel(), missing)).getMessage().contains("properties.amount"));
    NoticeTemplate template = template(); WechatTemplateConfig config = template.getWechat(); config.getFields().remove(0); template.setWechat(config);
    assertTrue(assertThrows(IllegalArgumentException.class, () -> WechatTemplateRenderer.validate(template)).getMessage().contains("deviceName"));
    config = template().getWechat(); config.getFields().get(1).setKey("deviceName"); template.setWechat(config);
    assertTrue(assertThrows(IllegalArgumentException.class, () -> WechatTemplateRenderer.validate(template)).getMessage().contains("重复"));
  }
  @Test void neverTreatsUnmappedLegacyContextAsWechatData() {
    NoticeTemplate legacy = template(); legacy.setWechat(null);
    assertThrows(IllegalArgumentException.class, () -> WechatTemplateRenderer.render(legacy, channel(), params()));
    assertEquals("legacy", WechatTemplateRenderer.render(legacy, channel(), Map.of("data", Map.of("thing1", "legacy explicit"))).getStr("template_id"));
  }
  @Test void testRuleAndExternalSendUseSamePayloadAndTestOnlyTargetsSpecifiedRecipient() {
    NoticeTemplateMapper mapper = mock(NoticeTemplateMapper.class);
    NoticeChannelService channels = mock(NoticeChannelService.class);
    NoticeRecordService records = mock(NoticeRecordService.class);
    WechatApiClient client = mock(WechatApiClient.class);
    WechatOpenIdService openIds = mock(WechatOpenIdService.class);
    NoticeTemplate template = template(); template.setReceivers("openid_default_user");
    when(mapper.selectByPrimaryKey(7L)).thenReturn(template);
    when(channels.getById(8L)).thenReturn(channel());
    when(client.token(anyString(), anyString())).thenReturn("stable-test");
    when(client.send(anyString(), any())).thenReturn(JSONUtil.parseObj("{\"errcode\":0,\"msgid\":12345}"));
    NoticeServiceImpl service = new NoticeServiceImpl(mapper, records, channels, List.of(new WechatTemplateNoticeSendChannel(openIds, client)));
    org.springframework.test.util.ReflectionTestUtils.setField(service, "access", mock(NoticeAccess.class));
    NoticeSendRequest request = new NoticeSendRequest(); request.setTemplateId(7L); request.setReceivers("openid_test_user"); request.setParams(params());
    assertTrue(service.test(request).isSuccess());
    ArgumentCaptor<JSONObject> sent = ArgumentCaptor.forClass(JSONObject.class);
    verify(client).send(eq("stable-test"), sent.capture());
    assertEquals("openid_test_user", sent.getValue().getStr("touser"));
    JSONObject accepted = JSONUtil.parseObj(sent.getValue().toString()); accepted.remove("touser");
    assertEquals(service.preview(request).get("payload").toString(), accepted.toString());
    clearInvocations(client);
    assertTrue(service.sendR(request).isSuccess());
    verify(client, times(2)).send(eq("stable-test"), sent.capture());
    for (JSONObject body : sent.getAllValues()) assertEquals("0", body.getJSONObject("data").getJSONObject("amount01").getStr("value"));
    request.setParams(Map.of()); clearInvocations(client);
    assertFalse(service.sendR(request).isSuccess()); verifyNoInteractions(client);
    ArgumentCaptor<NoticeSendRecord> saved = ArgumentCaptor.forClass(NoticeSendRecord.class); verify(records, times(3)).save(saved.capture());
    assertEquals("FAIL", saved.getAllValues().get(2).getStatus());
  }
  @Test void reportsPartialFailuresAndAcceptedMessageIds() {
    WechatApiClient client = mock(WechatApiClient.class);
    when(client.token(anyString(), anyString())).thenReturn("token");
    when(client.send(anyString(), any())).thenReturn(JSONUtil.parseObj("{\"errcode\":0,\"msgid\":123}"), JSONUtil.parseObj("{\"errcode\":43004,\"errmsg\":\"require subscribe\"}"));
    NoticeSendResult result = new WechatTemplateNoticeSendChannel(mock(WechatOpenIdService.class), client).send(WechatTemplateRenderer.render(template(), channel(), params()).toString(), "openid_first_user,openid_second_user", channel(), params());
    assertFalse(result.isSuccess()); assertEquals(List.of("123"), result.getMessageIds()); assertTrue(result.getErrorMessage().contains("43004"));
  }
  @Test void concurrentStableTokenUsesOrdinaryRefreshAndCaches() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    WechatApiClient client = new WechatApiClient() {
      @Override protected JSONObject post(String url, JSONObject body) {
        assertTrue(url.endsWith("/stable_token")); assertFalse(body.getBool("force_refresh"));
        calls.incrementAndGet(); return JSONUtil.parseObj("{\"access_token\":\"stable\",\"expires_in\":7200}");
      }
    };
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<String>> tasks = new ArrayList<>();
      for (int i=0;i<30;i++) tasks.add(executor.submit(() -> client.token("appid", "secret")));
      for (Future<String> task : tasks) assertEquals("stable", task.get());
    }
    assertEquals(1, calls.get());
  }
  @Test void refreshesOnlyAfterExplicitTokenRejection() {
    WechatApiClient client = mock(WechatApiClient.class);
    when(client.token(anyString(), anyString())).thenReturn("old", "stable-new");
    when(client.send(eq("old"), any())).thenReturn(JSONUtil.parseObj("{\"errcode\":42001}"));
    when(client.send(eq("stable-new"), any())).thenReturn(JSONUtil.parseObj("{\"errcode\":0,\"msgid\":99}"));
    NoticeSendResult result = new WechatTemplateNoticeSendChannel(mock(WechatOpenIdService.class), client).send(WechatTemplateRenderer.render(template(), channel(), params()).toString(), "openid_first_user", channel(), params());
    assertTrue(result.isSuccess()); verify(client).invalidate("wx123", "old");
    verify(client, times(2)).send(anyString(), any());
  }

}
