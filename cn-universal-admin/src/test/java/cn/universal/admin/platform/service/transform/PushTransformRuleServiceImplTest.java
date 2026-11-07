package cn.universal.admin.platform.service.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.transform.impl.PushTransformRuleServiceImpl;
import cn.universal.dm.device.service.push.transform.AppPushConfigChangeNotifier;
import cn.universal.dm.device.service.push.transform.PushTransformService;
import cn.universal.persistence.entity.IoTPushTransformRule;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTPushTransformRuleMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushTransformRuleServiceImplTest {

  @InjectMocks private PushTransformRuleServiceImpl service;

  @Mock private IoTPushTransformRuleMapper ruleMapper;
  @Mock private IoTUserApplicationMapper applicationMapper;
  @Mock private PushTransformService transformService;
  @Mock private AppPushConfigChangeNotifier configChangeNotifier;

  @Test
  void saveAndPublishShouldNormalizeToSingleApplicationRule() {
    IoTUserApplication application =
        IoTUserApplication.builder()
            .appUniqueId("app-1")
            .cfg("{\"http\":{\"enable\":true}}")
            .build();
    when(ruleMapper.selectByAppUniqueId("app-1")).thenReturn(List.of());
    when(applicationMapper.selectOne(any(IoTUserApplication.class))).thenReturn(application);
    when(ruleMapper.selectRuleById(anyLong()))
        .thenAnswer(
            invocation ->
                IoTPushTransformRule.builder()
                    .id(invocation.getArgument(0))
                    .appUniqueId("app-1")
                    .scopeType("GLOBAL")
                    .ruleType("TEMPLATE")
                    .status(1)
                    .build());
    IoTPushTransformRule input =
        IoTPushTransformRule.builder()
            .ruleName("产品级旧规则")
            .scopeType("PRODUCT")
            .productKey("pk-legacy")
            .ruleType("template")
            .content("{\"deviceId\":\"${deviceId}\"}")
            .build();

    IoTPushTransformRule result = service.saveAndPublishRule("app-1", input, "operator-1");

    ArgumentCaptor<IoTPushTransformRule> savedCaptor =
        ArgumentCaptor.forClass(IoTPushTransformRule.class);
    verify(ruleMapper).insertSelective(savedCaptor.capture());
    IoTPushTransformRule saved = savedCaptor.getValue();
    assertEquals("app-1", saved.getAppUniqueId());
    assertEquals("GLOBAL", saved.getScopeType());
    assertNull(saved.getProductKey());
    assertEquals("TEMPLATE", saved.getRuleType());
    assertEquals(1, saved.getStatus());
    assertNotNull(saved.getPublishTime());

    verify(ruleMapper).disablePublishedRulesForApp("app-1", saved.getId(), "operator-1");
    verify(ruleMapper).softDeleteOtherRulesForApp("app-1", saved.getId(), "operator-1");
    verify(configChangeNotifier)
        .notifyAppConfigChanged("app-1", "RULE_SAVED_AND_PUBLISHED", saved.getId());

    ArgumentCaptor<IoTUserApplication> applicationCaptor =
        ArgumentCaptor.forClass(IoTUserApplication.class);
    verify(applicationMapper).updateByPrimaryKeySelective(applicationCaptor.capture());
    JSONObject cfg = JSONUtil.parseObj(applicationCaptor.getValue().getCfg());
    assertEquals(true, cfg.getJSONObject("http").getBool("enable"));
    assertEquals(true, cfg.getJSONObject("transform").getBool("enable"));
    assertEquals("ORIGINAL", cfg.getJSONObject("transform").getStr("fallbackMode"));
    assertNotNull(result.getId());
  }
}
