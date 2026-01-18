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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import cn.hutool.core.bean.BeanUtil;
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
import cn.universal.persistence.mapper.SceneLinkageMapper;
import cn.universal.rule.enums.RunStatus;
import cn.universal.rule.express.ExpressTemplate;
import cn.universal.rule.model.ExeRunContext;
import cn.universal.rule.scene.deviceDown.SenceIoTDeviceDownService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDeviceUp implements DeviceUp {

  /** 场景联动类型 */
  public byte ruleLogType = 1;

  @Resource private SceneLinkageMapper sceneLinkageMapper;
  @Resource private StringRedisTemplate stringRedisTemplate;
  @Resource protected ExpressTemplate expressTemplate;
  @Resource private SenceIoTDeviceDownService senceIoTDeviceDownService;

  @Resource private IoTDeviceRuleLogMapper ioTDeviceRuleLogMapper;

  @Override
  public void consumer(UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    // 防御性编程：验证输入数据完整性
    if (upRequest == null || ioTDeviceDTO == null) {
      log.warn("[场景联动消费器] 输入参数为空，跳过处理");
      return;
    }
    
    if (upRequest.getProductKey() == null || upRequest.getDeviceId() == null) {
      log.warn("[场景联动消费器] 产品Key或设备ID为空，跳过处理");
      return;
    }
    
    log.debug("[场景联动消费器] 开始处理消息，产品Key: {}, 设备ID: {}, 消息类型: {}", 
        upRequest.getProductKey(), upRequest.getDeviceId(), upRequest.getMessageType());
    
    doTestTrigger(upRequest, ioTDeviceDTO);
  }

  /** 判断触发条件是否满足 */
  public void doTestTrigger(UPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    List<SceneLinkage> sceneLinkageList = null;
    
    try {
      // 查询启用的规则
      sceneLinkageList =
          sceneLinkageMapper.selectSceneLinkageListByProductKeyAndDeviceId(
              ioTDeviceDTO.getProductKey(), ioTDeviceDTO.getDeviceId());
      
      // 是否存在该设备的场景联动
      if (CollectionUtils.isEmpty(sceneLinkageList)) {
        log.debug("[场景联动] 未找到设备的场景联动配置，设备ID: {}, 产品Key: {}", 
            ioTDeviceDTO.getDeviceId(), ioTDeviceDTO.getProductKey());
        return;
      }
      
      log.info("[场景联动] 找到 {} 个场景联动配置，设备ID: {}", 
          sceneLinkageList.size(), ioTDeviceDTO.getDeviceId());
    } catch (Exception e) {
      log.error("[场景联动] 查询场景联动配置异常，设备ID: {}", ioTDeviceDTO.getDeviceId(), e);
      return;
    }
    
    List<IoTDeviceRuleLog> logRules = new ArrayList<>();
    sceneLinkageList.forEach(
        sceneLinkage -> {
          // 创建日志
          IoTDeviceRuleLog logRule =
              IoTDeviceRuleLog.builder()
                  .cId(sceneLinkage.getId() + "")
                  .conditions("系统自动")
                  .cName(sceneLinkage.getSceneName())
                  .createBy(sceneLinkage.getCreateBy())
                  .cType(ruleLogType)
                  .createTime(new Date())
                  .build();
          try {
            // 沉默周期判断
            String sleepKey =
                String.format(
                    "scene-trigger-sleep:%s:%s", sceneLinkage.getId(), ioTDeviceDTO.getDeviceId());
            if (StringUtils.isNotEmpty(stringRedisTemplate.opsForValue().get(sleepKey))) {
              log.info(
                  "[场景联动] 场景联动处于沉默周期内，场景ID: {}, 设备ID: {}",
                  sceneLinkage.getId(),
                  ioTDeviceDTO.getDeviceId());
              logRule.setCStatus(RunStatus.error.code);
              logRule.setContent("处于沉默周期中");
              return;
            }
            // 是否满足触发条件
            boolean isTouch = testDeviceTrigger(sceneLinkage, upRequest);
            // 进入沉默周期
            if (isTouch && !sceneLinkage.getSleepCycle().equals(0)) {
              stringRedisTemplate
                  .opsForValue()
                  .set(
                      sleepKey,
                      sceneLinkage.getSleepCycle().toString(),
                      sceneLinkage.getSleepCycle(),
                      TimeUnit.SECONDS);
            }
            if (!isTouch) {
              log.debug(
                  "[场景联动] 不满足触发条件，场景ID: {}, 设备ID: {}",
                  sceneLinkage.getId(),
                  ioTDeviceDTO.getDeviceId());
              return;
            }
            
            log.info(
                "[场景联动] 触发条件满足，开始执行动作，场景ID: {}, 设备ID: {}",
                sceneLinkage.getId(),
                ioTDeviceDTO.getDeviceId());
            
            // 执行动作，返回结果
            List<ExeRunContext> runContexts =
                senceIoTDeviceDownService.doIoTDeviceFunction(upRequest, sceneLinkage);
            logRule.setCStatus(senceIoTDeviceDownService.matchSuccess(runContexts).code);
            logRule.setCDeviceMeta(buildDeviceMetaInfo(sceneLinkage, upRequest));
            logRule.setContent(JSONUtil.toJsonStr(runContexts));
            logRules.add(logRule);
            // 记录日志
          } catch (Exception e) {
            e.printStackTrace();
            log.error(
                "[场景联动] 执行触发条件判断异常，场景ID: {}, 设备ID: {}, 异常信息: ",
                sceneLinkage.getId(),
                ioTDeviceDTO.getDeviceId(),
                e);
            logRule.setCStatus(RunStatus.error.code);
            logRule.setContent("执行场景联动触发条件判断错误: " + e.getMessage());
            logRules.add(logRule);
          }
        });
    if (CollectionUtil.isNotEmpty(logRules)) {
      try {
        ioTDeviceRuleLogMapper.insertList(logRules);
        log.info("[场景联动] 场景联动日志保存成功，共 {} 条日志", logRules.size());
      } catch (Exception e) {
        log.error("[场景联动] 保存场景联动日志异常", e);
      }
    }
  }

  /** 判断是否存在设备触发的条件 */
  public Boolean testDeviceTrigger(SceneLinkage sceneLinkage, UPRequest upRequest) {
    JSONArray jsonArray = JSONUtil.parseArray(sceneLinkage.getTriggerCondition());
    if (CollectionUtils.isEmpty(jsonArray)) {
      log.debug("触发条件为空deviceId={}", upRequest.getDeviceId());
      return false;
    }

    List<TriggerBO> triggers =
        jsonArray.stream()
            .map(o -> BeanUtil.toBean(o, TriggerBO.class))
            .filter(
                o ->
                    "device".equals(o.getTrigger())
                        && upRequest.getMessageType().name().equalsIgnoreCase(o.getType()))
            .collect(Collectors.toList());

    if (CollectionUtils.isEmpty(triggers)) {
      log.warn("触发行为为空");
      return false;
    }
    String separator = "one".equalsIgnoreCase(sceneLinkage.getTouch()) ? " || " : " && ";
    log.info(
        "触发条件执行,id={},touch={},separator={}",
        sceneLinkage.getId(),
        sceneLinkage.getTouch(),
        separator);
    return testAlarm(triggers, separator, upRequest);
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
              && upRequest.getDeviceId().equals(condition.getStr("deviceId"))) {

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
