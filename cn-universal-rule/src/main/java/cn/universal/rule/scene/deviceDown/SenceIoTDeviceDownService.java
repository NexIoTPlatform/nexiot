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

package cn.universal.rule.scene.deviceDown;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.core.message.UPRequest;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.metadata.FunctionParameterNormalizer;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.manager.notice.model.NoticeSendRequest;
import cn.universal.manager.notice.service.NoticeService;
import cn.universal.manager.notice.service.channel.NoticeSendResult;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceTags;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.persistence.entity.bo.TriggerBO.ExecData;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.persistence.query.IoTDeviceQuery;
import cn.universal.rule.enums.RunStatus;
import cn.universal.rule.model.ExeRunContext;
import cn.universal.rule.model.ExeRunContext.ExeRunContextBuilder;
import cn.universal.rule.model.bo.DownResult;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SenceIoTDeviceDownService {

  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private NoticeService noticeService;
  @Resource private cn.universal.manager.notice.mapper.NoticeTemplateMapper noticeTemplates;
  @Resource private IoTUserMapper ioTUserMapper;

  public RunStatus matchSuccess(List<ExeRunContext> exeRunContexts) {
    if (CollectionUtil.isEmpty(exeRunContexts)) {
      return RunStatus.error;
    }
    boolean allSuccess = exeRunContexts.stream().allMatch(ExeRunContext::isSuccess);
    boolean allError = exeRunContexts.stream().noneMatch(ExeRunContext::isSuccess);
    if (allSuccess) {
      return RunStatus.success;
    } else if (allError) {
      return RunStatus.error;
    } else {
      return RunStatus.exist_success;
    }
  }

  public List<ExeRunContext> doIoTDeviceFunction(UPRequest upRequest, SceneLinkage sceneLinkage) {
    // 解析execAction
    List<TriggerBO> execs =
        JSONUtil.parseArray(sceneLinkage.getExecAction()).stream()
            .map(o -> BeanUtil.toBean(o, TriggerBO.class))
            .collect(Collectors.toList());
    String unionId = sceneLinkage.getCreateBy();
    List<ExeRunContext> runContexts = new ArrayList<>();
    for (TriggerBO exec : execs) {
      ExeRunContextBuilder exeRunContextBuilder =
          ExeRunContext.builder().trigger(exec.getTrigger());
      if (TriggerBO.ExecTriggerType.device.name().equals(exec.getTrigger())) {
        exeRunContextBuilder.target(exec.getDeviceId());
        exeRunContextBuilder.param(exec.getExecData());
        try {
          String downRequest = getDownRequest(exec, unionId);
          if (StrUtil.isEmpty(downRequest)) {
            exeRunContextBuilder.result("设备不存在");
            runContexts.add(exeRunContextBuilder.build());
            continue;
          }
          DownResult downResult = functionDown(downRequest);
          log.debug("device场景联动结果={}", JSONUtil.toJsonStr(downResult));
          if (!downResult.getSuccess()) {
            log.warn("场景联动指令下发{},设备编号:{},场景编号:{}", "异常", exec.getDeviceId(), sceneLinkage.getId());
            exeRunContextBuilder.success(false);
          } else {
            exeRunContextBuilder.success(true);
          }
          exeRunContextBuilder.result(
              downResult.getDownResult() == null ? "" : downResult.getDownResult());
        } catch (IllegalArgumentException e) {
          exeRunContextBuilder.success(false);
          exeRunContextBuilder.result(e.getMessage());
          log.warn("场景联动参数错误，设备编号:{}，原因:{}", exec.getDeviceId(), e.getMessage());
        } catch (Exception e) {
          exeRunContextBuilder.success(false);
          exeRunContextBuilder.result("返回为空，失败");
          log.error("场景联动指令下发失败，设备编号:{}", exec.getDeviceId());
        }
        runContexts.add(exeRunContextBuilder.build());
      } else if (TriggerBO.ExecTriggerType.notice.name().equals(exec.getTrigger())) {
        exeRunContextBuilder.target(exec.getNoticeTemplateId());
        exeRunContextBuilder.targetName(exec.getNoticeTemplateName());
        // 场景可以复用本租户通知模板，禁止通过手写规则引用其他租户的渠道凭据。
        cn.universal.manager.notice.model.NoticeTemplate selectedTemplate = null;
        try {
          if (exec.getNoticeTemplateId() != null) selectedTemplate = noticeTemplates.selectByPrimaryKey(Long.valueOf(exec.getNoticeTemplateId()));
        } catch (NumberFormatException ignored) { }
        if (selectedTemplate == null || !canAccessSceneNotice(selectedTemplate.getCreator(), unionId)) {
          runContexts.add(exeRunContextBuilder.success(false).result("通知模板不存在或无权限使用").build());
          continue;
        }
        // 组装通知参数
        NoticeSendRequest req = new NoticeSendRequest();
        if (exec.getNoticeTemplateId() != null) {
          req.setTemplateId(Long.valueOf(exec.getNoticeTemplateId()));
        }
        // 组装params
        HashMap<String, Object> params = new HashMap<>();
        params.put("sceneName", sceneLinkage.getSceneName());
        params.put("sceneId", sceneLinkage.getId());
        params.put("execTime", DateUtil.now());
        params.put("trigger", sceneLinkage.getTriggerCondition());
        params.put("action", exec);
        params.putAll(JSONUtil.parseObj(upRequest));
        req.setParams(params);
        req.setReceivers(exec.getReceivers());
        req.setCreator(unionId);
        try {
          NoticeSendResult rs = noticeService.sendR(req);
          log.info("notice推送结果={}", JSONUtil.toJsonStr(rs));
          if (rs != null && rs.isSuccess()) {
            exeRunContextBuilder.success(true);
            exeRunContextBuilder.result(
                Map.of(
                    "receivers",
                    rs.getReceivers() == null ? "" : rs.getReceivers(),
                    "content",
                    rs.getContent() == null ? "" : rs.getContent()));
          } else {
            exeRunContextBuilder.success(false);
            exeRunContextBuilder.result(rs != null ? rs.getErrorMessage() : "");
          }
          log.info(
              "场景联动通知结果={}, 场景id:{}, 模板id:{}", rs != null && rs.isSuccess(), sceneLinkage.getId(), exec.getNoticeTemplateId());
        } catch (Exception e) {
          log.error(
              "场景联动触发通知失败, 场景id:{}, 模板id:{}", sceneLinkage.getId(), exec.getNoticeTemplateId(), e);
          exeRunContextBuilder.success(false);
        }
        runContexts.add(exeRunContextBuilder.build());
      }
    }
    return runContexts;
  }

  // 拼装功能下行指令
  private String getDownRequest(TriggerBO o, String unionId) {
    JSONObject downRequest = new JSONObject();
    IoTDevice ioTDevice =
        ioTDeviceMapper.getOneByDeviceId(
            IoTDeviceQuery.builder().deviceId(o.getDeviceId()).productKey(o.getProductKey()).build());
    if (ioTDevice == null) {
      return null;
    }
    String productKey = ioTDevice.getProductKey();
    downRequest.set("appUnionId", unionId);
    downRequest.set("productKey", productKey);
    downRequest.set("deviceId", o.getDeviceId());
    downRequest.set("cmd", "DEV_FUNCTION");

    JSONObject function = new JSONObject();
    function.set("messageType", "FUNCTIONS");
    function.set("function", o.getModelId());

    Map<String, Object> data = new LinkedHashMap<>();
    if (o.getExecData() != null) {
      for (ExecData execData : o.getExecData()) {
        data.put(execData.getId(), execData.getParams());
      }
    }
    IoTProduct product = ioTProductMapper.getProductByProductKey(productKey);
    function.set("data", FunctionParameterNormalizer.normalize(
        StrUtil.blankToDefault(ioTDevice.getDeriveMetadata(), product == null ? null : product.getMetadata()),
        o.getModelId(), data));
    downRequest.set("function", function);

    return JSONUtil.toJsonStr(downRequest);
  }

  // 指令下发
  private DownResult functionDown(String downRequest) {
    JSONObject jsonObject = JSONUtil.parseObj(downRequest);
    String deviceId = jsonObject.getStr("deviceId");
    String productKey = jsonObject.getStr("productKey");
    String appUnionId = jsonObject.getStr("appUnionId");
    IoTDevice ioTDeviceBo = ioTDeviceMapper.selectIoTDevice(productKey, deviceId);
    if (!canAccessSceneDevice(ioTDeviceBo, appUnionId)) {
      log.error("数据不属于你，用户{}，设备ID：{}", appUnionId, deviceId);
      return DownResult.builder().success(false).downResult("无权限操作设备。").build();
    }
    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(productKey);
    IoTDeviceTags devTag = new IoTDeviceTags();
    devTag.setKey("geoPoint");
    devTag.setIotId(ioTDeviceBo.getIotId());
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.fromJson(JSONUtil.parseObj(downRequest));
    command.setTenantId(ioTDeviceBo.getTenantId());
    R result = IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
    log.info(
        "场景联动,deviceId={} function={} 返回={}", deviceId, downRequest, JSONUtil.toJsonStr(result));
    if (result != null && result.isSuccess()) {
      return DownResult.builder().success(true).downResult(result.getData()).build();
    } else {
      log.warn("场景联动指令下发失败，错误信息：{}", result.getMsg());
      return DownResult.builder().success(false).downResult(result.getMsg()).build();
    }
  }

  private boolean canAccessSceneNotice(String creator, String actor) {
    if (StrUtil.isBlank(creator) || StrUtil.isBlank(actor)) return false;
    if (creator.equals(actor)) return true;
    String tenant = resolveTenantId(actor);
    return StrUtil.isNotBlank(tenant) && tenant.equals(resolveTenantId(creator));
  }

  private boolean canAccessSceneDevice(IoTDevice ioTDevice, String scopeUnionId) {
    if (ioTDevice == null || StrUtil.isBlank(scopeUnionId)) {
      return false;
    }
    String scopeTenantId = resolveTenantId(scopeUnionId);
    String resourceTenantId =
        StrUtil.blankToDefault(ioTDevice.getTenantId(), resolveTenantId(ioTDevice.getCreatorId()));
    if (StrUtil.isNotBlank(scopeTenantId) && StrUtil.isNotBlank(resourceTenantId)) {
      return StrUtil.equals(scopeTenantId, resourceTenantId);
    }
    return StrUtil.equals(ioTDevice.getCreatorId(), scopeUnionId);
  }

  private String resolveTenantId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    IoTUser user = ioTUserMapper.selectOne(IoTUser.builder().unionId(unionId).build());
    if (user == null) {
      return null;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }
}
