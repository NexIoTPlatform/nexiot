package cn.universal.admin.platform.service.transform;

import cn.universal.persistence.entity.IoTPushTransformRule;
import cn.universal.persistence.entity.bo.PushTransformPreviewBO;
import cn.universal.persistence.entity.bo.UPPushBO;
import cn.universal.persistence.entity.vo.PushTransformOverviewVO;
import cn.universal.persistence.entity.vo.PushTransformPreviewVO;

public interface IPushTransformRuleService {

  PushTransformOverviewVO getOverview(String appUniqueId);

  IoTPushTransformRule getRule(Long id);

  IoTPushTransformRule saveRule(String appUniqueId, IoTPushTransformRule rule, String operator);

  IoTPushTransformRule saveAndPublishRule(
      String appUniqueId, IoTPushTransformRule rule, String operator);

  IoTPushTransformRule publishRule(String appUniqueId, Long id, String operator);

  boolean deleteRule(String appUniqueId, Long id, String operator);

  UPPushBO.TransformConfig updateConfig(
      String appUniqueId, UPPushBO.TransformConfig transformConfig, String operator);

  PushTransformPreviewVO preview(PushTransformPreviewBO previewBO);
}
