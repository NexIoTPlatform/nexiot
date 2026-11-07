package cn.universal.dm.device.service.push.transform;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.push.transform.executor.ScriptPushTransformExecutor;
import cn.universal.dm.device.service.push.transform.executor.TemplatePushTransformExecutor;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTPushTransformRule;
import cn.universal.persistence.entity.bo.PushTransformPreviewBO;
import cn.universal.persistence.entity.bo.UPPushBO;
import cn.universal.persistence.entity.vo.PushTransformPresetVO;
import cn.universal.persistence.entity.vo.PushTransformPreviewVO;
import cn.universal.persistence.mapper.IoTPushTransformRuleMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushTransformService {

  private final Cache<String, Optional<IoTPushTransformRule>> appRuleCache =
      Caffeine.newBuilder().maximumSize(5000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  @Resource private IoTPushTransformRuleMapper ioTPushTransformRuleMapper;

  @Resource private TemplatePushTransformExecutor templatePushTransformExecutor;

  @Resource private ScriptPushTransformExecutor scriptPushTransformExecutor;

  @Resource private PushTransformContextBuilder contextBuilder;

  @Autowired(required = false)
  private List<PushTransformPresetTransformer> presetTransformers = new ArrayList<>();

  public String transformPayload(BaseUPRequest request, UPPushBO config, String originalPayload) {
    if (request == null || config == null || config.getTransform() == null) {
      return originalPayload;
    }

    UPPushBO.TransformConfig transformConfig = config.getTransform();
    if (!transformConfig.isSupport() || !transformConfig.isEnable()) {
      return originalPayload;
    }

    String appUniqueId = resolveAppUniqueId(request);
    if (StrUtil.isBlank(appUniqueId)) {
      return originalPayload;
    }

    try {
      IoTPushTransformRule rule = resolveRule(appUniqueId);
      if (rule == null) {
        return originalPayload;
      }
      Object transformed = executeRule(rule, request);
      if (transformed == null) {
        return fallback(originalPayload, transformConfig);
      }
      if (transformed instanceof String transformedString) {
        return transformedString;
      }
      return JSONUtil.toJsonStr(transformed);
    } catch (Exception e) {
      log.error(
          "[数据转换] 执行失败，应用={}, 设备={}, 错误={}",
          appUniqueId,
          request.getIotId(),
          e.getMessage(),
          e);
      return fallback(originalPayload, transformConfig);
    }
  }

  public PushTransformPreviewVO preview(PushTransformPreviewBO previewBO) {
    String sampleInput = previewBO == null ? null : previewBO.getSampleInput();
    BaseUPRequest request =
        contextBuilder.fromSample(previewBO == null ? null : previewBO.getAppUniqueId(), sampleInput);
    Object transformed = executeRule(buildPreviewRule(previewBO), request);
    return PushTransformPreviewVO.builder()
        .success(true)
        .inputJson(JSONUtil.toJsonStr(contextBuilder.source(request)))
        .outputJson(
            transformed instanceof String
                ? (String) transformed
                : JSONUtil.toJsonStr(transformed))
        .message("预览成功")
        .build();
  }

  public void validateForPublish(IoTPushTransformRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("规则不能为空");
    }
    String ruleType = StrUtil.blankToDefault(rule.getRuleType(), "").toUpperCase();
    switch (ruleType) {
      case "PRESET":
        PushTransformPresetTransformer presetTransformer = getPresetTransformer(rule.getPresetCode());
        if (presetTransformer == null || !presetTransformer.isImplemented()) {
          throw new IllegalArgumentException("未找到可用的内置格式：" + rule.getPresetCode());
        }
        return;
      case "TEMPLATE":
        templatePushTransformExecutor.validate(rule.getContent());
        return;
      case "SCRIPT":
        scriptPushTransformExecutor.validate(rule.getContent());
        return;
      default:
        throw new IllegalArgumentException("不支持的规则类型：" + rule.getRuleType());
    }
  }

  public List<PushTransformPresetVO> listPresets() {
    List<PushTransformPresetVO> presets = new ArrayList<>();
    for (PushTransformPresetTransformer transformer : presetTransformers) {
      presets.add(
          PushTransformPresetVO.builder()
              .code(transformer.getCode())
              .name(transformer.getName())
              .description(transformer.getDescription())
              .implemented(transformer.isImplemented())
              .build());
    }
    return presets;
  }

  public void refreshAppCache(String appUniqueId) {
    if (StrUtil.isNotBlank(appUniqueId)) {
      appRuleCache.invalidate(appUniqueId);
      log.info("[数据转换] 已刷新应用规则缓存: {}", appUniqueId);
    }
  }

  private String fallback(String originalPayload, UPPushBO.TransformConfig transformConfig) {
    if (transformConfig == null) {
      return originalPayload;
    }
    String fallbackMode = StrUtil.blankToDefault(transformConfig.getFallbackMode(), "ORIGINAL");
    if ("BLOCK".equalsIgnoreCase(fallbackMode)) {
      return null;
    }
    return originalPayload;
  }

  private IoTPushTransformRule resolveRule(String appUniqueId) {
    Optional<IoTPushTransformRule> cachedRule =
        appRuleCache.get(appUniqueId, this::loadPublishedRule);
    return cachedRule == null ? null : cachedRule.orElse(null);
  }

  private Optional<IoTPushTransformRule> loadPublishedRule(String appUniqueId) {
    List<IoTPushTransformRule> rules =
        ioTPushTransformRuleMapper.selectPublishedByAppUniqueId(appUniqueId);
    return rules == null || rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
  }

  private Object executeRule(IoTPushTransformRule rule, BaseUPRequest request) {
    Map<String, Object> source = contextBuilder.source(request);
    String ruleType = StrUtil.blankToDefault(rule.getRuleType(), "").toUpperCase();
    if ("PRESET".equals(ruleType)) {
      PushTransformPresetTransformer presetTransformer = getPresetTransformer(rule.getPresetCode());
      if (presetTransformer == null) {
        throw new IllegalArgumentException("未找到内置格式：" + rule.getPresetCode());
      }
      return presetTransformer.transform(source, parseOptions(rule.getOptions()));
    }
    if ("TEMPLATE".equals(ruleType)) {
      return templatePushTransformExecutor.execute(rule.getContent(), contextBuilder.templateBindings(source));
    }
    if ("SCRIPT".equals(ruleType)) {
      return scriptPushTransformExecutor.execute(
          rule.getId(), rule.getVersion(), rule.getContent(), contextBuilder.scriptBindings(request, source));
    }
    throw new IllegalArgumentException("不支持的规则类型：" + rule.getRuleType());
  }

  private PushTransformPresetTransformer getPresetTransformer(String presetCode) {
    if (StrUtil.isBlank(presetCode) || CollectionUtil.isEmpty(presetTransformers)) {
      return null;
    }
    for (PushTransformPresetTransformer transformer : presetTransformers) {
      if (presetCode.equalsIgnoreCase(transformer.getCode())) {
        return transformer;
      }
    }
    return null;
  }

  private JSONObject parseOptions(String options) {
    if (StrUtil.isBlank(options)) {
      return new JSONObject();
    }
    return JSONUtil.parseObj(options);
  }

  private IoTPushTransformRule buildPreviewRule(PushTransformPreviewBO previewBO) {
    if (previewBO == null) {
      throw new IllegalArgumentException("预览参数不能为空");
    }
    return IoTPushTransformRule.builder()
        .id(0L)
        .version(1)
        .ruleType(previewBO.getRuleType())
        .presetCode(previewBO.getPresetCode())
        .content(previewBO.getContent())
        .options(previewBO.getOptions())
        .build();
  }

  private String resolveAppUniqueId(BaseUPRequest request) {
    if (StrUtil.isNotBlank(request.getApplicationId())) {
      return request.getApplicationId();
    }
    if (request.getIoTDeviceDTO() != null
        && StrUtil.isNotBlank(request.getIoTDeviceDTO().getApplicationId())) {
      return request.getIoTDeviceDTO().getApplicationId();
    }
    return null;
  }

}
