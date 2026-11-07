package cn.universal.rule.scene.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.SceneLinkageBO;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.persistence.mapper.SceneLinkageMapper;
import cn.universal.quartz.domain.SysJob;
import cn.universal.quartz.service.ISysJobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SceneLinkageSaveTest {
  @Test
  void statusOnlyUpdatePreservesJsonAndOwnership() {
    var service = new SceneLinkageService(List.of());
    var mapper = mock(SceneLinkageMapper.class);
    var cache = mock(SceneRuleCache.class);
    var jobs = mock(ISysJobService.class);
    ReflectionTestUtils.setField(service, "sceneLinkageMapper", mapper);
    ReflectionTestUtils.setField(service, "sceneRuleCache", cache);
    ReflectionTestUtils.setField(service, "iSysJobService", jobs);
    var existing =
        SceneLinkage.builder()
            .id(1L)
            .status(0)
            .tenantId("t")
            .triggerCondition("[]")
            .execAction("[]")
            .build();
    when(mapper.selectSceneLinkageById(1L)).thenReturn(existing);
    when(mapper.updateSceneLinkage(any())).thenReturn(1);
    var patch = new SceneLinkageBO();
    patch.setId(1L);
    patch.setStatus(1);
    patch.setTenantId("wrong");
    patch.setCreateBy("wrong");
    assertEquals(1, service.updateSceneLinkage(patch));
    var saved = ArgumentCaptor.forClass(SceneLinkage.class);
    verify(mapper).updateSceneLinkage(saved.capture());
    assertNull(saved.getValue().getTriggerCondition());
    assertNull(saved.getValue().getExecAction());
    assertNull(saved.getValue().getTenantId());
    assertNull(saved.getValue().getCreateBy());
    verify(cache).invalidateAfterCommit();
  }

  @Test
  void bulkDeleteRemovesEveryScenesJobs() throws Exception {
    var service = new SceneLinkageService(List.of());
    var mapper = mock(SceneLinkageMapper.class);
    var jobs = mock(ISysJobService.class);
    ReflectionTestUtils.setField(service, "sceneLinkageMapper", mapper);
    ReflectionTestUtils.setField(service, "sceneRuleCache", mock(SceneRuleCache.class));
    ReflectionTestUtils.setField(service, "iSysJobService", jobs);
    when(mapper.deleteSceneLinkageByIds(any())).thenReturn(2);
    var a = new SysJob();
    a.setJobId(11L);
    var b = new SysJob();
    b.setJobId(22L);
    when(jobs.selectJobByName("sceneId:1")).thenReturn(List.of(a));
    when(jobs.selectJobByName("sceneId:2")).thenReturn(List.of(b));
    service.deleteSceneLinkageByIds(new Long[] {1L, 2L});
    verify(jobs).deleteJobByIds(new Long[] {11L});
    verify(jobs).deleteJobByIds(new Long[] {22L});
  }

  @Test
  void quartzDoesNotSkipSubsequentFiringsAndChecksEnabledState() {
    var service = new SceneLinkageService(List.of());
    var mapper = mock(SceneLinkageMapper.class);
    var down = mock(cn.universal.rule.scene.deviceDown.SenceIoTDeviceDownService.class);
    var logs = mock(cn.universal.persistence.mapper.IoTDeviceRuleLogMapper.class);
    ReflectionTestUtils.setField(service, "sceneLinkageMapper", mapper);
    ReflectionTestUtils.setField(service, "senceIoTDeviceDownService", down);
    ReflectionTestUtils.setField(service, "ioTDeviceRuleLogMapper", logs);
    var scene = SceneLinkage.builder().id(1L).status(0).build();
    when(mapper.selectSceneLinkageById(1L)).thenReturn(scene);
    when(down.doIoTDeviceFunction(any(), any())).thenReturn(List.of());
    when(down.matchSuccess(any())).thenReturn(cn.universal.rule.enums.RunStatus.success);
    service.quartzFunctionDown(1L);
    service.quartzFunctionDown(1L);
    verify(down, times(2)).doIoTDeviceFunction(any(), any());
    clearInvocations(down);
    scene.setStatus(1);
    service.quartzFunctionDown(1L);
    verifyNoInteractions(down);
  }

  @Test
  void schedulerFailureDoesNotReportSuccessfulSave() throws Exception {
    var service = new SceneLinkageService(List.of());
    var jobs = mock(ISysJobService.class);
    ReflectionTestUtils.setField(service, "iSysJobService", jobs);
    when(jobs.insertJob(any())).thenThrow(new org.quartz.SchedulerException("test failure"));
    var scene = new SceneLinkageBO();
    scene.setId(1L);
    scene.setStatus(1);
    scene.setTriggerCondition(
        JSONUtil.toList("[{\"trigger\":\"timer\",\"cron\":\"0 * * * * ?\"}]", TriggerBO.class));
    assertThrows(IllegalStateException.class, () -> service.addJob(scene, new SysJob()));
    var job = ArgumentCaptor.forClass(SysJob.class);
    verify(jobs).insertJob(job.capture());
    assertEquals("1", job.getValue().getStatus());
  }

  @Test
  void refusesAmbiguousAllAndInvalidFiltersButAllowsPartialStatusUpdate() {
    var patch = new SceneLinkageBO();
    patch.setStatus(1);
    assertDoesNotThrow(() -> SceneDefinitionValidator.validate(patch, false));
    patch.setTouch("all");
    patch.setTriggerCondition(
        JSONUtil.toList(
            """
            [{"trigger":"device","type":"event","deviceId":"d1","productKey":"p","modelId":"e"},
             {"trigger":"device","type":"event","deviceId":"d2","productKey":"p","modelId":"e"}]
            """,
            TriggerBO.class));
    assertThrows(
        IllegalArgumentException.class, () -> SceneDefinitionValidator.validate(patch, false));
    patch.setTouch("one");
    assertDoesNotThrow(() -> SceneDefinitionValidator.validate(patch, false));
    patch.setSleepCycle(-1L);
    assertThrows(
        IllegalArgumentException.class, () -> SceneDefinitionValidator.validate(patch, false));
  }

  @Test
  void eventFiltersAreValidatedBeforeRulesAreSaved() {
    var patch = new SceneLinkageBO();
    patch.setTouch("one");
    patch.setTriggerCondition(JSONUtil.toList("""
        [{"trigger":"device","type":"event","productKey":"p","deviceId":"d","modelId":"human",
          "filters":[{"key":"data.channelId","value":"0","operator":"exec"}]}]
        """, TriggerBO.class));
    assertThrows(IllegalArgumentException.class, () -> SceneDefinitionValidator.validate(patch, false));
    var filter = patch.getTriggerCondition().getFirst().getFilters().getFirst();
    filter.setOperator("ne");
    assertDoesNotThrow(() -> SceneDefinitionValidator.validate(patch, false));
    assertEquals("not", filter.getOperator());
    filter.setOperator("in"); filter.setValue("0,1");
    assertDoesNotThrow(() -> SceneDefinitionValidator.validate(patch, false));
    filter.setOperator("nin");
    assertDoesNotThrow(() -> SceneDefinitionValidator.validate(patch, false));
  }
}
