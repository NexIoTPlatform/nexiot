package cn.universal.rule.scene.deviceDown;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import cn.universal.core.message.UPRequest;
import cn.universal.manager.notice.mapper.NoticeTemplateMapper;
import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.model.NoticeTemplate;
import cn.universal.manager.notice.model.WechatTemplateConfig;
import cn.universal.manager.notice.service.NoticeChannelService;
import cn.universal.manager.notice.service.NoticeAccess;
import cn.universal.manager.notice.service.NoticeRecordService;
import cn.universal.manager.notice.service.WechatOpenIdService;
import cn.universal.manager.notice.service.channel.WechatApiClient;
import cn.universal.manager.notice.service.channel.WechatTemplateNoticeSendChannel;
import cn.universal.manager.notice.service.impl.NoticeServiceImpl;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.DeptDataPermissionSupport;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class WechatRuleNoticeTest {
  @Test void deviceUpEventFlowsThroughRuleActionIntoWechatAndFailurePropagates() {
    NoticeTemplate template = new NoticeTemplate(); template.setId(7L); template.setChannelId(8L); template.setStatus("1"); template.setChannelType("wechat"); template.setCreator("tenant-a");
    template.setContent("姓名: {{deviceName.DATA}} 金额: {{amount01.DATA}} 行程: {{thing01.DATA}} 日期: {{date01.DATA}}");
    template.setWechat(JSONUtil.toBean("""
      {"templateId":"f74lsL45O3dD7xbT67FP4QanYDy-tOf_z67GqfqYawE","fields":[
        {"key":"deviceName","value":"#{deviceName}"},
        {"key":"amount01","value":"#{properties.amount}"},
        {"key":"thing01","value":"#{properties.trip}"},
        {"key":"date01","value":"#{execTime}"}]}
      """, WechatTemplateConfig.class));
    NoticeChannel channel = new NoticeChannel(); channel.setId(8L); channel.setStatus("0"); channel.setChannelType("wechat"); channel.setConfig("{\"appId\":\"wxTest\",\"appSecret\":\"test-only\"}");
    template.setTenantId("tenant-a");
    channel.setTenantId("tenant-a");
    channel.setCreator("tenant-a");
    NoticeTemplateMapper mapper = mock(NoticeTemplateMapper.class); when(mapper.selectByPrimaryKey(7L)).thenReturn(template);
    NoticeChannelService channels = mock(NoticeChannelService.class); when(channels.getById(8L)).thenReturn(channel);
    WechatApiClient client = mock(WechatApiClient.class); when(client.token(anyString(), anyString())).thenReturn("stable");
    when(client.send(anyString(), any())).thenReturn(JSONUtil.parseObj("{\"errcode\":0,\"msgid\":123}"));
    NoticeServiceImpl notices = new NoticeServiceImpl(mapper, mock(NoticeRecordService.class), channels, List.of(new WechatTemplateNoticeSendChannel(mock(WechatOpenIdService.class), client)));
    IoTUser actor = new IoTUser(); actor.setUnionId("tenant-a"); actor.setTenantId("tenant-a");
    IoTUserService users = mock(IoTUserService.class);
    when(users.selectUserByUnionId("tenant-a")).thenReturn(actor);
    DeptDataPermissionScope scope = mock(DeptDataPermissionScope.class);
    when(scope.getScopeUnionIds()).thenReturn(Set.of("tenant-a"));
    DeptDataPermissionSupport departments = mock(DeptDataPermissionSupport.class);
    when(departments.resolveScope("tenant-a")).thenReturn(scope);
    ReflectionTestUtils.setField(notices, "access", new NoticeAccess(users, mock(OauthClientDetailsMapper.class), departments));
    SenceIoTDeviceDownService rules = new SenceIoTDeviceDownService(); ReflectionTestUtils.setField(rules, "noticeService", notices); ReflectionTestUtils.setField(rules, "noticeTemplates", mapper);
    ReflectionTestUtils.setField(rules, "ioTUserMapper", mock(cn.universal.persistence.mapper.IoTUserMapper.class));
    SceneLinkage scene = new SceneLinkage(); scene.setId(9L); scene.setSceneName("行程通知"); scene.setCreateBy("tenant-a"); scene.setExecAction("[{\"trigger\":\"notice\",\"noticeTemplateId\":\"7\",\"receivers\":\"openid_rule_user\"}]");
    UPRequest up = new UPRequest(); up.setDeviceName("车载设备"); up.setProperties(Map.of("amount", 12.5, "trip", "上海→杭州"));
    assertTrue(rules.doIoTDeviceFunction(up, scene).getFirst().isSuccess());
    ArgumentCaptor<JSONObject> sent = ArgumentCaptor.forClass(JSONObject.class); verify(client).send(eq("stable"), sent.capture());
    JSONObject data = sent.getValue().getJSONObject("data");
    assertEquals("12.5", data.getJSONObject("amount01").getStr("value"));
    assertEquals("上海→杭州", data.getJSONObject("thing01").getStr("value"));
    assertEquals("车载设备", data.getJSONObject("deviceName").getStr("value"));
    assertTrue(data.getJSONObject("date01").getStr("value").matches("\\d{4}-\\d{2}-\\d{2} .+"));
    up.setProperties(Map.of("trip", "缺少金额")); clearInvocations(client);
    assertFalse(rules.doIoTDeviceFunction(up, scene).getFirst().isSuccess());
    verifyNoInteractions(client);
    scene.setCreateBy("tenant-b");
    assertFalse(rules.doIoTDeviceFunction(up, scene).getFirst().isSuccess());
    verifyNoInteractions(client);
  }
}
