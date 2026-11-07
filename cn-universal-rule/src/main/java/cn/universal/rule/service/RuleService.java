/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.rule.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.RuleModel;
import cn.universal.persistence.entity.RuleModelInstance;
import cn.universal.persistence.entity.bo.RuleModelBO;
import cn.universal.persistence.entity.vo.RuleModelVO;
import cn.universal.persistence.mapper.RuleModelInstanceMapper;
import cn.universal.persistence.mapper.RuleModelMapper;
import cn.universal.rule.engine.RuleEngine;
import cn.universal.rule.enums.RuleDataLevel;
import cn.universal.rule.model.RuleConfig;
import cn.universal.rule.model.bo.RuleBo;
import cn.universal.rule.model.bo.RuleTargetTestBO;
import cn.universal.rule.transmit.RuleTransmitTemplate;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 规则引擎服务 @Author gitee.com/NexIoT
 *
 * @since 2023/1/13 14:34
 */
@Service
@Slf4j
public class RuleService {

  @Resource private RuleModelMapper ruleModelMapper;

  @Resource private RuleModelInstanceMapper ruleModelInstanceMapper;

  @Resource private RuleTransmitTemplate ruleTransmitTemplate;

  @Resource private RuleEngine ruleEngine;

  @Resource private RuleSubscriptionIndex subscriptions;

  public boolean hasRules(IoTDeviceDTO device) {
    return !subscriptions.match(device).isEmpty();
  }

  @Async("legacyRuleTaskExecutor")
  public void rule(UPRequest param, IoTDeviceDTO instance) {
    doExecuteRule(JSONUtil.parseObj(param), instance);
  }

  public void doExecuteRule(JSONObject param, IoTDeviceDTO instance) {
    var ruleModels = subscriptions.match(instance);
    if (CollectionUtils.isEmpty(ruleModels)) {
      return;
    }
    ruleModels.forEach(
        ruleModel -> {
          try {

            JSONObject result = ruleEngine.executeRule(param, ruleModel.sql(), instance.getAppId());

            if (Objects.nonNull(result)) {
              ruleModel
                  .targets()
                  .forEach(
                      target -> {
                        try {
                          log.info(
                              "匹配到规则引擎，调用转发ruleId:{},deviceId={}",
                              ruleModel.id(),
                              instance.getDeviceId());
                          ruleTransmitTemplate.transmit(result, target);
                        } catch (Exception e) {
                          log.error(
                              "执行规则错误,调用转发目标失败,deviceId:{},iotId:{},ruleId:{},data:{},target:{}",
                              instance.getDeviceId(),
                              instance.getIotId(),
                              ruleModel.id(),
                              result,
                              target);
                          log.error("执行规则错误,调用转发目标失败", e);
                        }
                      });
            }
          } catch (Exception e) {
            log.error(
                "执行规则错误,deviceId:{},iotId:{},ruleId:{}, ruleName:{}",
                instance.getDeviceId(),
                instance.getIotId(),
                ruleModel.id(),
                ruleModel.name(),
                e);
          }
        });
  }

  public JSONObject testExecuteRule(RuleBo sqlBo) {
    JSONObject jsonObject;
    try {
      jsonObject = JSONUtil.parseObj(sqlBo.getPayload());
    } catch (Exception e) {
      throw new IoTException("payload必须为json格式");
    }
    return ruleEngine.executeRule(
        jsonObject, sqlBo.getConfig().getSql(), sqlBo.getConfig().getAppId());
  }

  /**
   * 查询规则模型
   *
   * @param ruleModelBo
   * @return
   */
  public List<RuleModelVO> queryRuleListByBo(RuleModelBO ruleModelBo) {
    return ruleModelMapper.selectRuleListByBo(ruleModelBo);
  }

  public RuleModel queryByIdAndCreator(Long id, String creator, String tenantId) {
    RuleModel ruleModel = ruleModelMapper.selectByPrimaryKey(id);
    assertRuleAccess(ruleModel, creator, tenantId);
    return ruleModel;
  }

  /**
   * 查询规则Vo
   *
   * @param id
   * @param creator
   * @return
   */
  public RuleModelVO queryVoByIdAndCreator(Long id, String creator, String tenantId) {
    RuleModel ruleModel = queryByIdAndCreator(id, creator, tenantId);
    List<RuleModelInstance> instances =
        ruleModelInstanceMapper.select(RuleModelInstance.builder().modelId(id).build());

    return new RuleModelVO(ruleModel, instances);
  }

  /**
   * 更新规则状态
   *
   * @param ruleModelBo
   */
  public void changeStatus(RuleModelBO ruleModelBo) {
    RuleModel ruleModel =
        queryByIdAndCreator(
            ruleModelBo.getId(), ruleModelBo.getCreatorId(), ruleModelBo.getTenantId());
    ruleModel.setStatus(ruleModelBo.getStatus());
    ruleModel.setUpdateTime(new Date());
    ruleModelMapper.updateByPrimaryKeySelective(ruleModel);
    subscriptions.changedAfterCommit();
  }

  /**
   * 新增规则
   *
   * @param ruleBo
   */
  public Long addRule(RuleBo ruleBo) {
    RuleModel ruleModel = new RuleModel();
    ruleModel.setStatus("stop");
    ruleModel.setRuleName(ruleBo.getRuleName());
    ruleModel.setCreatorId(ruleBo.getCreatorId());
    ruleModel.setTenantId(resolveTenantId(ruleBo.getCreatorId(), ruleBo.getTenantId()));
    ruleModel.setConfig(JSONUtil.toJsonStr(ruleBo.getConfig()));
    ruleModel.setCreateTime(new Date());

    ruleModel.setDataLevel(ruleBo.getDataLevel());
    ruleModel.setProductKey(ruleBo.getProductKey());
    ruleModelMapper.insert(ruleModel);

    saveRuleInstance(ruleBo, ruleModel);
    subscriptions.changedAfterCommit();

    return ruleModel.getId();
  }

  public void deletedRule(Long id, String creator, String tenantId) {
    queryByIdAndCreator(id, creator, tenantId);
    ruleModelInstanceMapper.delete(RuleModelInstance.builder().modelId(id).build());
    ruleModelMapper.deleteByPrimaryKey(id);
    subscriptions.changedAfterCommit();
  }

  /**
   * 保存规则实例
   *
   * @param ruleBo
   * @param ruleModel
   */
  private void saveRuleInstance(RuleBo ruleBo, RuleModel ruleModel) {
    if (RuleDataLevel.product.name().equals(ruleBo.getDataLevel())) {
      if (StringUtils.isEmpty(ruleBo.getProductKey())) {
        throw new IoTException("ProductKey不能为空");
      }
      RuleModelInstance instance = new RuleModelInstance();
      instance.setModelId(ruleModel.getId());
      instance.setRelationType(ruleModel.getDataLevel());
      instance.setRelationId(ruleModel.getProductKey());
      ruleModelInstanceMapper.insertSelective(instance);
    } else if (RuleDataLevel.group.name().equals(ruleBo.getDataLevel())) {
      if (StringUtils.isEmpty(ruleBo.getGroupId())) {
        throw new IoTException("关联设备不能为空");
      }
      RuleModelInstance instance = new RuleModelInstance();
      instance.setModelId(ruleModel.getId());
      instance.setRelationType(ruleModel.getDataLevel());
      instance.setRelationId(ruleBo.getGroupId());
      ruleModelInstanceMapper.insertSelective(instance);
    } else {
      if (CollectionUtils.isEmpty(ruleBo.getRelationIds())) {
        throw new IoTException("关联设备不能为空");
      }
      ruleBo
          .getRelationIds()
          .forEach(
              id -> {
                RuleModelInstance instance = new RuleModelInstance();
                instance.setModelId(ruleModel.getId());
                instance.setRelationType(ruleModel.getDataLevel());
                instance.setRelationId(id);
                ruleModelInstanceMapper.insertSelective(instance);
              });
    }
  }

  /**
   * 更新规则
   *
   * @param ruleBo
   */
  @Transactional(rollbackFor = Exception.class)
  public void updateRule(RuleBo ruleBo) {
    RuleModel ruleModel =
        queryByIdAndCreator(ruleBo.getId(), ruleBo.getCreatorId(), ruleBo.getTenantId());
    ruleModel.setRuleName(ruleBo.getRuleName());
    ruleModel.setProductKey(ruleBo.getProductKey());
    ruleModel.setDataLevel(ruleBo.getDataLevel());
    ruleModel.setCreatorId(ruleBo.getCreatorId());
    ruleModel.setTenantId(
        resolveTenantId(
            ruleBo.getCreatorId(),
            StringUtils.hasText(ruleModel.getTenantId())
                ? ruleModel.getTenantId()
                : ruleBo.getTenantId()));
    ruleModel.setConfig(JSONUtil.toJsonStr(ruleBo.getConfig()));
    ruleModel.setUpdateTime(new Date());
    ruleModel.setDescription(ruleBo.getDescription());
    ruleModelMapper.updateByPrimaryKey(ruleModel);

    ruleModelInstanceMapper.delete(RuleModelInstance.builder().modelId(ruleBo.getId()).build());

    saveRuleInstance(ruleBo, ruleModel);
    subscriptions.changedAfterCommit();
  }

  /**
   * 测试转发调用目标
   *
   * @param testBo
   */
  public String testRuleTarget(RuleTargetTestBO testBo) {
    JSONObject jsonObject;
    try {
      jsonObject = JSONUtil.parseObj(testBo.getParam());
    } catch (Exception e) {
      throw new IoTException("请求参数必须为json格式");
    }
    return ruleTransmitTemplate.testTransmit(jsonObject, testBo.ruleTarget);
  }

  /**
   * 更新规则转发目标
   *
   * @param ruleBo
   */
  public void updateRuleTargets(RuleBo ruleBo) {
    RuleModel ruleModel =
        queryByIdAndCreator(ruleBo.getId(), ruleBo.getCreatorId(), ruleBo.getTenantId());

    RuleConfig ruleConfig = JSONUtil.toBean(ruleModel.getConfig(), RuleConfig.class);
    ruleConfig.setTargets(ruleBo.getConfig().getTargets());
    ruleModel.setConfig(JSONUtil.toJsonStr(ruleConfig));
    ruleModel.setUpdateTime(new Date());

    ruleModelMapper.updateByPrimaryKeySelective(ruleModel);
    subscriptions.changedAfterCommit();
  }

  private void assertRuleAccess(RuleModel ruleModel, String creatorId, String tenantId) {
    if (Objects.isNull(ruleModel)) {
      throw new IoTException("未能匹配到规则");
    }
    boolean creatorMatched =
        StringUtils.hasText(creatorId) && creatorId.equals(ruleModel.getCreatorId());
    if (StringUtils.hasText(tenantId)) {
      String resourceTenantId =
          StringUtils.hasText(ruleModel.getTenantId())
              ? ruleModel.getTenantId()
              : ruleModel.getCreatorId();
      if (tenantId.equals(resourceTenantId) || creatorMatched) {
        return;
      }
      throw new IoTException("未能匹配到规则");
    }
    if (StringUtils.hasText(creatorId) && !creatorMatched) {
      throw new IoTException("未能匹配到规则");
    }
  }

  private String resolveTenantId(String creatorId, String tenantId) {
    if (StringUtils.hasText(tenantId)) {
      return tenantId;
    }
    return creatorId;
  }
}
