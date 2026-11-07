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

package cn.universal.rule.scene.deviceUp;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceRuleLog;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.persistence.mapper.IoTDeviceRuleLogMapper;
import cn.universal.rule.enums.RunStatus;
import cn.universal.rule.model.ExeRunContext;
import cn.universal.rule.scene.deviceDown.SenceIoTDeviceDownService;
import cn.universal.rule.scene.service.SceneRuleCache;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

@Slf4j
public abstract class AbstractDeviceUp implements DeviceUp {

  /** 场景联动类型 */
  public byte ruleLogType = 1;

  @Resource private SceneRuleCache sceneRuleCache;
  @Resource private StringRedisTemplate stringRedisTemplate;
  @Resource private SenceIoTDeviceDownService senceIoTDeviceDownService;

  @Resource private IoTDeviceRuleLogMapper ioTDeviceRuleLogMapper;

  @Override
  public void consumer(UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    doTestTrigger(upRequest, ioTDeviceDTO);
  }

  /** 判断触发条件是否满足 */
  public void doTestTrigger(UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    // 查询启用的规则
    List<SceneRuleCache.Rule> sceneLinkageList =
        sceneRuleCache.find(
            ioTDeviceDTO.getProductKey(),
            ioTDeviceDTO.getDeviceId(),
            ioTDeviceDTO.getTenantId(),
            ioTDeviceDTO.getUserUnionId());
    // 是否存在该设备的场景联动
    if (CollectionUtils.isEmpty(sceneLinkageList)) {
      log.debug("场景联动结束，未能匹配到设备,设备id:{}", ioTDeviceDTO.getDeviceId());
      return;
    }
    List<IoTDeviceRuleLog> logRules = new ArrayList<>();
    sceneLinkageList.forEach(
        rule -> {
          SceneLinkage sceneLinkage = rule.scene();
          // 创建日志
          IoTDeviceRuleLog logRule =
              IoTDeviceRuleLog.builder()
                  .cId(sceneLinkage.getId() + "")
                  .conditions("系统自动")
                  .cName(sceneLinkage.getSceneName())
                  .createBy(sceneLinkage.getCreateBy())
                  .tenantId(sceneLinkage.getTenantId())
                  .cType(ruleLogType)
                  .createTime(new Date())
                  .build();
          try {
            if (!testDeviceTrigger(sceneLinkage, rule.triggers(), upRequest)) {
              return;
            }
            // 条件命中后才访问 Redis；SET NX EX 保证并发上报只能获得一次执行资格。
            Integer sleepCycle = sceneLinkage.getSleepCycle();
            if (sleepCycle != null && sleepCycle > 0) {
              String sleepKey =
                  String.format(
                      "scene-trigger-sleep:%s:%s",
                      sceneLinkage.getId(), ioTDeviceDTO.getDeviceId());
              if (!Boolean.TRUE.equals(
                  stringRedisTemplate
                      .opsForValue()
                      .setIfAbsent(sleepKey, "1", sleepCycle, TimeUnit.SECONDS))) {
                return;
              }
            }
            // 执行动作，返回结果
            List<ExeRunContext> runContexts =
                senceIoTDeviceDownService.doIoTDeviceFunction(upRequest, sceneLinkage);
            logRule.setCStatus(senceIoTDeviceDownService.matchSuccess(runContexts).code);
            logRule.setCDeviceMeta(buildDeviceMetaInfo(sceneLinkage, upRequest));
            logRule.setContent(JSONUtil.toJsonStr(runContexts));
            logRules.add(logRule);
            // 记录日志
          } catch (Exception e) {
            log.error(
                "执行场景联动触发条件判断错误，sceneId:{},deviceId:{}",
                sceneLinkage.getId(),
                ioTDeviceDTO.getDeviceId(),
                e);
            logRule.setCStatus(RunStatus.error.code);
            logRule.setContent("执行场景联动触发条件判断错误");
            logRules.add(logRule);
          }
        });
    if (CollectionUtil.isNotEmpty(logRules)) {
      ioTDeviceRuleLogMapper.insertList(logRules);
    }
  }

  /** 判断是否存在设备触发的条件 */
  public Boolean testDeviceTrigger(SceneLinkage sceneLinkage, UPRequest upRequest) {
    return testDeviceTrigger(
        sceneLinkage,
        JSONUtil.toList(sceneLinkage.getTriggerCondition(), TriggerBO.class),
        upRequest);
  }

  private boolean testDeviceTrigger(
      SceneLinkage sceneLinkage, List<TriggerBO> conditions, UPRequest upRequest) {
    if (conditions == null || conditions.isEmpty() || upRequest.getMessageType() == null)
      return false;
    List<TriggerBO> triggers =
        conditions.stream()
            .filter(
                o ->
                    "device".equals(o.getTrigger())
                        && Objects.equals(upRequest.getProductKey(), o.getProductKey())
                        && Objects.equals(upRequest.getDeviceId(), o.getDeviceId())
                        && upRequest.getMessageType().name().equalsIgnoreCase(o.getType()))
            .collect(Collectors.toList());
    if (triggers.isEmpty()) return false;
    boolean any = "one".equalsIgnoreCase(sceneLinkage.getTouch());
    // 不将其他设备、消息类型或定时条件静默丢弃后误判为“全部满足”。
    if (!any && triggers.size() != conditions.size()) return false;
    return testAlarm(triggers, any ? " || " : " && ", upRequest);
  }

  /** 具体判断事件或属性 */
  public boolean testAlarm(List<TriggerBO> triggers, String separator, UPRequest upRequest) {
    return false;
  }

  /**
   * 构建设备元数据信息，包含触发条件和实际数据值
   *
   * @param sceneLinkage 场景联动配置
   * @param upRequest 设备上行数据
   * @return 格式化的设备元数据JSON字符串
   */
  private String buildDeviceMetaInfo(SceneLinkage sceneLinkage, UPRequest upRequest) {
    try {
      JSONObject metaInfo = new JSONObject();
      // 基本信息
      metaInfo.set("sceneId", sceneLinkage.getId());
      metaInfo.set("sceneName", sceneLinkage.getSceneName());
      metaInfo.set("deviceId", upRequest.getDeviceId());
      metaInfo.set("productKey", upRequest.getProductKey());
      metaInfo.set("messageType", upRequest.getMessageType());
      metaInfo.set("timestamp", upRequest.getTime());

      // 触发条件配置
      JSONArray triggerConditions = JSONUtil.parseArray(sceneLinkage.getTriggerCondition());
      metaInfo.set("triggerConditions", triggerConditions);

      // 实际设备数据
      JSONObject actualData = new JSONObject();

      // 处理属性数据
      if (upRequest.getProperties() != null && !upRequest.getProperties().isEmpty()) {
        actualData.set("properties", upRequest.getProperties());
      }

      // 处理事件数据
      if (upRequest.getEvent() != null) {
        actualData.set("event", upRequest.getEvent());
        actualData.set("eventName", upRequest.getEventName());
      }

      // 处理其他数据
      if (upRequest.getData() != null && !upRequest.getData().isEmpty()) {
        actualData.set("data", upRequest.getData());
      }

      metaInfo.set("actualDeviceData", actualData);

      // 匹配的触发条件详情
      JSONArray matchedConditions = new JSONArray();
      if (triggerConditions != null) {
        for (Object conditionObj : triggerConditions) {
          JSONObject condition = (JSONObject) conditionObj;
          if ("device".equals(condition.getStr("trigger"))
              && Objects.equals(upRequest.getDeviceId(), condition.getStr("deviceId"))
              && Objects.equals(upRequest.getProductKey(), condition.getStr("productKey"))
              && upRequest.getMessageType().name().equalsIgnoreCase(condition.getStr("type"))) {

            JSONObject matchedCondition = new JSONObject();
            matchedCondition.set("deviceId", condition.getStr("deviceId"));
            matchedCondition.set("deviceName", condition.getStr("deviceName"));
            matchedCondition.set("trigger", condition.getStr("trigger"));
            matchedCondition.set("type", condition.getStr("type"));
            matchedCondition.set("filters", condition.get("filters"));

            // 添加实际匹配的数据值
            JSONObject actualValues = new JSONObject();
            if ("properties".equals(condition.getStr("type"))
                && upRequest.getProperties() != null) {
              actualValues.putAll(upRequest.getProperties());
            } else if ("event".equals(condition.getStr("type"))) {
              actualValues.set("event", upRequest.getEvent());
              actualValues.set("eventName", upRequest.getEventName());
            }
            matchedCondition.set("actualValues", actualValues);

            matchedConditions.add(matchedCondition);
          }
        }
      }
      metaInfo.set("matchedConditions", matchedConditions);
      return JSONUtil.toJsonStr(metaInfo);
    } catch (Exception e) {
      log.error("构建设备元数据信息失败", e);
      // 返回基本的设备信息
      JSONObject basicInfo = new JSONObject();
      basicInfo.set("deviceId", upRequest.getDeviceId());
      basicInfo.set("productKey", upRequest.getProductKey());
      basicInfo.set("messageType", upRequest.getMessageType());
      basicInfo.set("error", "构建设备元数据失败: " + e.getMessage());
      return JSONUtil.toJsonStr(basicInfo);
    }
  }
}
