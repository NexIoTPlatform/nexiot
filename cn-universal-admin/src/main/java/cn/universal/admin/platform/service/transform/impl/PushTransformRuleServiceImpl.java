package cn.universal.admin.platform.service.transform.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.transform.IPushTransformRuleService;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.push.transform.AppPushConfigChangeNotifier;
import cn.universal.dm.device.service.push.transform.PushTransformService;
import cn.universal.persistence.entity.IoTPushTransformRule;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.bo.PushTransformPreviewBO;
import cn.universal.persistence.entity.bo.UPPushBO;
import cn.universal.persistence.entity.vo.PushTransformOverviewVO;
import cn.universal.persistence.entity.vo.PushTransformPreviewVO;
import cn.universal.persistence.mapper.IoTPushTransformRuleMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PushTransformRuleServiceImpl implements IPushTransformRuleService {

  private static final int STATUS_DRAFT = 0;
  private static final int STATUS_PUBLISHED = 1;
  private static final String SCOPE_APPLICATION = "GLOBAL";
  private static final String DEFAULT_RULE_NAME = "应用消息格式";

  @Resource private IoTPushTransformRuleMapper ioTPushTransformRuleMapper;
  @Resource private IoTUserApplicationMapper ioTUserApplicationMapper;
  @Resource private PushTransformService pushTransformService;
  @Resource private AppPushConfigChangeNotifier appPushConfigChangeNotifier;

  @Override
  public PushTransformOverviewVO getOverview(String appUniqueId) {
    IoTPushTransformRule rule = findApplicationRule(appUniqueId);
    return PushTransformOverviewVO.builder()
        .config(readTransformConfig(appUniqueId))
        .rule(rule)
        .rules(rule == null ? List.of() : List.of(rule))
        .presets(pushTransformService.listPresets())
        .build();
  }

  @Override
  public IoTPushTransformRule getRule(Long id) {
    IoTPushTransformRule rule = ioTPushTransformRuleMapper.selectRuleById(id);
    if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
      throw new IoTException("规则不存在");
    }
    return rule;
  }

  @Override
  @Transactional
  public IoTPushTransformRule saveRule(
      String appUniqueId, IoTPushTransformRule input, String operator) {
    validateRule(appUniqueId, input);
    IoTPushTransformRule saved = upsertRule(appUniqueId, input, operator, STATUS_DRAFT, null);
    ioTPushTransformRuleMapper.softDeleteOtherRulesForApp(appUniqueId, saved.getId(), operator);
    appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "RULE_SAVED", saved.getId());
    return saved;
  }

  @Override
  @Transactional
  public IoTPushTransformRule saveAndPublishRule(
      String appUniqueId, IoTPushTransformRule input, String operator) {
    validateRule(appUniqueId, input);
    IoTPushTransformRule normalized = normalizedRule(appUniqueId, input);
    pushTransformService.validateForPublish(normalized);

    IoTPushTransformRule saved =
        upsertRule(appUniqueId, normalized, operator, STATUS_PUBLISHED, new Date());
    ioTPushTransformRuleMapper.disablePublishedRulesForApp(appUniqueId, saved.getId(), operator);
    ioTPushTransformRuleMapper.softDeleteOtherRulesForApp(appUniqueId, saved.getId(), operator);

    UPPushBO.TransformConfig config = readTransformConfig(appUniqueId);
    config.setEnable(true);
    config.setSupport(true);
    config.setDefaultRuleId(null);
    writeTransformConfig(appUniqueId, config);
    appPushConfigChangeNotifier.notifyAppConfigChanged(
        appUniqueId, "RULE_SAVED_AND_PUBLISHED", saved.getId());
    return getRule(saved.getId());
  }

  @Override
  @Transactional
  public IoTPushTransformRule publishRule(String appUniqueId, Long id, String operator) {
    IoTPushTransformRule rule = assertRuleOwned(appUniqueId, id);
    pushTransformService.validateForPublish(rule);
    ioTPushTransformRuleMapper.disablePublishedRulesForApp(appUniqueId, id, operator);

    IoTPushTransformRule update = new IoTPushTransformRule();
    update.setId(id);
    update.setScopeType(SCOPE_APPLICATION);
    update.setProductKey(null);
    update.setStatus(STATUS_PUBLISHED);
    update.setPublishTime(new Date());
    update.setUpdateBy(operator);
    update.setUpdateTime(new Date());
    ioTPushTransformRuleMapper.updateByPrimaryKeySelective(update);
    ioTPushTransformRuleMapper.softDeleteOtherRulesForApp(appUniqueId, id, operator);

    UPPushBO.TransformConfig config = readTransformConfig(appUniqueId);
    config.setEnable(true);
    config.setSupport(true);
    config.setDefaultRuleId(null);
    writeTransformConfig(appUniqueId, config);
    appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "RULE_PUBLISHED", id);
    return getRule(id);
  }

  @Override
  @Transactional
  public boolean deleteRule(String appUniqueId, Long id, String operator) {
    IoTPushTransformRule rule = assertRuleOwned(appUniqueId, id);
    IoTPushTransformRule update = new IoTPushTransformRule();
    update.setId(rule.getId());
    update.setDeleted(1);
    update.setStatus(2);
    update.setUpdateBy(operator);
    update.setUpdateTime(new Date());
    int result = ioTPushTransformRuleMapper.updateByPrimaryKeySelective(update);
    if (result > 0) {
      UPPushBO.TransformConfig config = readTransformConfig(appUniqueId);
      config.setEnable(false);
      config.setDefaultRuleId(null);
      writeTransformConfig(appUniqueId, config);
      appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "RULE_DELETED", id);
    }
    return result > 0;
  }

  @Override
  @Transactional
  public UPPushBO.TransformConfig updateConfig(
      String appUniqueId, UPPushBO.TransformConfig input, String operator) {
    UPPushBO.TransformConfig config =
        input == null
            ? UPPushBO.TransformConfig.builder()
                .enable(false)
                .support(true)
                .fallbackMode("ORIGINAL")
                .build()
            : input;
    String fallbackMode = StrUtil.blankToDefault(config.getFallbackMode(), "ORIGINAL");
    if (!"ORIGINAL".equalsIgnoreCase(fallbackMode) && !"BLOCK".equalsIgnoreCase(fallbackMode)) {
      throw new IoTException("转换失败处理方式仅支持 ORIGINAL 或 BLOCK");
    }
    if (config.isEnable() && findApplicationRule(appUniqueId) == null) {
      throw new IoTException("请先配置应用消息格式");
    }
    config.setSupport(true);
    config.setDefaultRuleId(null);
    config.setFallbackMode(fallbackMode.toUpperCase());
    writeTransformConfig(appUniqueId, config);
    appPushConfigChangeNotifier.notifyAppConfigChanged(appUniqueId, "CONFIG_UPDATED");
    return config;
  }

  @Override
  public PushTransformPreviewVO preview(PushTransformPreviewBO previewBO) {
    return pushTransformService.preview(previewBO);
  }

  private IoTPushTransformRule upsertRule(
      String appUniqueId,
      IoTPushTransformRule input,
      String operator,
      int status,
      Date publishTime) {
    IoTPushTransformRule existing =
        input.getId() == null
            ? findApplicationRule(appUniqueId)
            : assertRuleOwned(appUniqueId, input.getId());
    IoTPushTransformRule normalized = normalizedRule(appUniqueId, input);
    Date now = new Date();
    if (existing == null) {
      normalized.setId(IdUtil.getSnowflake().nextId());
      normalized.setStatus(status);
      normalized.setVersion(1);
      normalized.setDeleted(0);
      normalized.setCreateBy(operator);
      normalized.setUpdateBy(operator);
      normalized.setCreateTime(now);
      normalized.setUpdateTime(now);
      normalized.setPublishTime(publishTime);
      ioTPushTransformRuleMapper.insertSelective(normalized);
      return normalized;
    }

    normalized.setId(existing.getId());
    normalized.setStatus(status);
    normalized.setVersion(defaultInt(existing.getVersion()) + 1);
    normalized.setDeleted(0);
    normalized.setUpdateBy(operator);
    normalized.setUpdateTime(now);
    normalized.setPublishTime(publishTime);
    ioTPushTransformRuleMapper.updateByPrimaryKeySelective(normalized);
    return getRule(existing.getId());
  }

  private IoTPushTransformRule normalizedRule(String appUniqueId, IoTPushTransformRule input) {
    return IoTPushTransformRule.builder()
        .id(input.getId())
        .appUniqueId(appUniqueId)
        .ruleName(StrUtil.blankToDefault(input.getRuleName(), DEFAULT_RULE_NAME))
        .scopeType(SCOPE_APPLICATION)
        .productKey(null)
        .ruleType(StrUtil.blankToDefault(input.getRuleType(), "").toUpperCase())
        .presetCode(input.getPresetCode())
        .content(input.getContent())
        .options(StrUtil.blankToDefault(input.getOptions(), "{}"))
        .sampleInput(input.getSampleInput())
        .sampleOutput(input.getSampleOutput())
        .build();
  }

  private void validateRule(String appUniqueId, IoTPushTransformRule rule) {
    if (StrUtil.isBlank(appUniqueId)) {
      throw new IoTException("应用标识不能为空");
    }
    if (rule == null || StrUtil.isBlank(rule.getRuleType())) {
      throw new IoTException("请选择消息格式");
    }
    if ("PRESET".equalsIgnoreCase(rule.getRuleType()) && StrUtil.isBlank(rule.getPresetCode())) {
      throw new IoTException("请选择内置格式");
    }
    if (("TEMPLATE".equalsIgnoreCase(rule.getRuleType())
            || "SCRIPT".equalsIgnoreCase(rule.getRuleType()))
        && StrUtil.isBlank(rule.getContent())) {
      throw new IoTException("转换内容不能为空");
    }
  }

  private IoTPushTransformRule assertRuleOwned(String appUniqueId, Long id) {
    IoTPushTransformRule rule = getRule(id);
    if (!appUniqueId.equals(rule.getAppUniqueId())) {
      throw new IoTException("无权访问该规则");
    }
    return rule;
  }

  private IoTPushTransformRule findApplicationRule(String appUniqueId) {
    List<IoTPushTransformRule> rules =
        ioTPushTransformRuleMapper.selectByAppUniqueId(appUniqueId);
    return rules == null || rules.isEmpty() ? null : rules.get(0);
  }

  private int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }

  private UPPushBO.TransformConfig readTransformConfig(String appUniqueId) {
    IoTUserApplication application = getApplication(appUniqueId);
    if (application == null || StrUtil.isBlank(application.getCfg())) {
      return defaultTransformConfig();
    }
    JSONObject transform = JSONUtil.parseObj(application.getCfg()).getJSONObject("transform");
    if (transform == null) {
      return defaultTransformConfig();
    }
    return UPPushBO.TransformConfig.builder()
        .enable(transform.getBool("enable", false))
        .support(transform.getBool("support", true))
        .fallbackMode(transform.getStr("fallbackMode", "ORIGINAL"))
        .build();
  }

  private UPPushBO.TransformConfig defaultTransformConfig() {
    return UPPushBO.TransformConfig.builder()
        .enable(false)
        .support(true)
        .fallbackMode("ORIGINAL")
        .build();
  }

  private void writeTransformConfig(
      String appUniqueId, UPPushBO.TransformConfig transformConfig) {
    IoTUserApplication application = getApplication(appUniqueId);
    if (application == null) {
      throw new IoTException("应用不存在");
    }
    JSONObject cfg =
        StrUtil.isBlank(application.getCfg())
            ? new JSONObject()
            : JSONUtil.parseObj(application.getCfg());
    JSONObject transform = new JSONObject();
    transform.set("enable", transformConfig.isEnable());
    transform.set("support", true);
    transform.set(
        "fallbackMode", StrUtil.blankToDefault(transformConfig.getFallbackMode(), "ORIGINAL"));
    cfg.set("transform", transform);
    IoTUserApplication update = new IoTUserApplication();
    update.setAppUniqueId(appUniqueId);
    update.setCfg(cfg.toString());
    ioTUserApplicationMapper.updateByPrimaryKeySelective(update);
  }

  private IoTUserApplication getApplication(String appUniqueId) {
    return ioTUserApplicationMapper.selectOne(
        IoTUserApplication.builder().appUniqueId(appUniqueId).build());
  }
}
