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

package cn.universal.rule.scene.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceRuleLog;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.SceneLinkageBO;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.persistence.entity.vo.SceneLinkageLogVO;
import cn.universal.persistence.mapper.IoTDeviceRuleLogMapper;
import cn.universal.persistence.mapper.SceneLinkageMapper;
import cn.universal.quartz.domain.SysJob;
import cn.universal.quartz.service.ISysJobService;
import cn.universal.rule.model.ExeRunContext;
import cn.universal.rule.scene.deviceDown.SenceIoTDeviceDownService;
import cn.universal.rule.scene.deviceUp.DeviceUp;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

/**
 * 场景联动Service业务层处理 @Author gitee.com/NexIoT
 *
 * @since 2023-03-01
 */
@Service("sceneLinkageService")
@Slf4j
public class SceneLinkageService {

  @Resource private SceneLinkageMapper sceneLinkageMapper;
  @Resource private SceneRuleCache sceneRuleCache;
  @Resource private SenceIoTDeviceDownService senceIoTDeviceDownService;

  private final Map<String, DeviceUp> deviceUpMap;

  @Resource private ISysJobService iSysJobService;

  @Resource private IoTDeviceRuleLogMapper ioTDeviceRuleLogMapper;

  public SceneLinkageService(List<DeviceUp> deviceUps) {
    //    Map<String, DeviceUp> beans = SpringUtil.getBeansOfType(DeviceUp.class);
    deviceUpMap =
        deviceUps.stream().collect(Collectors.toMap(DeviceUp::messageType, Function.identity()));
  }

  // 设备消息上行 进行触发器判断
  @Async("sceneTaskExecutor")
  public void rule(UPRequest upRequest, IoTDeviceDTO instance) {
    DeviceUp deviceUp = deviceUpMap.get(upRequest.getMessageType().name());
    if (deviceUp == null) {
      return;
    }
    deviceUp.consumer(upRequest, instance);
  }

  /**
   * 查询场景联动
   *
   * @param id 场景联动ID
   * @return 场景联动
   */
  public SceneLinkage selectSceneLinkageById(Long id) {
    return sceneLinkageMapper.selectSceneLinkageById(id);
  }

  public Boolean checkSelf(Long id, String unionId) {
    return checkSelf(id, unionId, null);
  }

  public Boolean checkSelf(Long id, String unionId, String tenantId) {
    return sceneLinkageMapper.checkSelf(id, unionId, tenantId) > 0;
  }

  /**
   * 查询场景联动列表
   *
   * @param sceneLinkage 场景联动
   * @return 场景联动
   */
  public List<SceneLinkage> selectSceneLinkageList(SceneLinkage sceneLinkage) {
    return sceneLinkageMapper.selectSceneLinkageList(sceneLinkage);
  }

  /**
   * 新增场景联动
   *
   * @param sceneLinkage 场景联动
   * @return 结果
   */
  @Transactional
  public int insertSceneLinkage(SceneLinkageBO sceneLinkage) {
    if (sceneLinkage.getStatus() == null) sceneLinkage.setStatus(0);
    if (sceneLinkage.getSleepCycle() == null) sceneLinkage.setSleepCycle(0L);
    SceneDefinitionValidator.validate(sceneLinkage, true);
    SceneLinkage sceneLinkage1 = BeanUtil.toBean(sceneLinkage, SceneLinkage.class);
    sceneLinkage1.setTriggerCondition(JSONUtil.toJsonStr(sceneLinkage.getTriggerCondition()));
    sceneLinkage1.setExecAction(JSONUtil.toJsonStr(sceneLinkage.getExecAction()));
    int rows = sceneLinkageMapper.insertSceneLinkage(sceneLinkage1);
    //    int result = 0;
    if (rows != 0) {
      SysJob sysJob = new SysJob();
      sysJob.setCreateBy(sceneLinkage.getCreateBy());
      sysJob.setUpdateBy(sceneLinkage.getCreateBy());
      Date date = new Date();
      sysJob.setUpdateTime(date);
      sysJob.setCreateTime(date);
      sceneLinkage.setId(sceneLinkage1.getId());
      // 定时任务结果 -1为没有定时触发  0新增失败 1成功
      addJob(sceneLinkage, sysJob);
      sceneRuleCache.invalidateAfterCommit();
    }
    return rows;
  }

  /**
   * 修改场景联动
   *
   * @param sceneLinkage 场景联动
   * @return 结果
   */
  @Transactional
  public int updateSceneLinkage(SceneLinkageBO sceneLinkage) {
    if (sceneLinkage.getId() == null) throw new IllegalArgumentException("场景 ID 不能为空");
    SceneLinkage existing = sceneLinkageMapper.selectSceneLinkageById(sceneLinkage.getId());
    if (existing == null) throw new IllegalArgumentException("场景不存在");
    // 状态切换不能把缺省 JSON 序列化为字符串 null，也不能篡改原创建人或租户。
    sceneLinkage.setCreateBy(null);
    sceneLinkage.setCreateTime(null);
    sceneLinkage.setTenantId(null);
    if (sceneLinkage.getTriggerCondition() != null || sceneLinkage.getTouch() != null) {
      if (sceneLinkage.getTouch() == null) sceneLinkage.setTouch(existing.getTouch());
      if (sceneLinkage.getTriggerCondition() == null)
        sceneLinkage.setTriggerCondition(
            JSONUtil.toList(existing.getTriggerCondition(), TriggerBO.class));
    }
    SceneDefinitionValidator.validate(sceneLinkage, false);
    SceneLinkage sceneLinkage1 = BeanUtil.toBean(sceneLinkage, SceneLinkage.class);
    if (sceneLinkage.getTriggerCondition() != null)
      sceneLinkage1.setTriggerCondition(JSONUtil.toJsonStr(sceneLinkage.getTriggerCondition()));
    if (sceneLinkage.getExecAction() != null)
      sceneLinkage1.setExecAction(JSONUtil.toJsonStr(sceneLinkage.getExecAction()));
    int rows = sceneLinkageMapper.updateSceneLinkage(sceneLinkage1);
    if (rows > 0) {
      if (sceneLinkage.getStatus() == null) sceneLinkage.setStatus(existing.getStatus());
      sceneRuleCache.invalidateAfterCommit();
      // 场景联动修改
      if (sceneLinkage.getTriggerCondition() != null) {
        delJob(sceneLinkage.getId());
        SysJob sysJob = new SysJob();
        sysJob.setCreateBy(existing.getCreateBy());
        sysJob.setUpdateBy(sceneLinkage.getUpdateBy());
        Date date = new Date();
        sysJob.setUpdateTime(date);
        sysJob.setCreateTime(date);
        addJob(sceneLinkage, sysJob);
      }
      // 只更改了场景启用状态
      else {
        // 恢复场景
        if (sceneLinkage.getStatus() == 0) {
          resumeJob(sceneLinkage.getId());
        }
        // 暂停场景
        else {
          pauseJob(sceneLinkage.getId());
        }
      }
    }
    return rows;
  }

  /**
   * 删除场景联动对象
   *
   * @param ids 需要删除的数据ID
   * @return 结果
   */
  @Transactional
  public int deleteSceneLinkageByIds(Long[] ids) {
    int rows = sceneLinkageMapper.deleteSceneLinkageByIds(ids);
    if (rows > 0) {
      for (Long id : ids) delJob(id);
      sceneRuleCache.invalidateAfterCommit();
    }
    return rows;
  }

  /**
   * 删除场景联动信息
   *
   * @param id 场景联动ID
   * @return 结果
   */
  @Transactional
  public int deleteSceneLinkageById(Long id) {
    return deleteSceneLinkageByIds(new Long[] {id});
  }

  /** 执行动作 */
  public List<ExeRunContext> functionDown(Long id) {
    SceneLinkage sceneLinkage = selectSceneLinkageById(id);
    List<ExeRunContext> runContexts =
        senceIoTDeviceDownService.doIoTDeviceFunction(new UPRequest(), sceneLinkage);
    // 创建日志
    IoTDeviceRuleLog logRule =
        IoTDeviceRuleLog.builder()
            .cId(sceneLinkage.getId() + "")
            .cName(sceneLinkage.getSceneName())
            .cType((byte) 1)
            .createBy(sceneLinkage.getCreateBy())
            .tenantId(sceneLinkage.getTenantId())
            .conditions("手动操作")
            .createTime(new Date())
            .content(JSONUtil.toJsonStr(runContexts))
            .cStatus(senceIoTDeviceDownService.matchSuccess(runContexts).code)
            .build();
    ioTDeviceRuleLogMapper.insertSelective(logRule);
    return runContexts;
  }

  /** 执行动作 */
  public List<ExeRunContext> quartzFunctionDown(Long id) {
    // Quartz 已使用 JDBC 集群存储和 DisallowConcurrentExecution 按 JobKey 协调。
    // 不再叠加固定五分钟 Redis 锁，否则每分钟的合法调度会被跳过。
    SceneLinkage sceneLinkage = selectSceneLinkageById(id);
    if (sceneLinkage == null || !Integer.valueOf(0).equals(sceneLinkage.getStatus()))
      return List.of();
    List<ExeRunContext> runContexts =
        senceIoTDeviceDownService.doIoTDeviceFunction(new UPRequest(), sceneLinkage);
    // 创建日志
    IoTDeviceRuleLog logRule =
        IoTDeviceRuleLog.builder()
            .cId(sceneLinkage.getId() + "")
            .cName(sceneLinkage.getSceneName())
            .cType((byte) 1)
            .conditions("定时自动")
            .createBy(sceneLinkage.getCreateBy())
            .tenantId(sceneLinkage.getTenantId())
            .createTime(new Date())
            .content(JSONUtil.toJsonStr(runContexts))
            .cStatus(senceIoTDeviceDownService.matchSuccess(runContexts).code)
            .build();
    ioTDeviceRuleLogMapper.insertSelective(logRule);
    return runContexts;
  }

  // 新增调度任务
  public int addJob(SceneLinkageBO sceneLinkage, SysJob sysJob) {
    // 不存在定时任务
    int rows = -1;
    List<TriggerBO> timeTrigger =
        sceneLinkage.getTriggerCondition().stream()
            .filter(
                triggerBo -> {
                  return "timer".equals(triggerBo.getTrigger());
                })
            .collect(Collectors.toList());

    // 存在定时触发的条件
    if (!CollectionUtils.isEmpty(timeTrigger)) {
      Date date = new Date();
      sysJob.setJobGroup("场景联动功能下发");
      sysJob.setInvokeTarget(
          "sceneLinkageService.quartzFunctionDown(" + sceneLinkage.getId() + "L)");
      // 状态（0正常 1暂停）
      sysJob.setStatus(Integer.valueOf(1).equals(sceneLinkage.getStatus()) ? "1" : "0");
      // 是否并发执行（0允许 1禁止）
      sysJob.setConcurrent("1");
      // 计划执行错误策略（1立即执行 2执行一次 3放弃执行）
      sysJob.setMisfirePolicy("3");
      timeTrigger.forEach(
          triggerBo -> {
            sysJob.setJobId(null);
            sysJob.setJobName("sceneId:" + sceneLinkage.getId());
            sysJob.setCronExpression(triggerBo.getCron());
            try {
              if (iSysJobService.insertJob(sysJob) <= 0) throw new IllegalStateException("定时任务未写入");
            } catch (Exception e) {
              log.error(
                  "场景联动调度任务新增失败，场景={},名称={},cron={}",
                  sceneLinkage.getId(),
                  sceneLinkage.getSceneName(),
                  triggerBo.getCron(),
                  e);
              throw new IllegalStateException("场景定时任务创建失败", e);
            }
          });
    }

    return rows;
  }

  /** 根据场景联动id 删除调度任务 */
  public void delJob(Long id) {
    // 根据调度任务名查找对应任务
    List<SysJob> jobs = iSysJobService.selectJobByName("sceneId:" + id);
    if (!CollectionUtils.isEmpty(jobs)) {
      try {
        iSysJobService.deleteJobByIds(jobs.stream().map(SysJob::getJobId).toArray(Long[]::new));
      } catch (Exception e) {
        log.error("场景联动调度任务删除失败,场景id:{}", id, e);
        throw new IllegalStateException("场景定时任务删除失败", e);
      }
    }
  }

  /** 根据场景联动id 暂停调度任务 */
  public void pauseJob(Long id) {
    List<SysJob> jobs = iSysJobService.selectJobByName("sceneId:" + id);
    if (!CollectionUtils.isEmpty(jobs)) {
      jobs.forEach(
          sysJob -> {
            try {
              iSysJobService.pauseJob(sysJob);
            } catch (SchedulerException e) {
              log.error("场景联动调度任务暂停失败,场景id:{}", id, e);
              throw new IllegalStateException("场景定时任务暂停失败", e);
            }
          });
    }
  }

  /** 根据场景联动id 恢复调度任务 */
  public void resumeJob(Long id) {
    List<SysJob> jobs = iSysJobService.selectJobByName("sceneId:" + id);
    if (!CollectionUtils.isEmpty(jobs)) {
      jobs.forEach(
          sysJob -> {
            try {
              iSysJobService.resumeJob(sysJob);
            } catch (SchedulerException e) {
              log.error("场景联动调度任务恢复失败,场景id:{}", id, e);
              throw new IllegalStateException("场景定时任务恢复失败", e);
            }
          });
    }
  }

  /** 分页查询场景联动执行日志 */
  public List<IoTDeviceRuleLog> getSceneLinkageLogPage(String sceneId, int pageNum, int pageSize) {
    // 分页
    PageHelper.startPage(pageNum, pageSize);
    Example example = new Example(IoTDeviceRuleLog.class);
    example.createCriteria().andEqualTo("cId", sceneId).andEqualTo("cType", (byte) 1);
    example.orderBy("createTime").desc();
    List<IoTDeviceRuleLog> logs = ioTDeviceRuleLogMapper.selectByExample(example);
    return logs;
  }

  /** 使用MyBatis关联查询场景联动执行日志 */
  public List<SceneLinkageLogVO> getRuleLogPageWithSceneInfo(
      IoTDeviceRuleLog ioTDeviceRuleLog, String triggerType) {
    String createTimeStart = null;
    String createTimeEnd = null;

    if (ioTDeviceRuleLog.getCreateTime() != null) {
      createTimeStart = DateUtil.format(ioTDeviceRuleLog.getCreateTime(), "yyyy-MM-dd HH:mm:ss");
    }
    if (ioTDeviceRuleLog.getUpdateTime() != null) {
      createTimeEnd = DateUtil.format(ioTDeviceRuleLog.getUpdateTime(), "yyyy-MM-dd HH:mm:ss");
    }

    return ioTDeviceRuleLogMapper.selectSceneLinkageLogs(
        ioTDeviceRuleLog.getCName(),
        ioTDeviceRuleLog.getCStatus(),
        ioTDeviceRuleLog.getCId(),
        ioTDeviceRuleLog.getCType(),
        triggerType,
        ioTDeviceRuleLog.getCreateBy(),
        createTimeStart,
        createTimeEnd);
  }
}
